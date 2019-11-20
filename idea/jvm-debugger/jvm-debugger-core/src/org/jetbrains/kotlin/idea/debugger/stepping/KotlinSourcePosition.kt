/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.debugger.stepping

import com.intellij.debugger.SourcePosition
import org.jetbrains.kotlin.idea.core.util.getLineNumber
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.parents

data class KotlinSourcePosition(
    val file: KtFile, val declaration: KtDeclaration,
    val linesRange: IntRange, val sourcePosition: SourcePosition
) {
    companion object {
        fun create(sourcePosition: SourcePosition): KotlinSourcePosition? {
            val file = sourcePosition.file as? KtFile ?: return null
            if (sourcePosition.line < 0) return null

            val elementAt = sourcePosition.elementAt ?: return null

            val containingDeclaration = elementAt.parents
                .filterIsInstance<KtDeclaration>()
                .filter { it is KtFunction || it is KtProperty || it is KtClassInitializer }
                .firstOrNull { !KtPsiUtil.isLocal(it) }
                ?: return null

            val startLineNumber = containingDeclaration.getLineNumber(true) + 1
            val endLineNumber = containingDeclaration.getLineNumber(false) + 1
            if (startLineNumber > endLineNumber) return null

            val linesRange = startLineNumber..endLineNumber

            return KotlinSourcePosition(file, containingDeclaration, linesRange, sourcePosition)
        }
    }
}
