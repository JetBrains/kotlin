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

package org.jetbrains.kotlin.idea.decompiler.builtIns

import com.intellij.ide.highlighter.JavaClassFileType
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiManager
import com.intellij.psi.compiled.ClassFileDecompilers
import org.jetbrains.kotlin.builtins.BuiltInsSerializedResourcePaths
import org.jetbrains.kotlin.idea.decompiler.KotlinDecompiledFileViewProvider
import org.jetbrains.kotlin.idea.decompiler.KtDecompiledFile
import org.jetbrains.kotlin.idea.decompiler.common.toClassProto
import org.jetbrains.kotlin.idea.decompiler.common.toPackageProto
import org.jetbrains.kotlin.idea.decompiler.textBuilder.DecompiledText
import org.jetbrains.kotlin.idea.decompiler.textBuilder.buildDecompiledText
import org.jetbrains.kotlin.idea.decompiler.textBuilder.defaultDecompilerRendererOptions
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.serialization.ProtoBuf
import org.jetbrains.kotlin.serialization.builtins.BuiltInsProtoBuf
import org.jetbrains.kotlin.serialization.deserialization.NameResolverImpl

class KotlinBuiltInDecompiler : ClassFileDecompilers.Full() {
    private val stubBuilder = KotlinBuiltInStubBuilder()

    override fun accepts(file: VirtualFile): Boolean {
        return file.fileType == KotlinBuiltInClassFileType || file.fileType == KotlinBuiltInPackageFileType
    }

    override fun getStubBuilder() = stubBuilder

    override fun createFileViewProvider(file: VirtualFile, manager: PsiManager, physical: Boolean): FileViewProvider {
        return KotlinDecompiledFileViewProvider(manager, file, physical) { provider ->
            if (isInternalBuiltInFile(provider.virtualFile)) {
                null
            }
            else {
                KtDecompiledFile(provider) { file ->
                    buildDecompiledTextForBuiltIns(file)
                }
            }
        }
    }

    companion object {
        val LOG = Logger.getInstance(KotlinBuiltInDecompiler::class.java)
    }
}

private val decompilerRendererForBuiltIns = DescriptorRenderer.withOptions { defaultDecompilerRendererOptions() }

public fun buildDecompiledTextForBuiltIns(
        builtInFile: VirtualFile
): DecompiledText {
    val directory = builtInFile.parent!!
    val nameResolver = readStringTable(directory, KotlinBuiltInDecompiler.LOG)!!
    val content = builtInFile.contentsToByteArray()
    return when (builtInFile.fileType) {
        KotlinBuiltInPackageFileType -> {
            val packageFqName = content.toPackageProto(BuiltInsSerializedResourcePaths.extensionRegistry).packageFqName(nameResolver)
            val resolver = KotlinBuiltInDeserializerForDecompiler(directory, packageFqName, nameResolver)
            buildDecompiledText(packageFqName, resolver.resolveDeclarationsInFacade(packageFqName), decompilerRendererForBuiltIns)
        }
        KotlinBuiltInClassFileType -> {
            val classProto = content.toClassProto(BuiltInsSerializedResourcePaths.extensionRegistry)
            val classId = nameResolver.getClassId(classProto.fqName)
            val packageFqName = classId.packageFqName
            val resolver = KotlinBuiltInDeserializerForDecompiler(directory, packageFqName, nameResolver)
            buildDecompiledText(
                    packageFqName,
                    listOfNotNull(resolver.resolveTopLevelClass(classId)),
                    decompilerRendererForBuiltIns
            )
        }
        else -> error("Unexpected filetype ${builtInFile.fileType}")
    }
}

fun ProtoBuf.Package.packageFqName(nameResolver: NameResolverImpl): FqName {
    return nameResolver.getPackageFqName(getExtension(BuiltInsProtoBuf.packageFqName))
}

fun isInternalBuiltInFile(virtualFile: VirtualFile): Boolean {
    when (virtualFile.fileType) {
        KotlinBuiltInPackageFileType -> {
            // do not accept kotlin_package files without packageFqName extension to avoid failing on older runtimes
            // this check may be costly but there are few kotlin_package files
            val packageProto = virtualFile.contentsToByteArray().toPackageProto(BuiltInsSerializedResourcePaths.extensionRegistry)
            return !packageProto.hasExtension(BuiltInsProtoBuf.packageFqName)
        }
        KotlinBuiltInClassFileType -> {
            val correspondsToInnerClass = virtualFile.nameWithoutExtension.contains('.')
            if (correspondsToInnerClass) {
                return true
            }
            val classFileName = virtualFile.nameWithoutExtension + "." + JavaClassFileType.INSTANCE.defaultExtension
            val classFileForTheSameClassIsPresent = virtualFile.parent!!.findChild(classFileName) != null
            return classFileForTheSameClassIsPresent
        }
        else -> error("Unexpected filetype ${virtualFile.fileType}")
    }
}