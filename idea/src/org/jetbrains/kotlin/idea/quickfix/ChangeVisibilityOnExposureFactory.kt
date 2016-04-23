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
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.EffectiveVisibility.Permissiveness.LESS
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

    private fun addFixToTargetVisibility(
            modifierListOwner: KtModifierListOwner,
            descriptor: DeclarationDescriptorWithVisibility,
            targetVisibility: Visibility,
            boundVisibility: Visibility,
            protectedAllowed: Boolean,
            fixes: MutableList<IntentionAction>
    ) {
        val possibleVisibilities = when (targetVisibility) {
            PROTECTED -> if (protectedAllowed) listOf(boundVisibility, PROTECTED) else listOf(boundVisibility)
            INTERNAL -> listOf(boundVisibility, INTERNAL)
            boundVisibility -> listOf(boundVisibility)
            else -> listOf()
        }
        possibleVisibilities.mapNotNullTo(fixes) { ChangeVisibilityFix.create(modifierListOwner, descriptor, it) }
    }

    override fun doCreateActions(diagnostic: Diagnostic): List<IntentionAction> {
        @Suppress("UNCHECKED_CAST")
        val factory = diagnostic.factory as DiagnosticFactory3<*, EffectiveVisibility, DescriptorWithRelation, EffectiveVisibility>
        // We have USER that uses some EXPOSED object. USER visibility must be same or less permissive.
        val exposedDiagnostic = factory.cast(diagnostic)
        val exposedDescriptor = exposedDiagnostic.b.descriptor as? DeclarationDescriptorWithVisibility ?: return emptyList()
        val exposedDeclaration =
                DescriptorToSourceUtils.getSourceFromDescriptor(exposedDescriptor) as? KtModifierListOwner ?: return emptyList()
        val exposedVisibility = exposedDiagnostic.c
        val userVisibility = exposedDiagnostic.a
        val (targetUserVisibility, targetExposedVisibility) = when (exposedVisibility.relation(userVisibility)) {
            LESS -> Pair(exposedVisibility.toVisibility(), userVisibility.toVisibility())
            else -> Pair(PRIVATE, PUBLIC)
        }
        val result = ArrayList<IntentionAction>()
        val userDeclaration = diagnostic.psiElement.getParentOfType<KtDeclaration>(true)
        val protectedAllowed = exposedDeclaration.parent == userDeclaration?.parent
        if (userDeclaration != null) {
            val userDescriptor = userDeclaration.toDescriptor() as? DeclarationDescriptorWithVisibility
            if (userDescriptor != null && Visibilities.isVisibleIgnoringReceiver(exposedDescriptor, userDescriptor)) {
                addFixToTargetVisibility(userDeclaration, userDescriptor,
                                         targetUserVisibility, PRIVATE,
                                         protectedAllowed, result)
            }
        }
        addFixToTargetVisibility(exposedDeclaration, exposedDescriptor,
                                 targetExposedVisibility, PUBLIC,
                                 protectedAllowed, result)
        return result
    }
}