/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir

import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.TargetPlatform

abstract class FirModuleData : FirSessionComponent {
    abstract val name: Name
    abstract val dependencies: List<FirModuleData>
    abstract val dependsOnDependencies: List<FirModuleData>
    abstract val friendDependencies: List<FirModuleData>
    abstract val platform: TargetPlatform
    abstract val session: FirSession
}

class FirModuleDataImpl(
    override val name: Name,
    override val dependencies: List<FirModuleData>,
    override val dependsOnDependencies: List<FirModuleData>,
    override val friendDependencies: List<FirModuleData>,
    override val platform: TargetPlatform,
    override val session: FirSession,
) : FirModuleData()

val FirSession.moduleData: FirModuleData by FirSession.sessionComponentAccessor()
