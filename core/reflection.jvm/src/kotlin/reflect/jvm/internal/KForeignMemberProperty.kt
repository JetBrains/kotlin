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

import java.lang.reflect.*
import kotlin.reflect.*

open class KForeignMemberProperty<T : Any, out R>(
        override val name: String,
        protected val owner: KClassImpl<T>
) : KMemberProperty<T, R>, KPropertyImpl<R> {
    override val field: Field = try {
        owner.jClass.getField(name)
    }
    catch (e: NoSuchFieldException) {
        throw NoSuchPropertyException(e)
    }

    override val getter: Method? get() = null

    override fun get(receiver: T): R {
        try {
            return field.get(receiver) as R
        }
        catch (e: IllegalAccessException) {
            throw IllegalPropertyAccessException(e)
        }
    }

    override fun equals(other: Any?): Boolean =
            other is KForeignMemberProperty<*, *> && name == other.name && owner == other.owner

    override fun hashCode(): Int =
            name.hashCode() * 31 + owner.hashCode()

    // TODO: include visibility, return type
    override fun toString(): String =
            "val ${owner.jClass.getName()}.$name"
}

class KMutableForeignMemberProperty<T : Any, R>(
        name: String,
        owner: KClassImpl<T>
) : KMutableMemberProperty<T, R>, KMutablePropertyImpl<R>, KForeignMemberProperty<T, R>(name, owner) {
    override val setter: Method? get() = null

    override fun set(receiver: T, value: R) {
        try {
            field.set(receiver, value)
        }
        catch (e: IllegalAccessException) {
            throw IllegalPropertyAccessException(e)
        }
    }

    override fun toString(): String =
            "var ${owner.jClass.getName()}.$name"
}
