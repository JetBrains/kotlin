/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.tests.compilation

import org.jetbrains.kotlin.buildtools.tests.compilation.assertions.assertCompiledSources
import org.jetbrains.kotlin.buildtools.tests.compilation.assertions.expectFailWithError
import org.jetbrains.kotlin.buildtools.tests.compilation.model.DefaultStrategyAndPlatformAgnosticScenarioTest
import org.jetbrains.kotlin.buildtools.tests.compilation.model.ScenarioCreator
import org.jetbrains.kotlin.test.TestMetadata
import org.junit.jupiter.api.DisplayName

@DisplayName("Typealias changes in incremental compilation")
class TypealiasChangesTest : BaseCompilationTest() {
    @DefaultStrategyAndPlatformAgnosticScenarioTest
    @DisplayName("KT-28233: Changing a typealias should recompile the unchanged file whose signature carries its expansion")
    @TestMetadata("ic-scenarios/typealias-expansion-in-unchanged-signature")
    fun testChangingTypealiasBehindUnchangedSignature(scenario: ScenarioCreator) {
        scenario {
            val mod = module("ic-scenarios/typealias-expansion-in-unchanged-signature")

            mod.replaceFileWithVersion("alias.kt", "change-alias-type")
            mod.replaceFileWithVersion("ServiceUsage.kt", "change-alias-type")
            mod.replaceFileWithVersion("AliasUsage.kt", "change-alias-type")

            mod.compile {
                // TODO(KT-28233): `Service.kt` does not get into the dirty set, so `ServiceUsage.kt` is checked against
                //  the stale expansion and the build fails, unlike a clean build. Once fixed, it has to succeed with
                //  `assertCompiledSources("Service.kt", "alias.kt", "ServiceUsage.kt", "AliasUsage.kt")`.
                expectFailWithError(
                    ".*ServiceUsage\\.kt:\\d+:\\d+ Argument type mismatch: actual type is 'Int', but 'String' was expected.*".toRegex()
                )
                assertCompiledSources("alias.kt", "ServiceUsage.kt", "AliasUsage.kt")
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
                // TODO(KT-28233): `Base.kt` does not get into the dirty set, so the new `Derived.foo(): String` is
                //  checked against the stale `Base.foo(): Int` and the build fails, unlike a clean build.
                //  Once fixed, it has to succeed with `assertCompiledSources("Base.kt", "Derived.kt", "types.kt")`.
                expectFailWithError(".*Derived\\.kt:\\d+:\\d+ Return type of .* is not a subtype.*".toRegex())
                assertCompiledSources("Derived.kt", "types.kt")
            }
        }
    }
}
