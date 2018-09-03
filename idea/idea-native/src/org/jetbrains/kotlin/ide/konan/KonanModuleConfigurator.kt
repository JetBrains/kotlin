/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ide.konan

import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.roots.libraries.DummyLibraryProperties
import com.intellij.openapi.roots.libraries.Library
import com.intellij.openapi.roots.libraries.LibraryType
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.konan.analyser.index.KonanMetaFileIndex
import org.jetbrains.kotlin.idea.configuration.KotlinWithLibraryConfigurator
import org.jetbrains.kotlin.idea.configuration.LibraryKindSearchScope
import org.jetbrains.kotlin.idea.configuration.hasKotlinFilesOnlyInTests
import org.jetbrains.kotlin.idea.util.application.runReadAction
import org.jetbrains.kotlin.idea.util.runWithAlternativeResolveEnabled
import org.jetbrains.kotlin.idea.versions.LibraryJarDescriptor
import org.jetbrains.kotlin.idea.vfilefinder.hasSomethingInPackage
import org.jetbrains.kotlin.konan.library.KONAN_STDLIB_NAME
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.konan.platform.KonanPlatform

open class KonanModuleConfigurator : KotlinWithLibraryConfigurator() {

    override val name: String get() = NAME

    override val targetPlatform get() = KonanPlatform

    override val presentableText get() = PRESENTABLE_TEXT

    override fun isConfigured(module: Module) = hasKonanRuntimeInScope(module)

    override val libraryName get() = KonanStandardLibraryDescription.LIBRARY_NAME

    override val dialogTitle get() = KonanStandardLibraryDescription.DIALOG_TITLE

    override val libraryCaption get() = KonanStandardLibraryDescription.LIBRARY_CAPTION

    override val messageForOverrideDialog get() = KonanStandardLibraryDescription.KONAN_LIBRARY_CREATION

    override fun getLibraryJarDescriptors(sdk: Sdk?) = emptyList<LibraryJarDescriptor>()

    override val libraryMatcher: (Library, Project) -> Boolean = { library, _ ->
        library.getFiles(OrderRootType.CLASSES).firstOrNull { it.nameWithoutExtension == KONAN_STDLIB_NAME } != null
    }

    override val libraryType: LibraryType<DummyLibraryProperties>? get() = null

    companion object {
        const val NAME = "Konan"
        const val PRESENTABLE_TEXT = "Native"
    }
}

fun hasKonanRuntimeInScope(module: Module): Boolean {
    return runReadAction {
        val scope = module.getModuleWithDependenciesAndLibrariesScope(hasKotlinFilesOnlyInTests(module))
        hasKonanMetadataFile(module.project, LibraryKindSearchScope(module, scope, KonanLibraryKind))
    }
}

private val KONAN_FQ_NAME = FqName("kotlin.native")

fun hasKonanMetadataFile(project: Project, scope: GlobalSearchScope): Boolean {
    return runReadAction {
        project.runWithAlternativeResolveEnabled {
            KonanMetaFileIndex.hasSomethingInPackage(KONAN_FQ_NAME, scope)
        }
    }
}
