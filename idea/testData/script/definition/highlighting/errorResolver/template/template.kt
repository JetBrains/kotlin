package custom.scriptDefinition

import java.io.File
import kotlin.script.dependencies.*
import kotlin.script.experimental.dependencies.*
import kotlin.script.templates.ScriptTemplateDefinition
import kotlin.script.experimental.location.*

class TestDependenciesResolver : DependenciesResolver {
    override fun resolve(
            scriptContents: ScriptContents,
            environment: Environment
    ): DependenciesResolver.ResolveResult {
        return DependenciesResolver.ResolveResult.Failure(
            ScriptReport("Error"),
            ScriptReport("Fatal", ScriptReport.Severity.FATAL),
            ScriptReport("Info", ScriptReport.Severity.INFO),
            ScriptReport("Warning", ScriptReport.Severity.WARNING),
            ScriptReport("Debug", ScriptReport.Severity.DEBUG)
        )
    }
}

@ScriptExpectedLocations([ScriptExpectedLocation.Everywhere])
@ScriptTemplateDefinition(TestDependenciesResolver::class, scriptFilePattern = "script.kts")
open class Template