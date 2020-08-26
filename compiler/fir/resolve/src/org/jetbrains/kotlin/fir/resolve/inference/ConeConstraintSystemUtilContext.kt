/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.inference

import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.resolve.calls.inference.components.ConstraintSystemUtilContext
import org.jetbrains.kotlin.types.model.KotlinTypeMarker
import org.jetbrains.kotlin.types.model.TypeVariableMarker

object ConeConstraintSystemUtilContext : ConstraintSystemUtilContext {
    override fun TypeVariableMarker.shouldBeFlexible(): Boolean {
        // TODO
        return false
    }

    override fun TypeVariableMarker.hasOnlyInputTypesAttribute(): Boolean {
        // TODO
        return false
    }

    override fun KotlinTypeMarker.unCapture(): KotlinTypeMarker {
        require(this is ConeKotlinType)
        // TODO, see TypeUtils.kt
        return this
    }

    override fun TypeVariableMarker.isReified(): Boolean {
        // TODO
        return false
    }

    override fun KotlinTypeMarker.refineType(): KotlinTypeMarker {
        return this
    }
}
