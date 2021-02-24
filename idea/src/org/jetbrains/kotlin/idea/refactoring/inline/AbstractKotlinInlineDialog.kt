/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.refactoring.inline

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.ElementDescriptionUtil
import com.intellij.refactoring.inline.InlineOptionsDialog
import com.intellij.usageView.UsageViewTypeLocation
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.references.KtSimpleNameReference
import org.jetbrains.kotlin.psi.KtCallableDeclaration

abstract class AbstractKotlinInlineDialog(
    protected val callable: KtCallableDeclaration,
    protected val reference: KtSimpleNameReference?,
    project: Project = callable.project
) : InlineOptionsDialog(project, true, callable) {

    // NB: can be -1 in case of too expensive search!
    protected val occurrencesNumber = initOccurrencesNumber(callable)

    private val occurrencesString
        get() = if (occurrencesNumber >= 0) {
            "" + occurrencesNumber + " " + StringUtil.pluralize("occurrence", occurrencesNumber)
        } else null

    private val kind: String = ElementDescriptionUtil.getElementDescription(callable, UsageViewTypeLocation.INSTANCE)

    private val refactoringName get() = KotlinBundle.message("text.inline.0", StringUtil.capitalizeWords(kind, true))

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

    private fun getInlineText(verb: String) = KotlinBundle.message(
        "text.inline.all.references.and.verb.0.the.kind.1.occurrences.2",
        verb,
        kind,
        (occurrencesString?.let { "($it)" } ?: "")
    )

    override fun getInlineAllText() = getInlineText(KotlinBundle.message("text.remove"))

    override fun getKeepTheDeclarationText(): String? =
        // With non-writable callable refactoring does not work anyway (for both property or function)
        if (callable.isWritable && (occurrencesNumber > 1 || !myInvokedOnReference)) {
            getInlineText(KotlinBundle.message("text.keep"))
        } else {
            null
        }

    override fun getInlineThisText() = KotlinBundle.message("text.inline.this.reference.and.keep.the.0", kind)
}