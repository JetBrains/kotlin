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

package org.jetbrains.kotlin.idea.refactoring.inline

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.ElementDescriptionUtil
import com.intellij.refactoring.inline.InlineOptionsDialog
import com.intellij.usageView.UsageViewTypeLocation
import org.jetbrains.kotlin.idea.references.KtSimpleNameReference
import org.jetbrains.kotlin.psi.KtCallableDeclaration

abstract class AbstractKotlinInlineDialog(
        protected val callable: KtCallableDeclaration,
        protected val reference: KtSimpleNameReference?,
        project: Project = callable.project
) : InlineOptionsDialog(project, true, callable)  {

    // NB: can be -1 in case of too expensive search!
    protected val occurrencesNumber = initOccurrencesNumber(callable)

    private val occurrencesString get() = if (occurrencesNumber >= 0) {
        "" + occurrencesNumber + " " + StringUtil.pluralize("occurrence", occurrencesNumber)
    } else null

    private val kind: String = ElementDescriptionUtil.getElementDescription(callable, UsageViewTypeLocation.INSTANCE)

    private val refactoringName get() = "Inline ${StringUtil.capitalizeWords(kind, true)}"

    init {
        myInvokedOnReference = reference != null
        title = refactoringName
    }

    // If this is true, "inline all & remove" is disabled,
    // "inline all and keep" is disabled on references and enabled on original declaration.
    // Independent on this, "inline this only" is enabled on references and disabled on original declaration
    // If this is false, "inline all & remove" is dependent on next flag (allowInlineAll),
    // "inline all and keep" is enabled
    override fun canInlineThisOnly() = false

    // If this is false, "inline all & remove" is disabled
    // If this is true, it can be enabled if 'canInlineThisOnly' is false (see above)
    override fun allowInlineAll() = true

    override fun getBorderTitle() = refactoringName

    override fun getNameLabelText(): String {
        val occurrencesString = occurrencesString?.let { " - $it" } ?: ""
        return "${kind.capitalize()} ${callable.nameAsSafeName} $occurrencesString"
    }

    private fun getInlineText(verb: String) =
            "Inline all references and $verb the $kind " + (occurrencesString?.let { "($it)" } ?: "")

    override fun getInlineAllText() =
            getInlineText("remove")

    override fun getKeepTheDeclarationText(): String? =
            // With non-writable callable refactoring does not work anyway (for both property or function)
            if (callable.isWritable && (occurrencesNumber > 1 || !myInvokedOnReference)) {
                getInlineText("keep")
            }
            else {
                null
            }

    override fun getInlineThisText() = "Inline this reference and keep the $kind"
}