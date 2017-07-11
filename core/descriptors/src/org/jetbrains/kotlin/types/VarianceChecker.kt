/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.types

import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.types.checker.TypeCheckingProcedure
import org.jetbrains.kotlin.types.checker.TypeCheckingProcedure.EnrichedProjectionKind
import org.jetbrains.kotlin.utils.DO_NOTHING_3

interface TypeHolder<out D : TypeHolder<D>> {
    val type: KotlinType
    val arguments: List<TypeHolderArgument<D>?>
    val flexibleBounds: Pair<D, D>? get() = null
}

interface TypeHolderArgument<out D : TypeHolder<D>> {
    val projection: TypeProjection
    val typeParameter: TypeParameterDescriptor?
    val holder: D
}

fun <D : TypeHolder<D>> D.checkTypePosition(
        position: Variance,
        reportError: (TypeParameterDescriptor, D, Variance) -> Unit = DO_NOTHING_3,
        customVariance: (TypeParameterDescriptor) -> Variance? = { null }
): Boolean {
    flexibleBounds?.let {
        return it.first.checkTypePosition(position, reportError, customVariance) and
                    it.second.checkTypePosition(position, reportError, customVariance)
    }

    val classifierDescriptor = type.constructor.declarationDescriptor
    if (classifierDescriptor is TypeParameterDescriptor) {
        val declarationVariance = customVariance(classifierDescriptor) ?: classifierDescriptor.variance
        if (!declarationVariance.allowsPosition(position)
            && !type.annotations.hasAnnotation(org.jetbrains.kotlin.builtins.KotlinBuiltIns.FQ_NAMES.unsafeVariance)) {
            reportError(classifierDescriptor, this, position)
        }
        return declarationVariance.allowsPosition(position)
    }

    var noError = true
    for (argument in arguments) {
        if (argument?.typeParameter == null || argument.projection.isStarProjection) continue

        val projectionKind = TypeCheckingProcedure.getEffectiveProjectionKind(argument.typeParameter!!, argument.projection)!!
        val newPosition = when (projectionKind) {
            EnrichedProjectionKind.OUT -> position
            EnrichedProjectionKind.IN -> position.opposite()
            EnrichedProjectionKind.INV -> Variance.INVARIANT
            EnrichedProjectionKind.STAR -> null // CONFLICTING_PROJECTION error was reported
        }
        if (newPosition != null) {
            noError = noError and argument.holder.checkTypePosition(newPosition, reportError, customVariance)
        }
    }
    return noError
}
