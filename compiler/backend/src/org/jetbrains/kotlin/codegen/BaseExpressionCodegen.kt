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

package org.jetbrains.kotlin.codegen

import org.jetbrains.kotlin.codegen.inline.NameGenerator
import org.jetbrains.kotlin.codegen.inline.ReifiedTypeInliner.Companion.putReifiedOperationMarker
import org.jetbrains.kotlin.codegen.inline.ReifiedTypeInliner.OperationKind
import org.jetbrains.kotlin.codegen.inline.ReifiedTypeParametersUsages
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.types.TypeSystemCommonBackendContext
import org.jetbrains.kotlin.types.model.KotlinTypeMarker
import org.jetbrains.kotlin.types.model.TypeParameterMarker
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter

interface BaseExpressionCodegen {

    val frameMap: FrameMapBase<*>

    val visitor: InstructionAdapter

    val inlineNameGenerator: NameGenerator

    val typeSystem: TypeSystemCommonBackendContext

    val lastLineNumber: Int

    fun propagateChildReifiedTypeParametersUsages(reifiedTypeParametersUsages: ReifiedTypeParametersUsages)

    fun pushClosureOnStack(
        classDescriptor: ClassDescriptor,
        putThis: Boolean,
        callGenerator: CallGenerator,
        functionReferenceReceiver: StackValue?
    )

    fun markLineNumberAfterInlineIfNeeded(registerLineNumberAfterwards: Boolean)

    fun consumeReifiedOperationMarker(typeParameter: TypeParameterMarker)

    @JvmDefault
    fun putReifiedOperationMarkerIfTypeIsReifiedParameter(type: KotlinTypeMarker, operationKind: OperationKind) {
        with(typeSystem) {
            val (typeParameter, second) = extractReificationArgument(type) ?: return
            if (typeParameter.isReified()) {
                consumeReifiedOperationMarker(typeParameter)
                putReifiedOperationMarker(operationKind, second, visitor)
            }
        }
    }
}
