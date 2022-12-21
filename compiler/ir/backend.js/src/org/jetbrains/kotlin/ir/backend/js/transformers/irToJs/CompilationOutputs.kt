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

abstract class CompilationOutputs {
    var dependencies: Collection<Pair<String, CompilationOutputs>> = emptyList()

    abstract val tsDefinitions: TypeScriptFragment?

    abstract val jsProgram: JsProgram?

    abstract fun writeJsCode(outputJsFile: File, outputJsMapFile: File)

    fun writeAll(outputDir: File, outputName: String, genDTS: Boolean, moduleName: String, moduleKind: ModuleKind): List<String> {
        val writtenJsFiles = ArrayList<String>(dependencies.size + 1)

        val outputJsFile = outputDir.resolve("$outputName.js")

        outputJsFile.parentFile.mkdirs()
        writeJsCode(outputJsFile, outputJsFile.mapForJsFile)

        dependencies.forEach { (name, content) ->
            outputDir.resolve("$name.js").let { depJsFile ->
                depJsFile.parentFile.mkdirs()
                content.writeJsCode(depJsFile, depJsFile.mapForJsFile)
                writtenJsFiles += depJsFile.absolutePath
            }
        }

        writtenJsFiles += outputJsFile.absolutePath

        if (genDTS) {
            outputJsFile.dtsForJsFile.writeText(getFullTsDefinition(moduleName, moduleKind))
        }

        return writtenJsFiles
    }

    fun getFullTsDefinition(moduleName: String, moduleKind: ModuleKind): String {
        val allTsDefinitions = dependencies.mapNotNull { it.second.tsDefinitions } + listOfNotNull(tsDefinitions)
        return allTsDefinitions.toTypeScript(moduleName, moduleKind)
    }

    private val File.mapForJsFile
        get() = resolveSibling("$name.map")

    private val File.dtsForJsFile
        get() = resolveSibling("$nameWithoutExtension.d.ts")
}

class CompilationOutputsBuilt(
    private val rawJsCode: String,
    private val sourceMap: String?,
    override val tsDefinitions: TypeScriptFragment?,
    override val jsProgram: JsProgram?,
) : CompilationOutputs() {
    override fun writeJsCode(outputJsFile: File, outputJsMapFile: File) {
        var jsCodeWithSourceMap = rawJsCode

        sourceMap?.let {
            outputJsMapFile.writeText(it)
            jsCodeWithSourceMap = "$jsCodeWithSourceMap\n//# sourceMappingURL=${outputJsMapFile.name}\n"
        }
        outputJsFile.writeText(jsCodeWithSourceMap)
    }
}

private fun File.copyModificationTimeFrom(from: File) {
    val mtime = from.lastModified()
    if (mtime > 0) {
        setLastModified(mtime)
    }
}

class CompilationOutputsCached(
    private val jsCodeFilePath: String,
    private val sourceMapFilePath: String?,
    private val tsDefinitionsFilePath: String?
) : CompilationOutputs() {
    override val tsDefinitions: TypeScriptFragment?
        get() = tsDefinitionsFilePath?.let { TypeScriptFragment(File(it).readText()) }

    override val jsProgram: JsProgram?
        get() = null

    override fun writeJsCode(outputJsFile: File, outputJsMapFile: File) {
        File(jsCodeFilePath).copyToIfModified(outputJsFile)

        sourceMapFilePath?.let {
            File(it).copyToIfModified(outputJsMapFile)
        }
    }

    private fun File.copyToIfModified(target: File) {
        val thisMtime = lastModified()
        val targetMtime = target.lastModified()
        if (thisMtime <= 0 || targetMtime <= 0 || targetMtime > thisMtime) {
            copyTo(target, true)
            copyModificationTimeFrom(target)
        }
    }
}

class CompilationOutputsBuiltForCache(
    private val jsCodeFilePath: String,
    private val sourceMapFilePath: String,
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

        File(jsCodeFilePath).copyModificationTimeFrom(outputJsFile)
        File(sourceMapFilePath).copyModificationTimeFrom(outputJsMapFile)
    }
}
