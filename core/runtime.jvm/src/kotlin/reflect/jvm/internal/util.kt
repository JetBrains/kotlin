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

import java.lang.ref.WeakReference
import java.lang.reflect.Method
import java.util.Arrays
import kotlin.reflect.jvm.internal.pcollections.HashPMap

// TODO: use stdlib?
suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
private fun String.capitalizeWithJavaBeanConvention(): String {
    // The code is a bit crooked because otherwise there are overload resolution ambiguities caused by the fact
    // that we compile it with the built-ins both in source and as a compiled library
    val l = length
    if (l > 1 && Character.isUpperCase(get(1))) return this
    val first = get(0)
    this as java.lang.String
    return "" + Character.toUpperCase(first) + substring(1, l)
}

private fun getterName(propertyName: String): String = "get" + propertyName.capitalizeWithJavaBeanConvention()
private fun setterName(propertyName: String): String = "set" + propertyName.capitalizeWithJavaBeanConvention()


private fun Class<*>.getMaybeDeclaredMethod(name: String, vararg parameterTypes: Class<*>): Method {
    try {
        return getMethod(name, *parameterTypes)
    }
    catch (e: NoSuchMethodException) {
        // This is needed to support private methods
        return getDeclaredMethod(name, *parameterTypes)
    }
}


// TODO: collect nulls periodically
private var FOREIGN_K_CLASSES = HashPMap.empty<String, Array<WeakReference<KClassImpl<*>>>>()!!

fun <T> foreignKotlinClass(jClass: Class<T>): KClassImpl<T> {
    val name = jClass.getName()
    val cached = FOREIGN_K_CLASSES[name]
    if (cached != null) {
        var i = cached.size - 1
        while (i >= 0) {
            val kClass = cached[i].get()
            if (kClass?.jClass == jClass) {
                return kClass as KClassImpl<T>
            }
            i--
        }

        val newArray = Arrays.copyOf(cached, cached.size + 1) as Array<WeakReference<KClassImpl<*>>>
        val newKClass = KClassImpl<T>(jClass)
        newArray[cached.size] = WeakReference(newKClass)
        FOREIGN_K_CLASSES = FOREIGN_K_CLASSES.plus(name, newArray)!!
        return newKClass
    }

    val newKClass = KClassImpl<T>(jClass)
    val newArray = arrayOfNulls<WeakReference<KClassImpl<*>>>(1)
    newArray[0] = WeakReference(newKClass)
    FOREIGN_K_CLASSES = FOREIGN_K_CLASSES.plus(name, newArray as Array<WeakReference<KClassImpl<*>>>)!!
    return newKClass
}

private val K_OBJECT_CLASS = Class.forName("kotlin.jvm.internal.KObject")

fun <T> kotlinClass(jClass: Class<T>): KClassImpl<T> {
    if (K_OBJECT_CLASS.isAssignableFrom(jClass)) {
        val field = jClass.getDeclaredField("\$kotlinClass")
        return field.get(null) as KClassImpl<T>
    }
    // TODO: built-in classes
    return foreignKotlinClass(jClass)
}
