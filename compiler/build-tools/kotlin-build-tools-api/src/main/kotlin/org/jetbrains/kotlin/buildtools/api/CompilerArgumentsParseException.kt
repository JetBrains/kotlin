/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api

/**
 *  Represents an error during parsing Kotlin compiler arguments.
 *
 *  @param message A description of the exception.
 */
public class CompilerArgumentsParseException(message: String) : KotlinBuildToolsException(message)