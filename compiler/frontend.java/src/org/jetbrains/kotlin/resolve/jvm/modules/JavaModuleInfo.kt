/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.resolve.jvm.modules

import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.utils.compactIfPossible
/*
import org.jetbrains.org.objectweb.asm.ClassReader
import org.jetbrains.org.objectweb.asm.ClassVisitor
import org.jetbrains.org.objectweb.asm.ModuleVisitor
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.Opcodes.*
*/
import java.io.IOException

class JavaModuleInfo(
        val moduleName: String,
        val requires: List<Requires>,
        val exports: List<Exports>
) {
    data class Requires(val moduleName: String, val flags: Int) {
        val isTransitive get() = false // fix to compile
//        val isTransitive get() = (flags and ACC_TRANSITIVE) != 0
    }

    data class Exports(val packageFqName: FqName, val flags: Int, val toModules: List<String>)

    override fun toString(): String =
            "Module $moduleName (${requires.size} requires, ${exports.size} exports)"

    companion object {
        fun read(file: VirtualFile): JavaModuleInfo? = null // unsupported with this version of ASM
/*
        fun read(file: VirtualFile): JavaModuleInfo? {
            val contents = try { file.contentsToByteArray() } catch (e: IOException) { return null }

            var moduleName: String? = null
            val requires = arrayListOf<Requires>()
            val exports = arrayListOf<Exports>()

            ClassReader(contents).accept(object : ClassVisitor(Opcodes.ASM6) {
                override fun visitModule(name: String, access: Int, version: String?): ModuleVisitor {
                    moduleName = name

                    return object : ModuleVisitor(Opcodes.ASM6) {
                        override fun visitRequire(module: String, access: Int, version: String?) {
                            requires.add(Requires(module, access))
                        }

                        override fun visitExport(packageFqName: String, access: Int, modules: Array<String>?) {
                            exports.add(Exports(FqName(packageFqName), access, modules?.toList().orEmpty()))
                        }
                    }
                }
            }, ClassReader.SKIP_DEBUG or ClassReader.SKIP_CODE or ClassReader.SKIP_FRAMES)

            return if (moduleName != null)
                JavaModuleInfo(moduleName!!, requires.compactIfPossible(), exports.compactIfPossible())
            else null
        }
*/
    }
}
