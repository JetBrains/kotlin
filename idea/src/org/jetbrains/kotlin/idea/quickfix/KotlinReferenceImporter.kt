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

package org.jetbrains.kotlin.idea.quickfix

import com.intellij.codeInsight.daemon.ReferenceImporter
import com.intellij.codeInsight.daemon.impl.DaemonListeners
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.idea.actions.createSingleImportAction
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.caches.resolve.resolveImportReference
import org.jetbrains.kotlin.idea.codeInsight.KotlinCodeInsightSettings
import org.jetbrains.kotlin.idea.core.targetDescriptors
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtSimpleNameExpression
import org.jetbrains.kotlin.psi.psiUtil.collectDescendantsOfType
import org.jetbrains.kotlin.psi.psiUtil.elementsInRange
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode

class KotlinReferenceImporter : ReferenceImporter {
    override fun autoImportReferenceAtCursor(editor: Editor, file: PsiFile): Boolean {
        if (file !is KtFile) return false

        fun hasUnresolvedImportWhichCanImport(name: String): Boolean {
            return file.importDirectives.any {
                it.targetDescriptors().isEmpty() && (it.isAllUnder || it.importPath?.importedName?.asString() == name)
            }
        }

        fun KtSimpleNameExpression.autoImport(): Boolean {
            if (!KotlinCodeInsightSettings.getInstance().addUnambiguousImportsOnTheFly) return false
            if (!DaemonListeners.canChangeFileSilently(file)) return false
            if (hasUnresolvedImportWhichCanImport(getReferencedName())) return false

            val bindingContext = analyze(BodyResolveMode.PARTIAL)
            if (mainReference.resolveToDescriptors(bindingContext).isNotEmpty()) return false

            val suggestions = ImportFix(this).collectSuggestions()
            if (suggestions.size != 1) return false
            val descriptors = file.resolveImportReference(suggestions.single())

            // we do not auto-import nested classes because this will probably add qualification into the text and this will confuse the user
            if (descriptors.any { it is ClassDescriptor && it.containingDeclaration is ClassDescriptor }) return false

            var result = false
            CommandProcessor.getInstance().runUndoTransparentAction {
                result = createSingleImportAction(project, editor, this, suggestions).execute()
            }
            return result
        }

        val caretOffset = editor.caretModel.offset
        val document = editor.document
        val lineNumber = document.getLineNumber(caretOffset)
        val startOffset = document.getLineStartOffset(lineNumber)
        val endOffset = document.getLineEndOffset(lineNumber)

        return file.elementsInRange(TextRange(startOffset, endOffset))
            .flatMap { it.collectDescendantsOfType<KtSimpleNameExpression>() }
            .any { it.endOffset != caretOffset && it.autoImport() }
    }
}