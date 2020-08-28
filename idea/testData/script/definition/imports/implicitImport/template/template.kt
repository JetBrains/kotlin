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
    }
)

data class Attr(val name: String, val clazz: Class<*>)

class KotlinPlatformType  {
    companion object {
        val attribute = Attr("org.gradle.usage", Attr::class.java)
    }
}

class AttributesBuilder {
    fun attribute(attribute: Attr, name: String) {}
}

class ConfigurationsBuilder {
    fun attributes(configuration: AttributesBuilder.() -> Unit) {
        AttributesBuilder().apply(configuration)
    }
}

class Configurations {
    fun creating(initializer: ConfigurationsBuilder.() -> Unit) {
        ConfigurationsBuilder().apply(initializer)
    }
}

open class Base {
    val configurations = Configurations()

    val USAGE_ATTRIBUTE = Attr("org.gradle.usage", Attr::class.java)

    val JAVA_RUNTIME = "java-runtime"
}