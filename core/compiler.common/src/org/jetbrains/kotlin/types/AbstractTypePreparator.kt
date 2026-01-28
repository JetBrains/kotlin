/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.types

import org.jetbrains.kotlin.types.model.K2Only
import org.jetbrains.kotlin.types.model.KotlinTypeMarker
import org.jetbrains.kotlin.types.model.RigidTypeMarker

abstract class AbstractTypePreparator {
    abstract fun prepareType(type: KotlinTypeMarker): KotlinTypeMarker

    @K2Only
    open fun clearTypeFromUnnecessaryAttributes(type: RigidTypeMarker): RigidTypeMarker = type

    object Default : AbstractTypePreparator() {
        override fun prepareType(type: KotlinTypeMarker): KotlinTypeMarker {
            return type
        }
    }
}
