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

package org.jetbrains.kotlin.resolve.checkers

import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.diagnostics.DiagnosticSink
import org.jetbrains.kotlin.diagnostics.Errors.DELEGATED_MEMBER_HIDES_SUPERTYPE_OVERRIDE
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtDelegatedSuperTypeEntry
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.MemberComparator
import org.jetbrains.kotlin.resolve.OverridingUtil
import org.jetbrains.kotlin.utils.DFS
import org.jetbrains.kotlin.utils.keysToMapExceptNulls

class DelegationChecker : SimpleDeclarationChecker {
    override fun check(
            declaration: KtDeclaration,
            descriptor: DeclarationDescriptor,
            diagnosticHolder: DiagnosticSink,
            bindingContext: BindingContext
    ) {
        if (descriptor !is ClassDescriptor) return
        if (declaration !is KtClassOrObject) return

        val delegationDescriptors = descriptor.defaultType.memberScope.getContributedDescriptors().
                filterIsInstance<CallableMemberDescriptor>().filter { it.kind == CallableMemberDescriptor.Kind.DELEGATION }.sortedWith(MemberComparator.INSTANCE)

        for (specifier in declaration.superTypeListEntries) {
            if (specifier is KtDelegatedSuperTypeEntry) {
                val superType = specifier.typeReference?.let { bindingContext.get(BindingContext.TYPE, it) } ?: continue
                val superTypeDescriptor = superType.constructor.declarationDescriptor as? ClassDescriptor ?: continue

                val delegates = getDelegates(delegationDescriptors, superTypeDescriptor)
                delegates.forEach { (delegated, delegatedTo) ->
                    checkDescriptor(declaration, delegated, delegatedTo, diagnosticHolder)
                }
            }
        }
    }

    private fun checkDescriptor(
            classDeclaration: KtDeclaration,
            delegatedDescriptor: CallableMemberDescriptor,
            delegatedToDescriptor: CallableMemberDescriptor,
            diagnosticHolder: DiagnosticSink
    ) {
        val reachableFromDelegated = findAllReachableDeclarations(delegatedDescriptor)
        reachableFromDelegated.remove(delegatedDescriptor.original)
        val toRemove = linkedSetOf<CallableMemberDescriptor>()
        for (declaration in reachableFromDelegated) {
            val reachable = findAllReachableDeclarations(declaration.original)
            reachable.remove(declaration)
            toRemove.addAll(reachable)
        }
        reachableFromDelegated.removeAll(toRemove)
        reachableFromDelegated.remove(DescriptorUtils.unwrapFakeOverride(delegatedToDescriptor).original)

        val nonAbstractReachable = reachableFromDelegated.filter { it.modality != Modality.ABSTRACT }

        if (nonAbstractReachable.isNotEmpty()) {
            /*In case of MANY_IMPL_MEMBER_NOT_IMPLEMENTED error there could be several elements otherwise only one*/
            diagnosticHolder.report(DELEGATED_MEMBER_HIDES_SUPERTYPE_OVERRIDE.on(classDeclaration, delegatedDescriptor, nonAbstractReachable.toList()))
        }
    }

    fun getDelegates(
            delegatedMethods: Iterable<CallableMemberDescriptor>,
            toInterface: ClassDescriptor
    ): Map<CallableMemberDescriptor, CallableMemberDescriptor> {
        return delegatedMethods
                .keysToMapExceptNulls { delegatingMember ->
                    val actualDelegates = DescriptorUtils.getAllOverriddenDescriptors(delegatingMember)
                            .filter { it.containingDeclaration == toInterface }
                            .map { overriddenDescriptor ->
                                val scope = toInterface.defaultType.memberScope
                                val name = overriddenDescriptor.name

                                // this is the actual member of delegateExpressionType that we are delegating to
                                (scope.getContributedFunctions(name, NoLookupLocation.WHEN_CHECK_OVERRIDES) +
                                 scope.getContributedVariables(name, NoLookupLocation.WHEN_CHECK_OVERRIDES))
                                        .firstOrNull { it == overriddenDescriptor || OverridingUtil.overrides(it, overriddenDescriptor) }
                            }

                    actualDelegates.firstOrNull() as? CallableMemberDescriptor
                }
    }
}

private fun findAllReachableDeclarations(memberDescriptor: CallableMemberDescriptor): MutableSet<CallableMemberDescriptor> {
    val collector = object : DFS.NodeHandlerWithListResult<CallableMemberDescriptor, CallableMemberDescriptor>() {
        override fun afterChildren(current: CallableMemberDescriptor) {
            if (current.kind.isReal) {
                result.add(current.original)
            }
        }
    }

    DFS.dfs(listOf(memberDescriptor), { it.overriddenDescriptors }, collector)
    return java.util.HashSet(collector.result())
}
