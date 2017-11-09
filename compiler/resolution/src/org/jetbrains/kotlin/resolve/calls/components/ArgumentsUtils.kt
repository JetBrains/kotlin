/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.resolve.calls.components

import org.jetbrains.kotlin.descriptors.ParameterDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.resolve.calls.model.KotlinCallArgument
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValueWithSmartCastInfo
import org.jetbrains.kotlin.types.UnwrappedType
import org.jetbrains.kotlin.types.checker.intersectWrappedTypes
import org.jetbrains.kotlin.utils.addToStdlib.safeAs


internal fun unexpectedArgument(argument: KotlinCallArgument): Nothing =
        error("Unexpected argument type: $argument, ${argument.javaClass.canonicalName}.")

// if expression is not stable and has smart casts, then we create this type
internal val ReceiverValueWithSmartCastInfo.unstableType: UnwrappedType?
    get() {
        if (isStable || possibleTypes.isEmpty()) return null
        return intersectWrappedTypes(possibleTypes + receiverValue.type)
    }

// with all smart casts if stable
internal val ReceiverValueWithSmartCastInfo.stableType: UnwrappedType
    get() {
        if (!isStable || possibleTypes.isEmpty()) return receiverValue.type.unwrap()
        return intersectWrappedTypes(possibleTypes + receiverValue.type)
    }

internal fun KotlinCallArgument.getExpectedType(parameter: ParameterDescriptor) =
        if (this.isSpread) {
            parameter.type.unwrap()
        }
        else {
            parameter.safeAs<ValueParameterDescriptor>()?.varargElementType?.unwrap() ?: parameter.type.unwrap()
        }

val ValueParameterDescriptor.isVararg: Boolean get() = varargElementType != null
