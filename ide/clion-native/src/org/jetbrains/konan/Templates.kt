package org.jetbrains.konan

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
        private val openInEditor: Set<String>,
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
                    TemplateFile(relPath, relPath.path in openInEditor, file.readBytes())
                }
                .toList()

        return Loaded(extraFiles)
    }

    companion object {
        fun listAll(): List<KonanProjectTemplate> = withProjectTemplateDirectory { projectTemplatesDir ->
            val gradleTemplatesDir = projectTemplatesDir.resolve("gradle")
            check(gradleTemplatesDir.isDirectory)

            gradleTemplatesDir.listFiles()
                    .filter { it.isDirectory }
                    .mapNotNull(::getTemplate)
                    .sorted()
        }

        private const val TEMPLATE_INFO_FILE = "template.info"

        private const val TEMPLATE_INFO_PROPERTY_NAME = "name"
        private const val TEMPLATE_INFO_PROPERTY_OPEN_IN_EDITOR = "openInEditor"
        private const val TEMPLATE_INFO_PROPERTY_WEIGHT = "weight"

        private fun getTemplate(templateDir: File) : KonanProjectTemplate? {
            val templateInfo = Properties().apply {
                try {
                    val templateInfoFile = templateDir.resolve(TEMPLATE_INFO_FILE)
                    templateInfoFile.inputStream().use { load(it) }
                } catch (_: IOException) {}
            }

            val name = templateInfo[TEMPLATE_INFO_PROPERTY_NAME]?.toString() ?: templateDir.name
            val openInEditor = templateInfo[TEMPLATE_INFO_PROPERTY_OPEN_IN_EDITOR]?.toString()?.split(',')?.toSet() ?: emptySet()
            val weight = templateInfo[TEMPLATE_INFO_PROPERTY_WEIGHT]?.toString()?.toIntOrNull() ?: 100

            val readme: String? = try {
                templateDir.listFiles()
                        .firstOrNull { it.name.toLowerCase() == "readme.md" }
                        ?.readText(Charsets.UTF_8)
            } catch (_: IOException) {
                null
            }

            return KonanProjectTemplate(
                    templateDir = templateDir,
                    visibleName = name,
                    openInEditor = openInEditor,
                    weight = weight,
                    readme = readme
            )
        }
    }
}

private fun <T> withProjectTemplateDirectory(action: (File) -> T): T {
    val projectTemplatesDir = KotlinPluginUtil.getPluginPath().resolve("templates")
    check(projectTemplatesDir.isDirectory) { "Project templates directory does not exist: $projectTemplatesDir" }
    return action(projectTemplatesDir)
}
