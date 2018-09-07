/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ide.konan

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.SingleRootFileViewProvider
import com.intellij.psi.impl.PsiFileFactoryImpl
import com.intellij.psi.stubs.PsiFileStub
import com.intellij.testFramework.LightVirtualFile
import org.jetbrains.kotlin.analyzer.ModuleInfo
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.idea.caches.project.LibraryInfo
import org.jetbrains.kotlin.idea.decompiler.textBuilder.LoggingErrorReporter
import org.jetbrains.kotlin.konan.library.libraryResolver
import org.jetbrains.kotlin.konan.util.KonanFactories.DefaultResolvedDescriptorsFactory
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes
import org.jetbrains.kotlin.resolve.konan.platform.KonanPlatform
import org.jetbrains.kotlin.serialization.konan.KonanResolvedModuleDescriptors
import org.jetbrains.kotlin.storage.StorageManager

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
                dependency.getLibraryRoots().map { libraryRoot ->
                    libraryRoot to dependency
                }
            } else {
                emptyList()
            }
        }.toMap()

    val libraryPaths = libraryMap.keys.toList()

    val resolvedLibraries = KotlinNativePluginSearchPathResolver(libraryPaths)
        .libraryResolver(KONAN_CURRENT_ABI_VERSION)
        .resolveWithDependencies(libraryPaths)

    return DefaultResolvedDescriptorsFactory.createResolved(
        resolvedLibraries,
        storageManager,
        builtIns,
        languageVersionSettings,
        null
        // This is to preserve "capabilities" from the original IntelliJ LibraryInfo:
    ) { konanLibrary -> libraryMap[konanLibrary.libraryFile.path]?.capabilities ?: emptyMap() }
}
