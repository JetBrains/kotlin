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

package org.jetbrains.kotlin.idea.refactoring.rename

import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.SearchScope
import com.intellij.refactoring.rename.RenamePsiFileProcessor
import org.jetbrains.kotlin.asJava.classes.KtLightClass
import org.jetbrains.kotlin.fileClasses.JvmFileClassUtil
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.idea.util.ProjectRootsUtil
import org.jetbrains.kotlin.load.kotlin.PackagePartClassUtils
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.idea.statistics.KotlinEventTrigger
import org.jetbrains.kotlin.idea.statistics.KotlinStatisticsTrigger

class RenameKotlinFileProcessor : RenamePsiFileProcessor() {
    class FileRenamingPsiClassWrapper(
        private val psiClass: KtLightClass,
        private val file: KtFile
    ) : KtLightClass by psiClass {
        override fun isValid() = file.isValid
    }

    override fun canProcessElement(element: PsiElement) =
        element is KtFile && ProjectRootsUtil.isInProjectSource(element, includeScriptsOutsideSourceRoots = true)

    override fun prepareRenaming(element: PsiElement,
                                 newName: String,
                                 allRenames: MutableMap<PsiElement, String>,
                                 scope: SearchScope) {
        val jetFile = element as? KtFile ?: return
        if (FileTypeManager.getInstance().getFileTypeByFileName(newName) != KotlinFileType.INSTANCE) {
            return
        }

        val module = ModuleUtilCore.findModuleForPsiElement(element) ?: return

        val fileInfo = JvmFileClassUtil.getFileClassInfoNoResolve(jetFile)
        if (!fileInfo.withJvmName) {
            val facadeFqName = fileInfo.facadeClassFqName
            val project = jetFile.project
            val facadeClass = JavaPsiFacade.getInstance(project)
                .findClass(facadeFqName.asString(), GlobalSearchScope.moduleScope(module)) as? KtLightClass
            if (facadeClass != null) {
                allRenames[FileRenamingPsiClassWrapper(facadeClass, jetFile)] = PackagePartClassUtils.getFilePartShortName(newName)
            }
        }
    }

    override fun findReferences(element: PsiElement): MutableCollection<PsiReference> {
        return super.findReferences(element).also {
            KotlinStatisticsTrigger.trigger(KotlinEventTrigger.KotlinIdeRefactoringTrigger, this::class.java.name)
        }
    }
}
