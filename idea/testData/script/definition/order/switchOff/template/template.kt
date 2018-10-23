package custom.scriptDefinition

import java.io.File
import kotlin.script.dependencies.*
import kotlin.script.experimental.dependencies.*
import kotlin.script.templates.ScriptTemplateDefinition

class SuccessDependenciesResolver : DependenciesResolver {
    override fun resolve(
            scriptContents: ScriptContents,
            environment: Environment
    ): DependenciesResolver.ResolveResult {
        return ScriptDependencies.Empty.asSuccess()
    }
}

class ErrorDependenciesResolver : DependenciesResolver {
    override fun resolve(
        scriptContents: ScriptContents,
        environment: Environment
    ): DependenciesResolver.ResolveResult {
        return DependenciesResolver.ResolveResult.Failure(ScriptReport("Error"))
    }
}

@ScriptTemplateDefinition(ErrorDependenciesResolver::class, scriptFilePattern = "script.kts")
open class Template1: Base()

@ScriptTemplateDefinition(SuccessDependenciesResolver::class, scriptFilePattern = "script.kts")
open class Template2: Base()

open class Base {
    val i = 3
    val str = ""
}