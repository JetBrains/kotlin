/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.jvm.checkers

import org.jetbrains.kotlin.fir.analysis.checkers.FirPrimaryConstructorSuperTypeCheckerPlatformComponent
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.JvmStandardClassIds
import org.jetbrains.kotlin.name.StandardClassIds

object FirJvmPrimaryConstructorSuperTypeCheckerPlatformComponent : FirPrimaryConstructorSuperTypeCheckerPlatformComponent() {
    override val supertypesThatDontNeedInitializationInSubtypesConstructors: Set<ClassId> =
        setOf(StandardClassIds.Enum, JvmStandardClassIds.Java.Record)
}
