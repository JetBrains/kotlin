/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests.compilation.util

import kotlin.io.path.toPath

val currentKotlinStdlibLocation
    get() = KotlinVersion::class.java.protectionDomain.codeSource.location.toURI().toPath()