/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.session

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.ThreadSafeMutableState
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.deserialization.*
import org.jetbrains.kotlin.fir.java.deserialization.KotlinBuiltins
import org.jetbrains.kotlin.fir.scopes.FirKotlinScopeProvider
import org.jetbrains.kotlin.load.kotlin.PackageAndMetadataPartProvider
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.serialization.deserialization.KotlinMetadataFinder
import org.jetbrains.kotlin.serialization.deserialization.MetadataClassDataFinder
import org.jetbrains.kotlin.serialization.deserialization.builtins.BuiltInSerializerProtocol
import org.jetbrains.kotlin.serialization.deserialization.readProto

@ThreadSafeMutableState
class MetadataSymbolProvider(
    session: FirSession,
    moduleDataProvider: ModuleDataProvider,
    kotlinScopeProvider: FirKotlinScopeProvider,
    private val packageAndMetadataPartProvider: PackageAndMetadataPartProvider,
    private val kotlinClassFinder: KotlinMetadataFinder,
    defaultDeserializationOrigin: FirDeclarationOrigin = FirDeclarationOrigin.Library
) : AbstractFirDeserializedSymbolProvider(
    session, moduleDataProvider, kotlinScopeProvider, defaultDeserializationOrigin, BuiltInSerializerProtocol
) {
    private val classDataFinder = MetadataClassDataFinder(kotlinClassFinder)

    private val annotationDeserializer = MetadataBasedAnnotationDeserializer(session)

    private val constDeserializer = FirConstDeserializer(session, BuiltInSerializerProtocol)

    override fun computePackagePartsInfos(packageFqName: FqName): List<PackagePartsCacheData> {
        return packageAndMetadataPartProvider.findMetadataPackageParts(packageFqName.asString()).mapNotNull { partName ->
            if (partName in KotlinBuiltins) return@mapNotNull null
            val classId = ClassId(packageFqName, Name.identifier(partName))

            val stream = kotlinClassFinder.findMetadata(classId) ?: return@mapNotNull null
            val (proto, nameResolver, _) = readProto(stream)

            val context = FirDeserializationContext.createForPackage(
                packageFqName,
                proto.`package`,
                nameResolver,
                moduleDataProvider.allModuleData.last(),
                annotationDeserializer = annotationDeserializer,
                constDeserializer = constDeserializer,
                containerSource = null
            )

            return@mapNotNull PackagePartsCacheData(proto.`package`, context)
        }
    }

    override fun computePackageSetWithNonClassDeclarations() = packageAndMetadataPartProvider.computePackageSetWithNonClassDeclarations()

    override fun knownTopLevelClassesInPackage(packageFqName: FqName) = kotlinClassFinder.findMetadataTopLevelClassesInPackage(packageFqName)

    override fun extractClassMetadata(classId: ClassId, parentContext: FirDeserializationContext?): ClassMetadataFindResult? {
        val classData = classDataFinder.findClassData(classId) ?: return null
        return ClassMetadataFindResult.Metadata(
            classData.nameResolver,
            classData.classProto,
            annotationDeserializer = annotationDeserializer,
            moduleDataProvider.allModuleData.last(),
            sourceElement = null,
            classPostProcessor = null
        )
    }

    override fun isNewPlaceForBodyGeneration(classProto: ProtoBuf.Class) = false

    override fun getPackage(fqName: FqName) = null
}
