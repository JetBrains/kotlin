/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend.jvm

import org.jetbrains.kotlin.backend.common.ir.createParameterDeclarations
import org.jetbrains.kotlin.backend.common.ir.createSpecialAnnotationClass
import org.jetbrains.kotlin.backend.common.serialization.signature.PublicIdSignatureComputer
import org.jetbrains.kotlin.backend.jvm.*
import org.jetbrains.kotlin.backend.jvm.serialization.deserializeFromByteArray
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.config.JvmSerializeIrMode
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.fir.backend.Fir2IrComponents
import org.jetbrains.kotlin.fir.backend.Fir2IrExtensions
import org.jetbrains.kotlin.fir.backend.FirIrProvider
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.backend.jvm.serialization.JvmIrMangler
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrExternalPackageFragmentImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl
import org.jetbrains.kotlin.ir.symbols.impl.DescriptorlessExternalPackageFragmentSymbol
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.load.kotlin.FacadeClassSource
import org.jetbrains.kotlin.load.kotlin.JvmPackagePartSource
import org.jetbrains.kotlin.load.kotlin.KotlinJvmBinarySourceElement
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.jvm.JvmClassName

class JvmFir2IrExtensions(configuration: CompilerConfiguration) : Fir2IrExtensions, JvmGeneratorExtensions {

    override val classNameOverride: MutableMap<IrClass, JvmClassName> = mutableMapOf()
    override val cachedFields = CachedFieldsForObjectInstances(IrFactoryImpl, configuration.languageVersionSettings)

    private val kotlinIrInternalPackage =
        IrExternalPackageFragmentImpl(DescriptorlessExternalPackageFragmentSymbol(), IrBuiltIns.KOTLIN_INTERNAL_IR_FQN)

    private val specialAnnotationConstructors = mutableListOf<IrConstructor>()

    private val rawTypeAnnotationClass =
        createSpecialAnnotationClass(JvmSymbols.RAW_TYPE_ANNOTATION_FQ_NAME, kotlinIrInternalPackage)

    override val rawTypeAnnotationConstructor: IrConstructor =
        rawTypeAnnotationClass.constructors.single()

    private fun createSpecialAnnotationClass(fqn: FqName, parent: IrPackageFragment) =
        IrFactoryImpl.createSpecialAnnotationClass(fqn, parent).apply {
            specialAnnotationConstructors.add(constructors.single())
        }

    override fun registerDeclarations(symbolTable: SymbolTable) {
        val signatureComputer = PublicIdSignatureComputer(JvmIrMangler)
        specialAnnotationConstructors.forEach { constructor ->
            symbolTable.declareConstructorWithSignature(signatureComputer.composePublicIdSignature(constructor, false), constructor.symbol)
        }
    }

    override val irNeedsDeserialization: Boolean =
        configuration.get(JVMConfigurationKeys.SERIALIZE_IR, JvmSerializeIrMode.NONE) != JvmSerializeIrMode.NONE

    override fun generateOrGetFacadeClass(declaration: IrMemberWithContainerSource, components: Fir2IrComponents): IrClass? {
        val deserializedSource = declaration.containerSource ?: return null
        if (deserializedSource !is FacadeClassSource) return null
        val facadeName = deserializedSource.facadeClassName ?: deserializedSource.className
        return JvmFileFacadeClass(
            if (deserializedSource.facadeClassName != null) IrDeclarationOrigin.JVM_MULTIFILE_CLASS else IrDeclarationOrigin.FILE_CLASS,
            facadeName.fqNameForTopLevelClassMaybeWithDollars.shortName(),
            deserializedSource,
            deserializeIr = { irClass -> deserializeToplevelClass(irClass, components) }
        ).also {
            it.createParameterDeclarations()
            classNameOverride[it] = facadeName
        }
    }

    override fun deserializeToplevelClass(irClass: IrClass, components: Fir2IrComponents): Boolean {
        with(components) {
            val serializedIr = when (val source = irClass.source) {
                is KotlinJvmBinarySourceElement -> source.binaryClass.classHeader.serializedIr
                is JvmPackagePartSource -> source.knownJvmBinaryClass?.classHeader?.serializedIr
                else -> null
            } ?: return false
            deserializeFromByteArray(
                serializedIr,
                irBuiltIns, symbolTable, listOf(FirIrProvider(this)),
                irClass,
                JvmIrTypeSystemContext(irBuiltIns), allowErrorNodes = false
            )
            irClass.handleJvmStaticInSingletonObjects(irBuiltIns, cachedFields)
            return true
        }
    }
}
