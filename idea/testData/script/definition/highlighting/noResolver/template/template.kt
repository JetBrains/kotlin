package custom.scriptDefinition

import java.io.File
import kotlin.script.dependencies.*
import kotlin.script.templates.ScriptTemplateDefinition
import kotlin.script.experimental.location.*

@ScriptExpectedLocations([ScriptExpectedLocation.Everywhere])
@ScriptTemplateDefinition(scriptFilePattern = "script.kts")
class Template: Base()

open class Base {
    val i = 3
    val str = ""
}