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
                    is JvmScenarioDsl -> {
                        // TODO(KT-89857): the error recovery round runs once and follows a single hop of typealias lookups:
                        //  it pulls in `serviceAlias.kt` (looks up `ErrorSink`), but not `Service.kt` (looks up `ServiceSink`
                        //  only), so the retry fails with the same stale-expansion error. Red until the recovery is iterated
                        //  or computes the transitive closure of typealias users.
                        assertCompiledSources("Service.kt", "serviceAlias.kt", "alias.kt", "ServiceUsage.kt")
                    }
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
