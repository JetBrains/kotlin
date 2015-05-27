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

package org.jetbrains.kotlin.load.kotlin

import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.load.java.JvmAnnotationNames
import org.jetbrains.kotlin.resolve.calls.model.ExpressionValueArgument
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.ResolvedValueArgument

fun getJavaAnnotationCallValueArgumentsThatShouldBeNamed(resolvedCall: ResolvedCall<*>): Map<ValueParameterDescriptor, ResolvedValueArgument> =
    resolvedCall.getValueArguments().filter {
        p ->
        p.key.getName() != JvmAnnotationNames.DEFAULT_ANNOTATION_MEMBER_NAME &&
        p.value is ExpressionValueArgument &&
        !((p.value as ExpressionValueArgument).getValueArgument()?.isNamed() ?: true)
    }

