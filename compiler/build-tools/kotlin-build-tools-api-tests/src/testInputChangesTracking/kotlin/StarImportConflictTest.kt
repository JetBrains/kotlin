/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests.compilation

import org.jetbrains.kotlin.buildtools.tests.CompilerExecutionStrategyConfiguration
import org.jetbrains.kotlin.buildtools.tests.compilation.assertions.assertCompiledSources
import org.jetbrains.kotlin.buildtools.tests.compilation.assertions.assertLogContainsPatterns
import org.jetbrains.kotlin.buildtools.tests.compilation.model.DefaultStrategyAgnosticCompilationTest
import org.jetbrains.kotlin.buildtools.tests.compilation.model.DefaultStrategyAndPlatformAgnosticScenarioTest
import org.jetbrains.kotlin.buildtools.tests.compilation.model.LogLevel
import org.jetbrains.kotlin.buildtools.tests.compilation.model.ScenarioCreator
import org.jetbrains.kotlin.buildtools.tests.compilation.scenario.jvmScenario
import org.jetbrains.kotlin.test.TestMetadata
import org.junit.jupiter.api.DisplayName

@DisplayName("Star-import conflicts")
class StarImportConflictTest : BaseCompilationTest() {

    @DefaultStrategyAgnosticCompilationTest
    @DisplayName("KT-85241: Adding a Java nested class that conflicts with a Kotlin star import should trigger an ambiguity error in IC")
    @TestMetadata("ic-scenarios/kt-85241/java-nested")
    fun testStarImportAmbiguityDetectedAfterAddingJavaNestedClass(strategyConfig: CompilerExecutionStrategyConfiguration) {
        jvmScenario(strategyConfig) {
            val mod = module("ic-scenarios/kt-85241/java-nested")

            mod.replaceFileWithVersion("com/example/SomeClass.java", "add-nested-class")
            mod.compile {
                expectFail()
                assertLogContainsPatterns(LogLevel.ERROR, OVERLOAD_AMBIGUITY_ERROR)
                assertCompiledSources("foo.kt")
            }
        }
    }

    @DefaultStrategyAndPlatformAgnosticScenarioTest
    @DisplayName("KT-85241: Adding an unrelated package-level declaration must NOT recompile a file with a class star import")
    @TestMetadata("ic-scenarios/kt-85241/unrelated-package-declaration")
    fun testNoOverInvalidationOnUnrelatedPackageDeclaration(scenario: ScenarioCreator) {
        scenario {
            val mod = module("ic-scenarios/kt-85241/unrelated-package-declaration")

            mod.replaceFileWithVersion("com/example/Unrelated.kt", "add-class")
            mod.compile {
                assertCompiledSources("com/example/Unrelated.kt")
            }
        }
    }

    @DefaultStrategyAndPlatformAgnosticScenarioTest
    @DisplayName("KT-85241: Adding a deeply nested class via a two-level class star import should trigger an ambiguity error in IC")
    @TestMetadata("ic-scenarios/kt-85241/deeply-nested-class-import")
    fun testStarImportAmbiguityWithDeeplyNestedClassImport(scenario: ScenarioCreator) {
        scenario {
            val mod = module("ic-scenarios/kt-85241/deeply-nested-class-import")

            mod.replaceFileWithVersion("com/example/OuterInner.kt", "add-nested-class")
            mod.compile {
                expectFail()
                assertLogContainsPatterns(LogLevel.ERROR, OVERLOAD_AMBIGUITY_ERROR)
            }
        }
    }

    @DefaultStrategyAgnosticCompilationTest
    @DisplayName("KT-85241: Adding a Java static method that conflicts with a Kotlin star import should trigger an ambiguity error in IC")
    @TestMetadata("ic-scenarios/kt-85241/java-static-method")
    fun testStarImportAmbiguityDetectedAfterAddingJavaStaticMethod(strategyConfig: CompilerExecutionStrategyConfiguration) {
        jvmScenario(strategyConfig) {
            val mod = module("ic-scenarios/kt-85241/java-static-method")

            mod.replaceFileWithVersion("com/example/SomeClass.java", "add-static-method")
            mod.compile {
                expectFail()
                assertLogContainsPatterns(LogLevel.ERROR, OVERLOAD_AMBIGUITY_ERROR)
                assertCompiledSources("foo.kt")
            }
        }
    }

    @DefaultStrategyAndPlatformAgnosticScenarioTest
    @DisplayName("KT-85241: Adding a package-level declaration on the package side of a star import must still invalidate (fallback branch guard)")
    @TestMetadata("ic-scenarios/kt-85241/package-import-fallback")
    fun testStarImportAmbiguityViaPackageStarImportFallback(scenario: ScenarioCreator) {
        scenario {
            val mod = module("ic-scenarios/kt-85241/package-import-fallback")

            mod.replaceFileWithVersion("another.kt", "add-nested-class")
            mod.compile {
                expectFail()
                assertLogContainsPatterns(LogLevel.ERROR, OVERLOAD_AMBIGUITY_ERROR)
            }
        }
    }

    private val OVERLOAD_AMBIGUITY_ERROR =
        ".*Overload resolution ambiguity between candidates.*".toRegex(RegexOption.DOT_MATCHES_ALL)
}
