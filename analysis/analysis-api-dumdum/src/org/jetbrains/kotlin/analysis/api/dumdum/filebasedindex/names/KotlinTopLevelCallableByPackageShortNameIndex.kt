// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.jetbrains.kotlin.analysis.api.dumdum.filebasedindex.names

import com.intellij.util.indexing.FileContent
import com.intellij.util.indexing.ID
import org.jetbrains.kotlin.analysis.decompiler.konan.FileWithMetadata
import org.jetbrains.kotlin.load.kotlin.KotlinJvmBinaryClass
import org.jetbrains.kotlin.load.kotlin.header.KotlinClassHeader
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.deserialization.NameResolver
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtCallableDeclaration
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.serialization.deserialization.getName

class KotlinTopLevelCallableByPackageShortNameIndex : NameByPackageShortNameIndex() {
    companion object {
        val NAME = ID.create<FqName, List<Name>>(KotlinTopLevelCallableByPackageShortNameIndex::class.java.simpleName)
    }

    override val name: ID<FqName, List<Name>>
        get() = NAME

    override val version: Int
        get() = 3

    override fun getDeclarationNamesByKtFile(ktFile: KtFile): List<Name> = buildList {
        for (declaration in ktFile.declarations) {
            if (declaration is KtCallableDeclaration) {
                val name = declaration.nameAsName ?: continue
                if (name.isSpecial) continue
                add(name)
            }
        }
    }

    override fun getDeclarationNamesByMetadata(kotlinJvmBinaryClass: KotlinJvmBinaryClass): List<Name> {
        if (kotlinJvmBinaryClass.classHeader.kind == KotlinClassHeader.Kind.CLASS) return emptyList()
        if (kotlinJvmBinaryClass.classHeader.kind == KotlinClassHeader.Kind.SYNTHETIC_CLASS) return emptyList()
        if (kotlinJvmBinaryClass.classHeader.kind == KotlinClassHeader.Kind.MULTIFILE_CLASS) {
            // MULTIFILE_CLASS does not contain any callables, all callables are inside MULTIFILE_CLASS_PARTs
            return emptyList()
        }

        val (nameResolver, proto) = readProtoPackageData(kotlinJvmBinaryClass) ?: return emptyList()

        return getTopLevelDeclarationNamesFromProto(proto, nameResolver)
    }

    override fun getPackageAndNamesFromBuiltIns(fileContent: FileContent): Map<FqName, List<Name>> {
        val metadata = readKotlinMetadataDefinition(fileContent) ?: return emptyMap()
        //there are no top level properties in builtins
        return mapOf(metadata.packageFqName to metadata.proto.`package`.functionOrBuilderList.map { metadata.nameResolver.getName(it.name) })
    }

    override fun getDeclarationNamesByKnm(kotlinNativeMetadata: FileWithMetadata.Compatible): List<Name> {
        val nameResolver = kotlinNativeMetadata.nameResolver
        val packageProto = kotlinNativeMetadata.proto.`package`

        return getTopLevelDeclarationNamesFromProto(packageProto, nameResolver)
    }

    private fun getTopLevelDeclarationNamesFromProto(packageProto: ProtoBuf.Package, nameResolver: NameResolver): List<Name> {
        return buildList {
            packageProto.functionOrBuilderList.mapTo(this) { nameResolver.getName(it.name) }
            packageProto.propertyOrBuilderList.mapTo(this) { nameResolver.getName(it.name) }
        }
    }
}