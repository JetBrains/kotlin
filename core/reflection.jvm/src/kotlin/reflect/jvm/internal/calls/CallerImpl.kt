/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlin.reflect.jvm.internal.calls

import java.lang.reflect.Member
import java.lang.reflect.Modifier
import java.lang.reflect.Type
import java.lang.reflect.Constructor as ReflectConstructor
import java.lang.reflect.Field as ReflectField
import java.lang.reflect.Method as ReflectMethod

internal abstract class CallerImpl<out M : Member>(
    final override val member: M,
    final override val returnType: Type,
    val instanceClass: Class<*>?,
    valueParameterTypes: Array<Type>
) : Caller<M> {
    override val parameterTypes: List<Type> =
        instanceClass?.let { listOf(it, *valueParameterTypes) } ?: valueParameterTypes.toList()

    protected fun checkObjectInstance(obj: Any?) {
        if (obj == null || !member.declaringClass.isInstance(obj)) {
            throw IllegalArgumentException("An object member requires the object instance passed as the first argument.")
        }
    }

    // Constructors

    class Constructor(constructor: ReflectConstructor<*>) : CallerImpl<ReflectConstructor<*>>(
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

    // TODO fix 'callBy' for bound (and non-bound) inner class constructor references
    // See https://youtrack.jetbrains.com/issue/KT-14990
    class BoundConstructor(constructor: ReflectConstructor<*>, private val boundReceiver: Any?) :
        CallerImpl<ReflectConstructor<*>>(
            constructor, constructor.declaringClass, null,
            constructor.genericParameterTypes
        ) {
        override fun call(args: Array<*>): Any? {
            checkArguments(args)
            return member.newInstance(boundReceiver, *args)
        }
    }

    // Methods

    abstract class Method(
        method: ReflectMethod,
        requiresInstance: Boolean = !Modifier.isStatic(method.modifiers),
        parameterTypes: Array<Type> = method.genericParameterTypes
    ) : CallerImpl<ReflectMethod>(
        method,
        method.genericReturnType,
        if (requiresInstance) method.declaringClass else null,
        parameterTypes
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
            return callMethod(args[0], args.dropFirst())
        }
    }

    class JvmStaticInObject(method: ReflectMethod) : Method(method, requiresInstance = true) {
        override fun call(args: Array<*>): Any? {
            checkArguments(args)
            checkObjectInstance(args.firstOrNull())
            return callMethod(null, args.dropFirst())
        }
    }

    class BoundStaticMethod(method: ReflectMethod, private val boundReceiver: Any?) : Method(
        method, requiresInstance = false, parameterTypes = method.genericParameterTypes.dropFirst()
    ) {
        override fun call(args: Array<*>): Any? {
            checkArguments(args)
            return callMethod(null, arrayOf(boundReceiver, *args))
        }
    }

    class BoundInstanceMethod(method: ReflectMethod, private val boundReceiver: Any?) : Method(method, requiresInstance = false) {
        override fun call(args: Array<*>): Any? {
            checkArguments(args)
            return callMethod(boundReceiver, args)
        }
    }

    class BoundJvmStaticInObject(method: ReflectMethod) : Method(method, requiresInstance = false) {
        override fun call(args: Array<*>): Any? {
            checkArguments(args)
            return callMethod(null, args)
        }
    }

    // Field accessors

    abstract class FieldGetter(
        field: ReflectField,
        requiresInstance: Boolean
    ) : CallerImpl<ReflectField>(
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
        requiresInstance: Boolean
    ) : CallerImpl<ReflectField>(
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

    class StaticFieldGetter(field: ReflectField) : FieldGetter(field, requiresInstance = false)

    class InstanceFieldGetter(field: ReflectField) : FieldGetter(field, requiresInstance = true)

    class JvmStaticInObjectFieldGetter(field: ReflectField) : FieldGetter(field, requiresInstance = true) {
        override fun checkArguments(args: Array<*>) {
            super.checkArguments(args)
            checkObjectInstance(args.firstOrNull())
        }
    }

    class BoundInstanceFieldGetter(field: ReflectField, private val boundReceiver: Any?) : FieldGetter(field, requiresInstance = false) {
        override fun call(args: Array<*>): Any? {
            checkArguments(args)
            return member.get(boundReceiver)
        }
    }

    class BoundJvmStaticInObjectFieldGetter(field: ReflectField) : FieldGetter(field, requiresInstance = false)

    class StaticFieldSetter(field: ReflectField, notNull: Boolean) : FieldSetter(field, notNull, requiresInstance = false)

    class InstanceFieldSetter(field: ReflectField, notNull: Boolean) : FieldSetter(field, notNull, requiresInstance = true)

    class JvmStaticInObjectFieldSetter(field: ReflectField, notNull: Boolean) : FieldSetter(field, notNull, requiresInstance = true) {
        override fun checkArguments(args: Array<*>) {
            super.checkArguments(args)
            checkObjectInstance(args.firstOrNull())
        }
    }

    class BoundInstanceFieldSetter(field: ReflectField, notNull: Boolean, private val boundReceiver: Any?) :
        FieldSetter(field, notNull, requiresInstance = false) {
        override fun call(args: Array<*>): Any? {
            checkArguments(args)
            return member.set(boundReceiver, args.first())
        }
    }

    class BoundJvmStaticInObjectFieldSetter(field: ReflectField, notNull: Boolean) : FieldSetter(
        field, notNull, requiresInstance = false
    ) {
        override fun call(args: Array<*>): Any? {
            checkArguments(args)
            return member.set(null, args.last())
        }
    }

    companion object {
        @Suppress("UNCHECKED_CAST")
        inline fun <reified T> Array<out T>.dropFirst(): Array<T> =
            if (size <= 1) emptyArray() else copyOfRange(1, size) as Array<T>
    }
}
