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
        error("Exception from resolver")
    }
}

@ScriptExpectedLocations([ScriptExpectedLocation.Everywhere])
@ScriptTemplateDefinition(TestDependenciesResolver::class, scriptFilePattern = "script.kts")
open class Template