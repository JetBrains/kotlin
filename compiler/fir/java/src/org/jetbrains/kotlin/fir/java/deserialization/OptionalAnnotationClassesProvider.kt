/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.java.deserialization

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.deserialization.AbstractFirDeserializedSymbolProvider
import org.jetbrains.kotlin.fir.deserialization.FirDeserializationContext
import org.jetbrains.kotlin.fir.deserialization.ModuleDataProvider
import org.jetbrains.kotlin.fir.deserialization.PackagePartsCacheData
import org.jetbrains.kotlin.fir.scopes.FirKotlinScopeProvider
import org.jetbrains.kotlin.load.kotlin.PackagePartProvider
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.jvm.JvmProtoBuf
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmFlags
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.serialization.deserialization.ClassData
import org.jetbrains.kotlin.serialization.deserialization.getClassId

class OptionalAnnotationClassesProvider(
    session: FirSession,
    moduleDataProvider: ModuleDataProvider,
    kotlinScopeProvider: FirKotlinScopeProvider,
    val packagePartProvider: PackagePartProvider,
    defaultDeserializationOrigin: FirDeclarationOrigin = FirDeclarationOrigin.Library
) : AbstractFirDeserializedSymbolProvider(session, moduleDataProvider, kotlinScopeProvider, defaultDeserializationOrigin) {

    private val optionalAnnotationClassesAndPackages by lazy(LazyThreadSafetyMode.PUBLICATION) {
        val optionalAnnotationClasses = mutableMapOf<ClassId, ClassData>()
        val optionalAnnotationPackages = mutableSetOf<FqName>()

        for (klass in packagePartProvider.getAllOptionalAnnotationClasses()) {
            val classId = klass.nameResolver.getClassId(klass.classProto.fqName)
            optionalAnnotationClasses[classId] = klass
            optionalAnnotationPackages.add(classId.packageFqName)
        }

        return@lazy Pair(optionalAnnotationClasses, optionalAnnotationPackages)
    }

    override fun computePackagePartsInfos(packageFqName: FqName): List<PackagePartsCacheData> {
        return emptyList()
    }

    override fun extractClassMetadata(
        classId: ClassId,
        parentContext: FirDeserializationContext?
    ): ClassMetadataFindResult? {
        val optionalAnnotationClass = optionalAnnotationClassesAndPackages.first[classId] ?: return null

        return ClassMetadataFindResult.Metadata(
            optionalAnnotationClass.nameResolver,
            optionalAnnotationClass.classProto,
            null,
            moduleDataProvider.allModuleData.last(),
            null,
            classPostProcessor = null
        )
    }

    override fun isNewPlaceForBodyGeneration(classProto: ProtoBuf.Class): Boolean {
        return JvmFlags.IS_COMPILED_IN_JVM_DEFAULT_MODE.get(classProto.getExtension(JvmProtoBuf.jvmClassFlags))
    }

    override fun getPackage(fqName: FqName): FqName? = if (optionalAnnotationClassesAndPackages.second.contains(fqName)) fqName else null
}