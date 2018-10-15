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

package org.jetbrains.kotlin.idea.intentions

import com.intellij.openapi.editor.Editor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.core.CollectingNameValidator
import org.jetbrains.kotlin.idea.core.KotlinNameSuggester
import org.jetbrains.kotlin.idea.core.NewDeclarationNameValidator
import org.jetbrains.kotlin.psi.KtDestructuringDeclaration
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.createDestructuringDeclarationByPattern

class AddMissingDestructuringIntention : SelfTargetingIntention<KtDestructuringDeclaration>(KtDestructuringDeclaration::class.java, "Add missing component") {
    override fun isApplicableTo(element: KtDestructuringDeclaration, caretOffset: Int): Boolean {
        val entriesCount = element.entries.size

        val classDescriptor = element.classDescriptor() ?: return false
        if (!classDescriptor.isData) return false

        val primaryParameters = classDescriptor.primaryParameters() ?: return false
        return primaryParameters.size > entriesCount
    }

    override fun applyTo(element: KtDestructuringDeclaration, editor: Editor?) {
        val entries = element.entries
        val primaryParameters = element.classDescriptor()?.primaryParameters() ?: return

        val factory = KtPsiFactory(element)
        val nameValidator = CollectingNameValidator(
                filter = NewDeclarationNameValidator(element.parent.parent, null, NewDeclarationNameValidator.Target.VARIABLES)
        )

        val newEntries = entries.joinToString(postfix = ", ") { it.text } +
                         primaryParameters.asSequence().drop(entries.size).joinToString {
                             KotlinNameSuggester.suggestNameByName(it.name.asString(), nameValidator)
                         }
        val initializer = element.initializer ?: return

        val newDestructuringDeclaration = factory.createDestructuringDeclarationByPattern(
                if (element.isVar) "var ($0) = $1" else "val ($0) = $1",
                newEntries, initializer)
        element.replace(newDestructuringDeclaration)
    }

    private fun KtDestructuringDeclaration.classDescriptor(): ClassDescriptor? {
        val type = initializer?.let { it.analyze().getType(it) } ?: return null
        return type.constructor.declarationDescriptor as? ClassDescriptor ?: return null
    }

    private fun ClassDescriptor.primaryParameters() = constructors.firstOrNull { it.isPrimary }?.valueParameters
}