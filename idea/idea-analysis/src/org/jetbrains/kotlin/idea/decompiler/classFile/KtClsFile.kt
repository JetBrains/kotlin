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

import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.idea.decompiler.KotlinDecompiledFileViewProviderBase
import org.jetbrains.kotlin.idea.decompiler.KtDecompiledFileBase
import org.jetbrains.kotlin.idea.decompiler.textBuilder.DecompiledText
import org.jetbrains.kotlin.idea.decompiler.textBuilder.ResolverForDecompiler
import org.jetbrains.kotlin.idea.decompiler.textBuilder.buildDecompiledText
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.load.kotlin.KotlinBinaryClassCache
import org.jetbrains.kotlin.load.kotlin.header.isCompatibleClassKind
import org.jetbrains.kotlin.load.kotlin.header.isCompatibleFileFacadeKind
import org.jetbrains.kotlin.load.kotlin.header.isCompatibleMultifileClassKind
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.renderer.DescriptorRendererModifier
import org.jetbrains.kotlin.renderer.ExcludedTypeAnnotations
import org.jetbrains.kotlin.types.flexibility
import org.jetbrains.kotlin.types.isFlexible
import org.jetbrains.kotlin.utils.concurrent.block.LockedClearableLazyValue
import java.util.*

public class KtClsFile(provider: KotlinDecompiledFileViewProviderBase) : KtDecompiledFileBase(provider) {
    override fun buildDecompiledText() = buildDecompiledTextForClassFile(virtualFile)
}

private val descriptorRendererForClassFileDecompiler = DescriptorRenderer.withOptions {
    withDefinedIn = false
    classWithPrimaryConstructor = true
    typeNormalizer = { type -> if (type.isFlexible()) type.flexibility().lowerBound else type }
    secondaryConstructorsAsPrimary = false
    modifiers = DescriptorRendererModifier.ALL
    excludedTypeAnnotationClasses = ExcludedTypeAnnotations.annotationsForNullabilityAndMutability
}

private val FILE_ABI_VERSION_MARKER: String = "FILE_ABI"
private val CURRENT_ABI_VERSION_MARKER: String = "CURRENT_ABI"

public val INCOMPATIBLE_ABI_VERSION_GENERAL_COMMENT: String = "// This class file was compiled with different version of Kotlin compiler and can't be decompiled."
public val INCOMPATIBLE_ABI_VERSION_COMMENT: String =
        "$INCOMPATIBLE_ABI_VERSION_GENERAL_COMMENT\n" +
        "//\n" +
        "// Current compiler ABI version is $CURRENT_ABI_VERSION_MARKER\n" +
        "// File ABI version is $FILE_ABI_VERSION_MARKER"

public fun buildDecompiledTextForClassFile(
        classFile: VirtualFile,
        resolver: ResolverForDecompiler = DeserializerForClassfileDecompiler(classFile)
): DecompiledText {
    val kotlinClass = KotlinBinaryClassCache.getKotlinBinaryClass(classFile)
    assert(kotlinClass != null) { "Decompiled data factory shouldn't be called on an unsupported file: " + classFile }
    val classId = kotlinClass!!.getClassId()
    val classHeader = kotlinClass.getClassHeader()
    val packageFqName = classId.getPackageFqName()

    return when {
        !classHeader.isCompatibleAbiVersion -> {
            DecompiledText(
                    INCOMPATIBLE_ABI_VERSION_COMMENT
                            .replace(CURRENT_ABI_VERSION_MARKER, JvmAbi.VERSION.toString())
                            .replace(FILE_ABI_VERSION_MARKER, classHeader.version.toString()),
                    mapOf())
        }
        classHeader.isCompatibleFileFacadeKind() ->
            buildDecompiledText(packageFqName, ArrayList(resolver.resolveDeclarationsInFacade(classId.asSingleFqName())), descriptorRendererForClassFileDecompiler)
        classHeader.isCompatibleClassKind() ->
            buildDecompiledText(packageFqName, listOfNotNull(resolver.resolveTopLevelClass(classId)), descriptorRendererForClassFileDecompiler)
        classHeader.isCompatibleMultifileClassKind() -> {
            val partClasses = findMultifileClassParts(classFile, kotlinClass)
            val partMembers = partClasses.flatMap { partClass -> resolver.resolveDeclarationsInFacade(partClass.classId.asSingleFqName()) }
            buildDecompiledText(packageFqName, partMembers, descriptorRendererForClassFileDecompiler)
        }
        else ->
            throw UnsupportedOperationException("Unknown header kind: ${classHeader.kind} ${classHeader.isCompatibleAbiVersion}")
    }
}
