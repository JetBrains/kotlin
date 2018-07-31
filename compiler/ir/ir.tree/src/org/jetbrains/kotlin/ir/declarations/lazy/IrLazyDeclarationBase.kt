/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.declarations.lazy

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.IrElementBase
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrDeclarationParent
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.declarations.impl.IrValueParameterImpl
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.util.DeclarationStubGenerator
import org.jetbrains.kotlin.ir.util.TypeTranslator
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.types.KotlinType
import kotlin.reflect.KProperty

abstract class IrLazyDeclarationBase(
    startOffset: Int,
    endOffset: Int,
    override val origin: IrDeclarationOrigin,
    private val stubGenerator: DeclarationStubGenerator,
    protected val typeTranslator: TypeTranslator
) : IrElementBase(startOffset, endOffset), IrDeclaration {

    protected fun KotlinType.toIrType() = typeTranslator.translateType(this)

    protected fun ReceiverParameterDescriptor.generateReceiverParameterStub(): IrValueParameter =
        IrValueParameterImpl(
            UNDEFINED_OFFSET, UNDEFINED_OFFSET, origin, this,
            type.toIrType(), null
        )

    protected fun generateMemberStubs(memberScope: MemberScope, container: MutableList<IrDeclaration>) {
        generateChildStubs(memberScope.getContributedDescriptors(), container)
    }

    protected fun generateChildStubs(descriptors: Collection<DeclarationDescriptor>, declarations: MutableList<IrDeclaration>) {
        descriptors.mapTo(declarations) { generateMemberStub(it) }
    }

    private fun generateMemberStub(descriptor: DeclarationDescriptor): IrDeclaration =
        stubGenerator.generateMemberStub(descriptor)

    override var parent: IrDeclarationParent by lazyVar {
        createLazyParent()!!
    }

    override val annotations: MutableList<IrCall> = arrayListOf()

    private fun createLazyParent(): IrDeclarationParent? {
        val currentDescriptor = descriptor
        val containingDeclaration = currentDescriptor.containingDeclaration
        return when (containingDeclaration) {
            is PackageFragmentDescriptor -> stubGenerator.generateOrGetEmptyExternalPackageFragmentStub(containingDeclaration).also {
                it.declarations.add(this)
            }
            is ClassDescriptor -> stubGenerator.generateClassStub(containingDeclaration)
            is FunctionDescriptor -> stubGenerator.generateFunctionStub(containingDeclaration)
            else -> throw AssertionError("Package or class expected: $containingDeclaration; for $currentDescriptor")
        }
    }
}


internal fun <T> lazyVar(initializer: () -> T): UnsafeLazyVar<T> = UnsafeLazyVar(initializer)


internal class UnsafeLazyVar<T>(initializer: () -> T) {
    private var isInitialized = false;
    private var initializer: (() -> T)? = initializer
    private var _value: Any? = null

    private val value: T
        get() {
            if (!isInitialized) {
                _value = initializer!!()
                isInitialized = true
                initializer = null
            }
            @Suppress("UNCHECKED_CAST")
            return _value as T
        }

    override fun toString(): String = if (isInitialized) value.toString() else "Lazy value not initialized yet."

    operator fun getValue(thisRef: Any?, property: KProperty<*>): T = value

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        this._value = value
        isInitialized = true
    }
}