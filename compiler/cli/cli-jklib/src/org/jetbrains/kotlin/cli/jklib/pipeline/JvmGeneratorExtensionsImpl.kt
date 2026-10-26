/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.jklib.pipeline

import org.jetbrains.kotlin.K1Deprecation
import org.jetbrains.kotlin.backend.jvm.JvmIrSpecialAnnotationSymbolProvider
import org.jetbrains.kotlin.backend.jvm.classNameOverride
import org.jetbrains.kotlin.backend.jvm.createJvmFileFacadeClass
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.annotations.FilteredAnnotations
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrFactory
import org.jetbrains.kotlin.ir.expressions.IrAnnotation
import org.jetbrains.kotlin.ir.util.DeclarationStubGenerator
import org.jetbrains.kotlin.ir.util.IrErrorModuleFragment
import org.jetbrains.kotlin.ir.util.StubGeneratorExtensions
import org.jetbrains.kotlin.ir.util.createThisReceiverParameter
import org.jetbrains.kotlin.load.java.JvmAnnotationNames
import org.jetbrains.kotlin.load.java.descriptors.JavaCallableMemberDescriptor
import org.jetbrains.kotlin.load.java.descriptors.JavaClassDescriptor
import org.jetbrains.kotlin.load.java.typeEnhancement.hasEnhancedNullability
import org.jetbrains.kotlin.load.kotlin.FacadeClassSource
import org.jetbrains.kotlin.resolve.jvm.annotations.hasJvmFieldAnnotation
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DescriptorWithContainerSource
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.typeUtil.replaceAnnotations

@OptIn(K1Deprecation::class)
open class JvmGeneratorExtensionsImpl(
    private val configuration: CompilerConfiguration,
    private val generateFacades: Boolean = true,
) : StubGeneratorExtensions() {
    override fun getContainerSource(descriptor: DeclarationDescriptor): DeserializedContainerSource? {
        return (descriptor as? DescriptorWithContainerSource)?.containerSource
    }

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
        return createJvmFileFacadeClass(
            if (deserializedSource.facadeClassName != null) IrDeclarationOrigin.JVM_MULTIFILE_CLASS else IrDeclarationOrigin.FILE_CLASS,
            facadeName.fqNameForTopLevelClassMaybeWithDollars.shortName(),
            deserializedSource,
        ).also {
            it.createThisReceiverParameter()
            it.classNameOverride = facadeName
        }
    }

    override fun isPropertyWithPlatformField(descriptor: PropertyDescriptor): Boolean =
        descriptor.hasJvmFieldAnnotation()

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


    // This is deprecated code, so for the sake of simplicity, just ErrorModuleDescriptor is passed here instead of looking for a real module.
    private val specialAnnotations: JvmIrSpecialAnnotationSymbolProvider =
        JvmIrSpecialAnnotationSymbolProvider(IrErrorModuleFragment)

    override fun generateFlexibleNullabilityAnnotation(): IrAnnotation =
        specialAnnotations.generateFlexibleNullabilityAnnotation()

    override fun generateFlexibleMutabilityAnnotation(): IrAnnotation =
        specialAnnotations.generateFlexibleMutabilityAnnotation()

    override fun generateEnhancedNullabilityAnnotation(): IrAnnotation =
        specialAnnotations.generateEnhancedNullabilityAnnotation()

    override fun generateRawTypeAnnotation(): IrAnnotation =
        specialAnnotations.generateRawTypeAnnotation()
}
