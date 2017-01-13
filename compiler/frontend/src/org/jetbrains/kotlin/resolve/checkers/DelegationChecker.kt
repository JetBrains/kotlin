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

import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.diagnostics.DiagnosticSink
import org.jetbrains.kotlin.diagnostics.Errors.DELEGATED_MEMBER_HIDES_SUPERTYPE_OVERRIDE
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtDelegatedSuperTypeEntry
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DelegationResolver
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.OverridingUtil

class DelegationChecker : DeclarationChecker {
    override fun check(
            declaration: KtDeclaration,
            descriptor: DeclarationDescriptor,
            diagnosticHolder: DiagnosticSink,
            bindingContext: BindingContext,
            languageVersionSettings: LanguageVersionSettings
    ) {
        if (languageVersionSettings.languageVersion == LanguageVersion.KOTLIN_1_0) return

        if (descriptor !is ClassDescriptor) return
        if (declaration !is KtClassOrObject) return

        for (specifier in declaration.superTypeListEntries) {
            if (specifier is KtDelegatedSuperTypeEntry) {
                val superType = specifier.typeReference?.let { bindingContext.get(BindingContext.TYPE, it) } ?: continue
                val superTypeDescriptor = superType.constructor.declarationDescriptor as? ClassDescriptor ?: continue

                for ((delegated, delegatedTo) in DelegationResolver.getDelegates(descriptor, superTypeDescriptor)) {
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
        val reachableFromDelegated =
                OverridingUtil.filterOutOverridden(
                        DescriptorUtils.getAllOverriddenDescriptors(delegatedDescriptor).filter { it.kind.isReal }.toSet()
                ) - DescriptorUtils.unwrapFakeOverride(delegatedToDescriptor).original

        val nonAbstractReachable = reachableFromDelegated.filter { it.modality == Modality.OPEN }

        if (nonAbstractReachable.isNotEmpty()) {
            /*In case of MANY_IMPL_MEMBER_NOT_IMPLEMENTED error there could be several elements otherwise only one*/
            diagnosticHolder.report(DELEGATED_MEMBER_HIDES_SUPERTYPE_OVERRIDE.on(classDeclaration, delegatedDescriptor, nonAbstractReachable))
        }
    }
}
