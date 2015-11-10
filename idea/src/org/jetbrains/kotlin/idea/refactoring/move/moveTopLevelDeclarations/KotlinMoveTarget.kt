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

package org.jetbrains.kotlin.idea.refactoring.move.moveTopLevelDeclarations

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.refactoring.PackageWrapper
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtFile
import java.util.HashMap

public interface KotlinMoveTarget {
    val packageWrapper: PackageWrapper?
    fun getOrCreateTargetPsi(originalPsi: PsiElement): PsiFile?
    fun getTargetPsiIfExists(originalPsi: PsiElement): PsiFile?

    // Check possible errors and return corresponding message, or null if no errors are detected
    fun verify(file: PsiFile): String?
}

public object EmptyKotlinMoveTarget: KotlinMoveTarget {
    override val packageWrapper: PackageWrapper? get() = null
    override fun getOrCreateTargetPsi(originalPsi: PsiElement) = null
    override fun getTargetPsiIfExists(originalPsi: PsiElement) = null
    override fun verify(file: PsiFile) = null
}

public class KotlinMoveTargetForExistingFile(val targetFile: KtFile): KotlinMoveTarget {
    override val packageWrapper: PackageWrapper? = targetFile.getPackageFqName().asString().let { packageName ->
        PackageWrapper(PsiManager.getInstance(targetFile.getProject()), packageName)
    }

    override fun getOrCreateTargetPsi(originalPsi: PsiElement) = targetFile

    override fun getTargetPsiIfExists(originalPsi: PsiElement) = targetFile

    // No additional verification is needed
    override fun verify(file: PsiFile): String? = null
}

public class KotlinMoveTargetForDeferredFile(
        project: Project,
        private val packageFqName: FqName,
        private val createFile: (KtFile) -> KtFile?): KotlinMoveTarget {
    private val createdFiles = HashMap<KtFile, KtFile?>()

    override val packageWrapper: PackageWrapper = PackageWrapper(PsiManager.getInstance(project), packageFqName.asString())

    override fun getOrCreateTargetPsi(originalPsi: PsiElement): PsiFile? {
        val originalFile = originalPsi.containingFile as? KtFile ?: return null
        return createdFiles.getOrPut(originalFile) { createFile(originalFile) }
    }

    override fun getTargetPsiIfExists(originalPsi: PsiElement) = null

    // No additional verification is needed
    override fun verify(file: PsiFile): String? = null
}
