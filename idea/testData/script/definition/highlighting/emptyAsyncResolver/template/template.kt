package custom.scriptDefinition

import java.io.File
import kotlin.script.dependencies.*
import kotlin.script.experimental.dependencies.*
import kotlin.script.templates.ScriptTemplateDefinition

class TestDependenciesResolver : AsyncDependenciesResolver {
    override suspend fun resolveAsync(
            scriptContents: ScriptContents,
            environment: Environment
    ): DependenciesResolver.ResolveResult {
        return ScriptDependencies.Empty.asSuccess()
    }
}

@ScriptTemplateDefinition(TestDependenciesResolver::class, scriptFilePattern = "script.kts")
open class Template: Base()

open class Base {
    val i = 3
    val str = ""
}