import org.jetbrains.kotlin.buildtools.tests.CompilerExecutionStrategyConfiguration
import org.jetbrains.kotlin.buildtools.tests.compilation.BaseCompilationTest
import org.jetbrains.kotlin.buildtools.tests.compilation.assertions.assertCompiledSources
import org.jetbrains.kotlin.buildtools.tests.compilation.model.DefaultStrategyAgnosticCompilationTest
import org.jetbrains.kotlin.buildtools.tests.compilation.scenario.jvmScenario
import org.jetbrains.kotlin.test.TestMetadata
import org.junit.jupiter.api.DisplayName

@DisplayName("Class kind changes in incremental compilation")
class ClassKindChangesTest : BaseCompilationTest() {
    @DefaultStrategyAgnosticCompilationTest
    @DisplayName("KT-76515: Replacing a file with a @JvmField top-level property with an object should recompile usages")
    @TestMetadata("ic-scenarios/kt-76515")
    fun testReplacingJvmFieldTopLevelPropertyWithObjectRecompilesUsages(strategyConfig: CompilerExecutionStrategyConfiguration) {
        jvmScenario(strategyConfig) {
            val mod = module("ic-scenarios/kt-76515")

            mod.replaceFileWithVersion("Provider.kt", "change-to-object")
            mod.compile {
                expectFail()
                assertCompiledSources("Provider.kt", "Usage.kt")
            }
        }
    }
}
