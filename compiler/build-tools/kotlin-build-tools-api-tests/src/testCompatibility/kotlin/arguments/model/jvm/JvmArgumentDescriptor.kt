/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests.arguments.model.jvm

import org.jetbrains.kotlin.buildtools.api.arguments.JvmCompilerArguments.JvmCompilerArgument
import org.jetbrains.kotlin.buildtools.tests.arguments.model.ArgumentDescriptor

internal class JvmArgumentDescriptor<T>(
    override val argumentName: String,
    override val valueString: (T?) -> String?,
    override val expectedArgumentStringsFor: (String) -> List<String>,
    val argumentKey: JvmCompilerArgument<T>,
    val argumentValues: List<T>,
    val isEnum: Boolean,
    val isNullable: Boolean,
) : ArgumentDescriptor<T>