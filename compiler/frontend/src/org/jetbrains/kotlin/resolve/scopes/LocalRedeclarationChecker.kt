/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.resolve.scopes

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.OverloadUtil

interface LocalRedeclarationChecker {
    fun checkBeforeAddingToScope(scope: LexicalScope, newDescriptor: DeclarationDescriptor)

    object DO_NOTHING : LocalRedeclarationChecker {
        override fun checkBeforeAddingToScope(scope: LexicalScope, newDescriptor: DeclarationDescriptor) {}
    }
}

abstract class AbstractLocalRedeclarationChecker : LocalRedeclarationChecker {
    override fun checkBeforeAddingToScope(scope: LexicalScope, newDescriptor: DeclarationDescriptor) {
        val name = newDescriptor.name
        val location = NoLookupLocation.WHEN_CHECK_REDECLARATIONS
        when (newDescriptor) {
            is ClassifierDescriptor, is VariableDescriptor -> {
                val otherDescriptor = scope.getContributedClassifier(name, location)
                                      ?: scope.getContributedVariables(name, location).firstOrNull()
                if (otherDescriptor != null) {
                    handleRedeclaration(otherDescriptor, newDescriptor)
                }
            }
            is FunctionDescriptor -> {
                val otherFunctions = scope.getContributedFunctions(name, location)
                val otherClass = scope.getContributedClassifier(name, location)
                val potentiallyConflictingOverloads =
                        if (otherClass is ClassDescriptor)
                            otherFunctions + otherClass.constructors
                        else
                            otherFunctions

                for (overloadedDescriptor in potentiallyConflictingOverloads) {
                    if (!OverloadUtil.isOverloadable(overloadedDescriptor, newDescriptor)) {
                        handleConflictingOverloads(newDescriptor, overloadedDescriptor)
                        break
                    }
                }
            }
            else -> throw IllegalStateException("Unexpected type of descriptor: ${newDescriptor.javaClass.name}, descriptor: $newDescriptor")
        }
    }

    protected abstract fun handleRedeclaration(first: DeclarationDescriptor, second: DeclarationDescriptor)
    protected abstract fun handleConflictingOverloads(first: CallableMemberDescriptor, second: CallableMemberDescriptor)
}

object ThrowingLocalRedeclarationChecker : AbstractLocalRedeclarationChecker() {
    override fun handleRedeclaration(first: DeclarationDescriptor, second: DeclarationDescriptor) {
        throw IllegalStateException(String.format("Redeclaration: %s (%s) and %s (%s) (no line info available)",
                                                  DescriptorUtils.getFqName(first), first,
                                                  DescriptorUtils.getFqName(second), second))
    }

    override fun handleConflictingOverloads(first: CallableMemberDescriptor, second: CallableMemberDescriptor) {
        throw IllegalStateException(String.format("Conflicting overloads: %s (%s) and %s (%s) (no line info available)",
                                                  DescriptorUtils.getFqName(first), first,
                                                  DescriptorUtils.getFqName(second), second))
    }
}

class TraceBasedLocalRedeclarationChecker(val trace: BindingTrace): AbstractLocalRedeclarationChecker() {
    override fun handleRedeclaration(first: DeclarationDescriptor, second: DeclarationDescriptor) {
        reportRedeclaration(first)
        reportRedeclaration(second)
    }

    override fun handleConflictingOverloads(first: CallableMemberDescriptor, second: CallableMemberDescriptor) {
        reportConflictingOverloads(first, second.containingDeclaration)
        reportConflictingOverloads(second, first.containingDeclaration)
    }

    private fun reportConflictingOverloads(conflicting: CallableMemberDescriptor, withContainedIn: DeclarationDescriptor) {
        val reportElement = DescriptorToSourceUtils.descriptorToDeclaration(conflicting)
        if (reportElement != null) {
            trace.report(Errors.CONFLICTING_OVERLOADS.on(reportElement, conflicting, withContainedIn))
        }
        else {
            throw IllegalStateException("No declaration found for " + conflicting)
        }
    }

    private fun reportRedeclaration(descriptor: DeclarationDescriptor) {
        val firstElement = DescriptorToSourceUtils.descriptorToDeclaration(descriptor)
        if (firstElement != null) {
            trace.report(Errors.REDECLARATION.on(firstElement, descriptor.name.asString()))
        }
        else {
            throw IllegalStateException("No declaration found for " + descriptor)
        }
    }
}