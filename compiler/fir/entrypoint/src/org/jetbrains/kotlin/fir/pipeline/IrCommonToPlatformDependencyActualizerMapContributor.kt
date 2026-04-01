/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.pipeline

import org.jetbrains.kotlin.backend.common.actualizer.IrActualizerMapContributor
import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.backend.Fir2IrComponents
import org.jetbrains.kotlin.fir.declarations.fullyExpandedClass
import org.jetbrains.kotlin.fir.moduleData
import org.jetbrains.kotlin.fir.resolve.providers.impl.FirCachingCompositeSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.impl.FirCommonDeclarationsMappingSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.session.NativeForwardDeclarationsSymbolProvider
import org.jetbrains.kotlin.fir.session.structuredProviders
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrPropertySymbol
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.symbols.IrTypeAliasSymbol
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.classOrFail
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.utils.addIfNotNull
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull
import org.jetbrains.kotlin.utils.addToStdlib.shouldNotBeCalled


class IrCommonToPlatformDependencyActualizerMapContributor private constructor(
    private val platformSession: FirSession,
    private val platformMappingProvider: FirCommonDeclarationsMappingSymbolProvider,
    private val commonMappingProviders: List<FirCommonDeclarationsMappingSymbolProvider>,
    private val componentsPerSession: Map<FirSession, Fir2IrComponents>,
) : IrActualizerMapContributor() {
    companion object {
        fun create(
            platformSession: FirSession,
            componentsPerSession: Map<FirSession, Fir2IrComponents>,
        ): IrCommonToPlatformDependencyActualizerMapContributor? {
            val mappingProviders = mutableListOf<FirCommonDeclarationsMappingSymbolProvider>()

            fun process(session: FirSession) {
                val mappingProvider = (session.symbolProvider as FirCachingCompositeSymbolProvider)
                    .providers
                    .firstIsInstanceOrNull<FirCommonDeclarationsMappingSymbolProvider>()
                mappingProviders.addIfNotNull(mappingProvider)
                for (dependency in session.moduleData.dependsOnDependencies) {
                    process(dependency.session)
                }
            }
            process(platformSession)

            val (platformMappingProviders, commonMappingProviders) = mappingProviders.partition { it.session == platformSession }
            if (platformMappingProviders.isEmpty()) return null
            val platformMappingProvider = platformMappingProviders.single()
            return IrCommonToPlatformDependencyActualizerMapContributor(
                platformSession,
                platformMappingProvider,
                commonMappingProviders,
                componentsPerSession
            )
        }
    }

    private val dependencyToSourceSession = buildMap {
        for (sourceSession in componentsPerSession.keys) {
            val sourceModuleData = sourceSession.moduleData

            val process = { dependencyModuleData: FirModuleData ->
                if (dependencyModuleData.session.kind == FirSession.Kind.Library) {
                    put(dependencyModuleData, sourceSession)

                    // NativeForwardDeclarationsSymbolProviders have their own moduleData that we need to collect separately
                    dependencyModuleData.session.structuredProviders.dependencyProviders
                        .filterIsInstance<NativeForwardDeclarationsSymbolProvider>()
                        .forEach {
                            put(it.forwardDeclarationsModuleData, sourceSession)
                        }
                }
            }

            sourceModuleData.dependencies.forEach(process)
            sourceModuleData.friendDependencies.forEach(process)
        }
        val rootCommonSession = componentsPerSession.keys.first()
        val sharedDependenciesModuleData = rootCommonSession.structuredProviders.sharedProvider.session.moduleData
        put(sharedDependenciesModuleData, rootCommonSession)
    }

    private val classesMap: ActualClassInfo by lazy {
        val classMapping = mutableMapOf<IrClassSymbol, IrClassSymbol>()
        val actualTypeAliases = mutableMapOf<ClassId, IrTypeAliasSymbol>()

        fun processPairOfClasses(
            commonFirClassSymbol: FirClassLikeSymbol<*>,
            platformFirClassSymbol: FirClassLikeSymbol<*>,
        ) {
            val commonIrClassSymbol = commonFirClassSymbol.toIrSymbol() as? IrClassSymbol ?: return
            val platformClassSymbol = when (val platformSymbol = platformFirClassSymbol.toIrSymbol()) {
                is IrClassSymbol -> platformSymbol
                is IrTypeAliasSymbol -> {
                    actualTypeAliases[platformFirClassSymbol.classId] = platformSymbol
                    @OptIn(UnsafeDuringIrConstructionAPI::class)
                    platformSymbol.owner.expandedType.type.classOrFail
                }
                else -> error("Unexpected symbol: $commonIrClassSymbol")
            }
            classMapping[commonIrClassSymbol] = platformClassSymbol
        }

        fun handleCloneable() {
            val classId = StandardClassIds.Cloneable
            val fromPlatform = platformMappingProvider.platformSymbolProvider.getClassLikeSymbolByClassId(classId) ?: return
            val fromCommon = platformMappingProvider.commonSymbolProvider.getClassLikeSymbolByClassId(classId)
            if (fromCommon != null) return
            val fromShared = platformMappingProvider.session.structuredProviders.sharedProvider.getClassLikeSymbolByClassId(classId) ?: return
            processPairOfClasses(fromShared, fromPlatform)
        }

        for (commonMappingProvider in commonMappingProviders) {
            // List copy is required because by querying the platform symbol provider might trigger the common symbol provider
            for (pair in commonMappingProvider.classMapping.values.toList()) {
                val commonFirClassSymbol = pair.platformClass ?: pair.commonClass ?: continue
                val platformFirClassSymbol = platformSession.symbolProvider.getClassLikeSymbolByClassId(commonFirClassSymbol.classId)!!
                processPairOfClasses(commonFirClassSymbol, platformFirClassSymbol)
            }
        }

        handleCloneable()

        ActualClassInfo(classMapping, actualTypeAliases)
    }

    override fun collectClassesMap(): ActualClassInfo {
        return classesMap
    }

    @OptIn(UnsafeDuringIrConstructionAPI::class)
    private val topLevelCallablesMap by lazy {
        buildMap {
            for ((commonFirSymbol, platformFirSymbol) in platformMappingProvider.commonCallableToPlatformCallableMap) {
                val commonIrSymbol = commonFirSymbol.toIrSymbol()
                val platformIrSymbol = platformFirSymbol.toIrSymbol()
                put(commonIrSymbol, platformIrSymbol)

                if (commonIrSymbol is IrPropertySymbol && platformIrSymbol is IrPropertySymbol) {
                    val commonIrGetterSymbol = commonIrSymbol.owner.getter?.symbol
                    val platformIrGetterSymbol = platformIrSymbol.owner.getter?.symbol

                    if (commonIrGetterSymbol != null && platformIrGetterSymbol != null) {
                        put(commonIrGetterSymbol, platformIrGetterSymbol)
                    }

                    val commonIrSetterSymbol = commonIrSymbol.owner.setter?.symbol
                    val platformIrSetterSymbol = platformIrSymbol.owner.setter?.symbol

                    if (commonIrSetterSymbol != null && platformIrSetterSymbol != null) {
                        put(commonIrSetterSymbol, platformIrSetterSymbol)
                    }
                }
            }
        }
    }


    override fun collectTopLevelCallablesMap(): Map<IrSymbol, IrSymbol> {
        return topLevelCallablesMap
    }

    override fun actualizeClass(classId: ClassId): IrClassSymbol? {
        val symbol = platformSession.symbolProvider.getClassLikeSymbolByClassId(classId) ?: return null
        val fullyExpandedClass = symbol.fullyExpandedClass(platformMappingProvider.session) ?: return null
        if (fullyExpandedClass.moduleData !in dependencyToSourceSession) return null
        return fullyExpandedClass.toIrSymbol() as? IrClassSymbol
    }

    private fun FirBasedSymbol<*>.properComponents(): Fir2IrComponents {
        val sourceSession = dependencyToSourceSession.getValue(moduleData)
        return componentsPerSession.getValue(sourceSession)
    }

    private fun FirClassLikeSymbol<*>.toIrSymbol(): IrSymbol {
        val c = properComponents()
        return when (this) {
            is FirClassSymbol -> c.classifierStorage.getIrClassSymbol(this)
            is FirTypeAliasSymbol -> c.classifierStorage.getIrTypeAliasSymbol(this)
        }
    }

    private fun FirCallableSymbol<*>.toIrSymbol(): IrSymbol {
        val c = properComponents()
        return when (this) {
            is FirNamedFunctionSymbol -> c.declarationStorage.getIrFunctionSymbol(this)
            is FirPropertySymbol -> c.declarationStorage.getIrPropertySymbol(this)
            else -> shouldNotBeCalled()
        }
    }
}
