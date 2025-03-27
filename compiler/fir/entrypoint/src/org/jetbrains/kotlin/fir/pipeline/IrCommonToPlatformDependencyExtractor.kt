/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.pipeline

import org.jetbrains.kotlin.backend.common.actualizer.IrActualizerMapContributor
import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.backend.Fir2IrComponents
import org.jetbrains.kotlin.fir.moduleData
import org.jetbrains.kotlin.fir.resolve.providers.impl.FirCachingCompositeSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.impl.FirMppDeduplicatingSymbolProvider
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.session.structuredProviders
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.symbols.IrTypeAliasSymbol
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.classOrFail
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull
import org.jetbrains.kotlin.utils.addToStdlib.shouldNotBeCalled


class IrCommonToPlatformDependencyActualizerMapContributor(
    private val deduplicatingProvider: FirMppDeduplicatingSymbolProvider,
    private val componentsPerSession: Map<FirSession, Fir2IrComponents>,
) : IrActualizerMapContributor() {
    companion object {
        fun create(
            platformSession: FirSession,
            componentsPerSession: Map<FirSession, Fir2IrComponents>,
        ): IrCommonToPlatformDependencyActualizerMapContributor? {
            val deduplicatingProvider = (platformSession.symbolProvider as FirCachingCompositeSymbolProvider)
                .providers
                .firstIsInstanceOrNull<FirMppDeduplicatingSymbolProvider>()
            if (deduplicatingProvider == null) return null
            return IrCommonToPlatformDependencyActualizerMapContributor(deduplicatingProvider, componentsPerSession)
        }
    }

    private val dependencyToSourceSession = buildMap {
        for (sourceSession in componentsPerSession.keys) {
            val sourceModuleData = sourceSession.moduleData

            val process = { dependencyModuleData: FirModuleData ->
                if (dependencyModuleData.session.kind == FirSession.Kind.Library) {
                    put(dependencyModuleData, sourceSession)
                }
            }

            sourceModuleData.dependencies.forEach(process)
            sourceModuleData.friendDependencies.forEach(process)
        }
        val platformSession = componentsPerSession.keys.last()
        val sharedDependenciesModuleData = platformSession.structuredProviders.sharedProvider.session.moduleData
        put(sharedDependenciesModuleData, platformSession)
    }

    private val classesMap: ActualClassInfo by lazy {
        val classMapping = mutableMapOf<IrClassSymbol, IrClassSymbol>()
        val actualTypeAliases = mutableMapOf<ClassId, IrTypeAliasSymbol>()

        fun processPairOfClasses(
            commonFirClassSymbol: FirClassLikeSymbol<*>,
            platformFirClassSymbol: FirClassLikeSymbol<*>,
        ) {
            val commonIrClassSymbol = commonFirClassSymbol.toIrSymbol() as IrClassSymbol
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
            val fromPlatform = deduplicatingProvider.platformSymbolProvider.getClassLikeSymbolByClassId(classId) ?: return
            val fromCommon = deduplicatingProvider.commonSymbolProvider.getClassLikeSymbolByClassId(classId)
            if (fromCommon != null) return
            val fromShared = deduplicatingProvider.session.structuredProviders.sharedProvider.getClassLikeSymbolByClassId(classId) ?: return
            processPairOfClasses(fromShared, fromPlatform)
        }

        for ((commonFirClassSymbol, platformFirClassSymbol) in deduplicatingProvider.classMapping.values) {
            processPairOfClasses(commonFirClassSymbol, platformFirClassSymbol)
        }
        handleCloneable()

        ActualClassInfo(classMapping, actualTypeAliases)
    }

    override fun collectClassesMap(): ActualClassInfo {
        return classesMap
    }

    private val topLevelCallablesMap by lazy {
        deduplicatingProvider.commonCallableToPlatformCallableMap.entries.associate { (commonFirSymbol, platformFirSymbol) ->
            val commonIrSymbol = commonFirSymbol.toIrSymbol()
            val platformIrSymbol = platformFirSymbol.toIrSymbol()
            commonIrSymbol to platformIrSymbol
        }
    }


    override fun collectTopLevelCallablesMap(): Map<IrSymbol, IrSymbol> {
        return topLevelCallablesMap
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
