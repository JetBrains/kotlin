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

class ProtocolCallSite(private val lookup: MethodHandles.Lookup, name: String, type: MethodType, private val callableName: String, private val callableType: MethodType) {
    private var classCache: Class<*>? = null
    private var indyCache: MethodHandle? = null
    private var reflectCache: Method? = null

    companion object {
        @JvmStatic
        fun getBootstrap(lookup: MethodHandles.Lookup, name: String, type: MethodType, callableName: String, callableType: MethodType): CallSite {
            val instance = ProtocolCallSite(lookup, name, type, callableName, callableType)
            return ConstantCallSite(MethodHandles.constant(ProtocolCallSite::class.java, instance))
        }
    }

    fun getMethod(receiver: Any): MethodHandle? {
        val receiverClass = receiver.javaClass
        if (classCache != null && classCache == receiverClass) {
            return indyCache
        }

        classCache = receiverClass
        val method = resolveMethod(receiverClass)
        setupAccessible(method)
        indyCache = lookup.unreflect(method)
        return indyCache
    }

    fun getReflectMethod(receiver: Any): Method? {
        val receiverClass = receiver.javaClass
        if (classCache == null || classCache != receiverClass) {
            classCache = receiverClass
            reflectCache = resolveMethod(receiverClass)
            setupAccessible(reflectCache!!)
        }

        return reflectCache
    }

    private fun setupAccessible(method: Method) {
        if (!method.isAccessible) {
            method.isAccessible = true
        }
    }

    private fun resolveMethod(cls: Class<*>): Method {
        try {
            return cls.getDeclaredMethod(callableName, *callableType.parameterArray())
        } catch (e: NoSuchMethodException) {
            return findOverload(cls, callableName, callableType)
        }
    }

    private fun findOverload(target: Class<*>, callableName: String, callableType: MethodType): Method {
        val args = callableType.parameterArray()
        val methods = target.declaredMethods

        var candidate: Method? = null
        var bestDistance = Int.MAX_VALUE

        for (method in methods) {
            if (method.name != callableName || method.parameterCount != args.size) {
                continue
            }

            val parameters = method.parameters

            var i = 0
            var distance = 0
            while (i < args.size) {
                if (args[i] == parameters[i].type) {
                    continue
                } else if (args[i].isAssignableFrom(parameters[i].type)) {
                    distance++
                } else {
                    distance = -1
                    break
                }

                ++i
            }

            if (distance < bestDistance) {
                bestDistance = distance
                candidate = method
            }

            if (bestDistance == 0) {
                break
            }
        }

        return candidate!!
    }
}
