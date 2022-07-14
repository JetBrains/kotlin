/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.session

import org.jetbrains.kotlin.descriptors.SourceFile
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.deserialization.*
import org.jetbrains.kotlin.fir.isNewPlaceForBodyGeneration
import org.jetbrains.kotlin.fir.languageVersionSettings
import org.jetbrains.kotlin.fir.scopes.FirKotlinScopeProvider
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.library.metadata.KlibMetadataClassDataFinder
import org.jetbrains.kotlin.library.resolver.KotlinResolvedLibrary
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.deserialization.NameResolverImpl
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.CompilerDeserializationConfiguration
import org.jetbrains.kotlin.serialization.deserialization.IncompatibleVersionErrorData
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerAbiStability
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource
import java.nio.file.Paths

class KlibBasedSymbolProvider(
    session: FirSession,
    moduleDataProvider: ModuleDataProvider,
    kotlinScopeProvider: FirKotlinScopeProvider,
    private val resolvedLibrary: KotlinResolvedLibrary,
    defaultDeserializationOrigin: FirDeclarationOrigin = FirDeclarationOrigin.Library
) : AbstractFirDeserializedSymbolProvider(session, moduleDataProvider, kotlinScopeProvider, defaultDeserializationOrigin) {
    private val moduleHeader by lazy {
        resolvedLibrary.loadModuleHeader(resolvedLibrary.library)
    }

    private val fragmentNameList by lazy {
        moduleHeader.packageFragmentNameList.toSet()
    }

    private val annotationDeserializer = KlibBasedAnnotationDeserializer(session)
    private val constDeserializer = FirConstDeserializer(session)
    private val deserializationConfiguration = CompilerDeserializationConfiguration(session.languageVersionSettings)
    private val cachedFragments = mutableMapOf<Pair<String, String>, ProtoBuf.PackageFragment>()

    private fun getPackageFragment(packageStringName: String, packageMetadataPart: String): ProtoBuf.PackageFragment {
        return cachedFragments.getOrPut(packageStringName to packageMetadataPart) {
            resolvedLibrary.loadPackageFragment(resolvedLibrary.library, packageStringName, packageMetadataPart)
        }
    }

    override fun computePackagePartsInfos(packageFqName: FqName): List<PackagePartsCacheData> {
        val packageStringName = packageFqName.asString()

        if (packageStringName !in fragmentNameList) {
            return emptyList()
        }

        return resolvedLibrary.library.packageMetadataParts(packageStringName).mapNotNull {
            val fragment = getPackageFragment(packageStringName, it)

            val libraryPath = Paths.get(resolvedLibrary.library.libraryName)
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

    @OptIn(SymbolInternals::class)
    override fun extractClassMetadata(classId: ClassId, parentContext: FirDeserializationContext?): ClassMetadataFindResult? {
        val packageStringName = classId.packageFqName.asString()

        if (packageStringName !in fragmentNameList) {
            return null
        }

        resolvedLibrary.library.packageMetadataParts(packageStringName).forEach {
            val libraryPath = Paths.get(resolvedLibrary.library.libraryName)
            val fragment = getPackageFragment(packageStringName, it)

            val nameResolver = NameResolverImpl(
                fragment.strings,
                fragment.qualifiedNames,
            )

            val finder = KlibMetadataClassDataFinder(fragment, nameResolver)
            val classProto = finder.findClassData(classId)?.classProto ?: return@forEach

            val moduleData = moduleDataProvider.getModuleData(libraryPath) ?: return null

            return ClassMetadataFindResult.NoMetadata { symbol ->
                val source = object : DeserializedContainerSource {
                    override val incompatibility: IncompatibleVersionErrorData<*>? = null
                    override val isPreReleaseInvisible =
                        deserializationConfiguration.reportErrorsOnPreReleaseDependencies && (moduleHeader.flags and 1) != 0
                    override val abiStability = DeserializedContainerAbiStability.STABLE
                    override val presentableString = "Package '${classId.packageFqName}'"

                    override fun getContainingFile() = SourceFile.NO_SOURCE_FILE
                }

                deserializeClassToSymbol(
                    classId,
                    classProto,
                    symbol,
                    nameResolver,
                    session,
                    moduleData,
                    annotationDeserializer,
                    kotlinScopeProvider,
                    parentContext,
                    source,
                    origin = defaultDeserializationOrigin,
                    deserializeNestedClass = this::getClass,
                )
                symbol.fir.isNewPlaceForBodyGeneration = isNewPlaceForBodyGeneration(classProto)
            }
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
