/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.decompiler.psi.text

import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.analysis.decompiler.psi.DeserializerForClassfileDecompiler
import org.jetbrains.kotlin.analysis.decompiler.psi.ResolverForDecompiler
import org.jetbrains.kotlin.analysis.decompiler.stub.file.ClsClassFinder.findMultifileClassParts
import org.jetbrains.kotlin.analysis.decompiler.stub.file.ClsKotlinBinaryClassCache
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.load.kotlin.header.KotlinClassHeader
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmMetadataVersion
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.types.asFlexibleType
import org.jetbrains.kotlin.types.isFlexible

fun buildDecompiledTextForClassFile(
    classFile: VirtualFile,
    resolver: ResolverForDecompiler = DeserializerForClassfileDecompiler(classFile)
): DecompiledText {
    val classHeader =
        ClsKotlinBinaryClassCache.getInstance().getKotlinBinaryClassHeaderData(classFile)
            ?: error("Decompiled data factory shouldn't be called on an unsupported file: $classFile")

    val classId = classHeader.classId

    if (!classHeader.metadataVersion.isCompatibleWithCurrentCompilerVersion()) {
        return createIncompatibleAbiVersionDecompiledText(JvmMetadataVersion.INSTANCE, classHeader.metadataVersion)
    }

    fun buildText(declarations: List<DeclarationDescriptor>) = buildDecompiledText(
        classHeader.packageNameWithFallback,
        declarations, decompilerRendererForClassFiles
    )

    return when (classHeader.kind) {
        KotlinClassHeader.Kind.FILE_FACADE, KotlinClassHeader.Kind.MULTIFILE_CLASS_PART ->
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

private val decompilerRendererForClassFiles = DescriptorRenderer.withOptions {
    defaultDecompilerRendererOptions()
    typeNormalizer = { type -> if (type.isFlexible()) type.asFlexibleType().lowerBound else type }
}