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

package org.jetbrains.kotlin.idea.decompiler.classFile

import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.roots.FileIndexFacade
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.psi.compiled.ClassFileDecompilers
import org.jetbrains.kotlin.idea.caches.IDEKotlinBinaryClassCache
import org.jetbrains.kotlin.idea.decompiler.KotlinDecompiledFileViewProvider
import org.jetbrains.kotlin.idea.decompiler.KtDecompiledFile
import org.jetbrains.kotlin.idea.decompiler.textBuilder.DecompiledText
import org.jetbrains.kotlin.idea.decompiler.textBuilder.ResolverForDecompiler
import org.jetbrains.kotlin.idea.decompiler.textBuilder.buildDecompiledText
import org.jetbrains.kotlin.idea.decompiler.textBuilder.defaultDecompilerRendererOptions
import org.jetbrains.kotlin.load.kotlin.JvmMetadataVersion
import org.jetbrains.kotlin.load.kotlin.header.KotlinClassHeader
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.types.flexibility
import org.jetbrains.kotlin.types.isFlexible
import java.util.*

class KotlinClassFileDecompiler : ClassFileDecompilers.Full() {
    private val stubBuilder = KotlinClsStubBuilder()

    override fun accepts(file: VirtualFile) = isKotlinJvmCompiledFile(file)

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
    typeNormalizer = { type -> if (type.isFlexible()) type.flexibility().lowerBound else type }
}

internal val FILE_ABI_VERSION_MARKER: String = "FILE_ABI"
internal val CURRENT_ABI_VERSION_MARKER: String = "CURRENT_ABI"

val INCOMPATIBLE_ABI_VERSION_GENERAL_COMMENT: String = "// This class file was compiled with different version of Kotlin compiler and can't be decompiled."
val INCOMPATIBLE_ABI_VERSION_COMMENT: String =
        "$INCOMPATIBLE_ABI_VERSION_GENERAL_COMMENT\n" +
        "//\n" +
        "// Current compiler ABI version is $CURRENT_ABI_VERSION_MARKER\n" +
        "// File ABI version is $FILE_ABI_VERSION_MARKER"

fun buildDecompiledTextForClassFile(
        classFile: VirtualFile,
        resolver: ResolverForDecompiler = DeserializerForClassfileDecompiler(classFile)
): DecompiledText {
    val (classHeader, classId) = IDEKotlinBinaryClassCache.getKotlinBinaryClassHeaderData(classFile)
                                 ?: error("Decompiled data factory shouldn't be called on an unsupported file: " + classFile)

    if (!classHeader.metadataVersion.isCompatible()) {
        return DecompiledText(
                INCOMPATIBLE_ABI_VERSION_COMMENT
                        .replace(CURRENT_ABI_VERSION_MARKER, JvmMetadataVersion.INSTANCE.toString())
                        .replace(FILE_ABI_VERSION_MARKER, classHeader.metadataVersion.toString()),
                mapOf()
        )
    }

    return when (classHeader.kind) {
        KotlinClassHeader.Kind.FILE_FACADE ->
            buildDecompiledText(classId.packageFqName, ArrayList(resolver.resolveDeclarationsInFacade(classId.asSingleFqName())),
                                decompilerRendererForClassFiles)
        KotlinClassHeader.Kind.CLASS ->
            buildDecompiledText(classId.packageFqName, listOfNotNull(resolver.resolveTopLevelClass(classId)),
                                decompilerRendererForClassFiles)
        KotlinClassHeader.Kind.MULTIFILE_CLASS -> {
            val partClasses = findMultifileClassParts(classFile, classId, classHeader)
            val partMembers = partClasses.flatMap { partClass ->
                resolver.resolveDeclarationsInFacade(partClass.classId.asSingleFqName())
            }
            buildDecompiledText(classId.packageFqName, partMembers, decompilerRendererForClassFiles)
        }
        else ->
            throw UnsupportedOperationException("Unknown header kind: $classHeader, class $classId")
    }
}
