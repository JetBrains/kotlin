/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.jvm.environment

import org.jetbrains.kotlin.KtSourceFile
import org.jetbrains.kotlin.load.kotlin.KotlinClassFinder
import org.jetbrains.kotlin.load.kotlin.PackagePartProvider
import org.jetbrains.kotlin.resolve.jvm.modules.JavaModuleResolver
import org.jetbrains.kotlin.search.AbstractProjectFileSearchScope
import java.io.File
import java.nio.file.Path

/**
 * The JVM environment of one compilation: the file sets it may look in, and the JVM-specific views of them —
 * a [KotlinClassFinder], a [PackagePartProvider] and a [JavaModuleResolver]. The metadata and JKlib pipelines
 * use it as such, since they too read JVM-shaped roots.
 *
 * It hands out no Java view and knows nothing about FIR: which Java implementation serves a scope, and whether
 * the Kotlin declarations of a session are exposed to Java resolution, is a decision of the compilation, made
 * once by whoever builds its sessions (`FirJavaInterop`).
 */
interface JvmCompilationEnvironment {
    fun getKotlinClassFinder(fileSearchScope: AbstractProjectFileSearchScope): KotlinClassFinder

    fun getJavaModuleResolver(): JavaModuleResolver

    fun getPackagePartProvider(fileSearchScope: AbstractProjectFileSearchScope): PackagePartProvider

    fun getSearchScopeByIoFiles(files: Iterable<File>, allowOutOfProjectRoots: Boolean = false): AbstractProjectFileSearchScope

    fun getSearchScopeBySourceFiles(files: Iterable<KtSourceFile>, allowOutOfProjectRoots: Boolean = false): AbstractProjectFileSearchScope

    fun getSearchScopeByDirectories(directories: Iterable<File>): AbstractProjectFileSearchScope

    fun getSearchScopeByClassPath(paths: Iterable<Path>): AbstractProjectFileSearchScope

    fun getSearchScopeForProjectLibraries(): AbstractProjectFileSearchScope

    fun getSearchScopeForProjectJavaSources(): AbstractProjectFileSearchScope
}
