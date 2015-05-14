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

import com.intellij.refactoring.PackageWrapper
import com.intellij.refactoring.MoveDestination
import org.jetbrains.kotlin.psi.JetFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import org.jetbrains.kotlin.name.FqName
import com.intellij.openapi.project.Project
import kotlin.properties.Delegates

public trait KotlinMoveTarget {
    val packageWrapper: PackageWrapper?
    fun getOrCreateTargetPsi(originalPsi: PsiElement): PsiFile?
    fun getTargetPsiIfExists(originalPsi: PsiElement): PsiFile?

    // Check possible errors and return corresponding message, or null if no errors are detected
    fun verify(file: PsiFile): String?
}

public class JetFileKotlinMoveTarget(val targetFile: JetFile): KotlinMoveTarget {
    override val packageWrapper: PackageWrapper? = targetFile.getPackageFqName().asString().let { packageName ->
        PackageWrapper(PsiManager.getInstance(targetFile.getProject()), packageName)
    }

    override fun getOrCreateTargetPsi(originalPsi: PsiElement) = targetFile

    override fun getTargetPsiIfExists(originalPsi: PsiElement) = targetFile

    // No additional verification is needed
    override fun verify(file: PsiFile): String? = null
}

public class DeferredJetFileKotlinMoveTarget(
        project: Project,
        val packageFqName: FqName,
        createFile: () -> JetFile?): KotlinMoveTarget {
    val createdFile: JetFile? by Delegates.lazy(createFile)

    override val packageWrapper: PackageWrapper = PackageWrapper(PsiManager.getInstance(project), packageFqName.asString())

    override fun getOrCreateTargetPsi(originalPsi: PsiElement) = createdFile

    override fun getTargetPsiIfExists(originalPsi: PsiElement) = null

    // No additional verification is needed
    override fun verify(file: PsiFile): String? = null
}
