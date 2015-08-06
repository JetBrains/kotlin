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

package kotlin.reflect.jvm.internal

import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.annotations.Annotated
import java.util.ArrayList
import kotlin.reflect.KCallable
import kotlin.reflect.KParameter
import kotlin.reflect.KType

interface KCallableImpl<out R> : KCallable<R>, KAnnotatedElementImpl {
    val descriptor: CallableMemberDescriptor

    override val annotated: Annotated get() = descriptor

    override val parameters: List<KParameter>
        get() {
            val result = ArrayList<KParameter>()
            var index = 0

            if (descriptor.dispatchReceiverParameter != null) {
                result.add(KParameterImpl(index++) { descriptor.dispatchReceiverParameter!! })
            }

            if (descriptor.extensionReceiverParameter != null) {
                result.add(KParameterImpl(index++) { descriptor.extensionReceiverParameter!! })
            }

            for (i in descriptor.valueParameters.indices) {
                result.add(KParameterImpl(index++) { descriptor.valueParameters[i] })
            }

            result.trimToSize()
            return result
        }

    override val returnType: KType
        get() = KTypeImpl(descriptor.returnType!!)
}
