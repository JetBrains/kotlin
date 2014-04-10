/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.plugin.stubs

import com.intellij.psi.impl.PsiManagerImpl
import com.intellij.psi.PsiManager
import com.intellij.openapi.vfs.VirtualFileFilter
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.jet.plugin.JetFileType
import com.intellij.openapi.project.Project
import com.intellij.openapi.Disposable
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VfsUtilCore
import org.jetbrains.jet.InTextDirectivesUtils

object AstAccessControl {

    val ALLOW_AST_ACCESS_DIRECTIVE = "ALLOW_AST_ACCESS"

    fun prohibitAstAccessForKotlinFiles(project: Project, disposable: Disposable) {
        val manager = (PsiManager.getInstance(project) as PsiManagerImpl)
        val filter = VirtualFileFilter {
            file ->
            if (file!!.getFileType() != JetFileType.INSTANCE) {
                false
            }
            else {
                val text = VfsUtilCore.loadText(file)
                !InTextDirectivesUtils.isDirectiveDefined(text, ALLOW_AST_ACCESS_DIRECTIVE)
            }
        }
        manager.setAssertOnFileLoadingFilter(filter, disposable)
    }

}