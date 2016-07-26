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

package org.jetbrains.kotlin.idea.decompiler.js

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiManager
import com.intellij.psi.compiled.ClassFileDecompilers
import com.intellij.psi.compiled.ClsStubBuilder
import org.jetbrains.kotlin.fileClasses.OldPackageFacadeClassUtils
import org.jetbrains.kotlin.idea.decompiler.KotlinDecompiledFileViewProvider
import org.jetbrains.kotlin.idea.decompiler.KtDecompiledFile
import org.jetbrains.kotlin.idea.decompiler.textBuilder.DecompiledText
import org.jetbrains.kotlin.idea.decompiler.textBuilder.ResolverForDecompiler
import org.jetbrains.kotlin.idea.decompiler.textBuilder.buildDecompiledText
import org.jetbrains.kotlin.idea.decompiler.textBuilder.defaultDecompilerRendererOptions
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.serialization.js.KotlinJavascriptSerializationUtil
import java.util.*

class KotlinJavaScriptMetaFileDecompiler : ClassFileDecompilers.Full() {
    private val stubBuilder = KotlinJavaScriptStubBuilder()

    override fun accepts(file: VirtualFile): Boolean {
        return file.name.endsWith("." + KotlinJavascriptSerializationUtil.CLASS_METADATA_FILE_EXTENSION)
    }

    override fun getStubBuilder(): ClsStubBuilder {
        return stubBuilder
    }

    override fun createFileViewProvider(file: VirtualFile, manager: PsiManager, physical: Boolean): FileViewProvider {
        return KotlinDecompiledFileViewProvider(manager, file, physical) { provider ->
            if (JsMetaFileUtils.isKotlinJavaScriptInternalCompiledFile(file)) {
                null
            }
            else {
                KtDecompiledFile(provider) { file -> buildDecompiledTextFromJsMetadata(file) }
            }
        }
    }
}

private val decompilerRendererForJS = DescriptorRenderer.withOptions { defaultDecompilerRendererOptions() }

fun buildDecompiledTextFromJsMetadata(
        classFile: VirtualFile,
        resolver: ResolverForDecompiler = KotlinJavaScriptDeserializerForDecompiler(classFile)
): DecompiledText {
    val packageFqName = JsMetaFileUtils.getPackageFqName(classFile)
    val isPackageHeader = JsMetaFileUtils.isPackageHeader(classFile)

    if (isPackageHeader) {
        return buildDecompiledText(packageFqName,
                                   resolveDeclarationsInPackage(packageFqName, resolver),
                                   decompilerRendererForJS)
    }
    else {
        val classId = JsMetaFileUtils.getClassId(classFile)
        return buildDecompiledText(packageFqName, listOfNotNull(resolver.resolveTopLevelClass(classId)), decompilerRendererForJS)
    }
}

private fun resolveDeclarationsInPackage(packageFqName: FqName, resolver: ResolverForDecompiler) =
        ArrayList(resolver.resolveDeclarationsInFacade(OldPackageFacadeClassUtils.getPackageClassFqName(packageFqName)))