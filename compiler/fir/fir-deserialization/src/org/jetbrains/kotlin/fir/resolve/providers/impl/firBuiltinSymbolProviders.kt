/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.providers.impl

import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.NoMutableState
import org.jetbrains.kotlin.fir.ThreadSafeMutableState
import org.jetbrains.kotlin.fir.caches.*
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.deserialization.*
import org.jetbrains.kotlin.fir.resolve.providers.FirCompositeSymbolNamesProvider
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolNamesProvider
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProviderInternals
import org.jetbrains.kotlin.fir.scopes.FirKotlinScopeProvider
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.builtins.BuiltInsBinaryVersion
import org.jetbrains.kotlin.metadata.deserialization.NameResolver
import org.jetbrains.kotlin.metadata.deserialization.NameResolverImpl
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.serialization.deserialization.ProtoBasedClassDataFinder
import org.jetbrains.kotlin.serialization.deserialization.builtins.BuiltInSerializerProtocol
import org.jetbrains.kotlin.serialization.deserialization.getName
import org.jetbrains.kotlin.utils.mapToSetOrEmpty
import java.io.InputStream

@ThreadSafeMutableState
abstract class AbstractFirBuiltinSymbolProvider(
    session: FirSession,
    val moduleData: FirModuleData,
    val kotlinScopeProvider: FirKotlinScopeProvider,
    private val isFallback: Boolean,
) : FirSymbolProvider(session) {

    protected abstract val builtInsPackageFragments: Map<FqName, BuiltInsPackageFragment>

    fun getTopLevelClassifierNamesInPackage(packageFqName: FqName): Set<Name> =
        getTopLevelClassifierNamesInPackage(builtInsPackageFragments, packageFqName)

    private val allPackageFragments by lazy {
        builtInsPackageFragments.mapValues { (fqName, foo) ->
            BuiltInsPackageFragmentWrapper(foo, fqName, moduleData, kotlinScopeProvider, isFallback)
        }
    }

    override fun getPackage(fqName: FqName): FqName? {
        if (allPackageFragments.containsKey(fqName)) return fqName
        return null
    }

    override fun getClassLikeSymbolByClassId(classId: ClassId): FirRegularClassSymbol? {
        return allPackageFragments[classId.packageFqName]?.getClassLikeSymbolByClassId(classId)
    }

    override val symbolNamesProvider: FirSymbolNamesProvider = object : FirSymbolNamesProvider() {
        override fun getPackageNames(): Set<String> = allPackageFragments.keys.mapToSetOrEmpty(FqName::asString)

        override val hasSpecificClassifierPackageNamesComputation: Boolean get() = false
        override val hasSpecificCallablePackageNamesComputation: Boolean get() = false

        override fun getTopLevelClassifierNamesInPackage(packageFqName: FqName): Set<Name> =
            getTopLevelClassifierNamesInPackage(builtInsPackageFragments, packageFqName)

        override fun getTopLevelCallableNamesInPackage(packageFqName: FqName): Set<Name> =
            allPackageFragments[packageFqName]?.getTopLevelCallableNames()?.toSet().orEmpty()

        // This symbol provider delegates to `FirBuiltinSyntheticFunctionInterfaceProvider`, so synthetic function types can be provided.
        override val mayHaveSyntheticFunctionTypes: Boolean get() = true
    }

    @FirSymbolProviderInternals
    override fun getTopLevelCallableSymbolsTo(destination: MutableList<FirCallableSymbol<*>>, packageFqName: FqName, name: Name) {
        allPackageFragments[packageFqName]?.getTopLevelCallableSymbols(name)?.let { destination += it }
    }

    @FirSymbolProviderInternals
    override fun getTopLevelFunctionSymbolsTo(destination: MutableList<FirNamedFunctionSymbol>, packageFqName: FqName, name: Name) {
        getTopLevelFunctionSymbolsToByPackageFragments(destination, packageFqName, name)
    }

    private fun getTopLevelFunctionSymbolsToByPackageFragments(
        destination: MutableList<FirNamedFunctionSymbol>,
        packageFqName: FqName,
        name: Name,
    ) {
        allPackageFragments[packageFqName]?.getTopLevelFunctionSymbols(name)?.let { destination += it }
    }

    @FirSymbolProviderInternals
    override fun getTopLevelPropertySymbolsTo(destination: MutableList<FirPropertySymbol>, packageFqName: FqName, name: Name) {
    }

    class BuiltInsPackageFragment(stream: InputStream) {
        private val binaryVersionAndPackageFragment = BinaryVersionAndPackageFragment.createFromStream(stream)

        val version: BuiltInsBinaryVersion get() = binaryVersionAndPackageFragment.version
        val packageProto: ProtoBuf.PackageFragment get() = binaryVersionAndPackageFragment.packageFragment

        val nameResolver: NameResolver = NameResolverImpl(packageProto.strings, packageProto.qualifiedNames)
        val classDataFinder: ProtoBasedClassDataFinder =
            ProtoBasedClassDataFinder(packageProto, nameResolver, version) { SourceElement.NO_SOURCE }
    }

    private class BuiltInsPackageFragmentWrapper(
        private val builtInsPackageFragment: BuiltInsPackageFragment,
        val fqName: FqName,
        val moduleData: FirModuleData,
        val kotlinScopeProvider: FirKotlinScopeProvider,
        private val originateFromFallbackBuiltIns: Boolean,
    ) {

        private val packageProto get() = builtInsPackageFragment.packageProto
        private val nameResolver get() = builtInsPackageFragment.nameResolver
        val classDataFinder get() = builtInsPackageFragment.classDataFinder

        private val memberDeserializer by lazy {
            FirDeserializationContext.createForPackage(
                fqName, packageProto.`package`, nameResolver, moduleData,
                FirBuiltinAnnotationDeserializer(moduleData.session),
                FirTypeDeserializer.FlexibleTypeFactory.Default,
                FirConstDeserializer(BuiltInSerializerProtocol),
                containerSource = null
            ).memberDeserializer
        }

        private val classCache = moduleData.session.firCachesFactory.createCacheWithPostCompute(
            { classId: ClassId, context: FirDeserializationContext? -> FirRegularClassSymbol(classId) to context },
            { classId, symbol, parentContext ->
                val classData = classDataFinder.findClassData(classId)!!
                val classProto = classData.classProto

                deserializeClassToSymbol(
                    classId, classProto, symbol, nameResolver, moduleData.session, moduleData,
                    null, FirTypeDeserializer.FlexibleTypeFactory.Default,
                    kotlinScopeProvider, BuiltInSerializerProtocol, parentContext,
                    null,
                    origin = if (originateFromFallbackBuiltIns) FirDeclarationOrigin.BuiltInsFallback else FirDeclarationOrigin.BuiltIns,
                    this::findAndDeserializeClass,
                )
            }
        )

        private val functionCache: FirCache<Name, List<FirNamedFunctionSymbol>, Nothing?> =
            moduleData.session.firCachesFactory.createCache { name ->
                packageProto.`package`.functionList.filter { nameResolver.getName(it.name) == name }.map {
                    memberDeserializer.loadFunction(it).symbol
                }
            }

        private val functionsNameCache: FirLazyValue<List<Name>> = moduleData.session.firCachesFactory.createLazyValue {
            packageProto.`package`.functionList.map { nameResolver.getName(it.name) }
        }

        fun getClassLikeSymbolByClassId(classId: ClassId): FirRegularClassSymbol? =
            findAndDeserializeClass(classId)

        private fun findAndDeserializeClass(
            classId: ClassId,
            parentContext: FirDeserializationContext? = null,
        ): FirRegularClassSymbol? {
            val classIdExists = classId in classDataFinder.allClassIds
            if (!classIdExists) return null
            return classCache.getValue(classId, parentContext)
        }

        fun getTopLevelCallableSymbols(name: Name): List<FirCallableSymbol<*>> {
            return getTopLevelFunctionSymbols(name)
        }

        fun getTopLevelCallableNames(): Collection<Name> {
            return functionsNameCache.getValue()
        }

        fun getTopLevelFunctionSymbols(name: Name): List<FirNamedFunctionSymbol> {
            return functionCache.getValue(name)
        }
    }
}

/**
 * A fallback built-in provider ensures that builtin classes will be loaded regardless of stdlib presence in the classpath
 */
@ThreadSafeMutableState
class FirFallbackBuiltinSymbolProvider(
    session: FirSession,
    moduleData: FirModuleData,
    kotlinScopeProvider: FirKotlinScopeProvider,
) : AbstractFirBuiltinSymbolProvider(session, moduleData, kotlinScopeProvider, true) {

    override val builtInsPackageFragments: Map<FqName, BuiltInsPackageFragment>
        get() = FirFallbackBuiltinSymbolProvider.builtInsPackageFragments

    companion object {
        val builtInsPackageFragments: Map<FqName, BuiltInsPackageFragment> = run {
            val classLoader = FirFallbackBuiltinSymbolProvider::class.java.classLoader
            val streamProvider = { path: String -> classLoader?.getResourceAsStream(path) ?: ClassLoader.getSystemResourceAsStream(path) }
            val packageFqNames = StandardClassIds.builtInsPackages

            packageFqNames.associateWith { fqName ->
                val resourcePath = BuiltInSerializerProtocol.getBuiltInsFilePath(fqName)
                val inputStream =
                    streamProvider(resourcePath) ?: throw IllegalStateException("Resource not found in classpath: $resourcePath")
                BuiltInsPackageFragment(inputStream)
            }
        }
    }
}

/**
 * Uses classes defined in classpath to load builtins
 */
@ThreadSafeMutableState
class FirClasspathBuiltinSymbolProvider(
    session: FirSession,
    moduleData: FirModuleData,
    kotlinScopeProvider: FirKotlinScopeProvider,
    val findPackagePartData: (FqName) -> InputStream?,
) : AbstractFirBuiltinSymbolProvider(session, moduleData, kotlinScopeProvider, false) {

    override val builtInsPackageFragments: Map<FqName, BuiltInsPackageFragment> = buildMap {
        StandardClassIds.builtInsPackages.forEach { fqName ->
            val inputStream = findPackagePartData(fqName) ?: return@forEach
            put(fqName, BuiltInsPackageFragment(inputStream))
        }
    }
}

private data class BinaryVersionAndPackageFragment(
    val version: BuiltInsBinaryVersion,
    val packageFragment: ProtoBuf.PackageFragment,
) {
    companion object {
        fun createFromStream(stream: InputStream): BinaryVersionAndPackageFragment {
            val version = BuiltInsBinaryVersion.readFrom(stream)

            if (!version.isCompatibleWithCurrentCompilerVersion()) {
                // TODO: report a proper diagnostic
                throw UnsupportedOperationException(
                    "Kotlin built-in definition format version is not supported: " +
                            "expected ${BuiltInsBinaryVersion.INSTANCE}, actual $version. " +
                            "Please update Kotlin",
                )
            }

            val packageFragment = ProtoBuf.PackageFragment.parseFrom(stream, BuiltInSerializerProtocol.extensionRegistry)
            return BinaryVersionAndPackageFragment(version, packageFragment)
        }
    }
}

fun getTopLevelClassifierNamesInPackage(
    builtInsPackageFragments: Map<FqName, AbstractFirBuiltinSymbolProvider.BuiltInsPackageFragment>,
    packageFqName: FqName,
): Set<Name> {
    return builtInsPackageFragments[packageFqName]?.classDataFinder?.allClassIds?.mapTo(mutableSetOf()) { it.shortClassName }.orEmpty()
}

/**
 * This provider allows to get symbols for so-called built-in classes.
 *
 * Built-in classes are the classes which are considered mandatory for compiling any Kotlin code.
 * For this reason these classes must be provided even in a case when no standard library exists in classpath.
 * One can find a set of these classes inside core/builtins directory (look at both src/kotlin and native/kotlin subdirectories).
 * In particular: all primitives, all arrays, collection-like interfaces, Any, Nothing, Unit, etc.
 *
 * For non-JVM platforms, all / almost all built-in classes exist also in the standard library, so this provider works as a fallback.
 * For the JVM platform, some classes are mapped to Java classes and do not exist themselves,
 * so this provider is mandatory for the JVM compiler to work properly even with the standard library in classpath.
 *
 */

@NoMutableState
class FirBuiltinsSymbolProvider(
    session: FirSession,
    private val classpathBuiltinSymbolProvider: FirClasspathBuiltinSymbolProvider,
    private val fallbackBuiltinSymbolProvider: FirFallbackBuiltinSymbolProvider,
) : FirSymbolProvider(session) {
    override val symbolNamesProvider: FirSymbolNamesProvider
        get() = FirCompositeSymbolNamesProvider.fromSymbolProviders(listOf(classpathBuiltinSymbolProvider, fallbackBuiltinSymbolProvider))

    override fun getClassLikeSymbolByClassId(classId: ClassId): FirRegularClassSymbol? {
        return classpathBuiltinSymbolProvider.getClassLikeSymbolByClassId(classId)
            ?: fallbackBuiltinSymbolProvider.getClassLikeSymbolByClassId(classId)
    }

    @FirSymbolProviderInternals
    override fun getTopLevelCallableSymbolsTo(destination: MutableList<FirCallableSymbol<*>>, packageFqName: FqName, name: Name) {
        val initSize = destination.size
        classpathBuiltinSymbolProvider.getTopLevelCallableSymbolsTo(destination, packageFqName, name)
        if (initSize == destination.size) {
            fallbackBuiltinSymbolProvider.getTopLevelCallableSymbolsTo(destination, packageFqName, name)
        }
    }

    @FirSymbolProviderInternals
    override fun getTopLevelFunctionSymbolsTo(destination: MutableList<FirNamedFunctionSymbol>, packageFqName: FqName, name: Name) {
        val initSize = destination.size
        classpathBuiltinSymbolProvider.getTopLevelFunctionSymbolsTo(destination, packageFqName, name)
        if (initSize == destination.size) {
            fallbackBuiltinSymbolProvider.getTopLevelFunctionSymbolsTo(destination, packageFqName, name)
        }
    }

    @FirSymbolProviderInternals
    override fun getTopLevelPropertySymbolsTo(destination: MutableList<FirPropertySymbol>, packageFqName: FqName, name: Name) {
        val initSize = destination.size
        classpathBuiltinSymbolProvider.getTopLevelPropertySymbolsTo(destination, packageFqName, name)
        if (initSize == destination.size) {
            fallbackBuiltinSymbolProvider.getTopLevelPropertySymbolsTo(destination, packageFqName, name)
        }
    }

    override fun getPackage(fqName: FqName): FqName? {
        return classpathBuiltinSymbolProvider.getPackage(fqName) ?: fallbackBuiltinSymbolProvider.getPackage(fqName)
    }

}