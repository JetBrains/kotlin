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
