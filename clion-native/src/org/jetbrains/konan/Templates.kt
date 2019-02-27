package org.jetbrains.konan

import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.io.createSymbolicLink
import com.intellij.util.io.exists
import com.intellij.util.io.readText
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
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes
import java.util.stream.Collectors

class KonanProjectTemplate(
        val name: String,
        private val readme: String?

) {
    class Loaded(
            val extraFiles: List<TemplateFile>
    )

    interface TemplateFile {
        fun create(base: VirtualFile): VirtualFile {
            val dir = VfsUtil.createDirectoryIfMissing(base, relativePath.parent?.systemIndependentPath ?: "")
            return dir.findOrCreateChildData(this, relativePath.fileName.toString()).apply {
                setBinaryContent(content())
            }
        }

        val relativePath: Path
        val openInEditor: Boolean

        fun content(): ByteArray
    }

    class TemplateTextFile(
            override val relativePath: Path,
            private val textContent: String,
            override val openInEditor: Boolean
    ) : TemplateFile {
        override fun content() = textContent.toByteArray(Charsets.UTF_8)
    }

    class TemplateBinaryFile(
            override val relativePath: Path,
            private val binaryContent: ByteArray
    ) : TemplateFile {
        override val openInEditor: Boolean = false
        override fun content(): ByteArray = binaryContent
    }

    class TemplateSymlinkFile(
            override val relativePath: Path,
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

    fun load(): Loaded = withProjectTemplateDirectory { templatesDir ->
        val extraFiles = mutableListOf<TemplateFile>()
        val templateDir = templatesDir.resolve("gradle/$name")

        Files.walkFileTree(templateDir, object : SimpleFileVisitor<Path>() {
            override fun visitFile(filePath: Path, attributes: BasicFileAttributes): FileVisitResult {
                val file = filePath.toFile()
                val relPath = templateDir.relativize(filePath)

                when {
                    file.name == "template.symlinks" -> { /* Do we still need this? */
                        file.readLines().forEach { link ->
                            extraFiles += TemplateSymlinkFile(relPath.parent, link)
                        }
                    }
                    file.extension in MAYBE_TEMPLATE_FILES -> {
                        extraFiles.addIfNotNull(getTemplateTextFile(file, relPath))
                    }
                    else -> {
                        extraFiles += TemplateBinaryFile(relPath, file.readBytes())
                    }
                }
                return FileVisitResult.CONTINUE
            }
        })

        Loaded(extraFiles)
    }

    companion object {
        fun listAll(): List<KonanProjectTemplate> = withProjectTemplateDirectory { projectTemplatesDir ->
            val allTemplates = projectTemplatesDir.resolve("gradle")
            check(allTemplates.exists())

            Files.list(allTemplates).collect(Collectors.toList()).map { path ->
                val readme: String? = try {
                    Files.list(path)
                            .filter { it.fileName.toString().toLowerCase() == "readme.md" }
                            .findAny()
                            .orElse(null)
                            ?.readText()
                } catch (_: IOException) {
                    null
                }
                KonanProjectTemplate(path.fileName.toString(), readme)
            }
        }

        private val MAYBE_TEMPLATE_FILES = setOf("kt", "kts")

        private const val FILE_MARKER_OPEN_IN_EDITOR = "OPEN-IN-EDITOR"
        private const val FILE_MARKER_SKIP_IF_RELEASE = "SKIP-IF-RELEASE"

        private fun getTemplateTextFile(file: File, relPath: Path): TemplateTextFile? {
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
    }
}

private fun <T> withProjectTemplateDirectory(f: (Path) -> T): T {
    val projectTemplatesDir = Paths.get(cidrKotlinPlugin.path.path, "templates/")
    FileUtil.ensureExists(projectTemplatesDir.toFile())
    return f(projectTemplatesDir)
}
