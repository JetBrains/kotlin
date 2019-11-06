package custom.scriptDefinition

import kotlin.script.dependencies.*
import kotlin.script.experimental.dependencies.*
import kotlin.script.templates.*
import java.io.File
import kotlin.script.experimental.location.*

class TestDependenciesResolver : AsyncDependenciesResolver {
    suspend override fun resolveAsync(scriptContents: ScriptContents, environment: Environment): DependenciesResolver.ResolveResult {
        val text = scriptContents.text as String

        val result = when {
            text.startsWith("#BAD:") -> DependenciesResolver.ResolveResult.Failure(
                reports = listOf(ScriptReport(text))
            )
            else -> DependenciesResolver.ResolveResult.Success(
                dependencies = ScriptDependencies(
                    classpath = listOf(environment["template-classes"] as File),
                    imports = listOf("x_" + text.replace(Regex("#IGNORE_IN_CONFIGURATION"), ""))
                ),
                reports = listOf(ScriptReport(text))
            )
        }

        javaClass.classLoader
            .loadClass("org.jetbrains.kotlin.idea.script.ScriptConfigurationLoadingTest")
            .methods.single { it.name == "loadingScriptConfigurationCallback" }
            .invoke(null)

        return result
    }
}

@ScriptExpectedLocations([ScriptExpectedLocation.Everywhere])
@ScriptTemplateDefinition(TestDependenciesResolver::class, scriptFilePattern = "script.kts")
class Template : Base()

open class Base {
    val i = 3
    val str = ""
}