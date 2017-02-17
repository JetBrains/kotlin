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
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.psi.KtDestructuringDeclaration
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.createDestructuringDeclarationByPattern

class AddMissingDestructuringIntention : SelfTargetingIntention<KtDestructuringDeclaration>(KtDestructuringDeclaration::class.java, "Add missing component") {
    override fun isApplicableTo(element: KtDestructuringDeclaration, caretOffset: Int): Boolean {
        val entriesCount = element.entries.size

        val classDescriptor = element.classDescriptor() ?: return false
        if (!classDescriptor.isData) return false

        val primaryParameters = classDescriptor.primaryParameters() ?: return false
        if (primaryParameters.size <= entriesCount) return false

        return true
    }

    override fun applyTo(element: KtDestructuringDeclaration, editor: Editor?) {
        val entries = element.entries
        val classDescriptor = element.classDescriptor() ?: return
        val primaryParameters = classDescriptor.primaryParameters() ?: return

        val factory = KtPsiFactory(element)

        val valOrVarKeyword = element.valOrVarKeyword?.text ?: return
        val newEntries = entries.joinToString(postfix = ", ") { it.text } + primaryParameters.drop(entries.size).joinToString { it.name.asString() }
        val initializer = element.initializer?.text ?: return

        element.replace(factory.createDestructuringDeclarationByPattern(
                "$0 ($1) = $2", valOrVarKeyword, newEntries, initializer))
    }

    private fun KtDestructuringDeclaration.classDescriptor(): ClassDescriptor? {
        val initializer = initializer ?: return null
        val type = initializer.analyze().getType(initializer) ?: return null
        return type.constructor.declarationDescriptor as? ClassDescriptor ?: return null
    }

    private fun ClassDescriptor.primaryParameters(): List<ValueParameterDescriptor>? {
        return constructors.firstOrNull { it.isPrimary }?.valueParameters
    }
}