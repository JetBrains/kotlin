/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api.compilation

import org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi

/**
 * An exception class that represents an error that occurred while using the Kotlin build tools API.
 */
@ExperimentalBuildToolsApi
open class KotlinBuildToolsException(message: String) : Exception(message)