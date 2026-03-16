/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.serialization

import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.FirClassLikeDeclaration
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.toAnnotationClassId
import org.jetbrains.kotlin.fir.packageFqName
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.library.metadata.buildKlibPackageFragment
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.serialization.SerializableStringTable

// TODO: handle incremental/monolothic (see klib serializer) - maybe externally
fun serializeSingleFirFile(
    file: FirFile, session: FirSession, scopeSession: ScopeSession,
    actualizedExpectDeclarations: Set<FirDeclaration>?,
    serializerExtension: FirKLibSerializerExtension,
    languageVersionSettings: LanguageVersionSettings,
    produceHeaderKlib: Boolean = false,
): ProtoBuf.PackageFragment {
    val approximator = TypeApproximatorForMetadataSerializer(session)

    val packageSerializer = FirElementSerializer.createTopLevel(
        session, scopeSession, serializerExtension,
        approximator,
        languageVersionSettings,
        produceHeaderKlib
    )
    val packageProto = packageSerializer.packagePartProto(file, actualizedExpectDeclarations).build()

    val classesProto = mutableListOf<Pair<ProtoBuf.Class, Int>>()

    fun FirClass.makeClassProtoWithNested() {
        if (!isNotExpectOrShouldBeSerialized(actualizedExpectDeclarations) ||
            !isNotPrivateOrShouldBeSerialized(produceHeaderKlib)
        ) {
            return
        }

        val classSerializer = FirElementSerializer.create(
            session, scopeSession, klass = this, serializerExtension, parentSerializer = null,
            approximator, languageVersionSettings, produceHeaderKlib
        )
        val index = classSerializer.stringTable.getFqNameIndex(this)

        classesProto += classSerializer.classProto(this, file).build() to index

        for (nestedClassifierSymbol in classSerializer.computeNestedClassifiersForClass(symbol)) {
            (nestedClassifierSymbol as? FirClassSymbol<*>)?.fir?.makeClassProtoWithNested()
        }
    }

    for (declaration in file.declarations) {
        (declaration as? FirClass)?.makeClassProtoWithNested()
    }

    val fileAnnotationProtos = file.annotations
        .filter { it.toAnnotationClassId(session) in KLIB_FILE_ANNOTATION_CLASS_IDS }
        .mapNotNull { annotation ->
            serializerExtension.annotationSerializer.serializeAnnotation(annotation)
        }

    return buildKlibPackageFragment(
        packageProto,
        classesProto,
        file.packageFqName,
        isEmpty = packageProto.functionList.isEmpty() &&
                packageProto.propertyList.isEmpty() &&
                packageProto.typeAliasList.isEmpty() &&
                classesProto.isEmpty(),
        serializerExtension.stringTable as SerializableStringTable,
        fileAnnotations = fileAnnotationProtos,
    )
}

/**
 * The set of file-level annotation class IDs that are serialized into KLib metadata.
 *
 * Currently, only JS-specific annotations are supported to unblock work on `@JsExport`.
 * This restriction can be lifted in the future to include other file-level annotations.
 */
private val KLIB_FILE_ANNOTATION_CLASS_IDS: Set<ClassId> = run {
    val jsPackage = StandardClassIds.BASE_KOTLIN_PACKAGE.child(Name.identifier("js"))
    setOf(
        ClassId(jsPackage, Name.identifier("JsExport")),
        ClassId(jsPackage, Name.identifier("JsFileName")),
    )
}

class FirElementAwareSerializableStringTable : FirElementAwareStringTable, SerializableStringTable() {
    override fun getLocalClassLikeDeclarationIdReplacement(declaration: FirClassLikeDeclaration): ClassId = StandardClassIds.Any
}
