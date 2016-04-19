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
import java.util.*

object ChangeVisibilityOnExposureFactory : KotlinIntentionActionsFactory() {
    override fun doCreateActions(diagnostic: Diagnostic): List<IntentionAction> {
        @Suppress("UNCHECKED_CAST")
        val factory = diagnostic.factory as DiagnosticFactory3<*, EffectiveVisibility, DescriptorWithRelation, EffectiveVisibility>
        val exposedDiagnostic = factory.cast(diagnostic)
        val exposedDescriptor = exposedDiagnostic.b.descriptor as? DeclarationDescriptorWithVisibility ?: return emptyList()
        val exposedDeclaration =
                DescriptorToSourceUtils.getSourceFromDescriptor(exposedDescriptor) as? KtModifierListOwner ?: return emptyList()
        val exposedVisibility = exposedDiagnostic.c
        val exposingVisibility = exposedDiagnostic.a
        val (lowerBoundVisibility, upperBoundVisibility) = when (exposedVisibility.relation(exposingVisibility)) {
            LESS -> Pair(exposedVisibility.toVisibility(), exposingVisibility.toVisibility())
            else -> Pair(PRIVATE, PUBLIC)
        }
        val exposingDeclaration = diagnostic.psiElement.getParentOfType<KtDeclaration>(true)
        val exposingTargetVisibility = when (lowerBoundVisibility) {
            PUBLIC -> null
            PROTECTED -> if (exposedDeclaration.parent == exposingDeclaration?.parent) PROTECTED else PRIVATE
            else -> lowerBoundVisibility
        }
        val exposingDescriptor = exposingDeclaration?.toDescriptor() as? DeclarationDescriptorWithVisibility
        val result = ArrayList<IntentionAction>()
        if (exposingDeclaration != null && exposingDescriptor != null && exposingTargetVisibility != null &&
            Visibilities.isVisibleIgnoringReceiver(exposedDescriptor, exposingDescriptor)) {
            ChangeVisibilityFix.create(exposingDeclaration, exposingDescriptor, exposingTargetVisibility)?.let { result += it }
        }

        val exposedTargetVisibility = when (upperBoundVisibility) {
            PRIVATE -> null
            PROTECTED -> if (exposedDeclaration.parent == exposingDeclaration?.parent) PROTECTED else PUBLIC
            else -> upperBoundVisibility
        }
        if (exposedTargetVisibility != null) {
            ChangeVisibilityFix.create(exposedDeclaration, exposedDescriptor, exposedTargetVisibility)?.let { result += it }
        }
        return result
    }
}