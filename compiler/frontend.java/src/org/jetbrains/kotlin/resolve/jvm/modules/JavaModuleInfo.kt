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
import com.intellij.psi.PsiJavaModule
import com.intellij.psi.PsiModifier
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.utils.compactIfPossible
import org.jetbrains.org.objectweb.asm.ClassReader
import org.jetbrains.org.objectweb.asm.ClassVisitor
import org.jetbrains.org.objectweb.asm.ModuleVisitor
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.Opcodes.ACC_TRANSITIVE
import java.io.IOException

class JavaModuleInfo(
        val moduleName: String,
        val requires: List<Requires>,
        val exports: List<Exports>
) {
    data class Requires(val moduleName: String, val isTransitive: Boolean)

    data class Exports(val packageFqName: FqName, val toModules: List<String>)

    override fun toString(): String =
            "Module $moduleName (${requires.size} requires, ${exports.size} exports)"

    companion object {
        fun create(psiJavaModule: PsiJavaModule): JavaModuleInfo {
            return JavaModuleInfo(
                    psiJavaModule.name,
                    psiJavaModule.requires.mapNotNull { statement ->
                        statement.moduleName?.let { moduleName ->
                            JavaModuleInfo.Requires(moduleName, statement.hasModifierProperty(PsiModifier.TRANSITIVE))
                        }
                    },
                    psiJavaModule.exports.mapNotNull { statement ->
                        statement.packageName?.let { packageName ->
                            JavaModuleInfo.Exports(FqName(packageName), statement.moduleNames)
                        }
                    }
            )
        }

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
                            requires.add(Requires(module, (access and ACC_TRANSITIVE) != 0))
                        }

                        override fun visitExport(packageFqName: String, access: Int, modules: Array<String>?) {
                            // For some reason, '/' is the delimiter in packageFqName here
                            exports.add(Exports(FqName(packageFqName.replace('/', '.')), modules?.toList().orEmpty()))
                        }
                    }
                }
            }, ClassReader.SKIP_DEBUG or ClassReader.SKIP_CODE or ClassReader.SKIP_FRAMES)

            return if (moduleName != null)
                JavaModuleInfo(moduleName!!, requires.compactIfPossible(), exports.compactIfPossible())
            else null
        }
    }
}
