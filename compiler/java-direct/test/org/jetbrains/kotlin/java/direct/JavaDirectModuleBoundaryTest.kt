/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.java.direct

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File
import java.util.zip.ZipFile

/**
 * java-direct is the PSI-free Java frontend, so it must not reach into PSI, the virtual file system,
 * the IntelliJ core services or the CLI environment. Only the standalone `com.intellij.java.syntax` /
 * `com.intellij.platform.syntax` libraries and the light-tree types listed in
 * [ALLOWED_INTELLIJ_REFERENCES] are permitted; those go away together with the Kotlin light tree.
 *
 * Checked on the compiled output rather than on the module's dependencies, because
 * `:compiler:fir:entrypoint` legitimately brings PSI onto the compile classpath.
 */
class JavaDirectModuleBoundaryTest {
    @Test
    fun compiledOutputDoesNotReferencePsiVfsOrCli() {
        val output = File(JavaClassFinderOverBinaryIndex::class.java.protectionDomain.codeSource.location.toURI())
        assertTrue(output.exists(), "Production classes not found: $output")

        val violations = classFileContents(output)
            .flatMap { entry -> forbiddenReferences(entry.second).map { "${entry.first}: $it" } }
            .distinct()
            .sorted()
            .toList()

        assertTrue(
            violations.isEmpty(),
            "java-direct must not reference PSI, VFS, IntelliJ core or the CLI:\n" + violations.joinToString("\n")
        )
    }

    /** The module's own class files, whether it was compiled into a directory or packed into a jar. */
    private fun classFileContents(output: File): Sequence<Pair<String, String>> =
        if (output.isDirectory) {
            output.walkTopDown()
                .filter { it.isFile && it.extension == "class" }
                .map { it.toRelativeString(output) to it.readText(Charsets.ISO_8859_1) }
        } else {
            val zip = ZipFile(output)
            zip.entries().asSequence()
                .filter { it.name.endsWith(".class") }
                .map { it.name to zip.getInputStream(it).readBytes().toString(Charsets.ISO_8859_1) }
        }

    /** Matches internal names in the constant pool, so fully qualified usages are covered too. */
    private fun forbiddenReferences(classFileContent: String): Sequence<String> =
        FORBIDDEN_NAME_PATTERN.findAll(classFileContent)
            .map { it.value }
            .filterNot { name -> ALLOWED_INTELLIJ_REFERENCES.any(name::startsWith) }

    private companion object {
        private val FORBIDDEN_NAME_PATTERN =
            Regex("""(?:com/intellij|org/jetbrains/kotlin/cli)/[\w/$]+""")

        private val ALLOWED_INTELLIJ_REFERENCES = listOf(
            "com/intellij/java/syntax/",
            "com/intellij/platform/syntax/",
            "com/intellij/lang/Language",
            "com/intellij/lang/LighterASTNode",
            "com/intellij/openapi/util/Ref",
            "com/intellij/pom/java/LanguageLevel",
            "com/intellij/psi/tree/IElementType",
            "com/intellij/util/diff/FlyweightCapableTreeStructure",
        )
    }
}
