/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.util

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrFactory
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource
import org.jetbrains.kotlin.types.KotlinType

open class StubGeneratorExtensions {
    open fun computeExternalDeclarationOrigin(descriptor: DeclarationDescriptor): IrDeclarationOrigin? = null

    open fun generateFacadeClass(irFactory: IrFactory, source: DeserializedContainerSource): IrClass? = null

    open fun isPropertyWithPlatformField(descriptor: PropertyDescriptor): Boolean = false

    open fun isStaticFunction(descriptor: FunctionDescriptor): Boolean = false

    open fun recordLocalClassSymbol(classDescriptor: ClassDescriptor, classSymbol: IrClassSymbol) {}

    open val enhancedNullability: EnhancedNullability
        get() = EnhancedNullability

    open class EnhancedNullability {
        open fun hasEnhancedNullability(kotlinType: KotlinType): Boolean = false

        open fun stripEnhancedNullability(kotlinType: KotlinType): KotlinType = kotlinType

        companion object Instance : EnhancedNullability()
    }

    open val flexibleNullabilityAnnotationConstructor: IrConstructor?
        get() = null

    open val enhancedNullabilityAnnotationConstructor: IrConstructor?
        get() = null

    open val rawTypeAnnotationConstructor: IrConstructor?
        get() = null

    companion object {
        @JvmField
        val EMPTY = StubGeneratorExtensions()
    }
}
