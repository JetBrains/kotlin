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

import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor
import org.jetbrains.kotlin.descriptors.impl.AnonymousFunctionDescriptor
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.util.OperatorNameConventions

/**
 * @returns type of first value parameter if function is 'operator handleResult' in coroutines controller
 */
fun SimpleFunctionDescriptor.getExpectedTypeForCoroutineControllerHandleResult(): KotlinType? {
    if (!isOperator || name != OperatorNameConventions.COROUTINE_HANDLE_RESULT) return null

    return valueParameters.getOrNull(0)?.type
}

val CallableDescriptor.controllerTypeIfCoroutine: KotlinType?
    get() {
        if (this !is AnonymousFunctionDescriptor || !this.isCoroutine) return null

        return this.extensionReceiverParameter?.returnType
    }
