/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.idea.debugger

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.codegen.inline.API
import org.jetbrains.kotlin.codegen.inline.FileMapping
import org.jetbrains.kotlin.codegen.inline.SMAP
import org.jetbrains.kotlin.codegen.inline.SMAPParser
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.jvm.JvmClassName
import org.jetbrains.org.objectweb.asm.ClassReader
import org.jetbrains.org.objectweb.asm.ClassVisitor

enum class SourceLineKind {
    CALL_LINE,
    EXECUTED_LINE
}

fun mapStacktraceLineToSource(smapData: SmapData,
                              line: Int,
                              project: Project,
                              lineKind: SourceLineKind,
                              searchScope: GlobalSearchScope): Pair<KtFile, Int>? {
    val smap = when (lineKind) {
                   SourceLineKind.CALL_LINE -> smapData.kotlinDebugStrata
                   SourceLineKind.EXECUTED_LINE -> smapData.kotlinStrata
               } ?: return null

    val mappingInfo = smap.fileMappings.firstOrNull {
        it.getIntervalIfContains(line) != null
    } ?: return null

    val jvmName = JvmClassName.byInternalName(mappingInfo.path)
    val sourceFile = DebuggerUtils.findSourceFileForClassIncludeLibrarySources(
            project, searchScope, jvmName, mappingInfo.name) ?: return null

    val interval = mappingInfo.getIntervalIfContains(line)!!
    val sourceLine = when (lineKind) {
        SourceLineKind.CALL_LINE -> interval.source - 1
        SourceLineKind.EXECUTED_LINE -> interval.mapDestToSource(line) - 1
    }

    return sourceFile to sourceLine
}

fun readDebugInfo(bytes: ByteArray): SmapData? {
    val cr = ClassReader(bytes)
    var debugInfo: String? = null
    cr.accept(object : ClassVisitor(API) {
        override fun visitSource(source: String?, debug: String?) {
            debugInfo = debug
        }
    }, ClassReader.SKIP_FRAMES and ClassReader.SKIP_CODE)
    return debugInfo?.let(::SmapData)
}

class SmapData(debugInfo: String) {
    var kotlinStrata: SMAP?
    var kotlinDebugStrata: SMAP?

    init {
        val intervals = debugInfo.split(SMAP.END).filter(String::isNotBlank)
        when (intervals.count()) {
            1 -> {
                kotlinStrata = SMAPParser.parse(intervals[0] + SMAP.END)
                kotlinDebugStrata = null
            }
            2 -> {
                kotlinStrata = SMAPParser.parse(intervals[0] + SMAP.END)
                kotlinDebugStrata = SMAPParser.parse(intervals[1] + SMAP.END)
            }
            else -> {
                kotlinStrata = null
                kotlinDebugStrata = null
            }
        }
    }
}

private fun FileMapping.getIntervalIfContains(destLine: Int) = lineMappings.firstOrNull { it.contains(destLine) }
