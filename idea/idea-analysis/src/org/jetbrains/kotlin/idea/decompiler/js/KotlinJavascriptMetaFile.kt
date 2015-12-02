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

package org.jetbrains.kotlin.idea.decompiler.js

import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.idea.decompiler.KtDecompiledFileBase
import org.jetbrains.kotlin.idea.decompiler.textBuilder.DecompiledText
import org.jetbrains.kotlin.idea.decompiler.textBuilder.ResolverForDecompiler
import org.jetbrains.kotlin.idea.decompiler.textBuilder.buildDecompiledText
import org.jetbrains.kotlin.load.kotlin.OldPackageFacadeClassUtils
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.renderer.DescriptorRendererModifier
import org.jetbrains.kotlin.renderer.ExcludedTypeAnnotations
import org.jetbrains.kotlin.utils.concurrent.block.LockedClearableLazyValue
import java.util.*

public class KotlinJavascriptMetaFile(provider: KotlinJavascriptMetaFileViewProvider) : KtDecompiledFileBase(provider) {
    override fun buildDecompiledText() = buildDecompiledTextFromJsMetadata(virtualFile)
}

private val descriptorRendererForKotlinJavascriptDecompiler = DescriptorRenderer.withOptions {
    withDefinedIn = false
    classWithPrimaryConstructor = true
    secondaryConstructorsAsPrimary = false
    modifiers = DescriptorRendererModifier.ALL
    excludedTypeAnnotationClasses = ExcludedTypeAnnotations.annotationsForNullabilityAndMutability
}

public fun buildDecompiledTextFromJsMetadata(
        classFile: VirtualFile,
        resolver: ResolverForDecompiler = KotlinJavaScriptDeserializerForDecompiler(classFile)
): DecompiledText {
    val packageFqName = JsMetaFileUtils.getPackageFqName(classFile)
    val isPackageHeader = JsMetaFileUtils.isPackageHeader(classFile)

    if (isPackageHeader) {
        return buildDecompiledText(packageFqName,
                                   resolveDeclarationsInPackage(packageFqName, resolver),
                                   descriptorRendererForKotlinJavascriptDecompiler)
    }
    else {
        val classId = JsMetaFileUtils.getClassId(classFile)
        return buildDecompiledText(packageFqName, listOfNotNull(resolver.resolveTopLevelClass(classId)), descriptorRendererForKotlinJavascriptDecompiler)
    }
}

private fun resolveDeclarationsInPackage(packageFqName: FqName, resolver: ResolverForDecompiler) =
        ArrayList(resolver.resolveDeclarationsInFacade(OldPackageFacadeClassUtils.getPackageClassFqName(packageFqName)))
