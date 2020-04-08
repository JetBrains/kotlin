/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.debugger

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.codegen.inline.*
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.jvm.JvmClassName
import org.jetbrains.org.objectweb.asm.ClassReader
import org.jetbrains.org.objectweb.asm.ClassVisitor
import org.jetbrains.org.objectweb.asm.Opcodes

enum class SourceLineKind {
    CALL_LINE,
    EXECUTED_LINE
}

fun mapStacktraceLineToSource(
    smapData: SMAP,
    line: Int,
    project: Project,
    lineKind: SourceLineKind,
    searchScope: GlobalSearchScope
): Pair<KtFile, Int>? {
    val interval = smapData.findRange(line) ?: return null
    val location = when (lineKind) {
        SourceLineKind.CALL_LINE -> interval.callSite
        SourceLineKind.EXECUTED_LINE -> interval.mapDestToSource(line)
    } ?: return null

    val jvmName = JvmClassName.byInternalName(location.path)
    val sourceFile = DebuggerUtils.findSourceFileForClassIncludeLibrarySources(
        project, searchScope, jvmName, location.file
    ) ?: return null

    return sourceFile to location.line - 1
}

fun readDebugInfo(bytes: ByteArray): SMAP? {
    val cr = ClassReader(bytes)
    var debugInfo: String? = null
    cr.accept(object : ClassVisitor(Opcodes.API_VERSION) {
        override fun visitSource(source: String?, debug: String?) {
            debugInfo = debug
        }
    }, ClassReader.SKIP_FRAMES and ClassReader.SKIP_CODE)
    return debugInfo?.let(SMAPParser::parseOrNull)
}
