/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.session

import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.declarations.FirProperty
import org.jetbrains.kotlin.fir.declarations.FirTypeAlias
import org.jetbrains.kotlin.fir.declarations.utils.klibFileAnnotations
import org.jetbrains.kotlin.fir.declarations.utils.klibSourceFile
import org.jetbrains.kotlin.fir.deserialization.*
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
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
import org.jetbrains.kotlin.resolve.CommonCompilerDeserializationConfiguration
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource
import org.jetbrains.kotlin.serialization.deserialization.getClassId
import java.lang.ref.SoftReference

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

    private inner class CachedPackageFragment(
        val proto: ProtoBuf.PackageFragment
    ) {
        val nameResolver: NameResolver
            get() = NameResolverImpl(proto.strings, proto.qualifiedNames)

        val classDataFinder by lazy {
            // Assumes the fact that the nameResolver depends only on the fragment.
            KlibMetadataClassDataFinder(proto, nameResolver)
        }

        val fileAnnotations: List<FirAnnotation> by lazy {
            loadAnnotationsFromMetadata(session, proto.fileAnnotationList, nameResolver, AnnotationUseSiteTarget.FILE)
        }
    }

    protected abstract fun moduleData(library: L): FirModuleData?

    protected abstract val fragmentNamesInLibraries: Map<String, List<L>>

    protected abstract val knownPackagesInLibraries: Set<FqName>

    private val annotationDeserializer = KlibBasedAnnotationDeserializer(session)
    private val constDeserializer = FirConstDeserializer(KlibMetadataSerializerProtocol)
    protected val deserializationConfiguration: CommonCompilerDeserializationConfiguration =
        CommonCompilerDeserializationConfiguration(session.languageVersionSettings)

    private val cachedFragments: MutableMap<L, MutableMap<Pair<String, String>, SoftReference<CachedPackageFragment>>> = mutableMapOf()

    private fun getPackageFragment(
        resolvedLibrary: L, packageStringName: String, packageMetadataPart: String
    ): CachedPackageFragment {
        val slice = cachedFragments.getOrPut(resolvedLibrary) { mutableMapOf() }
        val key = packageStringName to packageMetadataPart

        return slice[key]?.get() ?: run {
            val packageFragment = CachedPackageFragment(
                parsePackageFragment(
                    metadataProvider(resolvedLibrary).getPackageFragment(
                        packageStringName,
                        packageMetadataPart
                    )
                )
            )
            slice[key] = SoftReference(packageFragment)
            packageFragment
        }
    }

    override fun computePackagePartsInfos(packageFqName: FqName): List<PackagePartsCacheData> {
        val packageStringName = if (packageFqName.isRoot) "" else packageFqName.asString()

        val librariesWithFragment = fragmentNamesInLibraries[packageStringName] ?: return emptyList()

        return librariesWithFragment.flatMap { resolvedLibrary ->

            val moduleData = moduleData(resolvedLibrary) ?: return@flatMap emptyList()

            metadataProvider(resolvedLibrary).getPackageFragmentNames(packageStringName).map {
                val fragment = getPackageFragment(resolvedLibrary, packageStringName, it)

                PackagePartsCacheData(
                    fragment.proto.`package`,
                    FirDeserializationContext.createForPackage(
                        packageFqName, fragment.proto.`package`, fragment.nameResolver, moduleData,
                        annotationDeserializer,
                        flexibleTypeFactory,
                        constDeserializer,
                        kdocDeserializer,
                        createDeserializedContainerSource(resolvedLibrary, packageFqName),
                    ),
                    (resolvedLibrary as? KotlinLibrary)?.let(::MetadataLibraryPackagePartCacheDataExtra),
                    fileAnnotations = fragment.fileAnnotations,
                )
            }
        }
    }

    override fun computePackageSetWithNonClassDeclarations(): Set<String> = fragmentNamesInLibraries.keys

    override fun knownTopLevelClassesInPackage(packageFqName: FqName): Set<String> =
        buildSet {
            forEachFragmentInPackage(packageFqName) { _, fragment ->
                for (classNameId in fragment.proto.getExtension(KlibMetadataProtoBuf.className).orEmpty()) {
                    add(fragment.nameResolver.getClassId(classNameId).shortClassName.asString())
                }
            }
        }

    @OptIn(SymbolInternals::class)
    override fun extractClassMetadata(classId: ClassId, parentContext: FirDeserializationContext?): ClassMetadataFindResult? {
        forEachFragmentInPackage(classId.packageFqName) { resolvedLibrary, fragment ->
            val finder = fragment.classDataFinder
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
                    fragment.nameResolver,
                    session,
                    moduleData,
                    annotationDeserializer,
                    kdocDeserializer,
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
                        resolvedLibrary, fragment.nameResolver, classProto, KlibMetadataProtoBuf.classFile
                    )
                }

                symbol.fir.isNewPlaceForBodyGeneration = isNewPlaceForBodyGeneration(classProto)

                val fileAnnotations = fragment.fileAnnotations
                if (fileAnnotations.isNotEmpty()) {
                    symbol.fir.klibFileAnnotations = fileAnnotations
                }
            }
        }

        return null
    }

    private inline fun forEachFragmentInPackage(
        packageFqName: FqName,
        f: (L, CachedPackageFragment) -> Unit
    ) {
        val packageStringName = packageFqName.asString()

        val librariesWithFragment = fragmentNamesInLibraries[packageStringName] ?: return

        for (resolvedLibrary in librariesWithFragment) {
            for (packageMetadataPart in metadataProvider(resolvedLibrary).getPackageFragmentNames(packageStringName)) {

                val fragment = getPackageFragment(resolvedLibrary, packageStringName, packageMetadataPart)

                f(resolvedLibrary, fragment)
            }
        }
    }

    override fun loadFunctionExtensions(packagePart: PackagePartsCacheData, proto: ProtoBuf.Function, fir: FirFunction) {
        fir.klibSourceFile = loadKlibSourceFileExtensionOrNull(packagePart, proto, KlibMetadataProtoBuf.functionFile) ?: return
    }

    override fun loadTypeAliasExtensions(
        packagePart: PackagePartsCacheData, proto: ProtoBuf.TypeAlias, fir: FirTypeAlias,
    ) {
        fir.klibSourceFile = loadKlibSourceFileExtensionOrNull(packagePart, proto, KlibMetadataProtoBuf.typeAliasFile) ?: return
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
