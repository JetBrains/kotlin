/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.konan

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.SingleRootFileViewProvider
import com.intellij.psi.impl.PsiFileFactoryImpl
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.stubs.PsiFileStub
import com.intellij.testFramework.LightVirtualFile
import org.jetbrains.kotlin.analyzer.ModuleInfo
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.context.ModuleContext
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.idea.caches.project.LibraryInfo
import org.jetbrains.kotlin.idea.decompiler.textBuilder.LoggingErrorReporter
import org.jetbrains.kotlin.konan.library.KonanLibrary
import org.jetbrains.kotlin.konan.library.libraryResolver
import org.jetbrains.kotlin.konan.utils.KonanFactories.DefaultResolvedDescriptorsFactory
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes
import org.jetbrains.kotlin.resolve.konan.platform.KonanPlatform
import org.jetbrains.kotlin.resolve.lazy.declarations.DeclarationProviderFactoryService
import org.jetbrains.kotlin.serialization.konan.KonanResolvedModuleDescriptors
import org.jetbrains.kotlin.storage.StorageManager
import java.io.File.separatorChar

const val KONAN_CURRENT_ABI_VERSION = 1

fun createFileStub(project: Project, text: String): PsiFileStub<*> {
    val virtualFile = LightVirtualFile("dummy.kt", KotlinFileType.INSTANCE, text)
    virtualFile.language = KotlinLanguage.INSTANCE
    SingleRootFileViewProvider.doNotCheckFileSizeLimit(virtualFile)

    val psiFileFactory = PsiFileFactory.getInstance(project) as PsiFileFactoryImpl
    val file = psiFileFactory.trySetupPsiForFile(virtualFile, KotlinLanguage.INSTANCE, false, false)!!
    return KtStubElementTypes.FILE.builder.buildStubTree(file) as PsiFileStub<*>
}

fun createLoggingErrorReporter(log: Logger) = LoggingErrorReporter(log)

fun <M : ModuleInfo> createDeclarationProviderFactory(
    project: Project,
    moduleContext: ModuleContext,
    syntheticFiles: Collection<KtFile>,
    moduleInfo: M,
    globalSearchScope: GlobalSearchScope?
) = DeclarationProviderFactoryService.createDeclarationProviderFactory(
    project,
    moduleContext.storageManager,
    syntheticFiles,
    globalSearchScope!!,
    moduleInfo
)

fun ModuleInfo.createResolvedModuleDescriptors(
    project: Project,
    storageManager: StorageManager,
    builtIns: KotlinBuiltIns,
    languageVersionSettings: LanguageVersionSettings
): KonanResolvedModuleDescriptors {

    // This is to preserve "capabilities" from the original IntelliJ LibraryInfo:
    val libraryMap =
        this.dependencies().filterIsInstance<LibraryInfo>().flatMap { dependency ->
            if (dependency.platform == KonanPlatform) {
                dependency.getLibraryRoots().map { file ->
                    file to dependency
                }
            } else {
                emptyList()
            }
        }.toMap()

    val resolvedLibraries =
        KonanPluginSearchPathResolver(project).libraryResolver(KONAN_CURRENT_ABI_VERSION)
            .resolveWithDependencies(libraryMap.keys.toList(), true, true)

    return DefaultResolvedDescriptorsFactory.createResolved(
        resolvedLibraries,
        storageManager,
        builtIns,
        languageVersionSettings,
        null
        // This is to preserve "capabilities" from the original IntelliJ LibraryInfo:
    ) { konanLibrary -> libraryMap[konanLibrary.libraryFile.path]?.capabilities ?: emptyMap() }
}

private val KonanLibrary.pureName
    get() = libraryName.substringAfterLast(separatorChar)
