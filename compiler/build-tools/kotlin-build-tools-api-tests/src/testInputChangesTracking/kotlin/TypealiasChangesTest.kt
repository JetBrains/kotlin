/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests.compilation

import org.jetbrains.kotlin.buildtools.tests.compilation.assertions.assertCompiledSources
import org.jetbrains.kotlin.buildtools.tests.compilation.assertions.expectFailWithError
import org.jetbrains.kotlin.buildtools.tests.compilation.model.DefaultStrategyAndPlatformAgnosticScenarioTest
import org.jetbrains.kotlin.buildtools.tests.compilation.model.ScenarioCreator
import org.jetbrains.kotlin.buildtools.tests.compilation.scenario.JsScenarioDsl
import org.jetbrains.kotlin.buildtools.tests.compilation.scenario.JvmScenarioDsl
import org.jetbrains.kotlin.buildtools.tests.compilation.scenario.WasmScenarioDsl
import org.jetbrains.kotlin.test.TestMetadata
import org.junit.jupiter.api.DisplayName

@DisplayName("Typealias changes in incremental compilation")
class TypealiasChangesTest : BaseCompilationTest() {
    @DefaultStrategyAndPlatformAgnosticScenarioTest
    @DisplayName("KT-89857: Changing a typealias should recompile the unchanged file whose signature carries its expansion")
    @TestMetadata("ic-scenarios/typealias-expansion-in-unchanged-signature")
    fun testChangingTypealiasBehindUnchangedSignature(scenario: ScenarioCreator) {
        scenario {
            val mod = module("ic-scenarios/typealias-expansion-in-unchanged-signature")

            mod.replaceFileWithVersion("alias.kt", "change-alias-type")
            mod.replaceFileWithVersion("ServiceUsage.kt", "change-alias-type")
            mod.replaceFileWithVersion("AliasUsage.kt", "change-alias-type")

            mod.compile {
                when (this@scenario) {
                    is JvmScenarioDsl -> assertCompiledSources("Service.kt", "alias.kt", "ServiceUsage.kt", "AliasUsage.kt")
                    is JsScenarioDsl, is WasmScenarioDsl -> {
                        // TODO(KT-89851): the klib IC cache does not track typealiases, so `Service.kt` does not get into
                        //  the dirty set, and `ServiceUsage.kt` is checked against the stale expansion.
                        expectFailWithError(
                            ".*ServiceUsage\\.kt:\\d+:\\d+ Argument type mismatch: actual type is 'Int', but 'String' was expected.*".toRegex()
                        )
                        assertCompiledSources("alias.kt", "ServiceUsage.kt", "AliasUsage.kt")
                    }
                    else -> error("Unsupported scenario type: ${this@scenario}")
                }
             }
        }
    }

    @DefaultStrategyAndPlatformAgnosticScenarioTest
    @DisplayName("KT-89857: Changing a typealias behind an alias-of-alias should recompile the unchanged file whose signature carries its expansion")
    @TestMetadata("ic-scenarios/typealias-chain-expansion-in-unchanged-signature")
    fun testChangingTypealiasBehindAliasChainInUnchangedSignature(scenario: ScenarioCreator) {
        scenario {
            val mod = module("ic-scenarios/typealias-chain-expansion-in-unchanged-signature")

            mod.replaceFileWithVersion("alias.kt", "change-alias-type")
            mod.replaceFileWithVersion("ServiceUsage.kt", "change-alias-type")

            mod.compile {
                when (this@scenario) {
                    is JvmScenarioDsl -> assertCompiledSources("Service.kt", "serviceAlias.kt", "alias.kt", "ServiceUsage.kt")
                    is JsScenarioDsl, is WasmScenarioDsl -> {
                        // TODO(KT-89851): the klib IC cache does not track typealiases, so `Service.kt` does not get into
                        //  the dirty set, and `ServiceUsage.kt` is checked against the stale expansion.
                        expectFailWithError(
                            ".*ServiceUsage\\.kt:\\d+:\\d+ Argument type mismatch: actual type is 'Int', but 'String' was expected.*".toRegex()
                        )
                        assertCompiledSources("alias.kt", "ServiceUsage.kt")
                    }
                    else -> error("Unsupported scenario type: ${this@scenario}")
                }
            }
        }
    }

    @DefaultStrategyAndPlatformAgnosticScenarioTest
    @DisplayName("KT-89857: Changing a typealias should recompile the unchanged file whose inferred type carries its expansion")
    @TestMetadata("ic-scenarios/typealias-expansion-through-inferred-type")
    fun testChangingTypealiasBehindInferredTypeInUnchangedSignature(scenario: ScenarioCreator) {
        scenario {
            val mod = module("ic-scenarios/typealias-expansion-through-inferred-type")

            mod.replaceFileWithVersion("alias.kt", "change-alias-type")
            mod.replaceFileWithVersion("ServiceUsage.kt", "change-alias-type")

            mod.compile {
                expectFailWithError(
                    ".*ServiceUsage\\.kt:\\d+:\\d+ Argument type mismatch: actual type is 'Int', but 'String' was expected.*".toRegex()
                )
                when (this@scenario) {
                    // TODO(KT-91450): the error recovery round retries only the sources that look up the typealias, so it adds
                    //  `sink.kt`, but not `Service.kt`, whose property type is inferred from `makeSink()`. Once fixed, it
                    //  has to succeed with `assertCompiledSources("alias.kt", "sink.kt", "Service.kt", "ServiceUsage.kt")`.
                    is JvmScenarioDsl -> assertCompiledSources("alias.kt", "sink.kt", "ServiceUsage.kt")
                    // TODO(KT-89851): the klib IC cache does not track typealiases, so there is no error recovery round.
                    is JsScenarioDsl, is WasmScenarioDsl -> assertCompiledSources("alias.kt", "ServiceUsage.kt")
                    else -> error("Unsupported scenario type: ${this@scenario}")
                }
            }
        }
    }

    @DefaultStrategyAndPlatformAgnosticScenarioTest
    @DisplayName("KT-86585: Replacing a typealias with a value class should recompile the unchanged file using it as a parameter type")
    @TestMetadata("ic-scenarios/typealias-to-value-class")
    fun testReplacingTypealiasWithValueClass(scenario: ScenarioCreator) {
        scenario {
            val mod = module("ic-scenarios/typealias-to-value-class")

            mod.replaceFileWithVersion("Foo.kt", "change-to-value-class")
            mod.replaceFileWithVersion("Oof.kt", "change-to-value-class")

            mod.compile {
                when (this@scenario) {
                    is JvmScenarioDsl -> assertCompiledSources("Foo.kt", "Bar.kt", "Oof.kt")
                    is JsScenarioDsl, is WasmScenarioDsl -> {
                        // TODO(KT-89851): the klib IC cache does not track typealiases, so `Bar.kt` does not get into
                        //  the dirty set, and `Oof.kt` is checked against the stale `bar(HashMap<String, String>)`.
                        expectFailWithError(
                            ".*Oof\\.kt:\\d+:\\d+ Argument type mismatch: actual type is 'Foo', but 'HashMap<String, String>' was expected.*".toRegex()
                        )
                        assertCompiledSources("Foo.kt", "Oof.kt")
                    }
                    else -> error("Unsupported scenario type: ${this@scenario}")
                }
            }
        }
    }

    @DefaultStrategyAndPlatformAgnosticScenarioTest
    @DisplayName("Replacing a value class with a typealias should recompile the unchanged file using it as a parameter type")
    @TestMetadata("ic-scenarios/value-class-to-typealias")
    fun testReplacingValueClassWithTypealias(scenario: ScenarioCreator) {
        scenario {
            // `@JvmInline` is required on JVM, but cannot be used in non-JVM sources
            val mod = when (this@scenario) {
                is JvmScenarioDsl -> module("ic-scenarios/value-class-to-typealias")
                is JsScenarioDsl, is WasmScenarioDsl -> module("ic-scenarios/value-class-to-typealias-klib")
                else -> error("Unsupported scenario type: ${this@scenario}")
            }

            mod.replaceFileWithVersion("Foo.kt", "change-to-typealias")
            mod.replaceFileWithVersion("Oof.kt", "change-to-typealias")

            mod.compile {
                assertCompiledSources("Foo.kt", "Bar.kt", "Oof.kt")
            }
        }
    }

    @DefaultStrategyAndPlatformAgnosticScenarioTest
    @DisplayName("KT-28233: Changing a typealias should recompile the interface using it as a member return type")
    @TestMetadata("ic-scenarios/typealias-in-interface-member-type")
    fun testChangingTypealiasUsedAsInterfaceMemberType(scenario: ScenarioCreator) {
        scenario {
            val mod = module("ic-scenarios/typealias-in-interface-member-type")

            mod.replaceFileWithVersion("types.kt", "change-alias-type")
            mod.replaceFileWithVersion("Derived.kt", "change-alias-type")

            mod.compile {
                when (this@scenario) {
                    is JvmScenarioDsl -> assertCompiledSources("Base.kt", "Derived.kt", "types.kt")
                    is JsScenarioDsl, is WasmScenarioDsl -> {
                        // TODO(KT-89851): the klib IC cache does not track typealiases, so `Base.kt` does not get into
                        //  the dirty set, and the new `Derived.foo(): String` is checked against the stale `Base.foo(): Int`.
                        expectFailWithError(".*Derived\\.kt:\\d+:\\d+ Return type of .* is not a subtype.*".toRegex())
                        assertCompiledSources("Derived.kt", "types.kt")
                    }
                    else -> error("Unsupported scenario type: ${this@scenario}")
                }
            }
        }
    }

    @DefaultStrategyAndPlatformAgnosticScenarioTest
    @DisplayName("KT-28233: Changing a typealias in a non-root package should recompile the interface using it as a member return type")
    @TestMetadata("ic-scenarios/typealias-in-interface-member-type-in-package")
    fun testChangingTypealiasInPackageUsedAsInterfaceMemberType(scenario: ScenarioCreator) {
        scenario {
            val mod = module("ic-scenarios/typealias-in-interface-member-type-in-package")

            mod.replaceFileWithVersion("types.kt", "change-alias-type")
            mod.replaceFileWithVersion("Derived.kt", "change-alias-type")

            mod.compile {
                when (this@scenario) {
                    is JvmScenarioDsl -> assertCompiledSources("Base.kt", "Derived.kt", "types.kt")
                    is JsScenarioDsl, is WasmScenarioDsl -> {
                        // TODO(KT-89851): the klib IC cache does not track typealiases, so `Base.kt` does not get into
                        //  the dirty set, and the new `Derived.foo(): String` is checked against the stale `Base.foo(): Int`.
                        expectFailWithError(".*Derived\\.kt:\\d+:\\d+ Return type of .* is not a subtype.*".toRegex())
                        assertCompiledSources("Derived.kt", "types.kt")
                    }
                    else -> error("Unsupported scenario type: ${this@scenario}")
                }
            }
        }
    }
}
