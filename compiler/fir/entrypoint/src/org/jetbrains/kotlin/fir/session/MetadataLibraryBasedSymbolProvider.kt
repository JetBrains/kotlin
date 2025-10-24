/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.session

import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.utils.klibSourceFile
import org.jetbrains.kotlin.fir.deserialization.*
import org.jetbrains.kotlin.fir.isNewPlaceForBodyGeneration
import org.jetbrains.kotlin.fir.languageVersionSettings
import org.jetbrains.kotlin.fir.scopes.FirKotlinScopeProvider
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.components.KlibMetadataComponent
import org.jetbrains.kotlin.library.metadata.*
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.deserialization.NameResolver
import org.jetbrains.kotlin.metadata.deserialization.NameResolverImpl
import org.jetbrains.kotlin.metadata.deserialization.getExtensionOrNull
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.protobuf.GeneratedMessageLite
import org.jetbrains.kotlin.protobuf.GeneratedMessageLite.GeneratedExtension
import org.jetbrains.kotlin.resolve.KlibCompilerDeserializationConfiguration
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource
import org.jetbrains.kotlin.serialization.deserialization.getClassId
import java.util.IdentityHashMap

abstract class MetadataLibraryBasedSymbolProvider<L>(
    session: FirSession,
    moduleDataProvider: ModuleDataProvider,
    kotlinScopeProvider: FirKotlinScopeProvider,
    private val flexibleTypeFactory: FirTypeDeserializer.FlexibleTypeFactory,
    defaultDeserializationOrigin: FirDeclarationOrigin = FirDeclarationOrigin.Library,
    protected val metadataProvider: (L) -> KlibMetadataComponent,
) : AbstractFirDeserializedSymbolProvider(
    session, moduleDataProvider, kotlinScopeProvider, defaultDeserializationOrigin, KlibMetadataSerializerProtocol
) {
    private class MetadataLibraryPackagePartCacheDataExtra(val library: KotlinLibrary) : PackagePartsCacheData.Extra

    protected abstract fun moduleData(library: L): FirModuleData?

    protected abstract val fragmentNamesInLibraries: Map<String, List<L>>

    protected abstract val knownPackagesInLibraries: Set<FqName>

    private val annotationDeserializer = KlibBasedAnnotationDeserializer(session)
    private val constDeserializer = FirConstDeserializer(KlibMetadataSerializerProtocol)
    protected val deserializationConfiguration: KlibCompilerDeserializationConfiguration =
        KlibCompilerDeserializationConfiguration(session.languageVersionSettings)
    private val cachedFragments: MutableMap<L, MutableMap<Pair<String, String>, ProtoBuf.PackageFragment>> = mutableMapOf()
    private val fragmentToNameResolver = IdentityHashMap<ProtoBuf.PackageFragment, NameResolver>()
    private val fragmentToKlibMetadataClassDataFinder = IdentityHashMap<ProtoBuf.PackageFragment, KlibMetadataClassDataFinder>()

    private fun getPackageFragment(
        resolvedLibrary: L, packageStringName: String, packageMetadataPart: String
    ): ProtoBuf.PackageFragment {
        return cachedFragments.getOrPut(resolvedLibrary) {
            mutableMapOf()
        }.getOrPut(packageStringName to packageMetadataPart) {
            parsePackageFragment(metadataProvider(resolvedLibrary).getPackageFragment(packageStringName, packageMetadataPart))
        }
    }

    private fun getNameResolver(fragment: ProtoBuf.PackageFragment): NameResolver {
        return fragmentToNameResolver.getOrPut(fragment) {
            NameResolverImpl(
                fragment.strings,
                fragment.qualifiedNames,
            )
        }
    }

    private fun getFinder(fragment: ProtoBuf.PackageFragment, resolver: NameResolver): KlibMetadataClassDataFinder {
        return fragmentToKlibMetadataClassDataFinder.getOrPut(fragment) {
            // Assumes the fact that the nameResolver depends only on the fragment.
            KlibMetadataClassDataFinder(fragment, resolver)
        }
    }

    override fun computePackagePartsInfos(packageFqName: FqName): List<PackagePartsCacheData> {
        val packageStringName = if (packageFqName.isRoot) "" else packageFqName.asString()

        val librariesWithFragment = fragmentNamesInLibraries[packageStringName] ?: return emptyList()

        return librariesWithFragment.flatMap { resolvedLibrary ->

            val moduleData = moduleData(resolvedLibrary) ?: return@flatMap emptyList()

            metadataProvider(resolvedLibrary).getPackageFragmentNames(packageStringName).map {
                val fragment = getPackageFragment(resolvedLibrary, packageStringName, it)

                val packageProto = fragment.`package`

                val nameResolver = getNameResolver(fragment)

                PackagePartsCacheData(
                    packageProto,
                    FirDeserializationContext.createForPackage(
                        packageFqName, packageProto, nameResolver, moduleData,
                        annotationDeserializer,
                        flexibleTypeFactory,
                        constDeserializer,
                        createDeserializedContainerSource(resolvedLibrary, packageFqName),
                    ),
                    (resolvedLibrary as? KotlinLibrary)?.let(::MetadataLibraryPackagePartCacheDataExtra)
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
            val finder = getFinder(fragment, nameResolver)
            val classProto = finder.findClassData(classId)?.classProto ?: return@forEachFragmentInPackage

            val moduleData = moduleData(resolvedLibrary) ?: return null

            return ClassMetadataFindResult.NoMetadata { symbol ->
                val source = createDeserializedContainerSource(
                    resolvedLibrary,
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
                    flexibleTypeFactory,
                    kotlinScopeProvider,
                    KlibMetadataSerializerProtocol,
                    parentContext,
                    source,
                    origin = defaultDeserializationOrigin,
                    deserializeNestedClass = this::getClass,
                    deserializeNestedTypeAlias = this::getTypeAlias,
                )

                if (resolvedLibrary is KotlinLibrary) {
                    symbol.fir.klibSourceFile = loadKlibSourceFileExtensionOrNull(
                        resolvedLibrary, nameResolver, classProto, KlibMetadataProtoBuf.classFile
                    )
                }

                symbol.fir.isNewPlaceForBodyGeneration = isNewPlaceForBodyGeneration(classProto)
            }
        }

        return null
    }

    private inline fun forEachFragmentInPackage(
        packageFqName: FqName,
        f: (L, ProtoBuf.PackageFragment, NameResolver) -> Unit
    ) {
        val packageStringName = packageFqName.asString()

        val librariesWithFragment = fragmentNamesInLibraries[packageStringName] ?: return

        for (resolvedLibrary in librariesWithFragment) {
            for (packageMetadataPart in metadataProvider(resolvedLibrary).getPackageFragmentNames(packageStringName)) {

                val fragment = getPackageFragment(resolvedLibrary, packageStringName, packageMetadataPart)

                val nameResolver = getNameResolver(fragment)

                f(resolvedLibrary, fragment, nameResolver)
            }
        }
    }

    override fun loadFunctionExtensions(packagePart: PackagePartsCacheData, proto: ProtoBuf.Function, fir: FirFunction) {
        fir.klibSourceFile = loadKlibSourceFileExtensionOrNull(packagePart, proto, KlibMetadataProtoBuf.functionFile) ?: return
    }

    override fun loadPropertyExtensions(packagePart: PackagePartsCacheData, proto: ProtoBuf.Property, fir: FirProperty) {
        fir.klibSourceFile = loadKlibSourceFileExtensionOrNull(packagePart, proto, KlibMetadataProtoBuf.propertyFile) ?: return
    }

    private fun <T : GeneratedMessageLite.ExtendableMessage<T>> loadKlibSourceFileExtensionOrNull(
        packagePart: PackagePartsCacheData, proto: T, sourceFileExtension: GeneratedExtension<T, Int>,
    ): DeserializedSourceFile? {
        val library = (packagePart.extra as? MetadataLibraryPackagePartCacheDataExtra)?.library ?: return null
        return loadKlibSourceFileExtensionOrNull(library, packagePart.context.nameResolver, proto, sourceFileExtension)
    }

    private fun <T : GeneratedMessageLite.ExtendableMessage<T>> loadKlibSourceFileExtensionOrNull(
        library: KotlinLibrary, nameResolver: NameResolver, proto: T, sourceFileExtension: GeneratedExtension<T, Int>,
    ): DeserializedSourceFile? {
        return proto.getExtensionOrNull(sourceFileExtension)
            ?.let { fileId -> nameResolver.getString(fileId) }
            ?.let { fileName -> DeserializedSourceFile(fileName, library) }
    }


    protected abstract fun createDeserializedContainerSource(
        resolvedLibrary: L,
        packageFqName: FqName
    ): DeserializedContainerSource?

    override fun isNewPlaceForBodyGeneration(classProto: ProtoBuf.Class): Boolean = false

    override fun hasPackage(fqName: FqName): Boolean {
        return fqName in knownPackagesInLibraries
    }
}
