/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.session

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.ThreadSafeMutableState
import org.jetbrains.kotlin.fir.caches.createCache
import org.jetbrains.kotlin.fir.caches.firCachesFactory
import org.jetbrains.kotlin.fir.caches.getValue
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
import org.jetbrains.kotlin.utils.addToStdlib.runIf

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

    private val constDeserializer = FirConstDeserializer(BuiltInSerializerProtocol)

    private val metadataTopLevelClassesInPackageCache = session.firCachesFactory.createCache(::findMetadataTopLevelClassesInPackage)

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
                FirTypeDeserializer.FlexibleTypeFactory.Default,
                constDeserializer = constDeserializer,
                containerSource = null,
                deserializeAsActual = false,
            )

            return@mapNotNull PackagePartsCacheData(proto.`package`, context)
        }
    }

    override fun computePackageSetWithNonClassDeclarations(): Set<String> {
        return packageAndMetadataPartProvider.computePackageSetWithNonClassDeclarations()
    }

    override fun knownTopLevelClassesInPackage(packageFqName: FqName): Set<String>? {
        return metadataTopLevelClassesInPackageCache.getValue(packageFqName)
    }

    override fun extractClassMetadata(classId: ClassId, parentContext: FirDeserializationContext?): ClassMetadataFindResult? {
        val classData = classDataFinder.findClassData(classId) ?: return null
        return ClassMetadataFindResult.Metadata(
            classData.nameResolver,
            classData.classProto,
            annotationDeserializer = annotationDeserializer,
            moduleDataProvider.allModuleData.last(),
            sourceElement = null,
            classPostProcessor = null,
            FirTypeDeserializer.FlexibleTypeFactory.Default,
        )
    }

    override fun isNewPlaceForBodyGeneration(classProto: ProtoBuf.Class): Boolean = false

    override fun getPackage(fqName: FqName): FqName? =
        runIf(metadataTopLevelClassesInPackageCache.getValue(fqName)?.isNotEmpty() == true) { fqName }

    private fun findMetadataTopLevelClassesInPackage(packageFqName: FqName): Set<String>? =
        kotlinClassFinder.findMetadataTopLevelClassesInPackage(packageFqName)
}
