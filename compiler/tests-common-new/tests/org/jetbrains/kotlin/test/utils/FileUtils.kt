/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.utils

import org.jetbrains.kotlin.test.directives.model.Directive
import java.io.File

private const val FIR_KT = ".fir.kt"
private const val KT = ".kt"
private const val FIR_KTS = ".fir.kts"
private const val KTS = ".kts"

val File.isFirTestData: Boolean
    get() = name.endsWith(FIR_KT) || name.endsWith((FIR_KTS))

val File.originalTestDataFile: File
    get() = if (isFirTestData) {
        val originalTestDataFileName =
            if (name.endsWith(KTS)) "${name.removeSuffix(FIR_KTS)}$KTS"
            else "${name.removeSuffix(FIR_KT)}$KT"
        parentFile.resolve(originalTestDataFileName)
    } else {
        this
    }

val File.firTestDataFile: File
    get() = if (isFirTestData) {
        this
    } else {
        val firTestDataFileName =
            if (name.endsWith(KTS)) "${name.removeSuffix(KTS)}$FIR_KTS"
            else "${name.removeSuffix(KT)}$FIR_KT"
        parentFile.resolve(firTestDataFileName)
    }

fun File.withExtension(extension: String): File {
    return withSuffixAndExtension(suffix = "", extension)
}

fun File.withSuffixAndExtension(suffix: String, extension: String): File {
    @Suppress("NAME_SHADOWING")
    val extension = extension.removePrefix(".")
    return parentFile.resolve("$nameWithoutExtension$suffix.$extension")
}

/*
 * Please use this method only in places where `TestModule` is not accessible
 * In other cases use testModule.directives
 */
fun File.isDirectiveDefined(directive: String): Boolean = this.useLines { line ->
    line.any { it == directive }
}

fun File.removeDirectiveFromFile(directive: Directive) {
    val directiveName = directive.name
    val directiveRegexp = "^// $directiveName(:.*)?$(\n)?".toRegex(RegexOption.MULTILINE)
    val text = readText()
    val directiveRange = directiveRegexp.find(text)?.range
        ?: error("Directive $directiveName was not found in $this")
    val textWithoutDirective = text.removeRange(directiveRange)
    writeText(textWithoutDirective)
}