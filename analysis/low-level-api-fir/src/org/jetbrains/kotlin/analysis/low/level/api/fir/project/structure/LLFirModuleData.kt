/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.project.structure

import org.jetbrains.kotlin.analysis.project.structure.KtModule
import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.moduleData
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.resolve.PlatformDependentAnalyzerServices

sealed class LLFirModuleData : FirModuleData()

val FirDeclaration.firModuleData: LLFirModuleData
    get() {
        return moduleData as LLFirModuleData
    }

val FirSession.firModuleData: LLFirModuleData
    get() {
        return moduleData as LLFirModuleData
    }

val FirSession.firKtModuleBasedModuleData: LLFirKtModuleBasedModuleData
    get() {
        return moduleData as LLFirKtModuleBasedModuleData
    }


val FirBasedSymbol<*>.firModuleData: LLFirModuleData
    get() = fir.firModuleData

class LLFirBuiltinsModuleData(val useSiteKtModule: KtModule) : LLFirModuleData() {
    override val name: Name
        get() = Name.special("<builtins for ${useSiteKtModule.moduleDescription}>")

    override val dependencies: List<FirModuleData> get() = emptyList()
    override val dependsOnDependencies: List<FirModuleData> get() = emptyList()
    override val friendDependencies: List<FirModuleData> get() = emptyList()

    override val platform: TargetPlatform get() = useSiteKtModule.platform
    override val analyzerServices: PlatformDependentAnalyzerServices get() = useSiteKtModule.analyzerServices

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as LLFirBuiltinsModuleData

        if (useSiteKtModule != other.useSiteKtModule) return false

        return true
    }

    override fun hashCode(): Int {
        return useSiteKtModule.hashCode()
    }
}

class LLFirKtModuleBasedModuleData(
    val ktModule: KtModule,
) : LLFirModuleData() {
    override val name: Name get() = Name.special("<${ktModule.moduleDescription}>")

    override val dependencies: List<FirModuleData> by lazy(LazyThreadSafetyMode.PUBLICATION) {
        ktModule.directRegularDependencies.map(::LLFirKtModuleBasedModuleData)
    }

    override val dependsOnDependencies: List<FirModuleData> by lazy(LazyThreadSafetyMode.PUBLICATION) {
        ktModule.directRefinementDependencies.map(::LLFirKtModuleBasedModuleData)
    }

    override val friendDependencies: List<FirModuleData> by lazy(LazyThreadSafetyMode.PUBLICATION) {
        ktModule.directRefinementDependencies.map(::LLFirKtModuleBasedModuleData)
    }

    override val platform: TargetPlatform get() = ktModule.platform

    override val analyzerServices: PlatformDependentAnalyzerServices get() = ktModule.analyzerServices

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as LLFirKtModuleBasedModuleData

        if (ktModule != other.ktModule) return false

        return true
    }

    override fun hashCode(): Int {
        return ktModule.hashCode()
    }
}
