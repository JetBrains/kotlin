/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.providers.impl

import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileVisitor
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiManager
import com.intellij.util.indexing.FileContentImpl
import org.jetbrains.kotlin.analysis.decompiler.konan.K2KotlinNativeMetadataDecompiler
import org.jetbrains.kotlin.analysis.decompiler.konan.KlibMetaFileType
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtEnumEntry
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtObjectDeclaration
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.stubs.impl.KotlinClassStubImpl
import org.jetbrains.kotlin.psi.stubs.impl.KotlinFileStubImpl
import org.jetbrains.kotlin.psi.stubs.impl.KotlinObjectStubImpl
import org.jetbrains.kotlin.psi.stubs.impl.KotlinStubBaseImpl

internal fun klibMetaFiles(
    root: VirtualFile,
): Collection<VirtualFile> {
    return buildList {
        VfsUtilCore.visitChildrenRecursively(root, object : VirtualFileVisitor<Void>() {
            override fun visitFile(file: VirtualFile): Boolean {
                if (file.fileType == KlibMetaFileType) {
                    add(file)
                }
                return true
            }
        })
    }
}

internal fun buildStubByVirtualFile(
    file: VirtualFile,
): KotlinFileStubImpl? {
    val fileContent = FileContentImpl.createByFile(file)
    return K2KotlinNativeMetadataDecompiler().stubBuilder.buildFileStub(fileContent) as? KotlinFileStubImpl
}

internal fun buildPsiClassByKotlinClassStub(
    psiManager: PsiManager,
    ktFile: KtFile,
    ktStub: KotlinStubBaseImpl<*>,
): PsiClass? {
    return when (ktStub) {
        is KotlinClassStubImpl -> {
            val ktClass = ktStub.psi
            KtFakeLightClassForKlib(ktClass, psiManager, ktClass.name.orAnonymous(ktClass), ktFile)
        }
        is KotlinObjectStubImpl -> {
            val ktObject = ktStub.psi
            KtFakeLightClassForKlib(ktObject, psiManager, ktObject.name.orAnonymous(ktObject), ktFile)
        }
        else -> null
    }
}

internal fun String?.orAnonymous(ktDeclaration: KtNamedDeclaration): String {
    if (this != null) return this
    return when (ktDeclaration) {
        is KtClass -> "<anonymous class>"
        is KtObjectDeclaration -> "<anonymous object>"
        is KtEnumEntry -> "<anonymous enum entry>"
        is KtNamedFunction -> "<anonymous function>"
        is KtProperty -> "<anonymous property>"
        else -> "<unknown ${ktDeclaration::class}>"
    }
}
