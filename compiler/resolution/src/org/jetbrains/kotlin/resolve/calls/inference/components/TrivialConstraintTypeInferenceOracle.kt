/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls.inference.components

import org.jetbrains.kotlin.resolve.calls.inference.model.Constraint
import org.jetbrains.kotlin.resolve.calls.inference.model.ConstraintKind
import org.jetbrains.kotlin.types.UnwrappedType
import org.jetbrains.kotlin.types.typeUtil.isNothing
import org.jetbrains.kotlin.types.typeUtil.isNullableNothing

class TrivialConstraintTypeInferenceOracle {
    // The idea is to add knowledge that constraint `Nothing(?) <: T` is quite useless and
    // it's totally fine to go and resolve postponed argument without fixation T to Nothing(?).
    // In other words, constraint `Nothing(?) <: T` is *not* proper
    fun isTrivialConstraint(constraint: Constraint): Boolean {
        // TODO: probably we also can take into account `T <: Any(?)` constraints
        return constraint.kind == ConstraintKind.LOWER && constraint.type.isNothingOrNullableNothing()
    }
}

private fun UnwrappedType.isNothingOrNullableNothing(): Boolean =
    isNothing() || isNullableNothing()