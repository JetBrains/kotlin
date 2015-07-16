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

import java.lang.reflect.Constructor as ReflectConstructor
import java.lang.reflect.Method as ReflectMethod

internal sealed class FunctionCaller {
    abstract fun call(args: Array<*>): Any?

    class Constructor(val constructor: ReflectConstructor<*>) : FunctionCaller() {
        override fun call(args: Array<*>): Any? {
            return constructor.newInstance(*args)
        }
    }

    abstract class Method(val method: ReflectMethod) : FunctionCaller() {
        private val isVoidMethod = method.returnType == Void.TYPE

        protected fun callMethod(instance: Any?, args: Array<*>): Any? {
            val result = method.invoke(instance, *args)

            // If this is a Unit function, the method returns void, Method#invoke returns null, while we should return Unit
            return if (isVoidMethod) Unit else result
        }
    }

    class StaticMethod(method: ReflectMethod) : Method(method) {
        override fun call(args: Array<*>): Any? {
            return callMethod(null, args)
        }
    }

    class InstanceMethod(method: ReflectMethod) : Method(method) {
        override fun call(args: Array<*>): Any? {
            return callMethod(args[0], args.asList().subList(1, args.size()).toTypedArray())
        }
    }

    class PlatformStaticInObject(method: ReflectMethod) : Method(method) {
        override fun call(args: Array<*>): Any? {
            if (args.isEmpty() || !method.declaringClass.isInstance(args[0])) {
                throw IllegalArgumentException("A function in an object requires the object instance passed as the first argument.")
            }
            return callMethod(null, args.asList().subList(1, args.size()).toTypedArray())
        }
    }
}
