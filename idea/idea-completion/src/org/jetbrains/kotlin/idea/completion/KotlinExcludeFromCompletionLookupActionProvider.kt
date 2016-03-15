/*
 * Copyright 2010-2015 JetBrains s.r.o.
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
package org.jetbrains.kotlin.idea.completion

import com.intellij.codeInsight.completion.ExcludeFromCompletionLookupActionProvider
import com.intellij.codeInsight.daemon.impl.actions.AddImportAction
import com.intellij.codeInsight.lookup.Lookup
import com.intellij.codeInsight.lookup.LookupActionProvider
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementAction
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.psi.util.PsiUtil
import com.intellij.util.Consumer
import org.jetbrains.kotlin.idea.core.completion.DeclarationLookupObject
import org.jetbrains.kotlin.idea.imports.importableFqName
import org.jetbrains.kotlin.name.FqName

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
        override fun performLookupAction(): LookupElementAction.Result {
            AddImportAction.excludeFromImport(project, exclude)
            return LookupElementAction.Result.HIDE_LOOKUP
        }
    }
}
