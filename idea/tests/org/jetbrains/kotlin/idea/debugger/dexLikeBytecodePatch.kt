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
    return object : ClassVisitor(Opcodes.ASM5, this) {
        override fun visitSource(source: String?, debug: String?) {
            super.visitSource(source, null)
        }
    }
}

private fun ClassVisitor.withRemoveSameLinesInLineTableVisitor(): ClassVisitor {
    return object : ClassVisitor(Opcodes.ASM5, this) {
        override fun visitMethod(access: Int, name: String?, desc: String?, signature: String?, exceptions: Array<out String>?): MethodVisitor? {
            val methodVisitor = super.visitMethod(access, name, desc, signature, exceptions) ?: return null

            return object : MethodVisitor(Opcodes.ASM5, methodVisitor) {
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
