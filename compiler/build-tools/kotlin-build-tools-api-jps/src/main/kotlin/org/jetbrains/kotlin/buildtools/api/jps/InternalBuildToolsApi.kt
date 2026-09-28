/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api.jps

@RequiresOptIn(
    message = "This part of the Build Tools API is internal to JetBrains and exists solely to serve the needs of the JPS build system. " +
            "It may be changed or removed at any time without notice, and no source, binary, or behavioral compatibility is guaranteed. " +
            "If you have a strong use case for this API, please leave a comment in https://youtrack.jetbrains.com/issue/KT-89497",
    level = RequiresOptIn.Level.ERROR,
)
public annotation class InternalBuildToolsApi
