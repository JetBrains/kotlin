/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.resolve.PlatformDependentAnalyzerServices

abstract class FirModuleData : FirSessionComponent {
    abstract val name: Name
    abstract val dependencies: List<FirModuleData>
    abstract val dependsOnDependencies: List<FirModuleData>
    abstract val friendDependencies: List<FirModuleData>
    abstract val platform: TargetPlatform

    // TODO: analyzerServices are needed only as default imports providers
    //   refactor them to make API clearer
    abstract val analyzerServices: PlatformDependentAnalyzerServices

    private var _session: FirSession? = null
    val session: FirSession
        get() = _session ?: error("module data not bound to session")

    fun bindSession(session: FirSession) {
        if (_session != null) {
            error("module data already bound to $this")
        }
        _session = session
    }
}

class FirModuleDataImpl(
    override val name: Name,
    override val dependencies: List<FirModuleData>,
    override val dependsOnDependencies: List<FirModuleData>,
    override val friendDependencies: List<FirModuleData>,
    override val platform: TargetPlatform,
    override val analyzerServices: PlatformDependentAnalyzerServices
) : FirModuleData()

val FirSession.nullableModuleData: FirModuleData? by FirSession.nullableSessionComponentAccessor()
val FirSession.moduleData: FirModuleData
    get() = nullableModuleData ?: error("Module data is not registered in $this")

