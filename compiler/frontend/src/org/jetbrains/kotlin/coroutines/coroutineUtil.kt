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

package org.jetbrains.kotlin.coroutines

import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameUnsafe
import org.jetbrains.kotlin.types.KotlinType

@JvmField
val CONTINUATION_INTERFACE_FQ_NAME = FqName("kotlin.coroutines.Continuation")

@JvmField
val HANDLE_RESULT_NAME = Name.identifier("handleResult")

/**
 * @returns type of first value parameter if function is 'operator handleResult' in coroutine controller
 */
fun SimpleFunctionDescriptor.getExpectedTypeForCoroutineControllerHandleResult(): KotlinType? {
    if (name != HANDLE_RESULT_NAME) return null
    if (valueParameters.size != 2) return null

    if (valueParameters[1].type.constructor.declarationDescriptor?.fqNameUnsafe != CONTINUATION_INTERFACE_FQ_NAME.toUnsafe()) return null

    return valueParameters[0].type
}
