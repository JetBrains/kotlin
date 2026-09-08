/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli

import org.jetbrains.kotlin.cli.common.ExitCode
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.K2MetadataCompilerArguments
import org.jetbrains.kotlin.cli.common.arguments.cliArgument
import org.jetbrains.kotlin.cli.metadata.KotlinMetadataCompiler
import org.jetbrains.kotlin.library.assertNoEmptyPackageFragmentsInKlib
import org.jetbrains.kotlin.test.TestCaseWithTmpdir
import org.jetbrains.kotlin.test.services.StandardLibrariesPathProviderForKotlinProject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.io.File

class MetadataKlibEmptyPackageFragmentsTest : TestCaseWithTmpdir() {
    @Test
    fun testEmptyPackageFragmentsAreNotSerialized() {
        val sourcesDir = File(tmpdir, "sources").apply { mkdirs() }
        File(sourcesDir, "withDeclarations.kt").writeText(
            """
                package foo.bar.baz

                fun baz() {}
            """.trimIndent()
        )
        File(sourcesDir, "withoutDeclarations.kt").writeText(
            """
                package foo.bar.empty
            """.trimIndent()
        )

        val klibDir = File(tmpdir, "lib")
        val [output, exitCode] = AbstractCliTest.executeCompilerGrabOutput(
            KotlinMetadataCompiler(),
            listOf(
                sourcesDir.path,
                K2JVMCompilerArguments::classpath.cliArgument,
                StandardLibrariesPathProviderForKotlinProject.commonStdlibForTests().path,
                K2JVMCompilerArguments::destination.cliArgument,
                klibDir.path,
                K2MetadataCompilerArguments::metadataKlib.cliArgument,
            )
        )
        assertEquals(ExitCode.OK, exitCode, output)

        assertNoEmptyPackageFragmentsInKlib(klibDir.toPath(), expectedPackageFqNames = setOf("foo.bar.baz"))
    }
}
