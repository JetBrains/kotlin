/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls.inference.components

import org.jetbrains.kotlin.resolve.calls.components.CreateFreshVariablesSubstitutor.shouldBeFlexible
import org.jetbrains.kotlin.resolve.calls.inference.model.NewTypeVariable
import org.jetbrains.kotlin.resolve.calls.inference.model.TypeVariableFromCallableDescriptor
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.checker.KotlinTypeRefiner
import org.jetbrains.kotlin.types.model.KotlinTypeMarker
import org.jetbrains.kotlin.types.model.TypeVariableMarker
import org.jetbrains.kotlin.types.refinement.TypeRefinement
import org.jetbrains.kotlin.types.typeUtil.unCapture as unCaptureKotlinType

class ClassicConstraintSystemUtilContext(val kotlinTypeRefiner: KotlinTypeRefiner) : ConstraintSystemUtilContext {
    override fun TypeVariableMarker.shouldBeFlexible(): Boolean {
        return this is TypeVariableFromCallableDescriptor && this.originalTypeParameter.shouldBeFlexible()
    }

    override fun TypeVariableMarker.hasOnlyInputTypesAttribute(): Boolean {
        require(this is NewTypeVariable)
        return hasOnlyInputTypesAnnotation()
    }

    override fun KotlinTypeMarker.unCapture(): KotlinTypeMarker {
        require(this is KotlinType)
        return unCaptureKotlinType().unwrap()
    }

    override fun TypeVariableMarker.isReified(): Boolean {
        if (this !is TypeVariableFromCallableDescriptor) return false
        return originalTypeParameter.isReified
    }

    @OptIn(TypeRefinement::class)
    override fun KotlinTypeMarker.refineType(): KotlinTypeMarker {
        require(this is KotlinType)
        return kotlinTypeRefiner.refineType(this)
    }
}
