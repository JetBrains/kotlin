package org.jetbrains.konan

import com.intellij.ide.plugins.PluginManager
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.io.*
import org.intellij.markdown.flavours.gfm.GFMFlavourDescriptor
import org.intellij.markdown.html.HtmlGenerator
import org.intellij.markdown.parser.MarkdownParser
import java.io.IOException
import java.nio.file.*
import java.nio.file.attribute.BasicFileAttributes
import java.util.stream.Collectors

class KonanTemplate(
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
        override fun content(): ByteArray {
            return textContent.toByteArray(Charsets.UTF_8)
        }
    }

    class TemplateBinaryFile(
            override val relativePath: Path,
            private val binaryContent: ByteArray
    ) : TemplateFile {
        override val openInEditor: Boolean = false
        override fun content(): ByteArray = binaryContent
    }

    class TemplateSymlinkFile(override val relativePath: Path,
                              private val link: String) : TemplateFile {
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

    fun load(): Loaded = withTemplateDirectory { templatesDir ->
        val extraFiles = mutableListOf<TemplateFile>()

        val templateDir = templatesDir.resolve("gradle/$name")
        Files.walkFileTree(templateDir, object : SimpleFileVisitor<Path>() {
            override fun visitFile(file: Path, attributes: BasicFileAttributes): FileVisitResult {
                val relPath = templateDir.relativize(file)
                val binaries = listOf(".bmp", ".png", ".wav", ".jar")

                fun addTextFile(file: Path) {
                    val lines = file.readText().lines()
                    val openInEditor = lines.any { "IDE main file" in it }
                    var text = lines.filter { "IDE main file" !in it }.joinToString("\n")
                    val platform = when {
                        SystemInfo.isMac -> "macosX64"
                        SystemInfo.isWindows -> "mingwX64"
                        else -> "linuxX64"
                    }
                    text = text.replace("{% platform %}", platform)
                    extraFiles += TemplateTextFile(relPath, text, openInEditor)
                }

                when {
                    file.fileName.toString() == "template.symlinks" -> {
                        val list = file.readText().lines()
                        list.forEach { link ->
                            extraFiles += TemplateSymlinkFile(relPath.parent, link)
                        }
                    }
                    binaries.any { file.fileName.toString().endsWith(it) } -> {
                        val binaryContent = file.readBytes()
                        extraFiles += TemplateBinaryFile(relPath, binaryContent)
                    }
                    else -> addTextFile(file)
                }
                return FileVisitResult.CONTINUE
            }
        })

        Loaded(extraFiles)
    }

    companion object {
        fun listAll(): List<KonanTemplate> = withTemplateDirectory { templatesDir ->
            val allTemplates = templatesDir.resolve("gradle")
            check(allTemplates.exists())

            Files.list(allTemplates).collect(Collectors.toList()).map { path ->
                val readme = try {
                    Files.list(path)
                      .filter { it.fileName.toString().toLowerCase() == "readme.md" }
                      .findAny()
                      .orElse(null)
                      ?.readText()
                }
                catch (_: IOException) {
                    null
                }
                KonanTemplate(path.fileName.toString(), readme)
            }
        }
    }
}

private fun <T> withTemplateDirectory(f: (Path) -> T): T {
    val id = PluginManager.getPluginByClassName(KonanTemplate::class.java.name)
    val pluginFile = PluginManager.getPlugin(id)!!.path
    val templatesDir = Paths.get(pluginFile.path, "templates/")
    FileUtil.ensureExists(templatesDir.toFile())
    return f(templatesDir)
}
