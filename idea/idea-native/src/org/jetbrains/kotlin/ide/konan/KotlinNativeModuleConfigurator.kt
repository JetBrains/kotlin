/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ide.konan

import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.ide.konan.index.KotlinNativeMetaFileIndex
import org.jetbrains.kotlin.idea.configuration.LibraryKindSearchScope
import org.jetbrains.kotlin.idea.configuration.hasKotlinFilesOnlyInTests
import org.jetbrains.kotlin.idea.util.runReadActionInSmartMode
import org.jetbrains.kotlin.idea.vfilefinder.hasSomethingInPackage
import org.jetbrains.kotlin.name.FqName

fun hasKotlinNativeRuntimeInScope(module: Module): Boolean = module.project.runReadActionInSmartMode {
    val scope = module.getModuleWithDependenciesAndLibrariesScope(hasKotlinFilesOnlyInTests(module))
    hasKotlinNativeMetadataFile(module.project, LibraryKindSearchScope(module, scope, NativeLibraryKind))
}

private val KOTLIN_NATIVE_FQ_NAMES = listOf(
    "kotlin.native",
    "konan.native" // Keep "konan.native" for backward compatibility with older versions of Kotlin/Native.
).map { FqName(it) }

fun hasKotlinNativeMetadataFile(project: Project, scope: GlobalSearchScope): Boolean = project.runReadActionInSmartMode {
    KOTLIN_NATIVE_FQ_NAMES.any { KotlinNativeMetaFileIndex.hasSomethingInPackage(it, scope) }
}
