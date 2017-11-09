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

package org.jetbrains.kotlin.codegen.intrinsics

import org.jetbrains.kotlin.codegen.AsmUtil
import org.jetbrains.kotlin.codegen.Callable
import org.jetbrains.kotlin.codegen.CallableMethod
import org.jetbrains.kotlin.resolve.jvm.AsmTypes.OBJECT_TYPE
import org.jetbrains.org.objectweb.asm.Type

internal val equalsMethodDescriptor: String =
        Type.getMethodDescriptor(Type.BOOLEAN_TYPE, Type.getType(Any::class.java));

class Equals : IntrinsicMethod() {
    override fun toCallable(method: CallableMethod): Callable =
            createBinaryIntrinsicCallable(
                    method.returnType,
                    OBJECT_TYPE,
                    nullOrObject(method.dispatchReceiverType),
                    nullOrObject(method.extensionReceiverType)
            ) {
                AsmUtil.genAreEqualCall(it)
            }
}

class EqualsThrowingNpeForNullReceiver(private val lhsType: Type) : IntrinsicMethod() {
    override fun toCallable(method: CallableMethod): Callable =
            createBinaryIntrinsicCallable(
                    method.returnType,
                    OBJECT_TYPE,
                    nullOrObject(method.dispatchReceiverType),
                    nullOrObject(method.extensionReceiverType)
            ) {
                it.invokevirtual(lhsType.internalName, "equals", equalsMethodDescriptor, false)
            }
}
