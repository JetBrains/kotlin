/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.reflect.jvm.internal

import java.lang.reflect.Field
import java.lang.reflect.Modifier
import kotlin.LazyThreadSafetyMode.PUBLICATION
import kotlin.reflect.KType
import kotlin.reflect.jvm.internal.calls.Caller
import kotlin.reflect.jvm.internal.calls.CallerImpl

internal abstract class JavaFieldKProperty<out V>(
    container: KDeclarationContainerImpl,
    field: Field,
    rawBoundReceiver: Any?,
    overriddenStorage: KCallableOverriddenStorage,
) : JavaKProperty<V>(container, field, rawBoundReceiver, overriddenStorage) {
    val jField: Field get() = member as Field

    override val signature: String
        get() = jField.jvmSignature

    override val returnType: KType by lazy(PUBLICATION) {
        substituteType(jField.genericType.toKType(emptyMap()))
    }

    override val isConst: Boolean
        get() = Modifier.isFinal(jField.modifiers) && Modifier.isStatic(jField.modifiers) &&
                (jField.type.isPrimitive || jField.type == String::class.java)

    override val javaField: Field? get() = jField

    interface Accessor<out PropertyType, out ReturnType> : JavaKProperty.Accessor<PropertyType, ReturnType> {
        abstract override val property: JavaFieldKProperty<PropertyType>
    }

    abstract class Getter<out V> : JavaKProperty.Getter<V>(), Accessor<V, V> {
        override val caller: Caller<*> by lazy(PUBLICATION) {
            computeCallerForAccessor(isGetter = true)
        }
    }

    abstract class Setter<V> : JavaKProperty.Setter<V>(), Accessor<V, Unit> {
        override val caller: Caller<*> by lazy(PUBLICATION) {
            computeCallerForAccessor(isGetter = false)
        }
    }
}

private fun JavaFieldKProperty.Accessor<*, *>.computeCallerForAccessor(isGetter: Boolean): Caller<*> {
    val field = property.jField
    return when {
        !Modifier.isStatic(field.modifiers) ->
            if (isGetter)
                if (isBound) CallerImpl.FieldGetter.BoundInstance(field, boundReceiver)
                else CallerImpl.FieldGetter.Instance(field)
            else
                if (isBound) CallerImpl.FieldSetter.BoundInstance(field, notNull = false, boundReceiver)
                else CallerImpl.FieldSetter.Instance(field, notNull = false)
        else ->
            if (isGetter) CallerImpl.FieldGetter.Static(field)
            else CallerImpl.FieldSetter.Static(field, notNull = false)
    }
}
