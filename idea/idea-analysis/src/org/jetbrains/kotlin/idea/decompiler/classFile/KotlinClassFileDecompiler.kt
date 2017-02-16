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

package org.jetbrains.kotlin.idea.decompiler.classFile

import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.roots.FileIndexFacade
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.psi.compiled.ClassFileDecompilers
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.idea.caches.IDEKotlinBinaryClassCache
import org.jetbrains.kotlin.idea.caches.resolve.lightClasses.BySignatureIndexer
import org.jetbrains.kotlin.idea.decompiler.KotlinDecompiledFileViewProvider
import org.jetbrains.kotlin.idea.decompiler.KtDecompiledFile
import org.jetbrains.kotlin.idea.decompiler.common.createIncompatibleAbiVersionDecompiledText
import org.jetbrains.kotlin.idea.decompiler.navigation.ByDescriptorIndexer
import org.jetbrains.kotlin.idea.decompiler.textBuilder.DecompiledText
import org.jetbrains.kotlin.idea.decompiler.textBuilder.ResolverForDecompiler
import org.jetbrains.kotlin.idea.decompiler.textBuilder.buildDecompiledText
import org.jetbrains.kotlin.idea.decompiler.textBuilder.defaultDecompilerRendererOptions
import org.jetbrains.kotlin.load.kotlin.JvmMetadataVersion
import org.jetbrains.kotlin.load.kotlin.header.KotlinClassHeader
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.types.asFlexibleType
import org.jetbrains.kotlin.types.isFlexible

class KotlinClassFileDecompiler : ClassFileDecompilers.Full() {
    private val stubBuilder = KotlinClsStubBuilder()

    override fun accepts(file: VirtualFile) = IDEKotlinBinaryClassCache.isKotlinJvmCompiledFile(file)

    override fun getStubBuilder() = stubBuilder

    override fun createFileViewProvider(file: VirtualFile, manager: PsiManager, physical: Boolean): KotlinDecompiledFileViewProvider {
        val project = manager.project
        return KotlinDecompiledFileViewProvider(manager, file, physical) { provider ->
            val virtualFile = provider.virtualFile
            val fileIndex = ServiceManager.getService(project, FileIndexFacade::class.java)
            when {
                !fileIndex.isInLibraryClasses(virtualFile) && fileIndex.isInSource(virtualFile) -> null
                isKotlinInternalCompiledFile(virtualFile) -> null
                else -> KtClsFile(provider)
            }
        }
    }
}

class KtClsFile(provider: KotlinDecompiledFileViewProvider) : KtDecompiledFile(provider, { file -> buildDecompiledTextForClassFile(file) })

private val decompilerRendererForClassFiles = DescriptorRenderer.withOptions {
    defaultDecompilerRendererOptions()
    typeNormalizer = { type -> if (type.isFlexible()) type.asFlexibleType().lowerBound else type }
}

fun buildDecompiledTextForClassFile(
        classFile: VirtualFile,
        resolver: ResolverForDecompiler = DeserializerForClassfileDecompiler(classFile)
): DecompiledText {
    val (classHeader, classId) = IDEKotlinBinaryClassCache.getKotlinBinaryClassHeaderData(classFile)
                                 ?: error("Decompiled data factory shouldn't be called on an unsupported file: " + classFile)

    if (!classHeader.metadataVersion.isCompatible()) {
        return createIncompatibleAbiVersionDecompiledText(JvmMetadataVersion.INSTANCE, classHeader.metadataVersion)
    }

    fun buildText(declarations: List<DeclarationDescriptor>) =
            buildDecompiledText(classId.packageFqName, declarations, decompilerRendererForClassFiles, listOf(ByDescriptorIndexer, BySignatureIndexer))

    return when (classHeader.kind) {
        KotlinClassHeader.Kind.FILE_FACADE ->
            buildText(resolver.resolveDeclarationsInFacade(classId.asSingleFqName()))
        KotlinClassHeader.Kind.CLASS -> {
            buildText(listOfNotNull(resolver.resolveTopLevelClass(classId)))
        }
        KotlinClassHeader.Kind.MULTIFILE_CLASS -> {
            val partClasses = findMultifileClassParts(classFile, classId, classHeader)
            val partMembers = partClasses.flatMap { partClass ->
                resolver.resolveDeclarationsInFacade(partClass.classId.asSingleFqName())
            }
            buildText(partMembers)
        }
        else ->
            throw UnsupportedOperationException("Unknown header kind: $classHeader, class $classId")
    }
}
