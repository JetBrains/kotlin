/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.transformers.irToJs

import org.jetbrains.kotlin.ir.backend.js.export.TypeScriptFragment
import org.jetbrains.kotlin.ir.backend.js.export.toTypeScript
import org.jetbrains.kotlin.js.backend.ast.JsProgram
import org.jetbrains.kotlin.serialization.js.ModuleKind
import java.io.File
import java.nio.file.Files

abstract class CompilationOutputs {
    var dependencies: Collection<Pair<String, CompilationOutputs>> = emptyList()

    abstract val tsDefinitions: TypeScriptFragment?

    abstract val jsProgram: JsProgram?

    abstract fun writeJsCode(outputJsFile: File, outputJsMapFile: File)

    fun writeAll(outputDir: File, outputName: String, genDTS: Boolean, moduleName: String, moduleKind: ModuleKind): Collection<File> {
        val writtenFiles = LinkedHashSet<File>(2 * (dependencies.size + 1) + 1)

        fun File.writeAsJsFile(out: CompilationOutputs) {
            parentFile.mkdirs()
            val jsMapFile = mapForJsFile
            val jsFile = canonicalFile
            out.writeJsCode(jsFile, jsMapFile)

            writtenFiles += jsFile
            writtenFiles += jsMapFile
        }

        dependencies.forEach { (name, content) ->
            outputDir.resolve("$name.js").writeAsJsFile(content)
        }

        val outputJsFile = outputDir.resolve("$outputName.js")
        outputJsFile.writeAsJsFile(this)

        if (genDTS) {
            val dtsFile = outputJsFile.dtsForJsFile
            dtsFile.writeText(getFullTsDefinition(moduleName, moduleKind))
            writtenFiles += dtsFile
        }

        Files.walk(outputDir.toPath()).map { it.toFile() }.filter { it != outputDir && it !in writtenFiles }.forEach(File::delete)

        return writtenFiles
    }

    fun getFullTsDefinition(moduleName: String, moduleKind: ModuleKind): String {
        val allTsDefinitions = dependencies.mapNotNull { it.second.tsDefinitions } + listOfNotNull(tsDefinitions)
        return allTsDefinitions.toTypeScript(moduleName, moduleKind)
    }

    private val File.mapForJsFile
        get() = resolveSibling("$name.map").canonicalFile

    private val File.dtsForJsFile
        get() = resolveSibling("$nameWithoutExtension.d.ts").canonicalFile
}

private fun File.copyModificationTimeFrom(from: File) {
    val mtime = from.lastModified()
    if (mtime > 0) {
        setLastModified(mtime)
    }
}

private fun File.asSourceMappingUrl(): String {
    return "\n//# sourceMappingURL=${name}\n"
}

class CompilationOutputsBuilt(
    private val rawJsCode: String,
    private val sourceMap: String?,
    override val tsDefinitions: TypeScriptFragment?,
    override val jsProgram: JsProgram?,
) : CompilationOutputs() {
    override fun writeJsCode(outputJsFile: File, outputJsMapFile: File) {
        val sourceMappingUrl = sourceMap?.let {
            outputJsMapFile.writeText(it)
            outputJsMapFile.asSourceMappingUrl()
        } ?: ""
        outputJsFile.writeText(rawJsCode + sourceMappingUrl)
    }

    fun writeJsCodeIntoModuleCache(outputJsFile: File, outputJsMapFile: File): CompilationOutputsBuiltForCache {
        sourceMap?.let { outputJsMapFile.writeText(it) }
        outputJsFile.writeText(rawJsCode)
        return CompilationOutputsBuiltForCache(outputJsFile, outputJsMapFile, this)
    }
}

class CompilationOutputsCached(
    private val jsCodeFile: File,
    private val sourceMapFile: File?,
    private val tsDefinitionsFile: File?
) : CompilationOutputs() {
    override val tsDefinitions: TypeScriptFragment?
        get() = tsDefinitionsFile?.let { TypeScriptFragment(it.readText()) }

    override val jsProgram: JsProgram?
        get() = null

    override fun writeJsCode(outputJsFile: File, outputJsMapFile: File) {
        val sourceMappingUrl = sourceMapFile?.let {
            if (it.isUpdateRequired(outputJsMapFile)) {
                it.copyTo(outputJsMapFile, true)
                it.copyModificationTimeFrom(outputJsMapFile)
            }
            outputJsMapFile.asSourceMappingUrl()
        } ?: ""

        if (jsCodeFile.isUpdateRequired(outputJsFile)) {
            outputJsFile.writeText(jsCodeFile.readText() + sourceMappingUrl)
            jsCodeFile.copyModificationTimeFrom(outputJsFile)
        }
    }

    private fun File.isUpdateRequired(target: File): Boolean {
        val thisMtime = lastModified()
        val targetMtime = target.lastModified()
        return thisMtime <= 0 || targetMtime <= 0 || targetMtime > thisMtime
    }
}

class CompilationOutputsBuiltForCache(
    private val jsCodeFile: File,
    private val sourceMapFile: File,
    private val outputBuilt: CompilationOutputsBuilt
) : CompilationOutputs() {

    init {
        dependencies = outputBuilt.dependencies
    }

    override val tsDefinitions: TypeScriptFragment?
        get() = outputBuilt.tsDefinitions

    override val jsProgram: JsProgram?
        get() = outputBuilt.jsProgram

    override fun writeJsCode(outputJsFile: File, outputJsMapFile: File) {
        outputBuilt.writeJsCode(outputJsFile, outputJsMapFile)

        jsCodeFile.copyModificationTimeFrom(outputJsFile)
        sourceMapFile.copyModificationTimeFrom(outputJsMapFile)
    }
}
