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
import java.lang.reflect.Method as ReflectMethod

internal sealed class FunctionCaller(
        private val expectedArgumentNumber: Int
) {
    abstract val member: Member

    abstract fun call(args: Array<*>): Any?

    protected open fun checkArguments(args: Array<*>) {
        if (expectedArgumentNumber != args.size()) {
            throw IllegalArgumentException("Callable expects $expectedArgumentNumber arguments, but ${args.size()} were provided.")
        }
    }

    protected fun checkObjectInstance(obj: Any?) {
        if (obj == null || !member.declaringClass.isInstance(obj)) {
            throw IllegalArgumentException("An object member requires the object instance passed as the first argument.")
        }
    }

    class Constructor(val constructor: ReflectConstructor<*>) : FunctionCaller(constructor.parameterTypes.size()) {
        override val member: Member get() = constructor

        override fun call(args: Array<*>): Any? {
            checkArguments(args)
            return constructor.newInstance(*args)
        }
    }

    abstract class Method(
            val method: ReflectMethod,
            requiresInstance: Boolean
    ) : FunctionCaller(method.parameterTypes.size() + (if (requiresInstance) 1 else 0)) {
        override val member: Member get() = method
        private val isVoidMethod = method.returnType == Void.TYPE

        protected fun callMethod(instance: Any?, args: Array<*>): Any? {
            val result = method.invoke(instance, *args)

            // If this is a Unit function, the method returns void, Method#invoke returns null, while we should return Unit
            return if (isVoidMethod) Unit else result
        }
    }

    class StaticMethod(method: ReflectMethod) : Method(method, requiresInstance = false) {
        override fun call(args: Array<*>): Any? {
            checkArguments(args)
            return callMethod(null, args)
        }
    }

    class InstanceMethod(method: ReflectMethod) : Method(method, requiresInstance = true) {
        override fun call(args: Array<*>): Any? {
            checkArguments(args)
            return callMethod(args[0], args.asList().subList(1, args.size()).toTypedArray())
        }
    }

    class PlatformStaticInObject(method: ReflectMethod) : Method(method, requiresInstance = true) {
        override fun call(args: Array<*>): Any? {
            checkArguments(args)
            checkObjectInstance(args.firstOrNull())
            return callMethod(null, args.asList().subList(1, args.size()).toTypedArray())
        }
    }

    abstract class FieldAccessor(val field: ReflectField, expectedArgumentNumber: Int) : FunctionCaller(expectedArgumentNumber) {
        override val member: Member get() = field
    }

    abstract class FieldGetter(
            field: ReflectField,
            private val requiresInstance: Boolean
    ) : FieldAccessor(field, if (requiresInstance) 1 else 0) {
        override fun call(args: Array<*>): Any? {
            checkArguments(args)
            return field.get(if (requiresInstance) args.first() else null)
        }
    }

    abstract class FieldSetter(
            field: ReflectField,
            private val requiresInstance: Boolean
    ) : FieldAccessor(field, if (requiresInstance) 2 else 1) {
        override fun call(args: Array<*>): Any? {
            checkArguments(args)
            return field.set(if (requiresInstance) args.first() else null, args.last())
        }
    }

    class StaticFieldGetter(field: ReflectField) : FieldGetter(field, requiresInstance = false)

    class InstanceFieldGetter(field: ReflectField) : FieldGetter(field, requiresInstance = true)

    class PlatformStaticInObjectFieldGetter(field: ReflectField) : FieldGetter(field, requiresInstance = true) {
        override fun checkArguments(args: Array<*>) {
            super.checkArguments(args)
            checkObjectInstance(args.firstOrNull())
        }
    }

    class StaticFieldSetter(field: ReflectField) : FieldSetter(field, requiresInstance = false)

    class InstanceFieldSetter(field: ReflectField) : FieldSetter(field, requiresInstance = true)

    class PlatformStaticInObjectFieldSetter(field: ReflectField) : FieldSetter(field, requiresInstance = true) {
        override fun checkArguments(args: Array<*>) {
            super.checkArguments(args)
            checkObjectInstance(args.firstOrNull())
        }
    }
}
