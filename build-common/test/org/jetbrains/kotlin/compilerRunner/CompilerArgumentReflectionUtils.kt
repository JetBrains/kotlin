/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.compilerRunner

import org.jetbrains.kotlin.cli.common.arguments.CommonToolArguments
import org.reflections.Reflections
import kotlin.reflect.KClass

private val reflections = Reflections("org.jetbrains.kotlin")

fun getCompilerArgumentImplementations(): List<KClass<out CommonToolArguments>> {
    return reflections.getSubTypesOf(CommonToolArguments::class.java)
        .map { it.kotlin }
        .filter { !it.isAbstract }
        .filterNot { it.isInner }
}
