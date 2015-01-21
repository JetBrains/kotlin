/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.resolve.calls.inference

import org.jetbrains.kotlin.resolve.calls.inference.constraintPosition.ConstraintPosition
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor

open class ConstraintError(val constraintPosition: ConstraintPosition)

class TypeConstructorMismatch(constraintPosition: ConstraintPosition): ConstraintError(constraintPosition)

class ErrorInConstrainingType(constraintPosition: ConstraintPosition): ConstraintError(constraintPosition)

class CannotCapture(constraintPosition: ConstraintPosition, val typeVariable: TypeParameterDescriptor): ConstraintError(constraintPosition)

fun ConstraintError.substituteTypeVariable(substitution: (TypeParameterDescriptor) -> TypeParameterDescriptor) =
        if (this is CannotCapture) CannotCapture(constraintPosition, substitution(typeVariable)) else this