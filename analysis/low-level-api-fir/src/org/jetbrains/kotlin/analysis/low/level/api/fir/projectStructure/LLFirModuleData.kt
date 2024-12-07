/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.projectStructure

import org.jetbrains.kotlin.analysis.api.projectStructure.KaModule
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.sessions.LLFirSessionCache
import org.jetbrains.kotlin.fir.FirElementWithResolveState
import org.jetbrains.kotlin.fir.FirModuleData
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.moduleData
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.platform.isCommon

val FirElementWithResolveState.llFirModuleData: LLFirModuleData
    get() = moduleData as LLFirModuleData

val FirSession.llFirModuleData: LLFirModuleData
    get() = moduleData as LLFirModuleData

val LLFirSession.moduleData: LLFirModuleData
    get() = llFirModuleData

val FirBasedSymbol<*>.llFirModuleData: LLFirModuleData
    get() = fir.llFirModuleData


class LLFirModuleData private constructor(val ktModule: KaModule) : FirModuleData() {
    constructor(session: LLFirSession) : this(session.ktModule) {
        bindSession(session)
    }

    override val name: Name get() = Name.special("<${ktModule.moduleDescription}>")

    override val dependencies: List<FirModuleData> by lazy(LazyThreadSafetyMode.PUBLICATION) {
        ktModule.directRegularDependencies.map(::LLFirModuleData)
    }

    override val dependsOnDependencies: List<FirModuleData> by lazy(LazyThreadSafetyMode.PUBLICATION) {
        ktModule.directDependsOnDependencies.map(::LLFirModuleData)
    }

    override val allDependsOnDependencies: List<FirModuleData> by lazy(LazyThreadSafetyMode.PUBLICATION) {
        ktModule.transitiveDependsOnDependencies.map(::LLFirModuleData)
    }

    override val friendDependencies: List<FirModuleData> by lazy(LazyThreadSafetyMode.PUBLICATION) {
        ktModule.directFriendDependencies.map(::LLFirModuleData)
    }

    override val platform: TargetPlatform get() = ktModule.targetPlatform

    override val isCommon: Boolean get() = ktModule.targetPlatform.isCommon()

    override val session: LLFirSession
        get() = boundSession?.let { it as LLFirSession }
            ?: LLFirSessionCache.getInstance(ktModule.project).getSession(ktModule, preferBinary = true)

    override fun equals(other: Any?): Boolean = this === other || other is LLFirModuleData && ktModule == other.ktModule
    override fun hashCode(): Int = ktModule.hashCode()
}
