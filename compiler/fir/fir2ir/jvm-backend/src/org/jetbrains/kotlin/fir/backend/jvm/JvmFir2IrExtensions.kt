/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend.jvm

import org.jetbrains.kotlin.backend.jvm.CachedFieldsForObjectInstances
import org.jetbrains.kotlin.backend.jvm.JvmGeneratorExtensions
import org.jetbrains.kotlin.backend.jvm.JvmIrDeserializer
import org.jetbrains.kotlin.backend.jvm.JvmSymbols
import org.jetbrains.kotlin.backend.jvm.overrides.IrJavaIncompatibilityRulesOverridabilityCondition
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.config.JvmSerializeIrMode
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.backend.Fir2IrComponents
import org.jetbrains.kotlin.fir.backend.Fir2IrConversionScope
import org.jetbrains.kotlin.fir.backend.Fir2IrExtensions
import org.jetbrains.kotlin.fir.backend.utils.InjectedValue
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.isDeserializedPropertyFromAnnotation
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.java.hasJvmFieldAnnotation
import org.jetbrains.kotlin.fir.references.FirReference
import org.jetbrains.kotlin.fir.scopes.jvm.FirJvmDelegatedMembersFilter.Companion.PLATFORM_DEPENDENT_ANNOTATION_CLASS_ID
import org.jetbrains.kotlin.fir.symbols.impl.FirValueParameterSymbol
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrExternalPackageFragmentImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl
import org.jetbrains.kotlin.ir.expressions.IrAnnotation
import org.jetbrains.kotlin.ir.expressions.impl.IrAnnotationImpl
import org.jetbrains.kotlin.ir.expressions.impl.fromSymbolOwner
import org.jetbrains.kotlin.ir.overrides.IrExternalOverridabilityCondition
import org.jetbrains.kotlin.ir.symbols.impl.DescriptorlessExternalPackageFragmentSymbol
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.load.java.JvmAnnotationNames
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.JvmStandardClassIds
import org.jetbrains.kotlin.utils.addToStdlib.runIf

class JvmFir2IrExtensions(
    configuration: CompilerConfiguration,
    private val irDeserializer: JvmIrDeserializer,
) : Fir2IrExtensions, JvmGeneratorExtensions {
    private var irBuiltIns: IrBuiltIns? = null
    private var symbolTable: SymbolTable? = null

    override val parametersAreAssignable: Boolean get() = true
    override val externalOverridabilityConditions: List<IrExternalOverridabilityCondition>
        get() = listOf(IrJavaIncompatibilityRulesOverridabilityCondition())

    override val cachedFields: CachedFieldsForObjectInstances =
        CachedFieldsForObjectInstances(IrFactoryImpl, configuration.languageVersionSettings)

    private val kotlinIrInternalPackage =
        IrExternalPackageFragmentImpl(DescriptorlessExternalPackageFragmentSymbol(), IrBuiltIns.KOTLIN_INTERNAL_IR_FQN)

    private val kotlinJvmInternalPackage =
        IrExternalPackageFragmentImpl(DescriptorlessExternalPackageFragmentSymbol(), JvmAnnotationNames.KOTLIN_JVM_INTERNAL)

    private val specialAnnotationConstructors = mutableListOf<IrConstructor>()

    private val rawTypeAnnotationClassConstructor: IrConstructor =
        createSpecialAnnotationClass(JvmSymbols.RAW_TYPE_ANNOTATION_FQ_NAME, kotlinIrInternalPackage).constructors.single()

    override fun generateRawTypeAnnotation(): IrAnnotation =
        rawTypeAnnotationClassConstructor.let {
            IrAnnotationImpl.fromSymbolOwner(
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

    override fun findInjectedInlineLambdaArgument(parameter: FirValueParameterSymbol): FirExpression? = null

    override val irNeedsDeserialization: Boolean = false

    override fun deserializeToplevelClass(irClass: IrClass, components: Fir2IrComponents): Boolean {
        val builtIns = irBuiltIns ?: error("BuiltIns are not initialized")
        val symbolTable = symbolTable ?: error("SymbolTable is not initialized")
        return irDeserializer.deserializeTopLevelClass(
            irClass, builtIns, symbolTable, components.irProviders, this
        )
    }

    override fun hasBackingField(property: FirProperty, session: FirSession): Boolean =
        property.origin is FirDeclarationOrigin.Java ||
                // Metadata for properties says that the backing field doesn't exist,
                // but the field has to be generated in IR anyway as this is the only way
                // to propagate default values
                property.isDeserializedPropertyFromAnnotation == true ||
                Fir2IrExtensions.Default.hasBackingField(property, session)

    override fun specialBackingFieldVisibility(firProperty: FirProperty, session: FirSession): Visibility? {
        return runIf(firProperty.hasJvmFieldAnnotation(session)) {
            firProperty.status.visibility
        }
    }

    override fun initializeIrBuiltInsAndSymbolTable(irBuiltIns: IrBuiltIns, symbolTable: SymbolTable) {
        require(this.irBuiltIns == null) { "BuiltIns are already initialized" }
        this.irBuiltIns = irBuiltIns
        require(this.symbolTable == null) { "SymboTable is already initialized" }
        this.symbolTable = symbolTable
    }

    // See FirJvmDelegatedMembersFilter for reference
    override fun shouldGenerateDelegatedMember(delegateMemberFromBaseType: IrOverridableDeclaration<*>): Boolean {
        val original = delegateMemberFromBaseType.resolveFakeOverride() ?: return true

        fun IrOverridableDeclaration<*>.isNonAbstractJavaMethod(): Boolean {
            return origin == IrDeclarationOrigin.IR_EXTERNAL_JAVA_DECLARATION_STUB && modality != Modality.ABSTRACT
        }

        fun IrOverridableDeclaration<*>.hasJvmDefaultAnnotation(): Boolean {
            return annotations.hasAnnotation(JvmStandardClassIds.JVM_DEFAULT_CLASS_ID)
        }

        fun IrOverridableDeclaration<*>.isBuiltInMemberMappedToJavaDefault(): Boolean {
            return modality != Modality.ABSTRACT &&
                    annotations.hasAnnotation(PLATFORM_DEPENDENT_ANNOTATION_CLASS_ID)
        }

        val shouldNotGenerate = original.isNonAbstractJavaMethod()
                || original.hasJvmDefaultAnnotation()
                || original.isBuiltInMemberMappedToJavaDefault()
        // TODO(KT-69150): Investigate need of this check
        //        || original.origin == FirDeclarationOrigin.Synthetic.FakeHiddenInPreparationForNewJdk

        return !shouldNotGenerate
    }
}
