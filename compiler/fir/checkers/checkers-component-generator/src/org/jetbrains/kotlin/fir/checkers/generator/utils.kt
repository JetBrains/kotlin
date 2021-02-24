/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.checkers.generator

import java.io.File
import kotlin.reflect.KClass
import kotlin.reflect.KType

fun getGenerationPath(rootPath: File, packageName: String): File =
    packageName
        .split(".")
        .fold(rootPath, File::resolve)
        .apply { mkdirs() }


@OptIn(ExperimentalStdlibApi::class)
fun KType.collectClassNamesTo(set: MutableSet<String>) {
    (classifier as? KClass<*>)?.qualifiedName?.let(set::add)
    for (argument in arguments) {
        argument.type?.collectClassNamesTo(set)
    }
}