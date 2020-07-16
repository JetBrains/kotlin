// WITH_RUNTIME
// FULL_JDK

class CliOption(
    val optionName: String,
    val valueDescription: String,
    val description: String,
    val required: Boolean = true,
    val allowMultipleOccurrences: Boolean = false
) {
    @Deprecated("Use optionName instead.", ReplaceWith("optionName"))
    val name: String
        get() = optionName
}


class SamWithReceiverCommandLineProcessor {
    companion object {
        val SUPPORTED_PRESETS = emptyMap<String, List<String>>()

        val ANNOTATION_OPTION = CliOption("annotation", "<fqname>", "Annotation qualified names",
                                          required = false, allowMultipleOccurrences = true)

        val PRESET_OPTION = CliOption("preset", "<name>", "Preset name (${SUPPORTED_PRESETS.keys.joinToString()})",
                                      required = false, allowMultipleOccurrences = true)

        val PLUGIN_ID = "org.jetbrains.kotlin.samWithReceiver"
    }

    val pluginId = PLUGIN_ID
    val pluginOptions = listOf(ANNOTATION_OPTION)
}

class PluginOption(val name: String, val annotation: String)

interface KotlinFacet
interface MavenProject

interface MavenProjectImportHandler {
    operator fun invoke(facet: KotlinFacet, mavenProject: MavenProject)
}

abstract class AbstractMavenImportHandler : MavenProjectImportHandler {
    abstract val compilerPluginId: String
    abstract val pluginName: String
    abstract val mavenPluginArtifactName: String
    abstract val pluginJarFileFromIdea: String

    override fun invoke(facet: KotlinFacet, mavenProject: MavenProject) {}

    abstract fun getOptions(enabledCompilerPlugins: List<String>, compilerPluginOptions: List<String>): List<PluginOption>?
}

class SamWithReceiverMavenProjectImportHandler : AbstractMavenImportHandler() {
    private companion object {
        val ANNOTATION_PARAMETER_PREFIX = "sam-with-receiver:${SamWithReceiverCommandLineProcessor.ANNOTATION_OPTION.optionName}="
    }

    override val compilerPluginId = SamWithReceiverCommandLineProcessor.PLUGIN_ID
    override val pluginName = "samWithReceiver"
    override val mavenPluginArtifactName = "kotlin-maven-sam-with-receiver"
    override val pluginJarFileFromIdea = "PathUtil.kotlinPathsForIdeaPlugin.samWithReceiverJarPath"

    override fun getOptions(enabledCompilerPlugins: List<String>, compilerPluginOptions: List<String>): List<PluginOption>? {
        if ("sam-with-receiver" !in enabledCompilerPlugins) {
            return null
        }

        val annotations = mutableListOf<String>()

        for ((presetName, presetAnnotations) in SamWithReceiverCommandLineProcessor.SUPPORTED_PRESETS) {
            if (presetName in enabledCompilerPlugins) {
                annotations.addAll(presetAnnotations)
            }
        }

        annotations.addAll(compilerPluginOptions.mapNotNull { text ->
            if (!text.startsWith(ANNOTATION_PARAMETER_PREFIX)) return@mapNotNull null
            text.substring(ANNOTATION_PARAMETER_PREFIX.length)
        })

        return annotations.map { PluginOption(SamWithReceiverCommandLineProcessor.ANNOTATION_OPTION.optionName, it) }
    }
}
