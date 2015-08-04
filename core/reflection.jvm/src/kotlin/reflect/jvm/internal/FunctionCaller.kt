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

import java.lang.reflect.Member
import java.lang.reflect.Constructor as ReflectConstructor
import java.lang.reflect.Field as ReflectField
import java.lang.reflect.Member as ReflectMember
import java.lang.reflect.Method as ReflectMethod

internal sealed class FunctionCaller {
    abstract val member: ReflectMember

    abstract fun call(args: Array<*>): Any?

    protected fun checkObjectInstance(obj: Any?) {
        if (obj == null || !member.declaringClass.isInstance(obj)) {
            throw IllegalArgumentException("An object member requires the object instance passed as the first argument.")
        }
    }

    class Constructor(val constructor: ReflectConstructor<*>) : FunctionCaller() {
        override val member: Member get() = constructor

        override fun call(args: Array<*>): Any? =
                constructor.newInstance(*args)
    }

    abstract class Method(val method: ReflectMethod) : FunctionCaller() {
        override val member: Member get() = method
        private val isVoidMethod = method.returnType == Void.TYPE

        protected fun callMethod(instance: Any?, args: Array<*>): Any? {
            val result = method.invoke(instance, *args)

            // If this is a Unit function, the method returns void, Method#invoke returns null, while we should return Unit
            return if (isVoidMethod) Unit else result
        }
    }

    class StaticMethod(method: ReflectMethod) : Method(method) {
        override fun call(args: Array<*>): Any? =
                callMethod(null, args)
    }

    class InstanceMethod(method: ReflectMethod) : Method(method) {
        override fun call(args: Array<*>): Any? =
                callMethod(args[0], args.asList().subList(1, args.size()).toTypedArray())
    }

    class PlatformStaticInObject(method: ReflectMethod) : Method(method) {
        override fun call(args: Array<*>): Any? {
            checkObjectInstance(args.firstOrNull())
            return callMethod(null, args.asList().subList(1, args.size()).toTypedArray())
        }
    }

    abstract class Field(val field: ReflectField) : FunctionCaller() {
        override val member: Member get() = field
    }

    class StaticFieldGetter(field: ReflectField) : Field(field) {
        override fun call(args: Array<*>): Any? =
                field.get(null)
    }

    class InstanceFieldGetter(field: ReflectField) : Field(field) {
        override fun call(args: Array<*>): Any? =
                field.get(args[0])
    }

    class PlatformStaticInObjectFieldGetter(field: ReflectField) : Field(field) {
        override fun call(args: Array<*>): Any? {
            checkObjectInstance(args.firstOrNull())
            return field.get(null)
        }
    }

    class StaticFieldSetter(field: ReflectField) : Field(field) {
        override fun call(args: Array<*>): Any? =
                field.set(null, args[0])
    }

    class InstanceFieldSetter(field: ReflectField) : Field(field) {
        override fun call(args: Array<*>): Any? =
                field.set(args[0], args[1])
    }

    class PlatformStaticInObjectFieldSetter(field: ReflectField) : Field(field) {
        override fun call(args: Array<*>): Any? {
            checkObjectInstance(args.firstOrNull())
            return field.set(null, args[1])
        }
    }
}
