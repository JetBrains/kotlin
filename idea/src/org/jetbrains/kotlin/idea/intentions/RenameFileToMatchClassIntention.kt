/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.intentions

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.io.FileUtilRt
import com.intellij.refactoring.RefactoringSettings
import com.intellij.refactoring.rename.RenameProcessor
import org.jetbrains.kotlin.psi.KtClassOrObject

class RenameFileToMatchClassIntention :
    SelfTargetingRangeIntention<KtClassOrObject>(KtClassOrObject::class.java, "", "Rename file to match top-level class name") {
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
            RefactoringSettings.getInstance().RENAME_SEARCH_FOR_TEXT_FOR_FILE
        ).run()
    }
}