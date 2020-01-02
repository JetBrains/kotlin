/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.idea.completion

import com.intellij.codeInsight.daemon.impl.actions.AddImportAction
import com.intellij.codeInsight.lookup.Lookup
import com.intellij.codeInsight.lookup.LookupActionProvider
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementAction
import com.intellij.openapi.project.Project
import com.intellij.util.Consumer
import org.jetbrains.kotlin.idea.core.completion.DeclarationLookupObject

class KotlinExcludeFromCompletionLookupActionProvider : LookupActionProvider {
    override fun fillActions(element: LookupElement, lookup: Lookup, consumer: Consumer<LookupElementAction>) {
        val lookupObject = element.`object` as? DeclarationLookupObject ?: return

        val project = lookup.psiFile?.project ?: return

        lookupObject.importableFqName?.let {
            addExcludes(consumer, project, it.asString())
            return
        }
    }

    private fun addExcludes(consumer: Consumer<LookupElementAction>, project: Project, fqName: String) {
        for (s in AddImportAction.getAllExcludableStrings(fqName)) {
            consumer.consume(ExcludeFromCompletionAction(project, s))
        }
    }

    private class ExcludeFromCompletionAction(
        private val project: Project,
        private val exclude: String
    ) : LookupElementAction(null, "Exclude '$exclude' from completion") {
        override fun performLookupAction(): Result {
            AddImportAction.excludeFromImport(project, exclude)
            return Result.HIDE_LOOKUP
        }
    }
}
