package custom.scriptDefinition

import java.io.File
import kotlin.script.dependencies.*
import kotlin.script.experimental.dependencies.*
import kotlin.script.templates.ScriptTemplateDefinition

class TestDependenciesResolver : DependenciesResolver {
    @kotlin.script.templates.AcceptedAnnotations(Anno::class)
    override fun resolve(
            scriptContents: ScriptContents,
            environment: Environment
    ): DependenciesResolver.ResolveResult {
        val annoFQN = Anno::class.qualifiedName!!
        assert(scriptContents.annotations.single().annotationClass.qualifiedName == annoFQN)
        return ScriptDependencies(
                classpath = listOf(environment["template-classes"] as File),
                imports = listOf(annoFQN)
        ).asSuccess()
    }
}

@Target(AnnotationTarget.FILE)
annotation class Anno

@Target(AnnotationTarget.FILE)
annotation class Anno2

@ScriptTemplateDefinition(TestDependenciesResolver::class, scriptFilePattern = "script.kts")
class Template