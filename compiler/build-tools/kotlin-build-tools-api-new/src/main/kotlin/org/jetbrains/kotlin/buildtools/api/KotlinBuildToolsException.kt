/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api

/**
 * A sealed class that represents exceptions that may occur during the execution of a Kotlin build tool.
 *
 * @property message A [String] that contains a description of the exception that occurred.
 */
public sealed class KotlinBuildToolsException(message: String) : Exception(message)