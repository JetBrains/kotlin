/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.util

import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource
import org.jetbrains.kotlin.types.KotlinType

interface DeserializableClass {
    fun loadIr(): Boolean
}

open class StubGeneratorExtensions {
    open fun computeExternalDeclarationOrigin(descriptor: DeclarationDescriptor): IrDeclarationOrigin? = null

    open fun generateFacadeClass(
        irFactory: IrFactory,
        deserializedSource: DeserializedContainerSource,
        stubGenerator: DeclarationStubGenerator,
    ): IrClass? = null


    // Extension point for the JVM Debugger IDEA plug-in: it compiles fragments
    // (conditions on breakpoints, "Evaluate expression...", watches, etc...)
    // in the context of an open intellij project that is being debugged. These
    // classes are supplied to the fragment evaluator as PSI, not class files,
    // as the old backend assumes for external declarations. Hence, we need to
    // intercept and supply "fake" deserialized sources.
    open fun getContainerSource(descriptor: DeclarationDescriptor): DeserializedContainerSource? = null

    // Extension point for the JVM Debugger IDEA plug-in: to replace accesses
    // to private properties _without_ accessor implementations, the fragment
    // compiler needs to predict the compilation output for properties.
    // To do this, we need to know whether the property accessors have explicit
    // bodies, information that is _not_ present in the IR structure, but _is_
    // available in the corresponding PSI. See `CodeFragmentCompiler` in the
    // plug-in for the implementation.
    open fun isAccessorWithExplicitImplementation(accessor: IrSimpleFunction): Boolean = false

    open fun isPropertyWithPlatformField(descriptor: PropertyDescriptor): Boolean = false

    open fun isStaticFunction(descriptor: FunctionDescriptor): Boolean = false

    open fun deserializeClass(
        irClass: IrClass,
        stubGenerator: DeclarationStubGenerator,
        parent: IrDeclarationParent,
    ): Boolean = false

    open val enhancedNullability: EnhancedNullability
        get() = EnhancedNullability

    open class EnhancedNullability {
        open fun hasEnhancedNullability(kotlinType: KotlinType): Boolean = false

        open fun stripEnhancedNullability(kotlinType: KotlinType): KotlinType = kotlinType

        companion object Instance : EnhancedNullability()
    }

    open val irDeserializationEnabled: Boolean = false

    open fun generateFlexibleNullabilityAnnotationCall(): IrConstructorCall? = null

    open fun generateFlexibleMutabilityAnnotationCall(): IrConstructorCall? = null

    open fun generateEnhancedNullabilityAnnotationCall(): IrConstructorCall? = null

    open fun generateRawTypeAnnotationCall(): IrConstructorCall? = null

    companion object {
        @JvmField
        val EMPTY = StubGeneratorExtensions()
    }
}
