/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.declarations.lazy

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.DescriptorBasedIr
import org.jetbrains.kotlin.ir.IrElementBase
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrValueParameterImpl
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.util.DeclarationStubGenerator
import org.jetbrains.kotlin.ir.util.TypeTranslator
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.types.KotlinType

@OptIn(DescriptorBasedIr::class)
abstract class IrLazyDeclarationBase(
    startOffset: Int,
    endOffset: Int,
    override var origin: IrDeclarationOrigin,
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
        descriptors.mapNotNullTo(declarations) { generateMemberStub(it) }
    }

    private fun generateMemberStub(descriptor: DeclarationDescriptor): IrDeclaration? {
        if (descriptor is DeclarationDescriptorWithVisibility && Visibilities.isPrivate(descriptor.visibility)) return null
        return stubGenerator.generateMemberStub(descriptor)
    }

    override var parent: IrDeclarationParent by lazyVar {
        createLazyParent()!!
    }

    override var annotations: List<IrConstructorCall> by lazyVar {
        descriptor.annotations.mapNotNull(typeTranslator.constantValueGenerator::generateAnnotationConstructorCall).toMutableList()
    }

    override var metadata: Nothing?
        get() = null
        set(_) = error("We should never need to store metadata of external declarations.")

    private fun createLazyParent(): IrDeclarationParent? {
        val currentDescriptor = descriptor

        val containingDeclaration =
            ((currentDescriptor as? PropertyAccessorDescriptor)?.correspondingProperty ?: currentDescriptor).containingDeclaration

        return when (containingDeclaration) {
            is PackageFragmentDescriptor -> run {
                val parent = this.takeUnless { it is IrClass }?.let {
                    stubGenerator.generateOrGetFacadeClass(descriptor)
                } ?: stubGenerator.generateOrGetEmptyExternalPackageFragmentStub(containingDeclaration)
                parent.declarations.add(this)
                parent
            }
            is ClassDescriptor -> stubGenerator.generateClassStub(containingDeclaration)
            is FunctionDescriptor -> stubGenerator.generateFunctionStub(containingDeclaration)
            is PropertyDescriptor -> stubGenerator.generateFunctionStub(containingDeclaration.run { getter ?: setter!! })
            else -> throw AssertionError("Package or class expected: $containingDeclaration; for $currentDescriptor")
        }
    }
}
