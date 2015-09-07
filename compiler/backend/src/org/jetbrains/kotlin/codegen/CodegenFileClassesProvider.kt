/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.codegen

import org.jetbrains.kotlin.fileClasses.JvmFileClassInfo
import org.jetbrains.kotlin.fileClasses.JvmFileClassUtil
import org.jetbrains.kotlin.fileClasses.JvmFileClassesProvider
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.JetFile
import org.jetbrains.kotlin.resolve.BindingContext
import java.util.*

public class CodegenFileClassesProvider private constructor(private val bindingContext: BindingContext) : JvmFileClassesProvider {
    private val fileParts = hashMapOf<JetFile, JvmFileClassInfo>()

    override fun getFileClassFqName(file: JetFile): FqName =
            getFileClassInfo(file).fileClassFqName

    public fun getFileClassInfo(file: JetFile): JvmFileClassInfo =
            fileParts.getOrPut(file) { createFileClassInfo(file) }

    private fun createFileClassInfo(file: JetFile): JvmFileClassInfo {
        val fileAnnotations = JvmFileClassUtil.collectFileAnnotations(file, bindingContext)
        val jvmClassNameAnnotation = JvmFileClassUtil.parseJvmFileClass(fileAnnotations)
        return JvmFileClassUtil.getFileClassInfo(file, jvmClassNameAnnotation)
    }

    internal fun addFileClassInfo(file: JetFile) {
        if (fileParts.containsKey(file)) return
        fileParts[file] = createFileClassInfo(file)
    }

    companion object {
        public @jvmStatic fun createForCodegenTask(
                bindingContext: BindingContext,
                files: Collection<JetFile>,
                packagesWithObsoleteParts: Collection<FqName>,
                multifileFacadesWithObsoleteParts: Collection<FqName>
        ) : CodegenFileClassesProvider {
            val codegenFileClassesManager = CodegenFileClassesProvider(bindingContext)
            files.forEach {
                codegenFileClassesManager.addFileClassInfo(it)
            }

            val packagesToProcess = HashSet<FqName>(packagesWithObsoleteParts)
            packagesToProcess.addAll(multifileFacadesWithObsoleteParts.map { it.parent() })
            for (packageFqName in packagesToProcess) {
                bindingContext.get(BindingContext.PACKAGE_TO_FILES, packageFqName)?.forEach {
                    codegenFileClassesManager.addFileClassInfo(it)
                }
            }

            return codegenFileClassesManager
        }
    }
}
