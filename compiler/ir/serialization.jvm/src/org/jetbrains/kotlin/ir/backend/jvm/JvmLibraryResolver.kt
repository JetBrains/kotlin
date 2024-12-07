/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.jvm

import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.library.*
import org.jetbrains.kotlin.library.impl.createKotlinLibraryComponents
import org.jetbrains.kotlin.library.metadata.resolver.KotlinLibraryResolveResult
import org.jetbrains.kotlin.library.metadata.resolver.impl.libraryResolver
import org.jetbrains.kotlin.util.Logger

class JvmLibraryResolver(
    directLibs: List<String>,
    distributionKlib: String?,
    skipCurrentDir: Boolean,
    logger: Logger
) : KotlinLibraryProperResolverWithAttributes<KotlinLibrary>(
    directLibs = directLibs,
    distributionKlib = distributionKlib,
    skipCurrentDir = skipCurrentDir,
    logger = logger,
    knownIrProviders = emptyList()
) {
    // Stick with the default KotlinLibrary for now.
    override fun libraryComponentBuilder(file: File, isDefault: Boolean) = createKotlinLibraryComponents(file, isDefault)

    // We do not need stdlib in klib form.
    override fun isProvidedByDefault(unresolved: UnresolvedLibrary): Boolean =
        unresolved.path == KOTLIN_NATIVE_STDLIB_NAME || unresolved.path == KOTLIN_JS_STDLIB_NAME
}

// TODO: This is a temporary set of library resolver policies for jvm compiler.
fun jvmResolveLibraries(libraries: List<String>, logger: Logger): KotlinLibraryResolveResult {
    val unresolvedLibraries = libraries.map { RequiredUnresolvedLibrary(it) }
    val libraryAbsolutePaths = libraries.map { File(it).absolutePath }
    // Configure the resolver to only work with absolute paths for now.
    val libraryResolver = JvmLibraryResolver(
        directLibs = libraryAbsolutePaths,
        distributionKlib = null,
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
