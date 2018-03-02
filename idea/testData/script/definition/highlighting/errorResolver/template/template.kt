package custom.scriptDefinition

import java.io.File
import kotlin.script.dependencies.*
import kotlin.script.experimental.dependencies.*
import kotlin.script.templates.ScriptTemplateDefinition

class TestDependenciesResolver : DependenciesResolver {
    override fun resolve(
            scriptContents: ScriptContents,
            environment: Environment
    ): DependenciesResolver.ResolveResult {
        return DependenciesResolver.ResolveResult.Failure(
            ScriptReport("Error"),
            ScriptReport("Info", ScriptReport.Severity.INFO),
            ScriptReport("Warning", ScriptReport.Severity.WARNING),
            ScriptReport("Debug", ScriptReport.Severity.DEBUG)
        )
    }
}

@ScriptTemplateDefinition(TestDependenciesResolver::class, scriptFilePattern = "script.kts")
open class Template