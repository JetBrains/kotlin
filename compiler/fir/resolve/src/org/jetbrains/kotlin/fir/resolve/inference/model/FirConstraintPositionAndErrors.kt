/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.inference.model

import org.jetbrains.kotlin.resolve.calls.inference.model.ArgumentConstraintPosition
import org.jetbrains.kotlin.resolve.calls.inference.model.DeclaredUpperBoundConstraintPosition
import org.jetbrains.kotlin.resolve.calls.inference.model.FixVariableConstraintPosition
import org.jetbrains.kotlin.types.model.TypeVariableMarker

class ConeDeclaredUpperBoundConstraintPosition : DeclaredUpperBoundConstraintPosition<Nothing?>(null)

class ConeFixVariableConstraintPosition(variable: TypeVariableMarker) : FixVariableConstraintPosition<Nothing?>(variable, null)

class ConeArgumentConstraintPosition : ArgumentConstraintPosition<Nothing?>(null)