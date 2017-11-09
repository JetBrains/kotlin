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
        error("Exception from resolver")
    }
}

@ScriptTemplateDefinition(TestDependenciesResolver::class, scriptFilePattern = "script.kts")
open class Template