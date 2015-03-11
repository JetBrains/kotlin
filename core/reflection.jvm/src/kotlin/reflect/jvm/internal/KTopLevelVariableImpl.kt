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

open class KTopLevelVariableImpl<out R>(
        override val name: String,
        protected val owner: KPackageImpl
) : KTopLevelVariable<R>, KVariableImpl<R> {
    // TODO: load the field from the corresponding package part
    override val field: Field? get() = null

    // TODO: extract, make lazy (weak?), use our descriptors knowledge, support Java fields
    override val getter: Method = try {
        owner.jClass.getMethod(getterName(name))
    }
    catch (e: NoSuchMethodException) {
        throw NoSuchPropertyException(e)
    }

    override fun get(): R {
        try {
            return getter(null) as R
        }
        catch (e: IllegalAccessException) {
            throw IllegalPropertyAccessException(e)
        }
    }

    override fun equals(other: Any?): Boolean =
            other is KTopLevelVariableImpl<*> && name == other.name && owner == other.owner

    override fun hashCode(): Int =
            name.hashCode() * 31 + owner.hashCode()

    // TODO: include visibility, return type, maybe package
    override fun toString(): String =
            "val $name"
}

class KMutableTopLevelVariableImpl<R>(
        name: String,
        owner: KPackageImpl
) : KMutableTopLevelVariable<R>, KMutableVariableImpl<R>, KTopLevelVariableImpl<R>(name, owner) {
    override val setter: Method = try {
        owner.jClass.getMethod(setterName(name), getter.getReturnType())
    }
    catch (e: NoSuchMethodException) {
        throw NoSuchPropertyException(e)
    }

    override fun set(value: R) {
        try {
            setter.invoke(null, value)
        }
        catch (e: IllegalAccessException) {
            throw IllegalPropertyAccessException(e)
        }
    }

    override fun toString(): String =
            "var $name"
}
