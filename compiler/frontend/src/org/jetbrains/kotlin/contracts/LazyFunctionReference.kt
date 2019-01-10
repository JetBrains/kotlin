/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.contracts

import org.jetbrains.kotlin.contracts.description.expressions.FunctionReference
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.resolve.scopes.getDescriptorsFiltered

class LazyFunctionReference(
    private val ownerDescriptor: DeclarationDescriptor,
    targetFunctionName: String,
    functionOwnerClassName: String?
) : FunctionReference {
    private val targetFunctionName = Name.identifier(targetFunctionName)
    private val functionOwnerClassName = functionOwnerClassName?.let { FqName(it) }

    override val descriptor: FunctionDescriptor by lazy {
        // TODO: is it true that IllegalStateException never wil be thrown?
        var scope = ownerDescriptor.findTopParentScope()
        if (functionOwnerClassName != null) {
            val classDescriptor = (scope.getDescriptorsFiltered(DescriptorKindFilter.CLASSIFIERS)
                .firstOrNull { it.fqNameSafe == this.functionOwnerClassName } as? ClassDescriptor
                ?: throw IllegalStateException())
            scope = classDescriptor.unsubstitutedMemberScope
        }
        scope.getDescriptorsFiltered(DescriptorKindFilter.FUNCTIONS) { it == this.targetFunctionName }
            .firstOrNull() as? FunctionDescriptor
            ?: throw IllegalStateException()
    }

    override fun toString(): String = descriptor.name.toString()
}

// TODO: is parent scope always exists?
private fun DeclarationDescriptor.findTopParentScope() = parentDeclarations
    .reversed().asSequence()
    .mapNotNull { (it as? PackageFragmentDescriptor)?.getMemberScope() }
    .firstOrNull() ?: throw IllegalStateException()

private val DeclarationDescriptor.parentDeclarations: List<DeclarationDescriptor>
    get() {
        val result = mutableListOf<DeclarationDescriptor>()
        var parent = containingDeclaration
        while (parent != null) {
            result += parent
            parent = parent.containingDeclaration
        }
        return result
    }