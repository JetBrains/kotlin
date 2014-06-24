/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

import java.lang.reflect.Method

open class KExtensionPropertyImpl<T, out R>(
        public override val name: String,
        protected val owner: KPackageImpl,
        protected val receiverClass: Class<T>
) : KExtensionProperty<T, R>, KPropertyImpl<R> {
    // TODO: extract, make lazy (weak?), use our descriptors knowledge, support Java fields
    val getter: Method = owner.jClass.getMethod(getterName(name), receiverClass)

    override fun get(receiver: T): R {
        return getter(null, receiver) as R
    }
}

class KMutableExtensionPropertyImpl<T, R>(
        name: String,
        owner: KPackageImpl,
        receiverClass: Class<T>
) : KMutableExtensionProperty<T, R>, KMutablePropertyImpl<R>, KExtensionPropertyImpl<T, R>(name, owner, receiverClass) {
    val setter = owner.jClass.getMethod(setterName(name), receiverClass, getter.getReturnType()!!)

    override fun set(receiver: T, value: R) {
        setter.invoke(null, receiver, value)
    }
}
