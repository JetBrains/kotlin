/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.debugger

import org.jetbrains.org.objectweb.asm.*
import java.io.File
import java.util.*

val DEX_BEFORE_PATCH_EXTENSION = "before_dex"

fun String.needDexPatch() = split('.').any { it.endsWith("Dex") }

fun patchDexTests(dir: File) {
    dir.listFiles({ file -> file.isDirectory && file.name.needDexPatch() }).forEach { dir ->
        dir.listFiles { testOutputFile -> testOutputFile.extension == "class" }.forEach(::applyDexLikePatch)
    }
}

private fun applyDexLikePatch(file: File) {
    file.copyTo(File(file.absolutePath + ".$DEX_BEFORE_PATCH_EXTENSION"))

    val reader = ClassReader(file.readBytes())
    val writer = ClassWriter(ClassWriter.COMPUTE_FRAMES)

    val visitor = writer
            .withRemoveSourceDebugExtensionVisitor()
            .withRemoveSameLinesInLineTableVisitor()

    reader.accept(visitor, 0)

    file.writeBytes(writer.toByteArray())
}

private fun ClassVisitor.withRemoveSourceDebugExtensionVisitor(): ClassVisitor {
    return object : ClassVisitor(Opcodes.API_VERSION, this) {
        override fun visitSource(source: String?, debug: String?) {
            super.visitSource(source, null)
        }
    }
}

private fun ClassVisitor.withRemoveSameLinesInLineTableVisitor(): ClassVisitor {
    return object : ClassVisitor(Opcodes.API_VERSION, this) {
        override fun visitMethod(access: Int, name: String?, desc: String?, signature: String?, exceptions: Array<out String>?): MethodVisitor? {
            val methodVisitor = super.visitMethod(access, name, desc, signature, exceptions) ?: return null

            return object : MethodVisitor(Opcodes.API_VERSION, methodVisitor) {
                val labels = HashSet<String>()

                override fun visitLineNumber(line: Int, start: Label?) {
                    val added = labels.add(start.toString())

                    if (added) {
                        super.visitLineNumber(line, start)
                    }
                }
            }
        }
    }
}
