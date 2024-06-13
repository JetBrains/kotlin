/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend.jvm

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
import org.jetbrains.kotlin.fir.backend.utils.InjectedValue
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.references.FirReference
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrExternalPackageFragmentImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.impl.IrConstructorCallImpl
import org.jetbrains.kotlin.ir.overrides.IrExternalOverridabilityCondition
import org.jetbrains.kotlin.ir.symbols.impl.DescriptorlessExternalPackageFragmentSymbol
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.load.java.JvmAnnotationNames
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.resolve.jvm.JvmClassName

class JvmFir2IrExtensions(
    configuration: CompilerConfiguration,
    private val irDeserializer: JvmIrDeserializer,
) : Fir2IrExtensions, JvmGeneratorExtensions {
    private var irBuiltIns: IrBuiltIns? = null
    private var symbolTable: SymbolTable? = null

    override val parametersAreAssignable: Boolean get() = true
    override val externalOverridabilityConditions: List<IrExternalOverridabilityCondition>
        get() = listOf(IrJavaIncompatibilityRulesOverridabilityCondition())

    override val classNameOverride: MutableMap<IrClass, JvmClassName> = mutableMapOf()
    override val cachedFields: CachedFieldsForObjectInstances =
        CachedFieldsForObjectInstances(IrFactoryImpl, configuration.languageVersionSettings)

    private val kotlinIrInternalPackage =
        IrExternalPackageFragmentImpl(DescriptorlessExternalPackageFragmentSymbol(), IrBuiltIns.KOTLIN_INTERNAL_IR_FQN)

    private val kotlinJvmInternalPackage =
        IrExternalPackageFragmentImpl(DescriptorlessExternalPackageFragmentSymbol(), JvmAnnotationNames.KOTLIN_JVM_INTERNAL)

    private val specialAnnotationConstructors = mutableListOf<IrConstructor>()

    private val rawTypeAnnotationClassConstructor: IrConstructor =
        createSpecialAnnotationClass(JvmSymbols.RAW_TYPE_ANNOTATION_FQ_NAME, kotlinIrInternalPackage).constructors.single()

    override fun generateRawTypeAnnotationCall(): IrConstructorCall =
        rawTypeAnnotationClassConstructor.let {
            IrConstructorCallImpl.fromSymbolOwner(
                UNDEFINED_OFFSET,
                UNDEFINED_OFFSET,
                it.constructedClassType,
                it.symbol
            )
        }

    init {
        createSpecialAnnotationClass(JvmAnnotationNames.ENHANCED_NULLABILITY_ANNOTATION, kotlinJvmInternalPackage)
        createSpecialAnnotationClass(JvmSymbols.FLEXIBLE_NULLABILITY_ANNOTATION_FQ_NAME, kotlinIrInternalPackage)
        createSpecialAnnotationClass(JvmSymbols.FLEXIBLE_MUTABILITY_ANNOTATION_FQ_NAME, kotlinIrInternalPackage)
    }

    private fun createSpecialAnnotationClass(fqn: FqName, parent: IrPackageFragment) =
        IrFactoryImpl.createSpecialAnnotationClass(fqn, parent).apply {
            specialAnnotationConstructors.add(constructors.single())
        }

    override fun findInjectedValue(calleeReference: FirReference, conversionScope: Fir2IrConversionScope): InjectedValue? {
        return null
    }

    override val irNeedsDeserialization: Boolean =
        configuration.get(JVMConfigurationKeys.SERIALIZE_IR, JvmSerializeIrMode.NONE) != JvmSerializeIrMode.NONE

    override fun deserializeToplevelClass(irClass: IrClass, components: Fir2IrComponents): Boolean {
        val builtIns = irBuiltIns ?: error("BuiltIns are not initialized")
        val symbolTable = symbolTable ?: error("SymbolTable is not initialized")
        return irDeserializer.deserializeTopLevelClass(
            irClass, builtIns, symbolTable, components.irProviders, this
        )
    }

    override fun hasBackingField(property: FirProperty, session: FirSession): Boolean =
        property.origin is FirDeclarationOrigin.Java || Fir2IrExtensions.Default.hasBackingField(property, session)

    override fun isTrueStatic(declaration: FirCallableDeclaration, session: FirSession): Boolean =
        declaration.hasAnnotation(StandardClassIds.Annotations.jvmStatic, session) ||
                (declaration as? FirPropertyAccessor)?.propertySymbol?.fir?.hasAnnotation(StandardClassIds.Annotations.jvmStatic, session) == true

    override fun initializeIrBuiltInsAndSymbolTable(irBuiltIns: IrBuiltIns, symbolTable: SymbolTable) {
        require(this.irBuiltIns == null) { "BuiltIns are already initialized" }
        this.irBuiltIns = irBuiltIns
        require(this.symbolTable == null) { "SymboTable is already initialized" }
        this.symbolTable = symbolTable
    }
}
