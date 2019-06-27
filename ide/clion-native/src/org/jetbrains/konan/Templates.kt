package org.jetbrains.konan

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.SystemInfoRt.isMac
import com.intellij.openapi.util.SystemInfoRt.isWindows
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.io.systemIndependentPath
import org.intellij.markdown.flavours.gfm.GFMFlavourDescriptor
import org.intellij.markdown.html.HtmlGenerator
import org.intellij.markdown.parser.MarkdownParser
import org.jetbrains.kotlin.idea.KotlinPluginUtil
import java.io.File
import java.io.IOException
import java.util.*

class KonanProjectTemplate(
        private val templateDir: File,
        val visibleName: String,
        private val filesToOpenInEditor: Set<String>,
        private val runtimeTemplateFiles: Set<String>,
        private val weight: Int, // the lesser, the more preferred
        private val readme: String?
) : Comparable<KonanProjectTemplate> {

    class Loaded(val templateFiles: List<TemplateFile>)

    class TemplateFile(
            private val relativePath: File,
            val openInEditor: Boolean,
            private val binaryContent: ByteArray
    ) {
        fun create(base: VirtualFile): VirtualFile {
            val dir = VfsUtil.createDirectoryIfMissing(base, relativePath.toPath().parent?.systemIndependentPath.orEmpty())
            return dir.findOrCreateChildData(this, relativePath.name).apply {
                setBinaryContent(binaryContent)
            }
        }
    }

    override fun compareTo(other: KonanProjectTemplate) = (weight - other.weight).takeIf { it != 0 } ?: visibleName.compareTo(other.visibleName)

    val htmlDescription: String?
        get() {
            if (readme == null) return null
            val flavour = GFMFlavourDescriptor()
            val root = MarkdownParser(flavour).buildMarkdownTreeFromString(readme)
            return HtmlGenerator(readme, root, flavour).generateHtml()
        }

    fun load(): Loaded {
        val extraFiles = templateDir.walkTopDown()
                .filter { it.isFile }
                .mapNotNull { file ->
                    if (file.name == TEMPLATE_INFO_FILE) return@mapNotNull null // skip

                    val relPath = file.relativeTo(templateDir)
                    val openInEditor = relPath.path in filesToOpenInEditor

                    val fileContent = if (relPath.path in runtimeTemplateFiles)
                        renderTemplate(file)
                    else
                        file.readBytes()

                    TemplateFile(relPath, openInEditor, fileContent)
                }
                .toList()

        return Loaded(extraFiles)
    }

    companion object {
        fun listAll(): List<KonanProjectTemplate> = withProjectTemplateDirectory { projectTemplatesDir ->
            val gradleTemplatesDir = projectTemplatesDir.resolve("gradle")
            check(gradleTemplatesDir.isDirectory)

            gradleTemplatesDir.listFiles()
                    ?.filter { it.isDirectory }
                    ?.mapNotNull(::getTemplate)
                    ?.sorted() ?: emptyList()
        }
    }
}

private val LOG = Logger.getInstance(KonanProjectTemplate::class.java)

private const val TEMPLATE_INFO_FILE = "template.info"

private const val TEMPLATE_INFO_PROPERTY_NAME = "name"
private const val TEMPLATE_INFO_PROPERTY_OPEN_IN_EDITOR = "openInEditor"
private const val TEMPLATE_INFO_PROPERTY_RUNTIME_TEMPLATES = "runtimeTemplates"
private const val TEMPLATE_INFO_PROPERTY_WEIGHT = "weight"

private val RUNTIME_TEMPLATE_PARAMETERS = mapOf(
        "RUNTIME_MPP_HOST_PLATFORM" to when {
            isWindows -> "mingwX64"
            isMac -> "macosX64"
            else -> "linuxX64"
        }
)

private fun <T> withProjectTemplateDirectory(action: (File) -> T): T {
    val projectTemplatesDir = KotlinPluginUtil.getPluginPath().resolve("templates")
    check(projectTemplatesDir.isDirectory) { "Project templates directory does not exist: $projectTemplatesDir" }
    return action(projectTemplatesDir)
}

private fun getTemplate(templateDir: File) : KonanProjectTemplate? {
    val templateInfo = Properties().apply {
        try {
            val templateInfoFile = templateDir.resolve(TEMPLATE_INFO_FILE)
            templateInfoFile.inputStream().use { load(it) }
        } catch (_: IOException) {}
    }

    fun <T : Any> Properties.getTyped(name: String, default: T, transform: (String) -> T?): T =
            this[name]?.toString()?.let { transform(it) } ?: default

    val name = templateInfo.getTyped(TEMPLATE_INFO_PROPERTY_NAME, templateDir.name) { it }
    val filesToOpenInEditor = templateInfo.getTyped(TEMPLATE_INFO_PROPERTY_OPEN_IN_EDITOR, emptySet()) { it.split(',').toSet() }
    val runtimeTemplateFiles = templateInfo.getTyped(TEMPLATE_INFO_PROPERTY_RUNTIME_TEMPLATES, emptySet()) { it.split(',').toSet() }
    val weight = templateInfo.getTyped(TEMPLATE_INFO_PROPERTY_WEIGHT, 100) { it.toIntOrNull() }

    val readme: String? = try {
        templateDir.listFiles()
                ?.firstOrNull { it.name.toLowerCase() == "readme.md" }
                ?.readText(Charsets.UTF_8)
    } catch (_: IOException) {
        null
    }

    return KonanProjectTemplate(
            templateDir = templateDir,
            visibleName = name,
            filesToOpenInEditor = filesToOpenInEditor,
            runtimeTemplateFiles = runtimeTemplateFiles,
            weight = weight,
            readme = readme
    )
}

private fun renderTemplate(template: File): ByteArray {
    val templateText = template.readText()
    var result = templateText

    val usedParameters = mutableListOf<String>()

    RUNTIME_TEMPLATE_PARAMETERS.entries.forEach { (name, value) ->
        val temp = result.replace("@@$name@@", value)
        if (result != temp) usedParameters += name
        result = temp
    }

    if (usedParameters.isEmpty())
        LOG.info("Project template \"$template\" was not rendered. It does not include any parameters.")
    else
        LOG.info("Project template \"$template\" was rendered with the following parameters: $usedParameters")

    if (result.isBlank() && templateText.isNotBlank())
        error("Project template rendering resulted is blank string, however template file is not blank: $template")

    return result.toByteArray()
}
