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

open class KTopLevelExtensionPropertyImpl<T, out R>(
        override val name: String,
        protected val owner: KPackageImpl,
        protected val receiverClass: Class<T>
) : KTopLevelExtensionProperty<T, R>, KPropertyImpl<R> {
    override val field: Field? get() = null

    // TODO: extract, make lazy (weak?), use our descriptors knowledge, support Java fields
    override val getter: Method = try {
        owner.jClass.getMethod(getterName(name), receiverClass)
    }
    catch (e: NoSuchMethodException) {
        throw NoSuchPropertyException(e)
    }

    override fun get(receiver: T): R {
        try {
            return getter(null, receiver) as R
        }
        catch (e: IllegalAccessException) {
            throw IllegalPropertyAccessException(e)
        }
    }

    override fun equals(other: Any?): Boolean =
            other is KTopLevelExtensionPropertyImpl<*, *> && name == other.name && owner == other.owner && receiverClass == other.receiverClass

    override fun hashCode(): Int =
            (name.hashCode() * 31 + owner.hashCode()) * 31 + receiverClass.hashCode()

    // TODO: include visibility, return type, maybe package
    override fun toString(): String =
            "val ${mapJavaClassToKotlin(receiverClass.getName())}.$name"
}

class KMutableTopLevelExtensionPropertyImpl<T, R>(
        name: String,
        owner: KPackageImpl,
        receiverClass: Class<T>
) : KMutableTopLevelExtensionProperty<T, R>, KMutablePropertyImpl<R>, KTopLevelExtensionPropertyImpl<T, R>(name, owner, receiverClass) {
    override val setter: Method = try {
        owner.jClass.getMethod(setterName(name), receiverClass, getter.getReturnType())
    }
    catch (e: NoSuchMethodException) {
        throw NoSuchPropertyException(e)
    }

    override fun set(receiver: T, value: R) {
        try {
            setter.invoke(null, receiver, value)
        }
        catch (e: IllegalAccessException) {
            throw IllegalPropertyAccessException(e)
        }
    }

    override fun toString(): String =
            "var ${mapJavaClassToKotlin(receiverClass.getName())}.$name"
}

private fun mapJavaClassToKotlin(name: String): String {
    if (name[0].isLowerCase()) {
        return when (name) {
            "boolean" -> "kotlin.Boolean"
            "char" -> "kotlin.Char"
            "byte" -> "kotlin.Byte"
            "short" -> "kotlin.Short"
            "int" -> "kotlin.Int"
            "float" -> "kotlin.Float"
            "long" -> "kotlin.Long"
            "double" -> "kotlin.Double"
            else -> name
        }
    }
    if (name[0] == '[') {
        val element = name.substring(1)
        return when (element[0]) {
            'Z' -> "kotlin.BooleanArray"
            'C' -> "kotlin.CharArray"
            'B' -> "kotlin.ByteArray"
            'S' -> "kotlin.ShortArray"
            'I' -> "kotlin.IntArray"
            'F' -> "kotlin.FloatArray"
            'J' -> "kotlin.LongArray"
            'D' -> "kotlin.DoubleArray"
            'L' -> "kotlin.Array<${mapJavaClassToKotlin(element.substring(1, element.length() - 1))}>"
            else -> "kotlin.Array<${mapJavaClassToKotlin(element)}>"
        }
    }
    return name
}
