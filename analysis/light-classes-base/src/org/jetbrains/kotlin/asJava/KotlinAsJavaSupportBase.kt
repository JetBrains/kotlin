/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.asJava

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.ModificationTracker
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import org.jetbrains.kotlin.analyzer.KotlinModificationTrackerService
import org.jetbrains.kotlin.asJava.classes.KtLightClassForFacade
import org.jetbrains.kotlin.fileClasses.isJvmMultifileClassFile
import org.jetbrains.kotlin.fileClasses.javaFileFacadeFqName
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtFile

abstract class KotlinAsJavaSupportBase<TModule>(protected val project: Project) : KotlinAsJavaSupport() {
    fun createLightFacade(file: KtFile): KtLightClassForFacade? {
        if (file.isScript()) return null

        val module = file.findModule().takeIf { facadeIsApplicable(it, file) } ?: return null
        val facadeFqName = file.javaFileFacadeFqName
        val facadeFiles = if (file.isJvmMultifileClassFile && !file.isCompiled) {
            findFilesForFacade(facadeFqName, module.contentSearchScope).filter(KtFile::isJvmMultifileClassFile)
        } else {
            listOf(file)
        }

        if (facadeFiles.none(KtFile::hasTopLevelCallables)) return null
        return when {
            facadeFiles.none(KtFile::isCompiled) -> createInstanceOfLightFacade(facadeFqName, facadeFiles, module)
            facadeFiles.all(KtFile::isCompiled) -> createInstanceOfDecompiledLightFacade(facadeFqName, facadeFiles, module)
            else -> error("Source and compiled files are mixed: $facadeFiles")
        }
    }

    abstract fun KtFile.findModule(): TModule
    abstract fun facadeIsApplicable(module: TModule, file: KtFile): Boolean
    abstract val TModule.contentSearchScope: GlobalSearchScope

    abstract fun createInstanceOfLightFacade(facadeFqName: FqName, files: List<KtFile>, module: TModule): KtLightClassForFacade
    abstract fun createInstanceOfDecompiledLightFacade(facadeFqName: FqName, files: List<KtFile>, module: TModule): KtLightClassForFacade?

    override fun createFacadeForSyntheticFile(file: KtFile): KtLightClassForFacade {
        return createInstanceOfLightFacade(file.javaFileFacadeFqName, listOf(file), file.findModule())
    }

    override fun getLightFacade(file: KtFile): KtLightClassForFacade? = CachedValuesManager.getCachedValue(file) {
        CachedValueProvider.Result.create(
            createLightFacade(file),
            tracker(file),
        )
    }

    open fun tracker(file: KtFile): ModificationTracker {
        return KotlinModificationTrackerService.getInstance(project).outOfBlockModificationTracker
    }

    override fun getFacadeClassesInPackage(packageFqName: FqName, scope: GlobalSearchScope): Collection<KtLightClassForFacade> {
        return findFilesForFacadeByPackage(packageFqName, scope).toFacadeClasses()
    }

    override fun getFacadeClasses(facadeFqName: FqName, scope: GlobalSearchScope): Collection<KtLightClassForFacade> {
        return findFilesForFacade(facadeFqName, scope).toFacadeClasses()
    }

    override fun getFacadeNames(packageFqName: FqName, scope: GlobalSearchScope): Collection<String> {
        return findFilesForFacadeByPackage(packageFqName, scope).mapNotNullTo(mutableSetOf()) { file ->
            file.takeIf { facadeIsApplicable(it.findModule(), file) }?.javaFileFacadeFqName?.shortName()?.asString()
        }.toSet()
    }

    private fun Collection<KtFile>.toFacadeClasses(): List<KtLightClassForFacade> = groupBy {
        FacadeKey(it.javaFileFacadeFqName, it.isJvmMultifileClassFile, it.findModule())
    }.mapNotNull { (key, files) ->
        files.firstOrNull { facadeIsApplicable(key.module, it) }?.let(::getLightFacade)
    }

    private data class FacadeKey<TModule>(val fqName: FqName, val isMultifile: Boolean, val module: TModule)
}

