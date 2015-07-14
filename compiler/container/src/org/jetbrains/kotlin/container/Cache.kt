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

package org.jetbrains.kotlin.container

import com.intellij.util.containers.ContainerUtil
import java.lang.reflect.*
import java.util.ArrayList
import java.util.LinkedHashSet

private object ClassTraversalCache {
    private val cache = ContainerUtil.createConcurrentWeakKeySoftValueMap<Class<*>, ClassInfo>()

    fun getClassInfo(c: Class<*>): ClassInfo {
        val classInfo = cache.get(c)
        if (classInfo == null) {
            val newClassInfo = traverseClass(c)
            cache.put(c, newClassInfo)
            return newClassInfo
        }
        return classInfo
    }
}

fun Class<*>.getInfo(): ClassInfo {
    return ClassTraversalCache.getClassInfo(this)
}

data class ClassInfo(
        val constructorInfo: ConstructorInfo?,
        val setterInfos: List<SetterInfo>,
        val registrations: List<Type>
)

data class ConstructorInfo(
        val constructor: Constructor<*>,
        val parameters: List<Type>
)

data class SetterInfo(
        val method: Method,
        val parameters: List<Type>
)

private fun traverseClass(c: Class<*>): ClassInfo {
    return ClassInfo(getConstructorInfo(c), getSetterInfos(c), getRegistrations(c))
}

private fun getSetterInfos(c: Class<*>): List<SetterInfo> {
    val setterInfos = ArrayList<SetterInfo>()
    for (method in c.getMethods()) {
        for (annotation in method.getDeclaredAnnotations()) {
            if (annotation.annotationType().getName().endsWith(".Inject")) {
                setterInfos.add(SetterInfo(method, method.getGenericParameterTypes().toList()))
            }
        }
    }
    return setterInfos
}

private fun getConstructorInfo(c: Class<*>): ConstructorInfo? {
    if (Modifier.isAbstract(c.getModifiers()) || c.isPrimitive())
        return null

    val constructors = c.getConstructors()
    val hasSinglePublicConstructor = constructors.singleOrNull()?.let { Modifier.isPublic(it.getModifiers()) } ?: false
    if (!hasSinglePublicConstructor)
        return null

    val constructor = constructors.single()
    return ConstructorInfo(constructor, constructor.getGenericParameterTypes().toList())
}


private fun collectInterfacesRecursive(type: Type, result: MutableSet<Type>) {
    // TODO: should apply generic substitution through hierarchy
    val klass : Class<*>? = when(type) {
        is Class<*> -> type
        is ParameterizedType -> type.getRawType() as? Class<*>
        else -> null
    }
    klass?.getGenericInterfaces()?.forEach {
        if (result.add(it)) {
            collectInterfacesRecursive(it, result)
        }
    }
}

private fun getRegistrations(klass: Class<*>): List<Type> {
    val registrations = ArrayList<Type>()

    val superClasses = sequence<Type>(klass) {
        when (it) {
            is Class<*> -> it.getGenericSuperclass()
            is ParameterizedType -> (it.getRawType() as? Class<*>)?.getGenericSuperclass()
            else -> null
        }
    }
    registrations.addAll(superClasses)

    val interfaces = LinkedHashSet<Type>()
    superClasses.forEach { collectInterfacesRecursive(it, interfaces) }
    registrations.addAll(interfaces)
    registrations.remove(javaClass<Any>())
    return registrations
}