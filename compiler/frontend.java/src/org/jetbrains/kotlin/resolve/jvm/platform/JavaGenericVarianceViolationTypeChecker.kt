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

package org.jetbrains.kotlin.resolve.jvm.platform

import org.jetbrains.kotlin.load.java.lazy.types.RawTypeImpl
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.resolve.calls.checkers.AdditionalTypeChecker
import org.jetbrains.kotlin.resolve.calls.context.ResolutionContext
import org.jetbrains.kotlin.resolve.jvm.diagnostics.ErrorsJvm
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.error.ErrorUtils
import org.jetbrains.kotlin.types.checker.KotlinTypeChecker
import org.jetbrains.kotlin.types.checker.TypeCheckingProcedure

object JavaGenericVarianceViolationTypeChecker : AdditionalTypeChecker {
    // Prohibits covariant type argument conversions `List<String> -> (MutableList<Any>..List<Any>)` when expected type's lower bound is invariant.
    // It's needed to prevent accident unsafe covariant conversions of mutable collections.
    //
    // Example:
    // class JavaClass { static void fillWithDefaultObjects(List<Object> list); // add Object's to list }
    //
    // val x: MutableList<String>
    // JavaClass.fillWithDefaultObjects(x) // using `x` after this call may lead to CCE
    override fun checkType(
            expression: KtExpression,
            expressionType: KotlinType,
            expressionTypeWithSmartCast: KotlinType,
            c: ResolutionContext<*>
    ) {
        val expectedType = c.expectedType
        if (TypeUtils.noExpectedType(expectedType) || ErrorUtils.containsErrorType(expectedType) || ErrorUtils.containsUninferredTypeVariable(expectedType)) return

        // optimization: if no arguments or flexibility, everything is OK
        if (expectedType.arguments.isEmpty() || !expectedType.isFlexible()) return

        val lowerBound = expectedType.asFlexibleType().lowerBound
        val upperBound = expectedType.asFlexibleType().upperBound

        // Use site variance projection is always the same for flexible types
        if (lowerBound.constructor == upperBound.constructor) return
        // Anything is acceptable for raw types
        if (expectedType.unwrap() is RawTypeImpl) return

        val correspondingSubType = TypeCheckingProcedure.findCorrespondingSupertype(expressionTypeWithSmartCast, lowerBound) ?: return

        assert(lowerBound.arguments.size == upperBound.arguments.size) {
            "Different arguments count in flexible bounds: " +
            "($lowerBound(${lowerBound.arguments.size})..$upperBound(${upperBound.arguments.size})"
        }

        assert(lowerBound.arguments.size == correspondingSubType.arguments.size) {
            "Different arguments count in corresponding subtype and supertype: " +
            "($lowerBound(${lowerBound.arguments.size})..$correspondingSubType(${correspondingSubType.arguments.size})"
        }


        val lowerParameters = lowerBound.constructor.parameters
        val upperParameters = upperBound.constructor.parameters
        val lowerArguments = lowerBound.arguments

        correspondingSubType.arguments.indices.forEach {
            index ->
            val lowerArgument = lowerArguments[index]
            // Currently we don't have flexible types with different constructors with contravariant arguments
            // So check just covariant case
            if (lowerParameters[index].variance == Variance.INVARIANT
                && upperParameters[index].variance == Variance.OUT_VARIANCE
                && lowerArgument.projectionKind != Variance.OUT_VARIANCE
                && !KotlinTypeChecker.DEFAULT.equalTypes(correspondingSubType.arguments[index].type, lowerArgument.type)
            ) {
                c.trace.report(ErrorsJvm.JAVA_TYPE_MISMATCH.on(expression, expressionTypeWithSmartCast, expectedType))
            }
        }
    }

}
