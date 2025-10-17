/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api.cri

import org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi

// TODO (KT-81585): once the CRI schema is stable, document public interfaces
@ExperimentalBuildToolsApi
public interface FileIdToPathEntry {
    public val fileId: Int?
    public val path: String?
}
