package org.jetbrains.konan

import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.io.createSymbolicLink
import com.intellij.util.io.systemIndependentPath
import org.intellij.markdown.flavours.gfm.GFMFlavourDescriptor
import org.intellij.markdown.html.HtmlGenerator
import org.intellij.markdown.parser.MarkdownParser
import org.jetbrains.konan.util.CidrKotlinReleaseType.RELEASE
import org.jetbrains.konan.util.TEMPLATE_ARGUMENTS_CIDR_NEW_PROJECT
import org.jetbrains.konan.util.cidrKotlinPlugin
import org.jetbrains.konan.util.defaultCidrKotlinVersion
import org.jetbrains.konan.util.mergeTemplate
import org.jetbrains.kotlin.utils.addIfNotNull
import java.io.File
import java.io.IOException
import java.nio.file.Paths
import java.util.*

class KonanProjectTemplate(
        private val templateDir: File,
        val visibleName: String,
        private val weight: Int, // the lesser, the more preferred
        private val readme: String?
) : Comparable<KonanProjectTemplate> {

    override fun compareTo(other: KonanProjectTemplate) = (weight - other.weight).takeIf { it != 0 } ?: visibleName.compareTo(other.visibleName)

    class Loaded(
            val extraFiles: List<TemplateFile>
    )

    interface TemplateFile {
        fun create(base: VirtualFile): VirtualFile {
            val dir = VfsUtil.createDirectoryIfMissing(base, relativePath.toPath().parent?.systemIndependentPath ?: "")
            return dir.findOrCreateChildData(this, relativePath.name).apply {
                setBinaryContent(content())
            }
        }

        val relativePath: File
        val openInEditor: Boolean

        fun content(): ByteArray
    }

    class TemplateTextFile(
            override val relativePath: File,
            private val textContent: String,
            override val openInEditor: Boolean
    ) : TemplateFile {
        override fun content() = textContent.toByteArray(Charsets.UTF_8)
    }

    class TemplateBinaryFile(
            override val relativePath: File,
            private val binaryContent: ByteArray
    ) : TemplateFile {
        override val openInEditor: Boolean = false
        override fun content(): ByteArray = binaryContent
    }

    class TemplateSymlinkFile(
            override val relativePath: File,
            private val link: String
    ) : TemplateFile {
        override val openInEditor: Boolean = false
        override fun content(): ByteArray = byteArrayOf()

        override fun create(base: VirtualFile): VirtualFile {
            val targetPath = Paths.get(base.path, relativePath.toString(), link)
            val linkPath = Paths.get(base.path, relativePath.toString(), targetPath.fileName.toString())
            linkPath.createSymbolicLink(targetPath)
            return base
        }
    }

    val htmlDescription: String?
        get() {
            if (readme == null) return null
            val flavour = GFMFlavourDescriptor()
            val root = MarkdownParser(flavour).buildMarkdownTreeFromString(readme)
            return HtmlGenerator(readme, root, flavour).generateHtml()
        }

    fun load(): Loaded {
        val extraFiles = mutableListOf<TemplateFile>()

        templateDir.walkTopDown().filter { it.isFile }.forEach { file ->
            val relPath = file.relativeTo(templateDir)

            when {
                file.name == TEMPLATE_SYMLINKS_FILE -> { /* Do we still need this? */
                    file.readLines().forEach { link ->
                        extraFiles += TemplateSymlinkFile(relPath.parentFile, link)
                    }
                }
                file.name == TEMPLATE_INFO_FILE -> { /* Skip */ }
                file.extension in MAYBE_TEMPLATE_FILES -> {
                    extraFiles.addIfNotNull(getTemplateTextFile(file, relPath))
                }
                else -> {
                    extraFiles += TemplateBinaryFile(relPath, file.readBytes())
                }
            }
        }

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

        private const val TEMPLATE_SYMLINKS_FILE = "template.symlinks"
        private const val TEMPLATE_INFO_FILE = "template.info"

        private const val TEMPLATE_INFO_PROPERTY_NAME = "name"
        private const val TEMPLATE_INFO_PROPERTY_WEIGHT = "weight"

        private val MAYBE_TEMPLATE_FILES = setOf("kt", "kts")

        private const val FILE_MARKER_OPEN_IN_EDITOR = "OPEN-IN-EDITOR"
        private const val FILE_MARKER_SKIP_IF_RELEASE = "SKIP-IF-RELEASE"

        private fun getTemplateTextFile(file: File, relPath: File): TemplateTextFile? {
            val fileContents = file.readText()
            val firstLine = fileContents.substringBefore('\n')

            val skipIfRelease = FILE_MARKER_SKIP_IF_RELEASE in firstLine
            if (skipIfRelease && defaultCidrKotlinVersion.releaseType == RELEASE)
                return null

            val openInEditor = FILE_MARKER_OPEN_IN_EDITOR in firstLine

            val meaningfulFileContents = if (skipIfRelease || openInEditor) fileContents.substringAfter('\n') else fileContents
            val renderedText = mergeTemplate(meaningfulFileContents, TEMPLATE_ARGUMENTS_CIDR_NEW_PROJECT)

            return TemplateTextFile(relPath, renderedText, openInEditor)
        }

        private fun getTemplate(templateDir: File) : KonanProjectTemplate? {
            val templateInfo = try {
                val templateInfoFile = templateDir.resolve(TEMPLATE_INFO_FILE).takeIf { it.isFile } ?: return null
                templateInfoFile.inputStream().use { istream -> Properties().also { it.load(istream) } }
            } catch (_: IOException) {
                null
            }

            val name = templateInfo?.get(TEMPLATE_INFO_PROPERTY_NAME)?.toString() ?: templateDir.name
            val weight = templateInfo?.get(TEMPLATE_INFO_PROPERTY_WEIGHT)?.toString()?.toInt() ?: 100

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
                    weight = weight,
                    readme = readme
            )
        }
    }
}

private fun <T> withProjectTemplateDirectory(f: (File) -> T): T {
    val projectTemplatesDir = Paths.get(cidrKotlinPlugin.path.path, "templates/").toFile()
    FileUtil.ensureExists(projectTemplatesDir)
    return f(projectTemplatesDir)
}
