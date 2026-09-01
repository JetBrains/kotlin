/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api.jvm

import org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi

/**
 * The declarations a previous compilation recorded for a single class file, as stored by the build system.
 *
 * The contents are opaque to the API consumer: a build system stores whatever
 * [CompilerIncrementalCache.getPackagePartData] is expected to hand back and returns it unchanged.
 *
 * @property data the recorded declarations
 * @property strings the names referenced by [data]
 * @since 2.5.20
 */
@ExperimentalBuildToolsApi
public class CompilerPackagePartData(
    public val data: ByteArray,
    public val strings: Array<String>,
)
