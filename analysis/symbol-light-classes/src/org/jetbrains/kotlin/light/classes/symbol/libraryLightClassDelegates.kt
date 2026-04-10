/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.light.classes.symbol

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiField
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiTypes
import org.jetbrains.kotlin.analysis.decompiled.light.classes.DecompiledLightClassesFactory
import org.jetbrains.kotlin.analysis.decompiled.light.classes.origin.KotlinDeclarationInCompiledFileSearcher
import org.jetbrains.kotlin.analysis.decompiler.psi.file.KtClsFile
import org.jetbrains.kotlin.asJava.KtClsJavaBasedLightClass
import org.jetbrains.kotlin.fileClasses.javaFileFacadeFqName
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtFile

internal data class BinaryLightClassDelegate(
    val file: KtClsFile,
    val clsDelegate: PsiClass,
) {
    private val searcher: KotlinDeclarationInCompiledFileSearcher
        get() = KotlinDeclarationInCompiledFileSearcher.getInstance()

    fun findMethod(
        targetDeclaration: KtDeclaration?,
        parameterCount: Int,
        preferredName: String? = null,
        isGetter: Boolean? = null,
    ): PsiMethod? {
        if (targetDeclaration == null) return null

        val candidates = clsDelegate.methods.filter { method ->
            method.parameterList.parametersCount == parameterCount &&
                    (isGetter == null || (method.returnType != PsiTypes.voidType()) == isGetter) &&
                    matches(targetDeclaration, searcher.findDeclarationInCompiledFile(file, method))
        }

        return if (preferredName != null) {
            candidates.firstOrNull { it.name == preferredName } ?: candidates.firstOrNull()
        } else {
            candidates.firstOrNull()
        }
    }

    fun findField(
        targetDeclaration: KtDeclaration?,
        preferredName: String? = null,
    ): PsiField? {
        if (targetDeclaration == null) return null

        val candidates = clsDelegate.fields.filter { field ->
            matches(targetDeclaration, searcher.findDeclarationInCompiledFile(file, field))
        }

        return if (preferredName != null) {
            candidates.firstOrNull { it.name == preferredName } ?: candidates.firstOrNull()
        } else {
            candidates.firstOrNull()
        }
    }

    private fun matches(targetDeclaration: KtDeclaration, candidate: KtDeclaration?): Boolean {
        return candidate != null && (candidate == targetDeclaration || candidate.isEquivalentTo(targetDeclaration))
    }
}

internal fun createBinaryLightClassDelegate(
    project: Project,
    classOrObjectDeclaration: KtClassOrObject?,
): BinaryLightClassDelegate? {
    val decompiledDeclaration = classOrObjectDeclaration ?: return null
    val file = decompiledDeclaration.containingKtFile as? KtClsFile ?: return null
    val lightClass = DecompiledLightClassesFactory.getLightClassForDecompiledClassOrObject(decompiledDeclaration, project)
        as? KtClsJavaBasedLightClass ?: return null
    return BinaryLightClassDelegate(file, lightClass.clsDelegate)
}

internal fun createBinaryLightClassDelegate(
    project: Project,
    facadeClassFqName: FqName,
    files: Collection<KtFile>,
): BinaryLightClassDelegate? {
    val file = files.firstOrNull { it.javaFileFacadeFqName == facadeClassFqName } as? KtClsFile ?: return null
    val lightClass = DecompiledLightClassesFactory.createLightFacadeForDecompiledKotlinFile(project, facadeClassFqName, files.toList())
        as? KtClsJavaBasedLightClass ?: return null
    return BinaryLightClassDelegate(file, lightClass.clsDelegate)
}
