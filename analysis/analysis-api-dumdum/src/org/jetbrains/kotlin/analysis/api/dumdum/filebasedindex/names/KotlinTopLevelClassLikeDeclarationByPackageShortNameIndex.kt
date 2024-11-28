// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.jetbrains.kotlin.analysis.api.dumdum.filebasedindex.names

import com.intellij.util.indexing.FileContent
import com.intellij.util.indexing.ID
import org.jetbrains.kotlin.analysis.decompiler.konan.FileWithMetadata
import org.jetbrains.kotlin.analysis.decompiler.psi.BuiltInDefinitionFile
import org.jetbrains.kotlin.load.kotlin.KotlinJvmBinaryClass
import org.jetbrains.kotlin.load.kotlin.header.KotlinClassHeader
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtClassLikeDeclaration
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.serialization.deserialization.getClassId
import org.jetbrains.kotlin.serialization.deserialization.getName

class KotlinTopLevelClassLikeDeclarationByPackageShortNameIndex : NameByPackageShortNameIndex() {
    companion object {
        val NAME = ID.create<FqName, List<Name>>(KotlinTopLevelClassLikeDeclarationByPackageShortNameIndex::class.java.simpleName)
    }

    override fun getName(): ID<FqName, List<Name>> = NAME

    override fun getVersion(): Int = 3

    override fun getDeclarationNamesByKtFile(ktFile: KtFile): List<Name> = buildList {
        for (declaration in ktFile.declarations) {
            if (declaration is KtClassLikeDeclaration) {
                val name = declaration.nameAsName ?: continue
                if (name.isSpecial) continue
                add(name)
            }
        }
    }

    override fun getDeclarationNamesByMetadata(kotlinJvmBinaryClass: KotlinJvmBinaryClass): List<Name> {
        return when (kotlinJvmBinaryClass.classHeader.kind) {
            KotlinClassHeader.Kind.CLASS -> {
                val className = kotlinJvmBinaryClass.classId.relativeClassName.pathSegments().first()
                if (className.isSpecial) return listOf()
                listOf(className)
            }

            KotlinClassHeader.Kind.FILE_FACADE, KotlinClassHeader.Kind.MULTIFILE_CLASS, KotlinClassHeader.Kind.MULTIFILE_CLASS_PART -> {
                val (nameResolver, proto) = readProtoPackageData(kotlinJvmBinaryClass) ?: return listOf()
                proto.typeAliasList.map { nameResolver.getName(it.name) }
            }

            KotlinClassHeader.Kind.UNKNOWN -> listOf()
            KotlinClassHeader.Kind.SYNTHETIC_CLASS -> listOf()
        }
    }

    override fun getPackageAndNamesFromBuiltIns(fileContent: FileContent): Map<FqName, List<Name>> {
        val builtins = readKotlinMetadataDefinition(fileContent) as? BuiltInDefinitionFile
            ?: return emptyMap()
       
        val names = buildList {
            builtins.proto.class_List.forEach { klass ->
                val classId = builtins.nameResolver.getClassId(klass.fqName)
                if (classId.isLocal) return@forEach
                add(classId.relativeClassName.pathSegments().first())
            }
        }

        return mapOf(builtins.packageFqName to names.distinct())
    }

    override fun getDeclarationNamesByKnm(kotlinNativeMetadata: FileWithMetadata.Compatible): List<Name> {
        val nameResolver = kotlinNativeMetadata.nameResolver
        val names = buildList {
            kotlinNativeMetadata.classesToDecompile.mapTo(this) { pbClass ->
                val classId = nameResolver.getClassId(pbClass.fqName)
                classId.shortClassName
            }
            kotlinNativeMetadata.proto.`package`.typeAliasOrBuilderList.mapTo(this) { pbTypeAlias ->
                nameResolver.getName(pbTypeAlias.name)
            }
        }

        return names
    }
}
