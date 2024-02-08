/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.java.deserialization

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.deserialization.*
import org.jetbrains.kotlin.fir.scopes.FirKotlinScopeProvider
import org.jetbrains.kotlin.load.kotlin.PackagePartProvider
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.jvm.JvmProtoBuf
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmFlags
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.serialization.deserialization.ClassData
import org.jetbrains.kotlin.serialization.deserialization.builtins.BuiltInSerializerProtocol
import org.jetbrains.kotlin.serialization.deserialization.getClassId

class OptionalAnnotationClassesProvider(
    session: FirSession,
    moduleDataProvider: ModuleDataProvider,
    kotlinScopeProvider: FirKotlinScopeProvider,
    val packagePartProvider: PackagePartProvider,
    defaultDeserializationOrigin: FirDeclarationOrigin = FirDeclarationOrigin.Library
) : AbstractFirDeserializedSymbolProvider(
    session, moduleDataProvider, kotlinScopeProvider, defaultDeserializationOrigin, BuiltInSerializerProtocol
) {

    private val annotationDeserializer = MetadataBasedAnnotationDeserializer(session)

    private val optionalAnnotationClassesAndPackages by lazy(LazyThreadSafetyMode.PUBLICATION) {
        val optionalAnnotationClasses = mutableMapOf<ClassId, ClassData>()
        val optionalAnnotationPackages = mutableSetOf<String>()

        for (klass in packagePartProvider.getAllOptionalAnnotationClasses()) {
            val classId = klass.nameResolver.getClassId(klass.classProto.fqName)
            optionalAnnotationClasses[classId] = klass
            optionalAnnotationPackages.add(classId.packageFqName.asString())
        }

        return@lazy Pair(optionalAnnotationClasses, optionalAnnotationPackages)
    }

    private val optionalAnnotationClassNamesByPackage: Map<FqName, Set<String>> by lazy(LazyThreadSafetyMode.PUBLICATION) {
        buildMap<FqName, MutableSet<String>> {
            for (classId in optionalAnnotationClassesAndPackages.first.keys) {
                getOrPut(classId.packageFqName, ::mutableSetOf).add(classId.shortClassName.asString())
            }
        }
    }

    override fun computePackagePartsInfos(packageFqName: FqName): List<PackagePartsCacheData> {
        return emptyList()
    }

    override fun computePackageSetWithNonClassDeclarations(): Set<String> = optionalAnnotationClassesAndPackages.second

    override fun knownTopLevelClassesInPackage(packageFqName: FqName): Set<String> =
        optionalAnnotationClassNamesByPackage[packageFqName] ?: emptySet()

    override fun extractClassMetadata(
        classId: ClassId,
        parentContext: FirDeserializationContext?
    ): ClassMetadataFindResult? {
        val optionalAnnotationClass = optionalAnnotationClassesAndPackages.first[classId] ?: return null

        return ClassMetadataFindResult.Metadata(
            optionalAnnotationClass.nameResolver,
            optionalAnnotationClass.classProto,
            annotationDeserializer,
            moduleDataProvider.allModuleData.last(),
            null,
            classPostProcessor = null
        )
    }

    override fun isNewPlaceForBodyGeneration(classProto: ProtoBuf.Class): Boolean {
        return JvmFlags.IS_COMPILED_IN_JVM_DEFAULT_MODE.get(classProto.getExtension(JvmProtoBuf.jvmClassFlags))
    }

    override fun getPackage(fqName: FqName): FqName? =
        if (optionalAnnotationClassesAndPackages.second.contains(fqName.asString())) fqName else null

    companion object {
        /**
         * Creates a new [OptionalAnnotationClassesProvider] if [packagePartProvider] has any optional annotation classes. Otherwise, the
         * symbol provider does not need to be created because it would provide no symbols.
         */
        fun createIfNeeded(
            session: FirSession,
            moduleDataProvider: ModuleDataProvider,
            kotlinScopeProvider: FirKotlinScopeProvider,
            packagePartProvider: PackagePartProvider,
            defaultDeserializationOrigin: FirDeclarationOrigin = FirDeclarationOrigin.Library,
        ): OptionalAnnotationClassesProvider? {
            if (!packagePartProvider.mayHaveOptionalAnnotationClasses()) return null

            return OptionalAnnotationClassesProvider(
                session,
                moduleDataProvider,
                kotlinScopeProvider,
                packagePartProvider,
                defaultDeserializationOrigin,
            )
        }
    }
}
