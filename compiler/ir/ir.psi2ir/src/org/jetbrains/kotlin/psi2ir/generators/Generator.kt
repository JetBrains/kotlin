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

package org.jetbrains.kotlin.psi2ir.generators

import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.ir.expressions.IrErrorExpressionImpl
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.util.slicedMap.ReadOnlySlice
import java.lang.AssertionError
import java.lang.RuntimeException


interface Generator {
    val context: GeneratorContext
}

interface GeneratorWithScope : Generator {
    val scope: Scope
}


fun <K, V : Any> Generator.get(slice: ReadOnlySlice<K, V>, key: K): V? =
        context.bindingContext[slice, key]

fun <K, V : Any> Generator.getOrFail(slice: ReadOnlySlice<K, V>, key: K): V =
        context.bindingContext[slice, key] ?: throw RuntimeException("No $slice for $key")

inline fun <K, V : Any> Generator.getOrFail(slice: ReadOnlySlice<K, V>, key: K, message: (K) -> String): V =
        context.bindingContext[slice, key] ?: throw RuntimeException(message(key))

fun Generator.getInferredTypeWithImplicitCasts(key: KtExpression): KotlinType? =
        context.bindingContext.getType(key)

fun Generator.getInferredTypeWithImplicitCastsOrFail(key: KtExpression): KotlinType =
        getInferredTypeWithImplicitCasts(key) ?: throw RuntimeException("No type for expression: ${key.text}")

fun Generator.getResolvedCall(key: KtElement): ResolvedCall<out CallableDescriptor>? =
        key.getResolvedCall(context.bindingContext)

fun Generator.createDummyExpression(ktExpression: KtExpression, description: String): IrErrorExpressionImpl =
        IrErrorExpressionImpl(ktExpression.startOffset, ktExpression.endOffset, getInferredTypeWithImplicitCastsOrFail(ktExpression), description)