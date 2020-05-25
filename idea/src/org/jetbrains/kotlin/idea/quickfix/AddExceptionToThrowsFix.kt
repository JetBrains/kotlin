/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.quickfix

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.core.ShortenReferences
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.resolve.konan.diagnostics.ErrorsNative.MISSING_EXCEPTION_IN_THROWS_ON_SUSPEND

class AddExceptionToThrowsFix(
    element: KtAnnotationEntry,
    private val argumentClassFqName: FqName
) : KotlinQuickFixAction<KtAnnotationEntry>(element) {

    override fun getText(): String = KotlinBundle.message(
        "fix.add.exception.to.throws",
        "${argumentClassFqName.shortName().asString()}::class"
    )

    override fun getFamilyName() = this.text

    override fun invoke(project: Project, editor: Editor?, file: KtFile) {
        val annotationEntry = element ?: return
        val psiFactory = KtPsiFactory(annotationEntry)

        val argumentText = "${argumentClassFqName.asString()}::class"
        val added = annotationEntry.valueArgumentList?.addArgument(psiFactory.createArgument(argumentText))

        if (added != null) ShortenReferences.DEFAULT.process(added)
    }

    companion object : KotlinSingleIntentionActionFactory() {
        override fun createAction(diagnostic: Diagnostic): IntentionAction? {
            val annotationEntry = diagnostic.psiElement as? KtAnnotationEntry ?: return null
            val valueArgumentsList = annotationEntry.valueArgumentList
                ?: return null // Note: shouldn't reach here, frontend doesn't report MISSING_EXCEPTION_IN_THROWS_ON_SUSPEND in this case.

            if (valueArgumentsList.arguments.firstOrNull()?.getArgumentName() != null) {
                // E.g. @Throws(exceptionClasses = [Foo::class]).
                // Unsupported at the moment:
                return null
                // Note: AddThrowsAnnotationIntention has the code to support this case, but a refactoring is required to use it.
            }

            val missingExceptionFqName = when (diagnostic.factory) {
                MISSING_EXCEPTION_IN_THROWS_ON_SUSPEND -> MISSING_EXCEPTION_IN_THROWS_ON_SUSPEND.cast(diagnostic).a
                else -> return null
            }

            return AddExceptionToThrowsFix(annotationEntry, missingExceptionFqName)
        }
    }
}
