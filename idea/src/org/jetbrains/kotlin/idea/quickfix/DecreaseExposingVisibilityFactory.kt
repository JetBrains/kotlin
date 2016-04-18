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

package org.jetbrains.kotlin.idea.quickfix

import com.intellij.codeInsight.intention.IntentionAction
import org.jetbrains.kotlin.descriptors.DeclarationDescriptorWithVisibility
import org.jetbrains.kotlin.descriptors.DescriptorWithRelation
import org.jetbrains.kotlin.descriptors.EffectiveVisibility
import org.jetbrains.kotlin.descriptors.EffectiveVisibility.Permissiveness.LESS
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.Visibilities.*
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory3
import org.jetbrains.kotlin.idea.core.toDescriptor
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtModifierListOwner
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils

object DecreaseExposingVisibilityFactory  : KotlinSingleIntentionActionFactory() {
    override fun createAction(diagnostic: Diagnostic): IntentionAction? {
        @Suppress("UNCHECKED_CAST")
        val factory = diagnostic.factory as DiagnosticFactory3<*, EffectiveVisibility, DescriptorWithRelation, EffectiveVisibility>
        val exposedDiagnostic = factory.cast(diagnostic)
        val exposedDescriptor = exposedDiagnostic.b.descriptor as? DeclarationDescriptorWithVisibility ?: return null
        val exposedDeclaration = DescriptorToSourceUtils.getSourceFromDescriptor(exposedDescriptor) as? KtModifierListOwner ?: return null
        val exposedVisibility = exposedDiagnostic.c
        val exposingVisibility = exposedDiagnostic.a
        val boundVisibility = when (exposedVisibility.relation(exposingVisibility)) {
            LESS -> exposedVisibility.toVisibility()
            else -> PRIVATE
        }
        val exposingDeclaration = diagnostic.psiElement.getParentOfType<KtDeclaration>(true) ?: return null
        val targetVisibility = when (boundVisibility) {
            PUBLIC -> return null
            PROTECTED -> if (exposedDeclaration.parent == exposingDeclaration.parent) PROTECTED else PRIVATE
            else -> boundVisibility
        }
        val exposingDescriptor = exposingDeclaration.toDescriptor() as? DeclarationDescriptorWithVisibility ?: return null
        if (!Visibilities.isVisibleIgnoringReceiver(exposedDescriptor, exposingDescriptor)) return null
        return ChangeVisibilityFix.create(exposingDeclaration, exposingDescriptor, targetVisibility)
    }
}