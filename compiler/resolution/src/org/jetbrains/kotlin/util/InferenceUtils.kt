/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.util

import org.jetbrains.kotlin.resolve.calls.components.KotlinResolutionCallbacks
import org.jetbrains.kotlin.resolve.calls.inference.NewConstraintSystem
import org.jetbrains.kotlin.resolve.calls.inference.model.TypeVariableTypeConstructor
import org.jetbrains.kotlin.resolve.calls.inference.model.typeForTypeVariable
import org.jetbrains.kotlin.types.model.TypeSubstitutorMarker

fun NewConstraintSystem.buildNotFixedVariablesToPossibleResultType(resolutionCallbacks: KotlinResolutionCallbacks): TypeSubstitutorMarker =
    asConstraintSystemCompleterContext().typeSubstitutorByTypeConstructor(
        getBuilder().currentStorage().notFixedTypeVariables.mapValues {
            val typeVariable = it.key as TypeVariableTypeConstructor
            resolutionCallbacks.findResultType(this, typeVariable) ?: typeVariable.typeForTypeVariable()
        }
)
