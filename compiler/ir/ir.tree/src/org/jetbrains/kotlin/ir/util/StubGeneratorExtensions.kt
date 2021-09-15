/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.util

import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.lazy.IrLazyClass
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

    open fun isPropertyWithPlatformField(descriptor: PropertyDescriptor): Boolean = false

    open fun isStaticFunction(descriptor: FunctionDescriptor): Boolean = false

    open fun deserializeClass(
        irClass: IrClass,
        stubGenerator: DeclarationStubGenerator,
        parent: IrDeclarationParent,
        allowErrorNodes: Boolean,
    ): Boolean = false

    open val enhancedNullability: EnhancedNullability
        get() = EnhancedNullability

    open class EnhancedNullability {
        open fun hasEnhancedNullability(kotlinType: KotlinType): Boolean = false

        open fun stripEnhancedNullability(kotlinType: KotlinType): KotlinType = kotlinType

        companion object Instance : EnhancedNullability()
    }

    open val irDeserializationEnabled: Boolean = false

    open val flexibleNullabilityAnnotationConstructor: IrConstructor?
        get() = null

    open val flexibleMutabilityAnnotationConstructor: IrConstructor?
        get() = null

    open val enhancedNullabilityAnnotationConstructor: IrConstructor?
        get() = null

    open val rawTypeAnnotationConstructor: IrConstructor?
        get() = null

    open fun registerDeclarations(symbolTable: SymbolTable) {}

    companion object {
        @JvmField
        val EMPTY = StubGeneratorExtensions()
    }
}
