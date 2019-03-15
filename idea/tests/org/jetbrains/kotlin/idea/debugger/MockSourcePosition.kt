/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.debugger

import com.intellij.debugger.SourcePosition
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile

class MockSourcePosition(
    val _file: PsiFile? = null,
    val _elementAt: PsiElement? = null,
    val _line: Int? = null,
    val _offset: Int? = null,
    val _editor: Editor? = null
): SourcePosition() {
    override fun getFile() = _file ?: throw UnsupportedOperationException("Parameter file isn't set for MockSourcePosition")
    override fun getElementAt() = _elementAt ?: throw UnsupportedOperationException("Parameter elementAt isn't set for MockSourcePosition")
    override fun getLine() = _line ?: throw UnsupportedOperationException("Parameter line isn't set for MockSourcePosition")
    override fun getOffset() = _offset ?: throw UnsupportedOperationException("Parameter offset isn't set for MockSourcePosition")
    override fun openEditor(requestFocus: Boolean) = _editor ?: throw UnsupportedOperationException("Parameter editor isn't set for MockSourcePosition")

    override fun navigate(requestFocus: Boolean) = throw UnsupportedOperationException("navigate() isn't supported for MockSourcePosition")
    override fun canNavigate() = throw UnsupportedOperationException("canNavigate() isn't supported for MockSourcePosition")
    override fun canNavigateToSource() = throw UnsupportedOperationException("canNavigateToSource() isn't supported for MockSourcePosition")
}
