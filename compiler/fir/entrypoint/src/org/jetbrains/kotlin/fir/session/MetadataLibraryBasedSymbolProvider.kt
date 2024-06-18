/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
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
import org.jetbrains.kotlin.library.MetadataLibrary
import org.jetbrains.kotlin.library.metadata.*
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.deserialization.NameResolver
import org.jetbrains.kotlin.metadata.deserialization.NameResolverImpl
import org.jetbrains.kotlin.metadata.deserialization.getExtensionOrNull
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.protobuf.GeneratedMessageLite
import org.jetbrains.kotlin.protobuf.GeneratedMessageLite.GeneratedExtension
import org.jetbrains.kotlin.resolve.CompilerDeserializationConfiguration
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource
import org.jetbrains.kotlin.serialization.deserialization.getClassId

abstract class MetadataLibraryBasedSymbolProvider<L : MetadataLibrary>(
    session: FirSession,
    moduleDataProvider: ModuleDataProvider,
    kotlinScopeProvider: FirKotlinScopeProvider,
    defaultDeserializationOrigin: FirDeclarationOrigin = FirDeclarationOrigin.Library,
) : AbstractFirDeserializedSymbolProvider(
    session, moduleDataProvider, kotlinScopeProvider, defaultDeserializationOrigin, KlibMetadataSerializerProtocol
) {
    private class MetadataLibraryPackagePartCacheDataExtra(val library: MetadataLibrary) : PackagePartsCacheData.Extra

    protected abstract fun moduleData(library: L): FirModuleData?

    protected abstract val fragmentNamesInLibraries: Map<String, List<L>>

    protected abstract val knownPackagesInLibraries: Set<FqName>

    private val annotationDeserializer = KlibBasedAnnotationDeserializer(session)
    private val constDeserializer = FirConstDeserializer(KlibMetadataSerializerProtocol)
    protected val deserializationConfiguration: CompilerDeserializationConfiguration =
        CompilerDeserializationConfiguration(session.languageVersionSettings)
    private val cachedFragments: MutableMap<L, MutableMap<Pair<String, String>, ProtoBuf.PackageFragment>> = mutableMapOf()

    private fun getPackageFragment(
        resolvedLibrary: L, packageStringName: String, packageMetadataPart: String
    ): ProtoBuf.PackageFragment {
        return cachedFragments.getOrPut(resolvedLibrary) {
            mutableMapOf()
        }.getOrPut(packageStringName to packageMetadataPart) {
            parsePackageFragment(resolvedLibrary.packageMetadata(packageStringName, packageMetadataPart))
        }
    }

    override fun computePackagePartsInfos(packageFqName: FqName): List<PackagePartsCacheData> {
        val packageStringName = if (packageFqName.isRoot) "" else packageFqName.asString()

        val librariesWithFragment = fragmentNamesInLibraries[packageStringName] ?: return emptyList()

        return librariesWithFragment.flatMap { resolvedLibrary ->
            resolvedLibrary.packageMetadataParts(packageStringName).mapNotNull {
                val fragment = getPackageFragment(resolvedLibrary, packageStringName, it)

                val moduleData = moduleData(resolvedLibrary) ?: return@mapNotNull null
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
                        FirTypeDeserializer.FlexibleTypeFactory.Default,
                        constDeserializer,
                        createDeserializedContainerSource(resolvedLibrary, packageFqName),
                        deserializeAsActual = false,
                    ),
                    MetadataLibraryPackagePartCacheDataExtra(resolvedLibrary)
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
                    FirTypeDeserializer.FlexibleTypeFactory.Default,
                    kotlinScopeProvider,
                    KlibMetadataSerializerProtocol,
                    parentContext,
                    source,
                    origin = defaultDeserializationOrigin,
                    deserializeNestedClass = this::getClass,
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
            for (packageMetadataPart in resolvedLibrary.packageMetadataParts(packageStringName)) {

                val fragment = getPackageFragment(resolvedLibrary, packageStringName, packageMetadataPart)

                val nameResolver = NameResolverImpl(
                    fragment.strings,
                    fragment.qualifiedNames,
                )

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
        val library = (packagePart.extra as? MetadataLibraryPackagePartCacheDataExtra)?.library as? KotlinLibrary ?: return null
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

    override fun getPackage(fqName: FqName): FqName? {
        return if (fqName in knownPackagesInLibraries) {
            fqName
        } else {
            null
        }
    }
}
