/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.caches.resolve

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiManager
import org.jetbrains.kotlin.asJava.KotlinAsJavaSupport
import org.jetbrains.kotlin.asJava.classes.KtLightClassForFacade
import org.jetbrains.kotlin.asJava.toLightClass
import org.jetbrains.kotlin.fileClasses.JvmFileClassUtil
import org.jetbrains.kotlin.fileClasses.javaFileFacadeFqName
import org.jetbrains.kotlin.idea.caches.project.getModuleInfo
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtFileClassProvider
import org.jetbrains.kotlin.psi.analysisContext

class KtFileClassProviderImpl(val kotlinAsJavaSupport: KotlinAsJavaSupport) :
    KtFileClassProvider {
    override fun getFileClasses(file: KtFile): Array<PsiClass> {
        // TODO We don't currently support finding light classes for scripts
        if (file.isCompiled || file.isScript()) {
            return PsiClass.EMPTY_ARRAY
        }

        val result = arrayListOf<PsiClass>()
        file.declarations.filterIsInstance<KtClassOrObject>().map { it.toLightClass() }.filterNotNullTo(result)

        val moduleInfo = file.getModuleInfoPreferringJvmPlatform()
        val jvmClassInfo = JvmFileClassUtil.getFileClassInfoNoResolve(file)
        val fileClassFqName = file.javaFileFacadeFqName

        val facadeClasses = when {
            file.analysisContext != null && file.hasTopLevelCallables() ->
                listOf(
                    KtLightClassForFacade.createForSyntheticFile(
                        PsiManager.getInstance(
                            file.project
                        ), fileClassFqName, file
                    )
                )

            jvmClassInfo.withJvmMultifileClass ->
                kotlinAsJavaSupport.getFacadeClasses(fileClassFqName, moduleInfo.contentScope())

            file.hasTopLevelCallables() ->
                (kotlinAsJavaSupport as IDEKotlinAsJavaSupport).createLightClassForFileFacade(
                    fileClassFqName, listOf(file), moduleInfo
                )

            else -> emptyList<PsiClass>()
        }

        facadeClasses.filterTo(result) {
            it is KtLightClassForFacade && file in it.files
        }

        return result.toTypedArray()
    }
}