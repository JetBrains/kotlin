/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.jvm.modules

import java.io.File
import javax.lang.model.SourceVersion

internal const val ALL_UNNAMED = "ALL-UNNAMED"

/**
 * A value of `-Xadd-reads` (`<module>=<other-module>(,<other-module>)*`) or `-Xadd-exports`
 * (`<module>/<package>=<other-module>(,<other-module>)*`). Names that are not valid Java names are in [badNames] and left out of
 * [otherModules]. If the left-hand side contains such a name, [source] is `null` and [otherModules] is empty.
 */
class JavaModuleAccessOption(val source: String?, val otherModules: List<String>, val badNames: List<String>)

private val ADD_READS_VALUE = Regex("([^=]+)=(,*[^,].*)")
private val ADD_EXPORTS_VALUE = Regex("([^/]+)/([^=]+)=(,*[^,].*)")
private val TARGET_SEPARATORS = Regex("[ ,]+")

/**
 * Parses a value of `-Xadd-reads` or `-Xadd-exports` the way javac parses `--add-reads` and `--add-exports`, or returns `null` if the
 * value is malformed.
 */
fun parseJavaModuleAccessOption(value: String, isExports: Boolean): JavaModuleAccessOption? {
    val groups = (if (isExports) ADD_EXPORTS_VALUE else ADD_READS_VALUE).matchEntire(value)?.groupValues?.drop(1) ?: return null
    val sourceParts = groups.dropLast(1)
    val targets = groups.last()
    val badSourceParts = sourceParts.filterNot(SourceVersion::isName)
    // Like javac, names are checked split on commas, but applied split on spaces and commas.
    val badNames = badSourceParts + targets.split(',').filterNot { it.isEmpty() || isValidTarget(it) }
    if (badSourceParts.isNotEmpty()) return JavaModuleAccessOption(null, emptyList(), badNames)
    return JavaModuleAccessOption(sourceParts.joinToString("/"), targets.split(TARGET_SEPARATORS).filter(::isValidTarget), badNames)
}

private fun isValidTarget(name: String): Boolean = name == ALL_UNNAMED || SourceVersion.isName(name)

private val PATCH_MODULE_VALUE = Regex("([^/]+)=(,*[^,].*)")

/**
 * Parses a value of `-Xpatch-module` (`<module>=<path>(<path-separator><path>)*`) the way javac parses `--patch-module` into the module
 * name and the paths, or returns `null` if the value is malformed.
 */
fun parseJavaModulePatch(value: String): Pair<String, List<String>>? {
    if (!PATCH_MODULE_VALUE.matches(value)) return null
    return value.substringBefore('=') to value.substringAfter('=').split(File.pathSeparatorChar).filter(String::isNotEmpty)
}

/**
 * The values of `-Xadd-reads` and `-Xadd-exports` for the modules in the module graph.
 */
class JavaModuleAccess(
    /** From a module name to the modules it reads or `ALL-UNNAMED`. */
    val additionalReads: Map<String, Set<String>>,
    /** From `<module>/<package>` to the modules the package is exported to or `ALL-UNNAMED`. */
    val additionalExports: Map<String, Set<String>>,
    /**
     * Modules read only from classpath roots that are also added to the module path (see
     * [org.jetbrains.kotlin.cli.jvm.config.JvmModulePathRoot.isFromClasspath]). For javac, those roots are in the unnamed module.
     */
    val modulesFromClasspath: Set<String>,
) {
    companion object {
        val EMPTY = JavaModuleAccess(emptyMap(), emptyMap(), emptySet())
    }
}
