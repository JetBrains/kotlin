/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.decompiler.classFile

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.psi.compiled.ClassFileDecompilers
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.idea.caches.IDEKotlinBinaryClassCache
import org.jetbrains.kotlin.idea.caches.lightClasses.BySignatureIndexer
import org.jetbrains.kotlin.idea.decompiler.KotlinDecompiledFileViewProvider
import org.jetbrains.kotlin.idea.decompiler.KtDecompiledFile
import org.jetbrains.kotlin.idea.decompiler.common.createIncompatibleAbiVersionDecompiledText
import org.jetbrains.kotlin.idea.decompiler.navigation.ByDescriptorIndexer
import org.jetbrains.kotlin.idea.decompiler.textBuilder.DecompiledText
import org.jetbrains.kotlin.idea.decompiler.textBuilder.ResolverForDecompiler
import org.jetbrains.kotlin.idea.decompiler.textBuilder.buildDecompiledText
import org.jetbrains.kotlin.idea.decompiler.textBuilder.defaultDecompilerRendererOptions
import org.jetbrains.kotlin.load.kotlin.header.KotlinClassHeader
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmMetadataVersion
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.types.asFlexibleType
import org.jetbrains.kotlin.types.isFlexible

class KotlinClassFileDecompiler : ClassFileDecompilers.Full() {
    private val stubBuilder = KotlinClsStubBuilder()

    override fun accepts(file: VirtualFile) = IDEKotlinBinaryClassCache.getInstance().isKotlinJvmCompiledFile(file)

    override fun getStubBuilder() = stubBuilder

    override fun createFileViewProvider(file: VirtualFile, manager: PsiManager, physical: Boolean): KotlinDecompiledFileViewProvider {
        return KotlinDecompiledFileViewProvider(manager, file, physical) factory@{ provider ->
            val virtualFile = provider.virtualFile

            if (isKotlinInternalCompiledFile(virtualFile))
                null
            else
                KtClsFile(provider)
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
    val classHeader =
        IDEKotlinBinaryClassCache.getInstance().getKotlinBinaryClassHeaderData(classFile)
            ?: error("Decompiled data factory shouldn't be called on an unsupported file: $classFile")

    val classId = classHeader.classId

    if (!classHeader.metadataVersion.isCompatible()) {
        return createIncompatibleAbiVersionDecompiledText(JvmMetadataVersion.INSTANCE, classHeader.metadataVersion)
    }

    fun buildText(declarations: List<DeclarationDescriptor>) = buildDecompiledText(
        classHeader.packageName?.let(::FqName) ?: classId.packageFqName,
        declarations, decompilerRendererForClassFiles, listOf(ByDescriptorIndexer, BySignatureIndexer)
    )

    return when (classHeader.kind) {
        KotlinClassHeader.Kind.FILE_FACADE ->
            buildText(resolver.resolveDeclarationsInFacade(classId.asSingleFqName()))
        KotlinClassHeader.Kind.CLASS -> {
            buildText(listOfNotNull(resolver.resolveTopLevelClass(classId)))
        }
        KotlinClassHeader.Kind.MULTIFILE_CLASS -> {
            val partClasses = findMultifileClassParts(classFile, classId, classHeader.partNamesIfMultifileFacade)
            val partMembers = partClasses.flatMap { partClass ->
                resolver.resolveDeclarationsInFacade(partClass.classId.asSingleFqName())
            }
            buildText(partMembers)
        }
        else ->
            throw UnsupportedOperationException("Unknown header kind: $classHeader, class $classId")
    }
}
