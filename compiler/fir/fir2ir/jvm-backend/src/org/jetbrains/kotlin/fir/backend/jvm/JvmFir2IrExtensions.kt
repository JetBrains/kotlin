/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend.jvm

import org.jetbrains.kotlin.backend.common.serialization.signature.PublicIdSignatureComputer
import org.jetbrains.kotlin.backend.jvm.*
import org.jetbrains.kotlin.backend.jvm.overrides.IrJavaIncompatibilityRulesOverridabilityCondition
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.config.JvmSerializeIrMode
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.backend.Fir2IrComponents
import org.jetbrains.kotlin.fir.backend.Fir2IrConversionScope
import org.jetbrains.kotlin.fir.backend.Fir2IrExtensions
import org.jetbrains.kotlin.fir.backend.InjectedValue
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.references.FirReference
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrExternalPackageFragmentImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl
import org.jetbrains.kotlin.ir.overrides.IrExternalOverridabilityCondition
import org.jetbrains.kotlin.ir.symbols.impl.DescriptorlessExternalPackageFragmentSymbol
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.load.java.JvmAnnotationNames
import org.jetbrains.kotlin.load.kotlin.FacadeClassSource
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.jvm.JvmClassName

class JvmFir2IrExtensions(
    configuration: CompilerConfiguration,
    private val irDeserializer: JvmIrDeserializer,
    private val mangler: KotlinMangler.IrMangler,
) : Fir2IrExtensions, JvmGeneratorExtensions {
    override val parametersAreAssignable: Boolean get() = true
    override val externalOverridabilityConditions: List<IrExternalOverridabilityCondition>
        get() = listOf(IrJavaIncompatibilityRulesOverridabilityCondition())

    override val classNameOverride: MutableMap<IrClass, JvmClassName> = mutableMapOf()
    override val cachedFields = CachedFieldsForObjectInstances(IrFactoryImpl, configuration.languageVersionSettings)

    private val kotlinIrInternalPackage =
        IrExternalPackageFragmentImpl(DescriptorlessExternalPackageFragmentSymbol(), IrBuiltIns.KOTLIN_INTERNAL_IR_FQN)

    private val kotlinJvmInternalPackage =
        IrExternalPackageFragmentImpl(DescriptorlessExternalPackageFragmentSymbol(), JvmAnnotationNames.KOTLIN_JVM_INTERNAL)

    private val specialAnnotationConstructors = mutableListOf<IrConstructor>()

    private val rawTypeAnnotationClass =
        createSpecialAnnotationClass(JvmSymbols.RAW_TYPE_ANNOTATION_FQ_NAME, kotlinIrInternalPackage)

    override val rawTypeAnnotationConstructor: IrConstructor =
        rawTypeAnnotationClass.constructors.single()

    init {
        createSpecialAnnotationClass(JvmAnnotationNames.ENHANCED_NULLABILITY_ANNOTATION, kotlinJvmInternalPackage)
        createSpecialAnnotationClass(JvmSymbols.FLEXIBLE_NULLABILITY_ANNOTATION_FQ_NAME, kotlinIrInternalPackage)
        createSpecialAnnotationClass(JvmSymbols.FLEXIBLE_MUTABILITY_ANNOTATION_FQ_NAME, kotlinIrInternalPackage)
    }

    private fun createSpecialAnnotationClass(fqn: FqName, parent: IrPackageFragment) =
        IrFactoryImpl.createSpecialAnnotationClass(fqn, parent).apply {
            specialAnnotationConstructors.add(constructors.single())
        }

    override fun registerDeclarations(symbolTable: SymbolTable) {
        val signatureComputer = PublicIdSignatureComputer(mangler)
        specialAnnotationConstructors.forEach { constructor ->
            symbolTable.declareConstructorWithSignature(signatureComputer.composePublicIdSignature(constructor, false), constructor.symbol)
        }
    }

    override fun findInjectedValue(calleeReference: FirReference, conversionScope: Fir2IrConversionScope): InjectedValue? {
        return null
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

    override fun deserializeToplevelClass(irClass: IrClass, components: Fir2IrComponents): Boolean =
        irDeserializer.deserializeTopLevelClass(
            irClass, components.irBuiltIns, components.symbolTable, components.irProviders, this
        )

    override fun hasBackingField(property: FirProperty, session: FirSession): Boolean =
        // NOTE maybe there might be platform-specific logic, similar to JvmGeneratorExtensionsImpl.isPropertyWithPlatformField()
        Fir2IrExtensions.Default.hasBackingField(property, session)
}
