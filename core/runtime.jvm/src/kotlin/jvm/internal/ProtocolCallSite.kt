/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

@file:JvmName("ProtocolCallSite")

package kotlin.jvm.internal

import java.lang.invoke.*
import java.lang.reflect.Method
import kotlin.jvm.internal.cache.Cache.Companion.makeCache
import kotlin.jvm.internal.cache.SmallArrayCache

class ProtocolCallSite(private val lookup: MethodHandles.Lookup, name: String, type: MethodType, private val callableName: String, private val callableType: MethodType, cacheType: Int, cacheSize: Int) {
    private val indyCache = makeCache<Class<*>, MethodHandle>(cacheType, cacheSize)
    private var reflectCache = makeCache<Class<*>, Method>(cacheType, cacheSize)

    companion object {
        @JvmStatic
        fun getBootstrap(lookup: MethodHandles.Lookup, name: String, type: MethodType, callableName: String, callableType: MethodType, cacheType: Int, cacheSize: Int): CallSite {
            val instance = ProtocolCallSite(lookup, name, type, callableName, callableType, cacheType, cacheSize)
            return ConstantCallSite(MethodHandles.constant(ProtocolCallSite::class.java, instance))
        }
    }

    fun getMethod(receiver: Any): MethodHandle? {
        val receiverClass = receiver.javaClass
        val cached = indyCache[receiverClass]

        if (cached != null) {
            return cached
        }

        val method = lookup.unreflect(resolveMethod(receiverClass))
        indyCache[receiverClass] = method

        return method
    }

    fun getReflectMethod(receiver: Any): Method? {
        val receiverClass = receiver.javaClass
        val cached = reflectCache[receiverClass]

        if (cached != null) {
            return cached
        }

        val method = resolveMethod(receiverClass)!!
        reflectCache[receiverClass] = method
        return method
    }

    private fun resolveMethod(target: Class<*>): Method? {
        val protocolArgs = callableType.parameterArray()
        val methods = target.declaredMethods

        var candidate: Method? = null
        var bestDistance = IntArray(protocolArgs.size)

        for (method in methods) {
            if (method.name != callableName || method.parameterCount != protocolArgs.size) {
                continue
            }

            val methodArgs = method.parameterTypes

            val distance = IntArray(protocolArgs.size)
            var fail = false
            for (i in 0..protocolArgs.size - 1) {
                val argDistance = argumentDistance(methodArgs[i], protocolArgs[i])
                if (argDistance < 0) {
                    fail = true
                    break
                }

                distance[i] = argDistance
            }

            if (fail.not() && (candidate == null || bestDistance.greater(distance))) {
                bestDistance = distance
                candidate = method
            }
        }

        return candidate
    }

    private fun argumentDistance(lhs: Class<*>, rhs: Class<*>): Int {
        if (lhs == rhs) {
            return 0
        }

        var convertedLhs = lhs
        var convertedRhs = rhs
        var distance = 1
        if (lhs.isPrimitive xor rhs.isPrimitive) {
            ++distance
            convertedLhs = convertedLhs.boxed()
            convertedRhs = convertedRhs.boxed()
        }

        if (convertedLhs.isAssignableFrom(convertedRhs)) {
            return distance
        }

        return -1
    }

    private fun IntArray.greater(other: IntArray): Boolean {
        for (i in 0..other.size - 1) {
            if (get(i) < other[i]) {
                return false
            }
        }

        return true
    }

    private fun Class<*>.boxed(): Class<*> = when (this) {
        Boolean::class.javaPrimitiveType -> Boolean::class.javaObjectType
        Byte::class.javaPrimitiveType -> Byte::class.javaObjectType
        Char::class.javaPrimitiveType -> Char::class.javaObjectType
        Double::class.javaPrimitiveType -> Double::class.javaObjectType
        Float::class.javaPrimitiveType -> Float::class.javaObjectType
        Int::class.javaPrimitiveType -> Int::class.javaObjectType
        Long::class.javaPrimitiveType -> Long::class.javaObjectType
        Short::class.javaPrimitiveType -> Short::class.javaObjectType
        else -> this
    }

}
