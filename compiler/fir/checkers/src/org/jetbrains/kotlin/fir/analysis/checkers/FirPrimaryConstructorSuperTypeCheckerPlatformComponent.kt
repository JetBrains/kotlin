/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.FirSessionComponent
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.StandardClassIds

abstract class FirPrimaryConstructorSuperTypeCheckerPlatformComponent : FirSessionComponent {
    abstract val supertypesThatDontNeedInitializationInSubtypesConstructors: Set<ClassId>

    object Default : FirPrimaryConstructorSuperTypeCheckerPlatformComponent() {
        override val supertypesThatDontNeedInitializationInSubtypesConstructors: Set<ClassId> = setOf(StandardClassIds.Enum)
    }
}

val FirSession.primaryConstructorSuperTypePlatformSupport by FirSession.sessionComponentAccessor<FirPrimaryConstructorSuperTypeCheckerPlatformComponent>()
