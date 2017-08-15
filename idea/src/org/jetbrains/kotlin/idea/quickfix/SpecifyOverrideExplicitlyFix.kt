/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.quickfix

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.idea.caches.resolve.analyzeFully
import org.jetbrains.kotlin.idea.core.ShortenReferences
import org.jetbrains.kotlin.idea.core.overrideImplement.OverrideMemberChooserObject
import org.jetbrains.kotlin.idea.core.overrideImplement.generateMember
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils

class SpecifyOverrideExplicitlyFix(
        element: KtClassOrObject, private val signature: String
) : KotlinQuickFixAction<KtClassOrObject>(element) {

    override fun getText() = "Specify override for '$signature' explicitly"

    override fun getFamilyName() = "Specify override explicitly"

    override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        val element = element ?: return
        val context = element.analyzeFully()
        val delegatedDescriptor = context.diagnostics.forElement(element).mapNotNull {
            if (it.factory == Errors.DELEGATED_MEMBER_HIDES_SUPERTYPE_OVERRIDE)
                Errors.DELEGATED_MEMBER_HIDES_SUPERTYPE_OVERRIDE.cast(it).a
            else
                null
        }.firstOrNull {
            DescriptorRenderer.ONLY_NAMES_WITH_SHORT_TYPES.render(it) == signature
        } ?: return
        for (specifier in element.superTypeListEntries) {
            if (specifier is KtDelegatedSuperTypeEntry) {
                val superType = specifier.typeReference?.let { context[BindingContext.TYPE, it] } ?: continue
                val superTypeDescriptor = superType.constructor.declarationDescriptor as? ClassDescriptor ?: continue
                val overriddenDescriptor = delegatedDescriptor.overriddenDescriptors.find {
                    it.containingDeclaration == superTypeDescriptor
                } ?: continue

                val delegateExpression = specifier.delegateExpression as? KtNameReferenceExpression
                val delegateTargetDescriptor = context[BindingContext.REFERENCE_TARGET, delegateExpression] ?: return
                if (delegateTargetDescriptor is ValueParameterDescriptor &&
                    delegateTargetDescriptor.containingDeclaration.let {
                        it is ConstructorDescriptor &&
                        it.isPrimary &&
                        it.containingDeclaration == delegatedDescriptor.containingDeclaration
                    }) {
                    val delegateParameter = DescriptorToSourceUtils.descriptorToDeclaration(
                            delegateTargetDescriptor) as? KtParameter
                    if (delegateParameter != null && !delegateParameter.hasValOrVar()) {
                        val factory = KtPsiFactory(project)
                        delegateParameter.addModifier(KtTokens.PRIVATE_KEYWORD)
                        delegateParameter.addAfter(factory.createValKeyword(), delegateParameter.modifierList)
                    }
                }

                val overrideMemberChooserObject = OverrideMemberChooserObject.create(
                        project, delegatedDescriptor, overriddenDescriptor,
                        OverrideMemberChooserObject.BodyType.Delegate(delegateTargetDescriptor.name.asString())
                )
                val member = overrideMemberChooserObject.generateMember(element, copyDoc = false)
                val insertedMember = element.addDeclaration(member)
                ShortenReferences.DEFAULT.process(insertedMember)
                return
            }
        }

    }

    companion object : KotlinSingleIntentionActionFactory() {
        override fun createAction(diagnostic: Diagnostic): IntentionAction? {
            val hidesOverrideError = Errors.DELEGATED_MEMBER_HIDES_SUPERTYPE_OVERRIDE.cast(diagnostic)
            val klass = hidesOverrideError.psiElement
            if (klass.superTypeListEntries.any {
                it is KtDelegatedSuperTypeEntry && it.delegateExpression !is KtNameReferenceExpression
            }) {
                return null
            }
            val properOverride = hidesOverrideError.a
            return SpecifyOverrideExplicitlyFix(klass, DescriptorRenderer.ONLY_NAMES_WITH_SHORT_TYPES.render(properOverride))
        }
    }
}