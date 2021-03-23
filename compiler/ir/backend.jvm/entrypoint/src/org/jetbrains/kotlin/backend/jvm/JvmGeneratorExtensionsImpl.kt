/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm

import org.jetbrains.kotlin.backend.common.ir.createImplicitParameterDeclarationWithWrappedDescriptor
import org.jetbrains.kotlin.backend.common.ir.createParameterDeclarations
import org.jetbrains.kotlin.codegen.JvmSamTypeFactory
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.FilteredAnnotations
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.builders.declarations.addConstructor
import org.jetbrains.kotlin.ir.builders.declarations.buildClass
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrExternalPackageFragmentImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl
import org.jetbrains.kotlin.ir.descriptors.IrBuiltIns
import org.jetbrains.kotlin.ir.expressions.IrDelegatingConstructorCall
import org.jetbrains.kotlin.ir.expressions.impl.IrDelegatingConstructorCallImpl
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.impl.DescriptorlessExternalPackageFragmentSymbol
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.load.java.JvmAnnotationNames
import org.jetbrains.kotlin.load.java.descriptors.JavaCallableMemberDescriptor
import org.jetbrains.kotlin.load.java.descriptors.JavaClassDescriptor
import org.jetbrains.kotlin.load.java.descriptors.getParentJavaStaticClassScope
import org.jetbrains.kotlin.load.java.sam.JavaSingleAbstractMethodUtils
import org.jetbrains.kotlin.load.java.typeEnhancement.hasEnhancedNullability
import org.jetbrains.kotlin.load.kotlin.JvmPackagePartSource
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
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.typeUtil.replaceAnnotations

open class JvmGeneratorExtensionsImpl(private val generateFacades: Boolean = true) : GeneratorExtensions(), JvmGeneratorExtensions {
    override val classNameOverride: MutableMap<IrClass, JvmClassName> = mutableMapOf()

    override val samConversion: SamConversion
        get() = JvmSamConversion

    open class JvmSamConversion : SamConversion() {

        override fun isPlatformSamType(type: KotlinType): Boolean =
            JavaSingleAbstractMethodUtils.isSamType(type)

        override fun getSamTypeForValueParameter(valueParameter: ValueParameterDescriptor): KotlinType? =
            JvmSamTypeFactory.createByValueParameter(valueParameter)?.type

        companion object Instance : JvmSamConversion()
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

    override fun generateFacadeClass(irFactory: IrFactory, source: DeserializedContainerSource): IrClass? {
        if (!generateFacades || source !is JvmPackagePartSource) return null
        val facadeName = source.facadeClassName ?: source.className
        return irFactory.buildClass {
            origin = if (source.facadeClassName != null) IrDeclarationOrigin.JVM_MULTIFILE_CLASS else IrDeclarationOrigin.FILE_CLASS
            name = facadeName.fqNameForTopLevelClassMaybeWithDollars.shortName()
        }.also {
            it.createParameterDeclarations()
            classNameOverride[it] = facadeName
        }
    }

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

    private fun createSpecialAnnotationClass(fqn: FqName, parent: IrPackageFragment) =
        IrFactoryImpl.buildClass {
            kind = ClassKind.ANNOTATION_CLASS
            name = fqn.shortName()
        }.apply {
            createImplicitParameterDeclarationWithWrappedDescriptor()
            this.parent = parent
            addConstructor {
                isPrimary = true
            }
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
            context.symbolTable.referenceConstructor(recordConstructor)
        )
    }

    override val shouldPreventDeprecatedIntegerValueTypeLiteralConversion: Boolean
        get() = true

    private val flexibleNullabilityAnnotationClass =
        createSpecialAnnotationClass(JvmSymbols.FLEXIBLE_NULLABILITY_ANNOTATION_FQ_NAME, kotlinIrInternalPackage)

    private val rawTypeAnnotationClass =
        createSpecialAnnotationClass(JvmSymbols.RAW_TYPE_ANNOTATION_FQ_NAME, kotlinIrInternalPackage)

    // NB Class 'kotlin.jvm.internal.EnhancedNullability' doesn't exist anywhere in descriptors or in bytecode
    private val enhancedNullabilityAnnotationClass =
        createSpecialAnnotationClass(JvmAnnotationNames.ENHANCED_NULLABILITY_ANNOTATION, kotlinJvmInternalPackage)

    override val flexibleNullabilityAnnotationConstructor: IrConstructor =
        flexibleNullabilityAnnotationClass.constructors.single()

    override val enhancedNullabilityAnnotationConstructor: IrConstructor =
        enhancedNullabilityAnnotationClass.constructors.single()

    override val rawTypeAnnotationConstructor: IrConstructor =
        rawTypeAnnotationClass.constructors.single()

    private val localClassMappings = mutableMapOf<ClassDescriptor, IrClassSymbol>()

    override fun referenceLocalClass(classDescriptor: ClassDescriptor): IrClassSymbol? = localClassMappings[classDescriptor]

    override fun recordLocalClassSymbol(classDescriptor: ClassDescriptor, classSymbol: IrClassSymbol) {
        localClassMappings[classDescriptor] = classSymbol
    }
}
