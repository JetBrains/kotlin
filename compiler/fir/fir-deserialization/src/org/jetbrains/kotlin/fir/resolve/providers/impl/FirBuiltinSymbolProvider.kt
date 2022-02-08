/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.providers.impl

import org.jetbrains.kotlin.builtins.functions.FunctionClassKind
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.ThreadSafeMutableState
import org.jetbrains.kotlin.fir.caches.createCache
import org.jetbrains.kotlin.fir.caches.firCachesFactory
import org.jetbrains.kotlin.fir.caches.getValue
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.builder.buildRegularClass
import org.jetbrains.kotlin.fir.declarations.builder.buildSimpleFunction
import org.jetbrains.kotlin.fir.declarations.builder.buildTypeParameter
import org.jetbrains.kotlin.fir.declarations.builder.buildValueParameter
import org.jetbrains.kotlin.fir.declarations.impl.FirResolvedDeclarationStatusImpl
import org.jetbrains.kotlin.fir.declarations.utils.addDeclaration
import org.jetbrains.kotlin.fir.deserialization.FirBuiltinAnnotationDeserializer
import org.jetbrains.kotlin.fir.deserialization.FirConstDeserializer
import org.jetbrains.kotlin.fir.deserialization.FirDeserializationContext
import org.jetbrains.kotlin.fir.deserialization.deserializeClassToSymbol
import org.jetbrains.kotlin.fir.resolve.defaultType
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.FirSymbolProviderInternals
import org.jetbrains.kotlin.fir.scopes.FirKotlinScopeProvider
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.fir.types.constructClassType
import org.jetbrains.kotlin.fir.types.impl.ConeTypeParameterTypeImpl
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.builtins.BuiltInsBinaryVersion
import org.jetbrains.kotlin.metadata.deserialization.NameResolverImpl
import org.jetbrains.kotlin.name.*
import org.jetbrains.kotlin.serialization.deserialization.ProtoBasedClassDataFinder
import org.jetbrains.kotlin.serialization.deserialization.builtins.BuiltInSerializerProtocol
import org.jetbrains.kotlin.serialization.deserialization.getName
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.util.OperatorNameConventions
import java.io.InputStream

@ThreadSafeMutableState
open class FirBuiltinSymbolProvider(
    session: FirSession,
    val moduleData: FirModuleData,
    val kotlinScopeProvider: FirKotlinScopeProvider
) : FirSymbolProvider(session) {
    private val allPackageFragments = loadBuiltIns().groupBy { it.fqName }
    private val syntheticFunctionalInterfaceCache = SyntheticFunctionalInterfaceCache(moduleData, kotlinScopeProvider)

    private fun loadBuiltIns(): List<BuiltInsPackageFragment> {
        val classLoader = this::class.java.classLoader
        val streamProvider = { path: String -> classLoader?.getResourceAsStream(path) ?: ClassLoader.getSystemResourceAsStream(path) }
        val packageFqNames = StandardClassIds.builtInsPackages

        return packageFqNames.map { fqName ->
            val resourcePath = BuiltInSerializerProtocol.getBuiltInsFilePath(fqName)
            val inputStream = streamProvider(resourcePath) ?: throw IllegalStateException("Resource not found in classpath: $resourcePath")
            BuiltInsPackageFragment(inputStream, fqName, moduleData, kotlinScopeProvider)
        }
    }

    override fun getPackage(fqName: FqName): FqName? {
        if (allPackageFragments.containsKey(fqName)) return fqName
        return null
    }

    override fun getClassLikeSymbolByClassId(classId: ClassId): FirRegularClassSymbol? {
        return allPackageFragments[classId.packageFqName]?.firstNotNullOfOrNull {
            it.getClassLikeSymbolByClassId(classId)
        } ?: syntheticFunctionalInterfaceCache.tryGetSyntheticFunctionalInterface(classId)
    }


    @FirSymbolProviderInternals
    override fun getTopLevelCallableSymbolsTo(destination: MutableList<FirCallableSymbol<*>>, packageFqName: FqName, name: Name) {
        allPackageFragments[packageFqName]?.flatMapTo(destination) {
            it.getTopLevelCallableSymbols(name)
        }
    }

    @FirSymbolProviderInternals
    override fun getTopLevelFunctionSymbolsTo(destination: MutableList<FirNamedFunctionSymbol>, packageFqName: FqName, name: Name) {
        getTopLevelFunctionSymbolsToByPackageFragments(destination, packageFqName, name)
    }

    protected fun getTopLevelFunctionSymbolsToByPackageFragments(destination: MutableList<FirNamedFunctionSymbol>, packageFqName: FqName, name: Name) {
        allPackageFragments[packageFqName]?.flatMapTo(destination) {
            it.getTopLevelFunctionSymbols(name)
        }
    }

    @FirSymbolProviderInternals
    override fun getTopLevelPropertySymbolsTo(destination: MutableList<FirPropertySymbol>, packageFqName: FqName, name: Name) {
    }

    private class BuiltInsPackageFragment(
        stream: InputStream, val fqName: FqName, val moduleData: FirModuleData,
        val kotlinScopeProvider: FirKotlinScopeProvider,
    ) {

        private val binaryVersionAndPackageFragment = BinaryVersionAndPackageFragment.createFromStream(stream)

        val version: BuiltInsBinaryVersion get() = binaryVersionAndPackageFragment.version
        val packageProto: ProtoBuf.PackageFragment get() = binaryVersionAndPackageFragment.packageFragment

        private val nameResolver = NameResolverImpl(packageProto.strings, packageProto.qualifiedNames)

        val classDataFinder = ProtoBasedClassDataFinder(packageProto, nameResolver, version) { SourceElement.NO_SOURCE }

        private val memberDeserializer by lazy {
            FirDeserializationContext.createForPackage(
                fqName, packageProto.`package`, nameResolver, moduleData,
                FirBuiltinAnnotationDeserializer(moduleData.session),
                FirConstDeserializer(moduleData.session),
                containerSource = null
            ).memberDeserializer
        }

        private val lookup = moduleData.session.firCachesFactory.createCacheWithPostCompute(
            { classId: ClassId, context: FirDeserializationContext? -> FirRegularClassSymbol(classId) to context }
        ) { classId, symbol, parentContext ->
            val classData = classDataFinder.findClassData(classId)!!
            val classProto = classData.classProto

            deserializeClassToSymbol(
                classId, classProto, symbol, nameResolver, moduleData.session, moduleData,
                null, kotlinScopeProvider, parentContext,
                null,
                origin = FirDeclarationOrigin.BuiltIns,
                this::findAndDeserializeClass,
            )
        }

        fun getClassLikeSymbolByClassId(classId: ClassId): FirRegularClassSymbol? =
            findAndDeserializeClass(classId)

        private fun findAndDeserializeClass(
            classId: ClassId,
            parentContext: FirDeserializationContext? = null,
        ): FirRegularClassSymbol? {
            val classIdExists = classId in classDataFinder.allClassIds
            if (!classIdExists) return null
            return lookup.getValue(classId, parentContext)
        }

        fun getTopLevelCallableSymbols(name: Name): List<FirCallableSymbol<*>> {
            return getTopLevelFunctionSymbols(name)
        }

        fun getTopLevelFunctionSymbols(name: Name): List<FirNamedFunctionSymbol> {
            return packageProto.`package`.functionList.filter { nameResolver.getName(it.name) == name }.map {
                memberDeserializer.loadFunction(it).symbol
            }
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

            if (!version.isCompatible()) {
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

private class SyntheticFunctionalInterfaceCache(private val moduleData: FirModuleData, private val kotlinScopeProvider: FirKotlinScopeProvider) {
    private val syntheticFunctionalInterfaceCache =
        moduleData.session.firCachesFactory.createCache(::createSyntheticFunctionalInterface)

    fun tryGetSyntheticFunctionalInterface(classId: ClassId): FirRegularClassSymbol? {
        return syntheticFunctionalInterfaceCache.getValue(classId)
    }

    private fun createSyntheticFunctionalInterface(classId: ClassId): FirRegularClassSymbol? {
        return with(classId) {
            val className = relativeClassName.asString()
            val kind = FunctionClassKind.byClassNamePrefix(packageFqName, className) ?: return@with null
            val prefix = kind.classNamePrefix
            val arity = className.substring(prefix.length).toIntOrNull() ?: return null
            FirRegularClassSymbol(classId).apply symbol@{
                buildRegularClass klass@{
                    moduleData = this@SyntheticFunctionalInterfaceCache.moduleData
                    origin = FirDeclarationOrigin.BuiltIns
                    name = relativeClassName.shortName()
                    status = FirResolvedDeclarationStatusImpl(
                        Visibilities.Public,
                        Modality.ABSTRACT,
                        EffectiveVisibility.Public
                    ).apply {
                        isExpect = false
                        isActual = false
                        isInner = false
                        isCompanion = false
                        isData = false
                        isInline = false
                    }
                    classKind = ClassKind.INTERFACE
                    scopeProvider = kotlinScopeProvider
                    symbol = this@symbol
                    resolvePhase = FirResolvePhase.ANALYZED_DEPENDENCIES
                    typeParameters.addAll(
                        (1..arity).map {
                            buildTypeParameter {
                                moduleData = this@SyntheticFunctionalInterfaceCache.moduleData
                                resolvePhase = FirResolvePhase.ANALYZED_DEPENDENCIES
                                origin = FirDeclarationOrigin.BuiltIns
                                name = Name.identifier("P$it")
                                symbol = FirTypeParameterSymbol()
                                containingDeclarationSymbol = this@symbol
                                variance = Variance.IN_VARIANCE
                                isReified = false
                                bounds += moduleData.session.builtinTypes.nullableAnyType
                            }
                        },
                    )
                    typeParameters.add(
                        buildTypeParameter {
                            moduleData = this@SyntheticFunctionalInterfaceCache.moduleData
                            resolvePhase = FirResolvePhase.ANALYZED_DEPENDENCIES
                            origin = FirDeclarationOrigin.BuiltIns
                            name = Name.identifier("R")
                            symbol = FirTypeParameterSymbol()
                            containingDeclarationSymbol = this@symbol
                            variance = Variance.OUT_VARIANCE
                            isReified = false
                            bounds += moduleData.session.builtinTypes.nullableAnyType
                        },
                    )
                    val name = OperatorNameConventions.INVOKE
                    val functionStatus = FirResolvedDeclarationStatusImpl(
                        Visibilities.Public,
                        Modality.ABSTRACT,
                        EffectiveVisibility.Public
                    ).apply {
                        isExpect = false
                        isActual = false
                        isOverride = false
                        isOperator = true
                        isInfix = false
                        isInline = false
                        isTailRec = false
                        isExternal = false
                        isSuspend =
                            kind == FunctionClassKind.SuspendFunction ||
                                    kind == FunctionClassKind.KSuspendFunction
                    }
                    val typeArguments = typeParameters.map {
                        buildResolvedTypeRef {
                            type = ConeTypeParameterTypeImpl(it.symbol.toLookupTag(), false)
                        }
                    }

                    fun createSuperType(
                        kind: FunctionClassKind,
                    ): FirResolvedTypeRef {
                        return buildResolvedTypeRef {
                            type = ConeClassLikeLookupTagImpl(kind.classId(arity))
                                .constructClassType(typeArguments.map { it.type }.toTypedArray(), isNullable = false)
                        }
                    }

                    superTypeRefs += when (kind) {
                        FunctionClassKind.Function -> listOf(
                            buildResolvedTypeRef {
                                type = ConeClassLikeLookupTagImpl(StandardClassIds.Function)
                                    .constructClassType(arrayOf(typeArguments.last().type), isNullable = false)
                            }
                        )

                        FunctionClassKind.SuspendFunction -> listOf(
                            buildResolvedTypeRef {
                                type = ConeClassLikeLookupTagImpl(StandardClassIds.Function)
                                    .constructClassType(arrayOf(typeArguments.last().type), isNullable = false)
                            }
                        )

                        FunctionClassKind.KFunction -> listOf(
                            buildResolvedTypeRef {
                                type = ConeClassLikeLookupTagImpl(StandardClassIds.KFunction)
                                    .constructClassType(arrayOf(typeArguments.last().type), isNullable = false)
                            },
                            createSuperType(FunctionClassKind.Function)
                        )

                        FunctionClassKind.KSuspendFunction -> listOf(
                            buildResolvedTypeRef {
                                type = ConeClassLikeLookupTagImpl(StandardClassIds.KFunction)
                                    .constructClassType(arrayOf(typeArguments.last().type), isNullable = false)
                            },
                            createSuperType(FunctionClassKind.SuspendFunction)
                        )
                    }
                    addDeclaration(
                        buildSimpleFunction {
                            moduleData = this@SyntheticFunctionalInterfaceCache.moduleData
                            resolvePhase = FirResolvePhase.ANALYZED_DEPENDENCIES
                            origin = FirDeclarationOrigin.BuiltIns
                            returnTypeRef = typeArguments.last()
                            this.name = name
                            status = functionStatus
                            symbol = FirNamedFunctionSymbol(
                                CallableId(packageFqName, relativeClassName, name)
                            )
                            resolvePhase = FirResolvePhase.ANALYZED_DEPENDENCIES
                            valueParameters += typeArguments.dropLast(1).mapIndexed { index, typeArgument ->
                                val parameterName = Name.identifier("p${index + 1}")
                                buildValueParameter {
                                    moduleData = this@SyntheticFunctionalInterfaceCache.moduleData
                                    origin = FirDeclarationOrigin.BuiltIns
                                    resolvePhase = FirResolvePhase.ANALYZED_DEPENDENCIES
                                    returnTypeRef = typeArgument
                                    this.name = parameterName
                                    symbol = FirValueParameterSymbol(parameterName)
                                    defaultValue = null
                                    isCrossinline = false
                                    isNoinline = false
                                    isVararg = false
                                }
                            }
                            dispatchReceiverType = classId.defaultType(this@klass.typeParameters.map { it.symbol })
                        }
                    )
                }
            }
        }
    }


    private fun FunctionClassKind.classId(arity: Int) = ClassId(packageFqName, numberedClassName(arity))
}
