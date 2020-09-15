/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.declarations.lazy

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.symbols.impl.IrValueParameterSymbolImpl
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.DeclarationStubGenerator
import org.jetbrains.kotlin.ir.util.TypeTranslator
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.types.KotlinType
import kotlin.properties.ReadWriteProperty

@OptIn(ObsoleteDescriptorBasedAPI::class)
interface IrLazyDeclarationBase : IrDeclaration {
    val stubGenerator: DeclarationStubGenerator
    val typeTranslator: TypeTranslator

    override val factory: IrFactory
        get() = stubGenerator.symbolTable.irFactory

    fun KotlinType.toIrType(): IrType =
        typeTranslator.translateType(this)

    fun ReceiverParameterDescriptor.generateReceiverParameterStub(): IrValueParameter =
        factory.createValueParameter(
            UNDEFINED_OFFSET, UNDEFINED_OFFSET, origin, IrValueParameterSymbolImpl(this),
            name, -1, type.toIrType(), null, isCrossinline = false, isNoinline = false, isAssignable = false
        )

    fun generateMemberStubs(memberScope: MemberScope, container: MutableList<IrDeclaration>) {
        generateChildStubs(memberScope.getContributedDescriptors(), container)
    }

    fun generateChildStubs(descriptors: Collection<DeclarationDescriptor>, declarations: MutableList<IrDeclaration>) {
        descriptors.mapNotNullTo(declarations) { descriptor ->
            if (descriptor is DeclarationDescriptorWithVisibility && DescriptorVisibilities.isPrivate(descriptor.visibility)) null
            else stubGenerator.generateMemberStub(descriptor)
        }
    }

    fun createLazyAnnotations(): ReadWriteProperty<Any?, List<IrConstructorCall>> = lazyVar {
        descriptor.annotations.mapNotNull(typeTranslator.constantValueGenerator::generateAnnotationConstructorCall).toMutableList()
    }

    fun createLazyParent(): ReadWriteProperty<Any?, IrDeclarationParent> = lazyVar {
        val currentDescriptor = descriptor

        val containingDeclaration =
            ((currentDescriptor as? PropertyAccessorDescriptor)?.correspondingProperty ?: currentDescriptor).containingDeclaration

        when (containingDeclaration) {
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
