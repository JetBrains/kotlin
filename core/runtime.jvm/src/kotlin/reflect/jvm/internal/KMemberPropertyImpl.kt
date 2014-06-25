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

import java.lang.reflect.*

open class KMemberPropertyImpl<T : Any, out R>(
        public override val name: String,
        protected val owner: KClassImpl<T>
) : KMemberProperty<T, R>, KPropertyImpl<R> {
    override val field: Field? =
            if (owner.origin == KClassOrigin.FOREIGN) {
                owner.jClass.getField(name)
            }
            else null

    // TODO: extract, make lazy (weak?), use our descriptors knowledge
    override val getter: Method? =
            if (owner.origin == KClassOrigin.KOTLIN) {
                owner.jClass.getMaybeDeclaredMethod(getterName(name))
            }
            else null

    // TODO: built-in classes
    override fun get(receiver: T): R {
        if (getter != null) {
            return getter!!(receiver) as R
        }
        return field!!.get(receiver) as R
    }
}

class KMutableMemberPropertyImpl<T : Any, R>(
        name: String,
        owner: KClassImpl<T>
) : KMutableMemberProperty<T, R>, KMutablePropertyImpl<R>, KMemberPropertyImpl<T, R>(name, owner) {
    override val setter: Method? =
            if (owner.origin == KClassOrigin.KOTLIN) {
                owner.jClass.getMaybeDeclaredMethod(setterName(name), getter!!.getReturnType()!!)
            }
            else null

    override fun set(receiver: T, value: R) {
        if (setter != null) {
            setter!!(receiver, value)
            return
        }
        field!!.set(receiver, value)
    }
}
