/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests.compilation

import org.jetbrains.kotlin.buildtools.tests.CompilerExecutionStrategyConfiguration
import org.jetbrains.kotlin.buildtools.tests.compilation.assertions.assertClassDeclarations
import org.jetbrains.kotlin.buildtools.tests.compilation.assertions.assertClassDeclarationsContain
import org.jetbrains.kotlin.buildtools.tests.compilation.assertions.assertCompiledSources
import org.jetbrains.kotlin.buildtools.tests.compilation.assertions.assertOutputs
import org.jetbrains.kotlin.buildtools.tests.compilation.assertions.expectFailWithError
import org.jetbrains.kotlin.buildtools.tests.compilation.model.DefaultStrategyAgnosticCompilationTest
import org.jetbrains.kotlin.buildtools.tests.compilation.scenario.jvmScenario
import org.jetbrains.kotlin.test.TestMetadata
import org.junit.jupiter.api.DisplayName

@DisplayName("JVM file facade (@file:JvmName / @JvmMultifileClass) changes in incremental compilation")
class JvmFileFacadeChangesTest : BaseCompilationTest() {
    @DefaultStrategyAgnosticCompilationTest
    @DisplayName("Renaming a @file:JvmName facade should remove the stale facade and generate the new one")
    @TestMetadata("ic-scenarios/file-facade-rename")
    fun testRenamingFileFacade(strategyConfig: CompilerExecutionStrategyConfiguration) {
        jvmScenario(strategyConfig) {
            val mod = module("ic-scenarios/file-facade-rename")

            mod.replaceFileWithVersion("A.kt", "renamed-facade")

            mod.compile {
                assertCompiledSources("A.kt")
                assertOutputs("test/RenamedFacade.class")
                assertClassDeclarationsContain(
                    classFqn = "test.RenamedFacade",
                    setOf(
                        "public static final java.lang.String a();",
                    )
                )
            }
        }
    }

    @DefaultStrategyAgnosticCompilationTest
    @DisplayName("KT-89352: Replacing an object with a same-named file facade should recompile the usages of its members")
    @TestMetadata("ic-scenarios/object-to-file-facade")
    fun testReplacingObjectWithSameNamedFileFacade(strategyConfig: CompilerExecutionStrategyConfiguration) {
        jvmScenario(strategyConfig) {
            val mod = module("ic-scenarios/object-to-file-facade")

            mod.replaceFileWithVersion("Foo.kt", "unwrap-object")

            mod.compile {
                // TODO(KT-89352): only `Foo.kt` is recompiled. `test.Foo` stops being a classifier and becomes a file
                //  facade of the same JVM name, but IC never rechecks `Usage.kt`, whose import is unresolvable now, so
                //  the build wrongly succeeds. Once fixed, it has to `expectFail()` and recompile `Usage.kt` as well.
                assertCompiledSources("Foo.kt")
            }

            mod.changeFile("Usage.kt") { "$it\n" }

            mod.compile {
                // the incremental round reports the last unresolved segment, a clean build reports the qualifier
                expectFailWithError(".*Usage\\.kt:\\d+:\\d+ Unresolved reference '(Foo|bar)'.*".toRegex())
            }
        }
    }

    @DefaultStrategyAgnosticCompilationTest
    @DisplayName("KT-89326: Moving a part of a @JvmMultifileClass facade to another @JvmName should keep the original facade")
    @TestMetadata("ic-scenarios/multifile-facade")
    fun testMovingMultifileClassPartToAnotherFacade(strategyConfig: CompilerExecutionStrategyConfiguration) {
        jvmScenario(strategyConfig) {
            val mod = module("ic-scenarios/multifile-facade")

            mod.replaceFileWithVersion("b.kt", "another-facade")

            mod.compile {
                assertCompiledSources("b.kt")
                // TODO(KT-89326): `test/Test.class` is wrongly missing here. IC deletes the facade together with the
                //  moved part `test/Test__BKt.class` and never recompiles `a.kt`, so the facade that `a.kt` still
                //  declares is not regenerated. Once the issue is fixed, `test/Test.class` has to be expected as well.
                assertOutputs("test/Test1.class", "test/Test__AKt.class", "test/Test1__BKt.class")
            }
        }
    }

    @DefaultStrategyAgnosticCompilationTest
    @DisplayName("KT-89326: Deleting a part of a @JvmMultifileClass facade should keep the facade declared by the remaining part")
    @TestMetadata("ic-scenarios/multifile-facade")
    fun testDeletingMultifileClassPart(strategyConfig: CompilerExecutionStrategyConfiguration) {
        jvmScenario(strategyConfig) {
            val mod = module("ic-scenarios/multifile-facade")

            mod.deleteFile("b.kt")

            mod.compile {
                // TODO(KT-89326): `test/Test.class` is wrongly missing here: IC deletes the facade together with the output of the
                //  removed part `test/Test__BKt.class` and never recompiles `a.kt`, so the facade that `a.kt` still
                //  declares is not regenerated. Once the issue is fixed, `test/Test.class` has to be expected as well.
                assertOutputs("test/Test__AKt.class")
            }
        }
    }

    @DefaultStrategyAgnosticCompilationTest
    @DisplayName("KT-89326: Moving a part of a @JvmMultifileClass facade back should restore the facade")
    @TestMetadata("ic-scenarios/multifile-facade")
    fun testRestoringMultifileClassFacade(strategyConfig: CompilerExecutionStrategyConfiguration) {
        jvmScenario(strategyConfig) {
            val mod = module("ic-scenarios/multifile-facade")

            mod.replaceFileWithVersion("b.kt", "another-facade")

            mod.compile {
                // TODO(KT-89326): `test/Test.class` is wrongly missing here
                assertOutputs("test/Test1.class", "test/Test__AKt.class", "test/Test1__BKt.class")
            }

            mod.replaceFileWithVersion("b.kt", "original")

            mod.compile {
                assertCompiledSources("b.kt")
                assertOutputs("test/Test.class", "test/Test__AKt.class", "test/Test__BKt.class")
                // TODO(KT-89326): the facade is restored only formally. It is generated from `b.kt` alone, so it
                //  exposes `b()` and not `a()`, while `test/Test__AKt.class` is left over as an orphan the facade does
                //  not reference anymore. Once the issue is fixed, `a()` has to be expected here as well.
                assertClassDeclarations(
                    classFqn = "test.Test",
                    setOf(
                        "public static final java.lang.String b();",
                    )
                )
            }
        }
    }
}
