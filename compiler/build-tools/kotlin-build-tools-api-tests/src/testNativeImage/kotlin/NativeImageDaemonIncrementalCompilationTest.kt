/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.buildtools.tests.compilation

import org.jetbrains.kotlin.buildtools.api.KotlinToolchains
import org.jetbrains.kotlin.buildtools.api.SourcesChanges
import org.jetbrains.kotlin.buildtools.tests.compilation.assertions.assertCompiledSources
import org.jetbrains.kotlin.buildtools.tests.compilation.assertions.assertOutputs
import org.jetbrains.kotlin.buildtools.tests.compilation.assertions.expectFailWithError
import org.jetbrains.kotlin.buildtools.tests.compilation.model.jvmProject
import org.jetbrains.kotlin.buildtools.tests.compilation.util.btaClassloader
import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime
import org.jetbrains.kotlin.test.TestMetadata
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.nio.file.Path
import kotlin.io.path.writeText

@DisplayName("Incremental compilation with the native image daemon execution policy")
class NativeImageDaemonIncrementalCompilationTest : BaseCompilationTest() {
    val nativeImageDistPath: Path = ForTestCompileRuntime.kotlinNativeImageDistForTests().toPath()

    @Test
    @DisplayName("Known source changes recompile dependent sources")
    @TestMetadata("basic-multimodule-project/module-1")
    fun testKnownChangesRecompileDependentSources() {
        val kotlinToolchains = KotlinToolchains.loadImplementation(btaClassloader)
        runSingleShotNativeImageDaemonTest(kotlinToolchains, nativeImageDistPath) { daemonPolicy, _ ->
            jvmProject(kotlinToolchains to daemonPolicy) {
                val module = module("basic-multimodule-project/module-1")
                val fooKt = module.sourcesDirectory.resolve("foo.kt")
                val barKt = module.sourcesDirectory.resolve("bar.kt")

                fooKt.writeText("""fun foo(): String = "42"""")
                barKt.writeText("fun bar(): String = foo()")

                module.compileIncrementally(SourcesChanges.Unknown) {
                    assertOutputs("FooKt.class", "BarKt.class", "BazKt.class")
                }

                fooKt.writeText("fun foo(): Int = 42")

                module.compileIncrementally(
                    SourcesChanges.Known(
                        modifiedFiles = listOf(fooKt.toFile()),
                        removedFiles = emptyList(),
                    )
                ) {
                    assertCompiledSources("foo.kt", "bar.kt")
                    expectFailWithError(
                        ".*bar\\.kt:.*Return type mismatch: expected 'String', actual 'Int'\\.".toRegex()
                    )
                }
            }
        }
    }
}
