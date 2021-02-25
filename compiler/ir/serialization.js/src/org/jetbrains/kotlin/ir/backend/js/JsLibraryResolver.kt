/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js

import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.KotlinLibraryProperResolverWithAttributes
import org.jetbrains.kotlin.library.impl.createKotlinLibraryComponents
import org.jetbrains.kotlin.util.Logger

class JsLibraryResolver(
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
}
