/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.util

/*
import com.intellij.openapi.module.Module
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.PsiJavaModule
import com.intellij.psi.PsiRequiresStatement
import com.intellij.psi.search.FilenameIndex


fun findFirstPsiJavaModule(module: Module): PsiJavaModule? {
    val project = module.project

    val moduleInfoFiles = FilenameIndex.getFilesByName(project, PsiJavaModule.MODULE_INFO_FILE, module.moduleScope)
    return moduleInfoFiles
            .asSequence()
            .filterIsInstance<PsiJavaFile>()
            .map { it.moduleDeclaration }
            .firstOrNull { it != null }


    return null
}

fun findRequireDirective(module: PsiJavaModule, requiredName: String): PsiRequiresStatement? =
        module.requires.find { it.moduleName == requiredName }
*/