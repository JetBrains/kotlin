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

package org.jetbrains.kotlin.idea.intentions

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.io.FileUtilRt
import com.intellij.refactoring.RefactoringSettings
import com.intellij.refactoring.rename.RenameProcessor
import org.jetbrains.kotlin.psi.KtClassOrObject

class RenameFileToMatchClassIntention : SelfTargetingRangeIntention<KtClassOrObject>(KtClassOrObject::class.java, "", "Rename file to match top-level class name") {
    override fun applicabilityRange(element: KtClassOrObject): TextRange? {
        if (!element.isTopLevel()) return null
        val fileName = element.containingKtFile.name
        if (FileUtil.getNameWithoutExtension(fileName) == element.name) return null
        text = "Rename file to ${element.name}.${FileUtilRt.getExtension(fileName)}"
        return element.nameIdentifier?.textRange
    }

    override fun startInWriteAction() = false

    override fun applyTo(element: KtClassOrObject, editor: Editor?) {
        val file = element.containingKtFile
        val extension = FileUtilRt.getExtension(file.name)
        RenameProcessor(
                file.project,
                file,
                "${element.name}.$extension",
                RefactoringSettings.getInstance().RENAME_SEARCH_IN_COMMENTS_FOR_FILE,
                RefactoringSettings.getInstance().RENAME_SEARCH_FOR_TEXT_FOR_FILE).run()
    }
}