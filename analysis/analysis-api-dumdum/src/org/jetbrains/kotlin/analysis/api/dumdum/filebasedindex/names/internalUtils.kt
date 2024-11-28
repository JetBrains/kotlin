// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.jetbrains.kotlin.analysis.api.dumdum.filebasedindex.names

import com.intellij.openapi.diagnostic.ControlFlowException
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.indexing.FileContent
import org.jetbrains.kotlin.analysis.decompiler.konan.FileWithMetadata
import org.jetbrains.kotlin.analysis.decompiler.psi.BuiltInDefinitionFile
import org.jetbrains.kotlin.analysis.decompiler.psi.KotlinBuiltInFileType
import org.jetbrains.kotlin.load.kotlin.KotlinBinaryClassCache
import org.jetbrains.kotlin.load.kotlin.KotlinClassFinder
import org.jetbrains.kotlin.load.kotlin.KotlinJvmBinaryClass
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.deserialization.MetadataVersion
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmNameResolver
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmProtoBufUtil
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.serialization.deserialization.DOT_METADATA_FILE_EXTENSION
import org.jetbrains.kotlin.serialization.deserialization.builtins.BuiltInSerializerProtocol
import org.jetbrains.kotlin.analysis.decompiler.stub.file.KotlinMetadataStubBuilder.FileWithMetadata.Compatible as CompatibleMetadata

private val ALLOWED_METADATA_EXTENSIONS = listOf(
    BuiltInSerializerProtocol.DOT_DEFAULT_EXTENSION,
    DOT_METADATA_FILE_EXTENSION
)

fun readKotlinMetadataDefinition(fileContent: FileContent): CompatibleMetadata? {
    if (fileContent.fileType != KotlinBuiltInFileType) {
        return null
    }

    val fileName = fileContent.fileName
    if (ALLOWED_METADATA_EXTENSIONS.none { fileName.endsWith(it) }) {
        return null
    }

    val definition = BuiltInDefinitionFile.read(fileContent.content, fileContent.file.parent) as? CompatibleMetadata ?: return null

    // '.kotlin_builtins' files sometimes appear in random libraries.
    // Below there's an additional check that the file is likely to be an actual part of built-ins.

    val nestingLevel = definition.packageFqName.pathSegments().size
    val rootPackageDirectory = generateSequence(fileContent.file) { it.parent }.drop(nestingLevel + 1).firstOrNull() ?: return null
    val metaInfDirectory = rootPackageDirectory.findChild("META-INF") ?: return null

    if (metaInfDirectory.children.none { it.extension == "kotlin_module" }) {
        // Here can be a more strict check.
        // For instance, we can check if the manifest file has a 'Kotlin-Runtime-Component' attribute.
        // It's unclear if it would break use-cases when the standard library is embedded, though.
        return null
    }

    return definition
}

internal fun readProtoPackageData(kotlinJvmBinaryClass: KotlinJvmBinaryClass): Pair<JvmNameResolver, ProtoBuf.Package>? {
    val header = kotlinJvmBinaryClass.classHeader
    val data = header.data ?: header.incompatibleData ?: return null
    val strings = header.strings ?: return null
    return JvmProtoBufUtil.readPackageDataFrom(data, strings)
}

internal fun FileContent.toKotlinJvmBinaryClass(): KotlinJvmBinaryClass? {
    val result = try {
        KotlinBinaryClassCache.getKotlinBinaryClassOrClassFileContent(file, MetadataVersion.INSTANCE, content) ?: return null
    } catch (e: Exception) {
        if (e is ControlFlowException) throw e

        // If the class file cannot be read, e.g. when it is broken, we don't need to index it.
        return null
    }
    val kotlinClass = result as? KotlinClassFinder.Result.KotlinClass ?: return null
    return kotlinClass.kotlinJvmBinaryClass
}

internal val KotlinJvmBinaryClass.packageName: FqName
    get() = classHeader.packageName?.let(::FqName) ?: classId.packageFqName

internal fun FileContent.toCompatibleFileWithMetadata(): FileWithMetadata.Compatible? =
    FileWithMetadata.forPackageFragment(file) as? FileWithMetadata.Compatible
