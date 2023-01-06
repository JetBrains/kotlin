/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.session

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.caches.FirCache
import org.jetbrains.kotlin.fir.caches.firCachesFactory
import org.jetbrains.kotlin.fir.caches.getValue
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.deserialization.*
import org.jetbrains.kotlin.fir.isNewPlaceForBodyGeneration
import org.jetbrains.kotlin.fir.languageVersionSettings
import org.jetbrains.kotlin.fir.scopes.FirKotlinScopeProvider
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.library.metadata.KlibDeserializedContainerSource
import org.jetbrains.kotlin.library.metadata.KlibMetadataClassDataFinder
import org.jetbrains.kotlin.library.metadata.KlibMetadataProtoBuf
import org.jetbrains.kotlin.library.metadata.KlibMetadataSerializerProtocol
import org.jetbrains.kotlin.library.metadata.resolver.KotlinResolvedLibrary
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.deserialization.NameResolver
import org.jetbrains.kotlin.metadata.deserialization.NameResolverImpl
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.CompilerDeserializationConfiguration
import org.jetbrains.kotlin.serialization.deserialization.getClassId
import org.jetbrains.kotlin.utils.SmartList
import java.nio.file.Paths

class KlibBasedSymbolProvider(
    session: FirSession,
    moduleDataProvider: ModuleDataProvider,
    kotlinScopeProvider: FirKotlinScopeProvider,
    private val resolvedLibraries: Collection<KotlinResolvedLibrary>,
    defaultDeserializationOrigin: FirDeclarationOrigin = FirDeclarationOrigin.Library
) : AbstractFirDeserializedSymbolProvider(
    session, moduleDataProvider, kotlinScopeProvider, defaultDeserializationOrigin, KlibMetadataSerializerProtocol
) {
    private val moduleHeaders by lazy {
        resolvedLibraries.associate { it to it.loadModuleHeader(it.library) }
    }

    private val fragmentNamesInLibraries: Map<String, List<KotlinResolvedLibrary>> by lazy {
        buildMap<String, SmartList<KotlinResolvedLibrary>> {
            for ((library, header) in moduleHeaders) {
                for (fragmentName in header.packageFragmentNameList) {
                    getOrPut(fragmentName) { SmartList() }
                        .add(library)
                }
            }
        }
    }

    private val annotationDeserializer = KlibBasedAnnotationDeserializer(session)
    private val constDeserializer = FirConstDeserializer(session, KlibMetadataSerializerProtocol)
    private val deserializationConfiguration = CompilerDeserializationConfiguration(session.languageVersionSettings)
    private val cachedFragments = mutableMapOf<KotlinResolvedLibrary, MutableMap<Pair<String, String>, ProtoBuf.PackageFragment>>()

    private fun getPackageFragment(
        resolvedLibrary: KotlinResolvedLibrary, packageStringName: String, packageMetadataPart: String
    ): ProtoBuf.PackageFragment {
        return cachedFragments.getOrPut(resolvedLibrary) {
            mutableMapOf()
        }.getOrPut(packageStringName to packageMetadataPart) {
            resolvedLibrary.loadPackageFragment(resolvedLibrary.library, packageStringName, packageMetadataPart)
        }
    }

    override fun computePackagePartsInfos(packageFqName: FqName): List<PackagePartsCacheData> {
        val packageStringName = if (packageFqName.isRoot) "" else packageFqName.asString()

        val librariesWithFragment = fragmentNamesInLibraries[packageStringName] ?: return emptyList()

        return librariesWithFragment.flatMap { resolvedLibrary ->
            resolvedLibrary.library.packageMetadataParts(packageStringName).mapNotNull {
                val fragment = getPackageFragment(resolvedLibrary, packageStringName, it)

                val libraryPath = Paths.get(resolvedLibrary.library.libraryFile.path)
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
                        createDeserializedContainerSource(resolvedLibrary, packageFqName),
                    ),
                )
            }
        }
    }

    override fun computePackageSetWithNonClassDeclarations(): Set<String> = fragmentNamesInLibraries.keys

    override fun knownTopLevelClassesInPackage(packageFqName: FqName): Set<String> =
        buildSet {
            forEachFragmentInPackage(packageFqName) { _, fragment, nameResolver ->
                for (classNameId in fragment.getExtension(KlibMetadataProtoBuf.className).orEmpty()) {
                    add(nameResolver.getClassId(classNameId).shortClassName.asString())
                }
            }
        }

    @OptIn(SymbolInternals::class)
    override fun extractClassMetadata(classId: ClassId, parentContext: FirDeserializationContext?): ClassMetadataFindResult? {
        forEachFragmentInPackage(classId.packageFqName) { resolvedLibrary, fragment, nameResolver ->
            val finder = KlibMetadataClassDataFinder(fragment, nameResolver)
            val classProto = finder.findClassData(classId)?.classProto ?: return@forEachFragmentInPackage

            val libraryPath = Paths.get(resolvedLibrary.library.libraryFile.path)
            val moduleData = moduleDataProvider.getModuleData(libraryPath) ?: return null

            return ClassMetadataFindResult.NoMetadata { symbol ->
                val source = createDeserializedContainerSource(resolvedLibrary,

                    classId.packageFqName
                )

                deserializeClassToSymbol(
                    classId,
                    classProto,
                    symbol,
                    nameResolver,
                    session,
                    moduleData,
                    annotationDeserializer,
                    kotlinScopeProvider,
                    KlibMetadataSerializerProtocol,
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

    private inline fun forEachFragmentInPackage(
        packageFqName: FqName,
        f: (KotlinResolvedLibrary, ProtoBuf.PackageFragment, NameResolver) -> Unit
    ) {
        val packageStringName = packageFqName.asString()

        val librariesWithFragment = fragmentNamesInLibraries[packageStringName] ?: return

        for (resolvedLibrary in librariesWithFragment) {
            for (packageMetadataPart in resolvedLibrary.library.packageMetadataParts(packageStringName)) {

                val fragment = getPackageFragment(resolvedLibrary, packageStringName, packageMetadataPart)

                val nameResolver = NameResolverImpl(
                    fragment.strings,
                    fragment.qualifiedNames,
                )

                f(resolvedLibrary, fragment, nameResolver)
            }
        }
    }

    private fun createDeserializedContainerSource(
        resolvedLibrary: KotlinResolvedLibrary,
        packageFqName: FqName
    ) = KlibDeserializedContainerSource(
        resolvedLibrary.library,
        moduleHeaders[resolvedLibrary]!!,
        deserializationConfiguration,
        packageFqName
    )

    override fun isNewPlaceForBodyGeneration(classProto: ProtoBuf.Class) = false

    override fun getPackage(fqName: FqName): FqName? {
        return if (fqName.toString() in fragmentNamesInLibraries) {
            fqName
        } else {
            null
        }
    }
}
