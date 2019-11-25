package custom.scriptDefinition

import kotlin.script.experimental.annotations.KotlinScript
import kotlin.script.experimental.jvm.dependenciesFromClassContext
import kotlin.script.experimental.jvm.jvm
import kotlin.script.experimental.api.*

@KotlinScript(
    displayName = "Definition for tests",
    fileExtension = "kts",
    compilationConfiguration = TemplateDefinition::class
)
open class Template(val args: Array<String>)

@Suppress("UNCHECKED_CAST")
object TemplateDefinition : ScriptCompilationConfiguration(
    {
        baseClass(Base::class)
        jvm {
            dependenciesFromClassContext(TemplateDefinition::class)
        }
        ide {
            acceptedLocations(ScriptAcceptedLocation.Everywhere)
        }
        refineConfiguration {
            beforeCompiling { (_, config, _) ->
                config.with {
                    implicitReceivers(ImplicitBase::class)
                }.asSuccess()
            }
        }
    }
)

open class Base {
    fun fooBase() {}
}

class ImplicitBase {
    fun fooImplicitBase() {}
}