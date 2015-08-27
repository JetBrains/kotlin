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

package org.jetbrains.kotlin.idea.debugger.breakpoints

import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.xdebugger.XDebuggerUtil
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.idea.JetFileType
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.psi.JetElement
import org.jetbrains.kotlin.psi.JetParameter
import org.jetbrains.kotlin.psi.JetProperty
import org.jetbrains.kotlin.resolve.BindingContext

fun canPutAt(file: VirtualFile, line: Int, project: Project, breakpointTypeClass: Class<*>): Boolean {
    val psiFile = PsiManager.getInstance(project).findFile(file)

    if (psiFile == null || psiFile.getVirtualFile().getFileType() != JetFileType.INSTANCE) {
        return false
    }

    val document = FileDocumentManager.getInstance().getDocument(file) ?: return false

    var canPutAt = false
    XDebuggerUtil.getInstance().iterateLine(project, document, line, fun (el: PsiElement): Boolean {
        // avoid comments
        if (el is PsiWhiteSpace || PsiTreeUtil.getParentOfType(el, javaClass<PsiComment>(), false) != null) {
            return true
        }

        var element = el
        var parent = element.getParent()
        while (parent != null) {
            val offset = parent.getTextOffset()
            if (offset >= 0 && document.getLineNumber(offset) != line) break

            element = parent
            parent = element.getParent()
        }

        if (element is JetProperty || element is JetParameter) {
            val bindingContext = (element as JetElement).analyze()
            var descriptor = bindingContext.get(BindingContext.DECLARATION_TO_DESCRIPTOR, element)
            if (descriptor is ValueParameterDescriptor) {
                descriptor = bindingContext.get(BindingContext.VALUE_PARAMETER_AS_PROPERTY, descriptor)
            }
            if (descriptor is PropertyDescriptor) {
                canPutAt = true
            }
            return false
        }

        return true
    })

    return canPutAt
}


