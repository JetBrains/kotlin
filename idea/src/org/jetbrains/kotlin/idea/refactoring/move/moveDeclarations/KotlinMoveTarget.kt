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

package org.jetbrains.kotlin.idea.refactoring.move.moveDeclarations

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.idea.core.getOrCreateCompanionObject
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.utils.getOrPutNullable
import java.util.*

interface KotlinMoveTarget {
    val targetContainerFqName: FqName?
    val targetFile: VirtualFile?

    fun getOrCreateTargetPsi(originalPsi: PsiElement): KtElement?
    fun getTargetPsiIfExists(originalPsi: PsiElement): KtElement?

    // Check possible errors and return corresponding message, or null if no errors are detected
    fun verify(file: PsiFile): String?
}

interface KotlinDirectoryBasedMoveTarget : KotlinMoveTarget {
    val directory: PsiDirectory?
}

object EmptyKotlinMoveTarget: KotlinMoveTarget {
    override val targetContainerFqName = null
    override val targetFile = null

    override fun getOrCreateTargetPsi(originalPsi: PsiElement) = null
    override fun getTargetPsiIfExists(originalPsi: PsiElement) = null
    override fun verify(file: PsiFile) = null
}

class KotlinMoveTargetForExistingElement(val targetElement: KtElement): KotlinMoveTarget {
    override val targetContainerFqName = targetElement.containingKtFile.packageFqName

    override val targetFile: VirtualFile? = targetElement.containingKtFile.virtualFile

    override fun getOrCreateTargetPsi(originalPsi: PsiElement) = targetElement

    override fun getTargetPsiIfExists(originalPsi: PsiElement) = targetElement

    // No additional verification is needed
    override fun verify(file: PsiFile): String? = null
}

class KotlinMoveTargetForCompanion(val targetClass: KtClass): KotlinMoveTarget {
    override val targetContainerFqName = targetClass.companionObjects.firstOrNull()?.fqName
                                         ?: targetClass.fqName!!.child(SpecialNames.DEFAULT_NAME_FOR_COMPANION_OBJECT)

    override val targetFile: VirtualFile? = targetClass.containingKtFile.virtualFile

    override fun getOrCreateTargetPsi(originalPsi: PsiElement) = targetClass.getOrCreateCompanionObject()

    override fun getTargetPsiIfExists(originalPsi: PsiElement) = targetClass.companionObjects.firstOrNull()

    // No additional verification is needed
    override fun verify(file: PsiFile): String? = null
}

class KotlinMoveTargetForDeferredFile(
        override val targetContainerFqName: FqName,
        override val directory: PsiDirectory?,
        override val targetFile: VirtualFile? = directory?.virtualFile,
        private val createFile: (KtFile) -> KtFile?
): KotlinDirectoryBasedMoveTarget {
    private val createdFiles = HashMap<KtFile, KtFile?>()

    override fun getOrCreateTargetPsi(originalPsi: PsiElement): KtElement? {
        val originalFile = originalPsi.containingFile as? KtFile ?: return null
        return createdFiles.getOrPutNullable(originalFile) { createFile(originalFile) }
    }

    override fun getTargetPsiIfExists(originalPsi: PsiElement) = null

    // No additional verification is needed
    override fun verify(file: PsiFile): String? = null
}
