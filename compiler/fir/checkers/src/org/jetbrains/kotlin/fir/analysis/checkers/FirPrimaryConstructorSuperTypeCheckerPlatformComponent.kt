/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers

import org.jetbrains.kotlin.fir.FirComposableSessionComponent
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.SessionConfiguration
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.StandardClassIds

abstract class FirPrimaryConstructorSuperTypeCheckerPlatformComponent : FirComposableSessionComponent<FirPrimaryConstructorSuperTypeCheckerPlatformComponent> {
    abstract val supertypesThatDontNeedInitializationInSubtypesConstructors: Set<ClassId>

    object Default : FirPrimaryConstructorSuperTypeCheckerPlatformComponent(), FirComposableSessionComponent.Single<FirPrimaryConstructorSuperTypeCheckerPlatformComponent> {
        override val supertypesThatDontNeedInitializationInSubtypesConstructors: Set<ClassId> = setOf(StandardClassIds.Enum)
    }

    class Composed(
        override val components: List<FirPrimaryConstructorSuperTypeCheckerPlatformComponent>
    ) : FirPrimaryConstructorSuperTypeCheckerPlatformComponent(), FirComposableSessionComponent.Composed<FirPrimaryConstructorSuperTypeCheckerPlatformComponent> {
        override val supertypesThatDontNeedInitializationInSubtypesConstructors: Set<ClassId> =
            components.flatMapTo(mutableSetOf()) { it.supertypesThatDontNeedInitializationInSubtypesConstructors }
    }

    @SessionConfiguration
    override fun createComposed(components: List<FirPrimaryConstructorSuperTypeCheckerPlatformComponent>): Composed {
        return Composed(components)
    }
}

val FirSession.primaryConstructorSuperTypePlatformSupport: FirPrimaryConstructorSuperTypeCheckerPlatformComponent by FirSession.sessionComponentAccessor<FirPrimaryConstructorSuperTypeCheckerPlatformComponent>()
