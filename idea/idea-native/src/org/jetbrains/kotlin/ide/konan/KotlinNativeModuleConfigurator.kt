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
import org.jetbrains.kotlin.ide.konan.index.KotlinNativeMetaFileIndex
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

open class KotlinNativeModuleConfigurator : KotlinWithLibraryConfigurator() {

    override val name: String get() = NAME

    override val targetPlatform get() = KonanPlatform

    override val presentableText get() = PRESENTABLE_TEXT

    override fun isConfigured(module: Module) = hasKotlinNativeRuntimeInScope(module)

    override val libraryName get() = NativeStandardLibraryDescription.LIBRARY_NAME

    override val dialogTitle get() = NativeStandardLibraryDescription.DIALOG_TITLE

    override val libraryCaption get() = NativeStandardLibraryDescription.LIBRARY_CAPTION

    override val messageForOverrideDialog get() = NativeStandardLibraryDescription.NATIVE_LIBRARY_CREATION

    override fun getLibraryJarDescriptors(sdk: Sdk?) = emptyList<LibraryJarDescriptor>()

    override val libraryMatcher: (Library, Project) -> Boolean = { library, _ ->
        library.getFiles(OrderRootType.CLASSES).any { it.nameWithoutExtension == KONAN_STDLIB_NAME }
    }

    override val libraryType: LibraryType<DummyLibraryProperties>? get() = null

    companion object {
        const val NAME = "KotlinNative"
        const val PRESENTABLE_TEXT = "Native"
    }
}

fun hasKotlinNativeRuntimeInScope(module: Module): Boolean {
    return runReadAction {
        val scope = module.getModuleWithDependenciesAndLibrariesScope(hasKotlinFilesOnlyInTests(module))
        hasKotlinNativeMetadataFile(module.project, LibraryKindSearchScope(module, scope, NativeLibraryKind))
    }
}

private val KOTLIN_NATIVE_FQ_NAMES = listOf(
    "kotlin.native",
    "konan.native" // Keep "konan.native" for backward compatibility with older versions of Kotlin/Native.
).map { FqName(it) }

fun hasKotlinNativeMetadataFile(project: Project, scope: GlobalSearchScope): Boolean {
    return runReadAction {
        project.runWithAlternativeResolveEnabled {
            KOTLIN_NATIVE_FQ_NAMES.any { KotlinNativeMetaFileIndex.hasSomethingInPackage(it, scope) }
        }
    }
}
