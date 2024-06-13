/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.jvm

import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.KotlinLibraryProperResolverWithAttributes
import org.jetbrains.kotlin.library.RequiredUnresolvedLibrary
import org.jetbrains.kotlin.library.UnresolvedLibrary
import org.jetbrains.kotlin.library.impl.createKotlinLibraryComponents
import org.jetbrains.kotlin.library.metadata.resolver.KotlinLibraryResolveResult
import org.jetbrains.kotlin.library.metadata.resolver.impl.libraryResolver
import org.jetbrains.kotlin.util.Logger

val jvmLibrariesProvidedByDefault = setOf("stdlib", "kotlin")

class JvmLibraryResolver(
    repositories: List<String>,
    directLibs: List<String>,
    distributionKlib: String?,
    localKotlinDir: String?,
    skipCurrentDir: Boolean,
    logger: Logger
) : KotlinLibraryProperResolverWithAttributes<KotlinLibrary>(
    repositories,
    directLibs,
    distributionKlib,
    localKotlinDir,
    skipCurrentDir,
    logger,
    emptyList()
) {
    // Stick with the default KotlinLibrary for now.
    override fun libraryComponentBuilder(file: File, isDefault: Boolean) = createKotlinLibraryComponents(file, isDefault)

    // We do not need stdlib in klib form.
    override fun isProvidedByDefault(unresolved: UnresolvedLibrary): Boolean =
        unresolved.path in jvmLibrariesProvidedByDefault
}

// TODO: This is a temporary set of library resolver policies for jvm compiler.
fun jvmResolveLibraries(libraries: List<String>, logger: Logger): KotlinLibraryResolveResult {
    val unresolvedLibraries = libraries.map { RequiredUnresolvedLibrary(it) }
    val libraryAbsolutePaths = libraries.map { File(it).absolutePath }
    // Configure the resolver to only work with absolute paths for now.
    val libraryResolver = JvmLibraryResolver(
        repositories = emptyList(),
        directLibs = libraryAbsolutePaths,
        distributionKlib = null,
        localKotlinDir = null,
        skipCurrentDir = false,
        logger = logger
    ).libraryResolver()
    val resolvedLibraries =
        libraryResolver.resolveWithDependencies(
            unresolvedLibraries = unresolvedLibraries,
            noStdLib = true,
            noDefaultLibs = true,
            noEndorsedLibs = true
        )
    return resolvedLibraries
}
