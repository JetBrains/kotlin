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
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.load.java.JavaClassFinder
import org.jetbrains.kotlin.load.java.structure.JavaAnnotation
import org.jetbrains.kotlin.load.java.structure.impl.JavaAnnotationImpl
import org.jetbrains.kotlin.load.java.structure.impl.classFiles.BinaryClassSignatureParser
import org.jetbrains.kotlin.load.java.structure.impl.classFiles.BinaryJavaAnnotation
import org.jetbrains.kotlin.load.java.structure.impl.classFiles.ClassifierResolutionContext
import org.jetbrains.kotlin.load.java.structure.impl.convert
import org.jetbrains.kotlin.load.java.structure.impl.source.JavaElementSourceFactory
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.jvm.KotlinCliJavaFileManager
import org.jetbrains.kotlin.utils.compact
import org.jetbrains.org.objectweb.asm.*
import org.jetbrains.org.objectweb.asm.Opcodes.ACC_TRANSITIVE
import java.io.IOException

class JavaModuleInfo(
    val moduleName: String,
    val requires: List<Requires>,
    val exports: List<Exports>,
    val annotations: List<JavaAnnotation>
) {
    data class Requires(val moduleName: String, val isTransitive: Boolean)

    data class Exports(val packageFqName: FqName, val toModules: List<String>)

    override fun toString() = "Module $moduleName (${requires.size} requires, ${exports.size} exports)"

    companion object {
        fun create(psiJavaModule: PsiJavaModule) = JavaModuleInfo(
            psiJavaModule.name,
            psiJavaModule.requires.mapNotNull { statement ->
                statement.moduleName?.let { moduleName ->
                    Requires(moduleName, statement.hasModifierProperty(PsiModifier.TRANSITIVE))
                }
            },
            psiJavaModule.exports.mapNotNull { statement ->
                statement.packageName?.let { packageName ->
                    Exports(FqName(packageName), statement.moduleNames)
                }
            },
            psiJavaModule.annotations.convert {
                JavaAnnotationImpl(
                    JavaElementSourceFactory.getInstance(psiJavaModule.project).createPsiSource(it)
                )
            }
        )

        fun read(file: VirtualFile, javaFileManager: KotlinCliJavaFileManager, searchScope: GlobalSearchScope): JavaModuleInfo? {
            val contents = try {
                file.contentsToByteArray()
            } catch (e: IOException) {
                return null
            }
            var moduleName: String? = null
            val requires = arrayListOf<Requires>()
            val exports = arrayListOf<Exports>()
            val annotations = arrayListOf<JavaAnnotation>()

            try {
                ClassReader(contents).accept(object : ClassVisitor(Opcodes.API_VERSION) {
                    override fun visitModule(name: String, access: Int, version: String?): ModuleVisitor {
                        moduleName = name

                        return object : ModuleVisitor(Opcodes.API_VERSION) {
                            override fun visitRequire(module: String, access: Int, version: String?) {
                                requires.add(Requires(module, (access and ACC_TRANSITIVE) != 0))
                            }

                            override fun visitExport(packageFqName: String, access: Int, modules: Array<String>?) {
                                // For some reason, '/' is the delimiter in packageFqName here
                                exports.add(Exports(FqName(packageFqName.replace('/', '.')), modules?.toList().orEmpty()))
                            }
                        }
                    }

                    override fun visitAnnotation(descriptor: String?, visible: Boolean): AnnotationVisitor? {
                        if (descriptor == null) return null

                        val (annotation, visitor) = BinaryJavaAnnotation.createAnnotationAndVisitor(
                            descriptor,
                            ClassifierResolutionContext { javaFileManager.findClass(JavaClassFinder.Request(it), searchScope) },
                            BinaryClassSignatureParser(),
                            isFreshlySupportedTypeUseAnnotation = true
                        )

                        annotations.add(annotation)

                        return visitor
                    }
                }, ClassReader.SKIP_DEBUG or ClassReader.SKIP_CODE or ClassReader.SKIP_FRAMES)
            } catch (e: Exception) {
                throw IllegalStateException(
                    "Could not load module definition from: $file. The file might be broken " +
                            "by incorrect post-processing via bytecode tools. Please remove this file from the classpath.",
                    e
                )
            }

            return moduleName?.let { JavaModuleInfo(it, requires.compact(), exports.compact(), annotations) }
        }
    }
}
