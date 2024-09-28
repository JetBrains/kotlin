/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen.inline

import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap
import org.jetbrains.kotlin.codegen.SourceInfo
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.org.objectweb.asm.Label
import org.jetbrains.org.objectweb.asm.MethodVisitor
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.tree.MethodNode
import java.util.*
import kotlin.math.max

object SMAPBuilder {
    fun build(fileMappings: List<FileMapping>, backwardsCompatibleSyntax: Boolean): String? {
        if (fileMappings.isEmpty()) {
            return null
        }

        val debugMappings = linkedMapOf<Pair<String, String>, FileMapping>()
        for (fileMapping in fileMappings) {
            for ((_, dest, range, callSite) in fileMapping.lineMappings) {
                callSite?.let { (line, file, path) ->
                    debugMappings.getOrPut(file to path) { FileMapping(file, path) }.mapNewInterval(line, dest, range)
                }
            }
        }

        // Old versions of kotlinc and the IDEA plugin have incorrect implementations of SMAPParser:
        //   1. they require *E between strata, which is not correct syntax according to JSR-045;
        //   2. in KotlinDebug, they use `1#2,3:4` to mean "map lines 4..6 to line 1 of #2", when in reality (and in
        //      the non-debug stratum) this maps lines 4..6 to lines 1..3. The correct syntax is `1#2:4,3`.
        val defaultStrata = fileMappings.toSMAP(KOTLIN_STRATA_NAME, mapToFirstLine = false)
        val debugStrata = debugMappings.values.toSMAP(KOTLIN_DEBUG_STRATA_NAME, mapToFirstLine = !backwardsCompatibleSyntax)
        if (backwardsCompatibleSyntax && defaultStrata.isNotEmpty() && debugStrata.isNotEmpty()) {
            return "SMAP\n${fileMappings[0].name}\n$KOTLIN_STRATA_NAME\n$defaultStrata${SMAP.END}\n$debugStrata${SMAP.END}\n"
        }
        return "SMAP\n${fileMappings[0].name}\n$KOTLIN_STRATA_NAME\n$defaultStrata$debugStrata${SMAP.END}\n"
    }

    private fun Collection<FileMapping>.toSMAP(stratumName: String, mapToFirstLine: Boolean): String = if (isEmpty()) "" else
        "${SMAP.STRATA_SECTION} $stratumName\n" +
                "${SMAP.FILE_SECTION}\n${mapIndexed { id, file -> file.toSMAPFile(id + 1) }.joinToString("")}" +
                "${SMAP.LINE_SECTION}\n${mapIndexed { id, file -> file.toSMAPMapping(id + 1, mapToFirstLine) }.joinToString("")}"

    private fun RangeMapping.toSMAP(fileId: Int, oneLine: Boolean): String =
        if (range == 1) "$source#$fileId:$dest\n" else if (oneLine) "$source#$fileId:$dest,$range\n" else "$source#$fileId,$range:$dest\n"

    private fun FileMapping.toSMAPFile(id: Int): String =
        "+ $id $name\n$path\n"

    private fun FileMapping.toSMAPMapping(id: Int, mapToFirstLine: Boolean): String =
        lineMappings.joinToString("") { it.toSMAP(id, mapToFirstLine) }
}

/**
 * This class is an "SMAP delegate". It uses the info from [smap] and fills [parent].
 * Used mostly to fill line numbers of an inlined function.
 */
class SourceMapCopier(val parent: SourceMapper, private val smap: SMAP, val callSite: SourcePosition? = null) {
    private val visitedLines = Int2IntOpenHashMap()
    private var lastVisitedRange: RangeMapping? = null

    fun mapLineNumber(lineNumber: Int): Int {
        val mappedLineNumber = visitedLines.get(lineNumber)
        if (mappedLineNumber > 0) {
            return mappedLineNumber
        }

        val range = lastVisitedRange?.takeIf { lineNumber in it } ?: smap.findRange(lineNumber) ?: return -1
        lastVisitedRange = range
        val newLineNumber = parent.mapLineNumber(range.mapDestToSource(lineNumber), callSite ?: range.callSite)

        visitedLines.put(lineNumber, newLineNumber)
        return newLineNumber
    }
}

class SourceMapCopyingMethodVisitor(private val smapCopier: SourceMapCopier, mv: MethodVisitor) : MethodVisitor(Opcodes.API_VERSION, mv) {
    constructor(target: SourceMapper, source: SMAP, mv: MethodVisitor) : this(SourceMapCopier(target, source), mv)

    override fun visitLineNumber(line: Int, start: Label) =
        super.visitLineNumber(smapCopier.mapLineNumber(line), start)

    override fun visitLocalVariable(name: String, descriptor: String, signature: String?, start: Label, end: Label, index: Int) =
        if (JvmAbi.isFakeLocalVariableForInline(name))
            super.visitLocalVariable(updateCallSiteLineNumber(name, smapCopier::mapLineNumber), descriptor, signature, start, end, index)
        else
            super.visitLocalVariable(name, descriptor, signature, start, end, index)
}

/**
 * This class allows us to build new SMAP from scratch by mapping lines one by one.
 */
class SourceMapper(val sourceInfo: SourceInfo?) {
    constructor(name: String?, original: SMAP) : this(original.fileMappings.firstOrNull { it.name == name }?.toSourceInfo())

    private var maxUsedValue: Int = sourceInfo?.linesInFile ?: 0
    private var fileMappings: LinkedHashMap<Pair<String, String>, FileMapping> = linkedMapOf()

    val resultMappings: List<FileMapping>
        get() = fileMappings.values.toList()

    companion object {
        const val FAKE_FILE_NAME = "fake.kt"
        const val FAKE_PATH = "kotlin/jvm/internal/FakeKt"
        const val LOCAL_VARIABLE_INLINE_ARGUMENT_SYNTHETIC_LINE_NUMBER = 1
    }

    init {
        sourceInfo?.let { sourceInfo ->
            // If 'sourceFileName' is null we are dealing with a synthesized class
            // (e.g., multi-file class facade with multiple parts). Such classes
            // only have synthetic debug information and we use a fake file name.
            val sourceFileName = sourceInfo.sourceFileName ?: FAKE_FILE_NAME
            // Explicitly map the file to itself -- we'll probably need a lot of lines from it, so this will produce fewer ranges.
            getOrRegisterNewSource(sourceFileName, sourceInfo.pathOrCleanFQN)
                .mapNewInterval(1, 1, sourceInfo.linesInFile)
        }
    }

    val isTrivial: Boolean
        get() = maxUsedValue == 0 || maxUsedValue == sourceInfo?.linesInFile

    private fun getOrRegisterNewSource(name: String, path: String): FileMapping =
        fileMappings.getOrPut(name to path) { FileMapping(name, path) }

    fun mapLineNumber(inlineSource: SourcePosition, inlineCallSite: SourcePosition?): Int {
        val fileMapping = getOrRegisterNewSource(inlineSource.file, inlineSource.path)
        val mappedLineIndex = fileMapping.mapNewLineNumber(inlineSource.line, maxUsedValue, inlineCallSite)
        maxUsedValue = max(maxUsedValue, mappedLineIndex)
        return mappedLineIndex
    }

    fun mapSyntheticLineNumber(id: Int): Int {
        return mapLineNumber(SourcePosition(id, FAKE_FILE_NAME, FAKE_PATH), null)
    }
}

data class SMAPAndMethodNode(val node: MethodNode, val classSMAP: SMAP)
