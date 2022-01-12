/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.session

import org.jetbrains.kotlin.descriptors.SourceFile
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.deserialization.*
import org.jetbrains.kotlin.fir.scopes.FirKotlinScopeProvider
import org.jetbrains.kotlin.library.metadata.KlibMetadataClassDataFinder
import org.jetbrains.kotlin.library.packageFqName
import org.jetbrains.kotlin.library.resolver.KotlinResolvedLibrary
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.deserialization.NameResolverImpl
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.serialization.deserialization.IncompatibleVersionErrorData
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerAbiStability
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource
import java.nio.file.Paths

class KlibBasedSymbolProvider(
    session: FirSession,
    moduleDataProvider: ModuleDataProvider,
    kotlinScopeProvider: FirKotlinScopeProvider,
    private val library: KotlinResolvedLibrary,
    defaultDeserializationOrigin: FirDeclarationOrigin = FirDeclarationOrigin.Library
) : AbstractFirDeserializedSymbolProvider(session, moduleDataProvider, kotlinScopeProvider, defaultDeserializationOrigin) {
    private val fragmentNameList by lazy {
        library.loadModuleHeader(library.library).packageFragmentNameList.toSet()
    }

    private val annotationDeserializer = KlibBasedAnnotationDeserializer(session)
    private val constDeserializer = FirConstDeserializer(session)

    override fun computePackagePartsInfos(packageFqName: FqName): List<PackagePartsCacheData> {
        val packageStringName = packageFqName.asString()

        if (packageStringName !in fragmentNameList) {
            return emptyList()
        }

        return library.library.packageMetadataParts(packageStringName).mapNotNull {
            val fragment = library.loadPackageFragment(library.library, packageStringName, it)

            val libraryPath = Paths.get(library.library.libraryName)
            val moduleData = moduleDataProvider.getModuleData(libraryPath) ?: return@mapNotNull null
            val packageProto = fragment.`package`

            val nameResolver = NameResolverImpl(
                fragment.strings,
                fragment.qualifiedNames,
            )

            PackagePartsCacheData(
                packageProto,
                FirDeserializationContext.createForPackage(
                    packageFqName, packageProto, nameResolver, moduleData,
                    annotationDeserializer,
                    constDeserializer,
                    null,
                ),
            )
        }
    }

    override fun extractClassMetadata(classId: ClassId, parentContext: FirDeserializationContext?): ClassMetadataFindResult? {
        val packageStringName = classId.packageFqName.asString()

        if (packageStringName !in fragmentNameList) {
            return null
        }

        library.library.packageMetadataParts(packageStringName).forEach {
            val libraryPath = Paths.get(library.library.libraryName)
            val fragment = library.loadPackageFragment(library.library, packageStringName, it)

            val nameResolver = NameResolverImpl(
                fragment.strings,
                fragment.qualifiedNames,
            )

            val finder = KlibMetadataClassDataFinder(fragment, nameResolver)

            val a = finder.findClassData(classId)?.classProto ?: return@forEach

            val source = object : DeserializedContainerSource {
                override val incompatibility: IncompatibleVersionErrorData<*>? = null
                override val isPreReleaseInvisible = false
                override val abiStability = DeserializedContainerAbiStability.STABLE
                override val presentableString = a.toString()

                override fun getContainingFile() = SourceFile.NO_SOURCE_FILE
            }

            return ClassMetadataFindResult.Metadata(
                nameResolver,
                a,
                annotationDeserializer,
                libraryPath,
                source,
                classPostProcessor = {}
            )
        }

        return null
    }

    override fun isNewPlaceForBodyGeneration(classProto: ProtoBuf.Class) = false

    override fun getPackage(fqName: FqName): FqName? {
        return if (fqName.toString() in fragmentNameList) {
            fqName
        } else {
            null
        }
    }
}
