/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.checkers.generator

import org.jetbrains.kotlin.generators.util.GeneratorsFileUtil
import org.jetbrains.kotlin.utils.SmartPrinter
import java.io.File
import kotlin.reflect.KClass
import kotlin.reflect.KType

private val COPYRIGHT = File("license/COPYRIGHT_HEADER.txt").readText()

internal fun SmartPrinter.printCopyright() {
    println(COPYRIGHT)
    println()
}

internal fun SmartPrinter.printGeneratedMessage() {
    println(GeneratorsFileUtil.GENERATED_MESSAGE)
    println()
}


fun getGenerationPath(rootPath: File, packageName: String): File =
    packageName
        .split(".")
        .fold(rootPath, File::resolve)
        .apply { mkdirs() }


fun KType.collectClassNamesTo(set: MutableSet<String>) {
    (classifier as? KClass<*>)?.qualifiedName?.let(set::add)
    for (argument in arguments) {
        argument.type?.collectClassNamesTo(set)
    }
}
