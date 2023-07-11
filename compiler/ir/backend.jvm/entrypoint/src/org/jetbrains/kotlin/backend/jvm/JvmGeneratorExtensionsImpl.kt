/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm

import org.jetbrains.kotlin.backend.common.serialization.signature.PublicIdSignatureComputer
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.FilteredAnnotations
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.backend.jvm.serialization.JvmIrMangler
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrExternalPackageFragmentImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl
import org.jetbrains.kotlin.ir.expressions.IrDelegatingConstructorCall
import org.jetbrains.kotlin.ir.expressions.impl.IrDelegatingConstructorCallImpl
import org.jetbrains.kotlin.ir.symbols.impl.DescriptorlessExternalPackageFragmentSymbol
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.load.java.JvmAnnotationNames
import org.jetbrains.kotlin.load.java.descriptors.JavaCallableMemberDescriptor
import org.jetbrains.kotlin.load.java.descriptors.JavaClassDescriptor
import org.jetbrains.kotlin.load.java.descriptors.getParentJavaStaticClassScope
import org.jetbrains.kotlin.load.java.sam.JavaSingleAbstractMethodUtils
import org.jetbrains.kotlin.load.java.typeEnhancement.hasEnhancedNullability
import org.jetbrains.kotlin.load.kotlin.FacadeClassSource
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtPureClassOrObject
import org.jetbrains.kotlin.psi.psiUtil.pureEndOffset
import org.jetbrains.kotlin.psi.psiUtil.pureStartOffset
import org.jetbrains.kotlin.psi2ir.generators.GeneratorContext
import org.jetbrains.kotlin.psi2ir.generators.GeneratorExtensions
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.annotations.hasJvmStaticAnnotation
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.resolve.descriptorUtil.resolveTopLevelClass
import org.jetbrains.kotlin.resolve.jvm.JAVA_LANG_RECORD_FQ_NAME
import org.jetbrains.kotlin.resolve.jvm.JvmClassName
import org.jetbrains.kotlin.resolve.jvm.annotations.hasJvmFieldAnnotation
import org.jetbrains.kotlin.resolve.jvm.annotations.isJvmRecord
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DescriptorWithContainerSource
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource
import org.jetbrains.kotlin.synthetic.SyntheticJavaPropertyDescriptor
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.typeUtil.replaceAnnotations

open class JvmGeneratorExtensionsImpl(
    private val configuration: CompilerConfiguration,
    private val generateFacades: Boolean = true,
) : GeneratorExtensions(), JvmGeneratorExtensions {
    override val classNameOverride: MutableMap<IrClass, JvmClassName> = mutableMapOf()

    override val irDeserializationEnabled: Boolean = configuration.get(JVMConfigurationKeys.SERIALIZE_IR) != JvmSerializeIrMode.NONE

    override val cachedFields = CachedFieldsForObjectInstances(IrFactoryImpl, configuration.languageVersionSettings)

    override val samConversion: SamConversion = JvmSamConversion()

    inner class JvmSamConversion : SamConversion() {
        override fun isPlatformSamType(type: KotlinType): Boolean =
            JavaSingleAbstractMethodUtils.isSamType(type)

        override fun isCarefulApproximationOfContravariantProjection(): Boolean =
            configuration.get(JVMConfigurationKeys.SAM_CONVERSIONS) != JvmClosureGenerationScheme.CLASS
    }

    override fun getContainerSource(descriptor: DeclarationDescriptor): DeserializedContainerSource? {
        return (descriptor as? DescriptorWithContainerSource)?.containerSource
    }

    override fun computeFieldVisibility(descriptor: PropertyDescriptor): DescriptorVisibility? =
        if (descriptor.hasJvmFieldAnnotation() || descriptor is JavaCallableMemberDescriptor)
            descriptor.visibility
        else
            null

    override fun computeExternalDeclarationOrigin(descriptor: DeclarationDescriptor): IrDeclarationOrigin? =
        if (descriptor is JavaCallableMemberDescriptor || descriptor is JavaClassDescriptor)
            IrDeclarationOrigin.IR_EXTERNAL_JAVA_DECLARATION_STUB
        else
            IrDeclarationOrigin.IR_EXTERNAL_DECLARATION_STUB

    override fun generateFacadeClass(
        irFactory: IrFactory,
        deserializedSource: DeserializedContainerSource,
        stubGenerator: DeclarationStubGenerator
    ): IrClass? {
        if (!generateFacades || deserializedSource !is FacadeClassSource) return null
        val facadeName = deserializedSource.facadeClassName ?: deserializedSource.className
        return JvmFileFacadeClass(
            if (deserializedSource.facadeClassName != null) IrDeclarationOrigin.JVM_MULTIFILE_CLASS else IrDeclarationOrigin.FILE_CLASS,
            facadeName.fqNameForTopLevelClassMaybeWithDollars.shortName(),
            deserializedSource,
            deserializeIr = { facade -> deserializeClass(facade, stubGenerator, facade.parent) }
        ).also {
            it.createParameterDeclarations()
            classNameOverride[it] = facadeName
        }
    }

    override fun deserializeClass(
        irClass: IrClass,
        stubGenerator: DeclarationStubGenerator,
        parent: IrDeclarationParent,
    ): Boolean = JvmIrDeserializerImpl().deserializeTopLevelClass(
        irClass, stubGenerator.irBuiltIns, stubGenerator.symbolTable, listOf(stubGenerator), this
    )

    override fun isPropertyWithPlatformField(descriptor: PropertyDescriptor): Boolean =
        descriptor.hasJvmFieldAnnotation()

    override fun isStaticFunction(descriptor: FunctionDescriptor): Boolean =
        DescriptorUtils.isNonCompanionObject(descriptor.containingDeclaration) &&
                (descriptor.hasJvmStaticAnnotation() ||
                        descriptor is PropertyAccessorDescriptor && descriptor.correspondingProperty.hasJvmStaticAnnotation())

    override val enhancedNullability: EnhancedNullability
        get() = JvmEnhancedNullability

    open class JvmEnhancedNullability : EnhancedNullability() {
        override fun hasEnhancedNullability(kotlinType: KotlinType): Boolean =
            kotlinType.hasEnhancedNullability()

        override fun stripEnhancedNullability(kotlinType: KotlinType): KotlinType =
            if (kotlinType.hasEnhancedNullability())
                kotlinType.replaceAnnotations(
                    FilteredAnnotations(kotlinType.annotations, true) {
                        it != JvmAnnotationNames.ENHANCED_NULLABILITY_ANNOTATION
                    }
                )
            else
                kotlinType

        companion object Instance : JvmEnhancedNullability()
    }

    override fun getParentClassStaticScope(descriptor: ClassDescriptor): MemberScope? =
        descriptor.getParentJavaStaticClassScope()

    private val kotlinIrInternalPackage =
        IrExternalPackageFragmentImpl(DescriptorlessExternalPackageFragmentSymbol(), IrBuiltIns.KOTLIN_INTERNAL_IR_FQN)

    private val kotlinJvmInternalPackage =
        IrExternalPackageFragmentImpl(DescriptorlessExternalPackageFragmentSymbol(), JvmAnnotationNames.KOTLIN_JVM_INTERNAL)

    private val specialAnnotationConstructors = mutableListOf<IrConstructor>()

    private fun createSpecialAnnotationClass(fqn: FqName, parent: IrPackageFragment) =
        IrFactoryImpl.createSpecialAnnotationClass(fqn, parent).apply {
            specialAnnotationConstructors.add(constructors.single())
        }

    override fun createCustomSuperConstructorCall(
        ktPureClassOrObject: KtPureClassOrObject,
        descriptor: ClassDescriptor,
        context: GeneratorContext
    ): IrDelegatingConstructorCall? {
        if (!descriptor.isJvmRecord()) return null

        val recordClass =
            // We assume j.l.Record is in the classpath because otherwise it should be a compile time error
            descriptor.module.resolveTopLevelClass(JAVA_LANG_RECORD_FQ_NAME, NoLookupLocation.FROM_BACKEND)
                ?: error("Class not found: $JAVA_LANG_RECORD_FQ_NAME")

        val recordConstructor = recordClass.constructors.single()
        // OptIn is needed for the same as for Any constructor at BodyGenerator::generateAnySuperConstructorCall
        @OptIn(ObsoleteDescriptorBasedAPI::class)
        return IrDelegatingConstructorCallImpl.fromSymbolDescriptor(
            ktPureClassOrObject.pureStartOffset, ktPureClassOrObject.pureEndOffset,
            context.irBuiltIns.unitType,
            context.symbolTable.descriptorExtension.referenceConstructor(recordConstructor)
        )
    }

    override fun registerDeclarations(symbolTable: SymbolTable) {
        val signatureComputer = PublicIdSignatureComputer(JvmIrMangler)
        specialAnnotationConstructors.forEach { constructor ->
            symbolTable.declareConstructorWithSignature(signatureComputer.composePublicIdSignature(constructor, false), constructor.symbol)
        }
        super.registerDeclarations(symbolTable)
    }

    override val shouldPreventDeprecatedIntegerValueTypeLiteralConversion: Boolean
        get() = true

    private val rawTypeAnnotationClass =
        createSpecialAnnotationClass(JvmSymbols.RAW_TYPE_ANNOTATION_FQ_NAME, kotlinIrInternalPackage)

    // NB Class 'kotlin.jvm.internal.EnhancedNullability' doesn't exist anywhere in descriptors or in bytecode
    private val enhancedNullabilityAnnotationClass =
        createSpecialAnnotationClass(JvmAnnotationNames.ENHANCED_NULLABILITY_ANNOTATION, kotlinJvmInternalPackage)

    override val flexibleNullabilityAnnotationConstructor: IrConstructor =
        createSpecialAnnotationClass(JvmSymbols.FLEXIBLE_NULLABILITY_ANNOTATION_FQ_NAME, kotlinIrInternalPackage).constructors.single()

    override val flexibleMutabilityAnnotationConstructor: IrConstructor =
        createSpecialAnnotationClass(JvmSymbols.FLEXIBLE_MUTABILITY_ANNOTATION_FQ_NAME, kotlinIrInternalPackage).constructors.single()

    override val enhancedNullabilityAnnotationConstructor: IrConstructor =
        enhancedNullabilityAnnotationClass.constructors.single()

    override val rawTypeAnnotationConstructor: IrConstructor =
        rawTypeAnnotationClass.constructors.single()

    override fun unwrapSyntheticJavaProperty(descriptor: PropertyDescriptor): Pair<FunctionDescriptor, FunctionDescriptor?>? {
        if (descriptor is SyntheticJavaPropertyDescriptor) {
            return descriptor.getMethod to descriptor.setMethod
        }
        return null
    }

    override val parametersAreAssignable: Boolean
        get() = true

    override val debugInfoOnlyOnVariablesInDestructuringDeclarations: Boolean
        get() = true
}
