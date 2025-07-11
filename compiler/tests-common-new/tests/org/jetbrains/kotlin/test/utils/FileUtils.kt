/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.utils

import org.jetbrains.kotlin.test.directives.model.Directive
import java.nio.file.Path
import kotlin.io.path.*

fun Path.withExtension(extension: String): Path {
    return withSuffixAndExtension(suffix = "", extension)
}

fun Path.withSuffixAndExtension(suffix: String, extension: String): Path {
    @Suppress("NAME_SHADOWING")
    val extension = extension.removePrefix(".")
    return parent.resolve("$nameWithoutExtension$suffix.$extension")
}

/*
 * Please use this method only in places where `TestModule` is not accessible
 * In other cases use testModule.directives
 */
fun Path.isDirectiveDefined(directive: String): Boolean = this.useLines { line ->
    line.any { it == directive }
}

fun Path.removeDirectiveFromFile(directive: Directive) {
    if (!exists()) return

    val directiveName = directive.name
    val directiveRegexp = "^// $directiveName(:.*)?$(\n)?".toRegex(RegexOption.MULTILINE)
    val text = readText()
    val directiveRange = directiveRegexp.find(text)?.range
        ?: error("Directive $directiveName was not found in $this")
    val textWithoutDirective = text.removeRange(directiveRange)
    writeText(textWithoutDirective)
}
