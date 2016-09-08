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
import java.lang.reflect.Modifier
import java.lang.reflect.Type
import java.lang.reflect.Constructor as ReflectConstructor
import java.lang.reflect.Field as ReflectField
import java.lang.reflect.Method as ReflectMethod

internal abstract class FunctionCaller<out M : Member>(
        internal val member: M,
        internal val returnType: Type,
        internal val instanceClass: Class<*>?,
        valueParameterTypes: Array<Type>
) {
    val parameterTypes: List<Type> =
            instanceClass?.let { listOf(it, *valueParameterTypes) } ?:
            valueParameterTypes.toList()

    val arity: Int
        get() = parameterTypes.size

    abstract fun call(args: Array<*>): Any?

    protected open fun checkArguments(args: Array<*>) {
        if (arity != args.size) {
            throw IllegalArgumentException("Callable expects $arity arguments, but ${args.size} were provided.")
        }
    }

    protected fun checkObjectInstance(obj: Any?) {
        if (obj == null || !member.declaringClass.isInstance(obj)) {
            throw IllegalArgumentException("An object member requires the object instance passed as the first argument.")
        }
    }

    // Constructors

    class Constructor(constructor: ReflectConstructor<*>) : FunctionCaller<ReflectConstructor<*>>(
            constructor,
            constructor.declaringClass,
            constructor.declaringClass.let { klass ->
                val outerClass = klass.declaringClass
                if (outerClass != null && !Modifier.isStatic(klass.modifiers)) outerClass else null
            },
            constructor.genericParameterTypes
    ) {
        override fun call(args: Array<*>): Any? {
            checkArguments(args)
            return member.newInstance(*args)
        }
    }

    // Methods

    abstract class Method(
            method: ReflectMethod,
            requiresInstance: Boolean = !Modifier.isStatic(method.modifiers)
    ) : FunctionCaller<ReflectMethod>(
            method,
            method.genericReturnType,
            if (requiresInstance) method.declaringClass else null,
            method.genericParameterTypes
    ) {
        private val isVoidMethod = returnType == Void.TYPE

        protected fun callMethod(instance: Any?, args: Array<*>): Any? {
            val result = member.invoke(instance, *args)

            // If this is a Unit function, the method returns void, Method#invoke returns null, while we should return Unit
            return if (isVoidMethod) Unit else result
        }
    }

    class StaticMethod(method: ReflectMethod) : Method(method) {
        override fun call(args: Array<*>): Any? {
            checkArguments(args)
            return callMethod(null, args)
        }
    }

    class InstanceMethod(method: ReflectMethod) : Method(method) {
        override fun call(args: Array<*>): Any? {
            checkArguments(args)
            return callMethod(args[0], args.copyOfRange(1, args.size))
        }
    }

    class JvmStaticInObject(method: ReflectMethod) : Method(method, requiresInstance = true) {
        override fun call(args: Array<*>): Any? {
            checkArguments(args)
            checkObjectInstance(args.firstOrNull())
            return callMethod(null, args.copyOfRange(1, args.size))
        }
    }

    // Field accessors

    abstract class FieldGetter(
            field: ReflectField,
            requiresInstance: Boolean = !Modifier.isStatic(field.modifiers)
    ) : FunctionCaller<ReflectField>(
            field,
            field.genericType,
            if (requiresInstance) field.declaringClass else null,
            emptyArray()
    ) {
        override fun call(args: Array<*>): Any? {
            checkArguments(args)
            return member.get(if (instanceClass != null) args.first() else null)
        }
    }

    abstract class FieldSetter(
            field: ReflectField,
            private val notNull: Boolean,
            requiresInstance: Boolean = !Modifier.isStatic(field.modifiers)
    ) : FunctionCaller<ReflectField>(
            field,
            Void.TYPE,
            if (requiresInstance) field.declaringClass else null,
            arrayOf(field.genericType)
    ) {
        override fun checkArguments(args: Array<*>) {
            super.checkArguments(args)
            if (notNull && args.last() == null) {
                throw IllegalArgumentException("null is not allowed as a value for this property.")
            }
        }

        override fun call(args: Array<*>): Any? {
            checkArguments(args)
            return member.set(if (instanceClass != null) args.first() else null, args.last())
        }
    }

    class StaticFieldGetter(field: ReflectField) : FieldGetter(field)

    class InstanceFieldGetter(field: ReflectField) : FieldGetter(field)

    class JvmStaticInObjectFieldGetter(field: ReflectField) : FieldGetter(field, requiresInstance = true) {
        override fun checkArguments(args: Array<*>) {
            super.checkArguments(args)
            checkObjectInstance(args.firstOrNull())
        }
    }

    class ClassCompanionFieldGetter(
            field: ReflectField,
            klass: Class<*>
    ) : FunctionCaller<ReflectField>(
            field,
            field.genericType,
            klass,
            emptyArray()
    ) {
        override fun call(args: Array<*>): Any? {
            checkArguments(args)
            return member.get(args.first())
        }
    }

    class StaticFieldSetter(field: ReflectField, notNull: Boolean) : FieldSetter(field, notNull)

    class InstanceFieldSetter(field: ReflectField, notNull: Boolean) : FieldSetter(field, notNull)

    class JvmStaticInObjectFieldSetter(field: ReflectField, notNull: Boolean) : FieldSetter(field, notNull, requiresInstance = true) {
        override fun checkArguments(args: Array<*>) {
            super.checkArguments(args)
            checkObjectInstance(args.firstOrNull())
        }
    }

    class ClassCompanionFieldSetter(
            field: ReflectField,
            klass: Class<*>
    ) : FunctionCaller<ReflectField>(
            field,
            Void.TYPE,
            klass,
            arrayOf(field.genericType)
    ) {
        override fun call(args: Array<*>): Any? {
            checkArguments(args)
            return member.set(instanceClass, args.last())
        }
    }
}
