/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.reflect.jvm.internal

import java.lang.reflect.*
import kotlin.jvm.internal.KotlinGenericDeclaration
import kotlin.jvm.internal.findMethodBySignature
import kotlin.reflect.KParameter
import kotlin.reflect.KProperty
import kotlin.reflect.full.IllegalPropertyDelegateAccessException
import kotlin.reflect.jvm.internal.DescriptorKProperty.Companion.EXTENSION_PROPERTY_DELEGATE
import kotlin.reflect.jvm.isAccessible

internal interface ReflectKProperty<out V> : ReflectKCallable<V>, KProperty<V>, KotlinGenericDeclaration {
    val signature: String

    val javaField: Field?

    override fun findJavaDeclaration(): GenericDeclaration? = container.findMethodBySignature(signature)
}

internal val ReflectKProperty<*>.isLocalDelegated: Boolean
    get() = KDeclarationContainerImpl.LOCAL_PROPERTY_SIGNATURE.matches(signature)

internal fun ReflectKProperty<*>.getDelegateImpl(fieldOrMethod: Member?, receiver1: Any?, receiver2: Any?): Any? =
    try {
        if (receiver1 === EXTENSION_PROPERTY_DELEGATE || receiver2 === EXTENSION_PROPERTY_DELEGATE) {
            if (parameters.none { it.kind == KParameter.Kind.EXTENSION_RECEIVER }) {
                throw RuntimeException(
                    "'$this' is not an extension property and thus getExtensionDelegate() " +
                            "is not going to work, use getDelegate() instead"
                )
            }
        }

        val realReceiver1 = (if (isBound) boundReceiver else receiver1).takeIf { it !== EXTENSION_PROPERTY_DELEGATE }
        val realReceiver2 = (if (isBound) receiver1 else receiver2).takeIf { it !== EXTENSION_PROPERTY_DELEGATE }
        (fieldOrMethod as? AccessibleObject)?.isAccessible = isAccessible
        when (fieldOrMethod) {
            null -> null
            is Field -> fieldOrMethod.get(realReceiver1)
            is Method -> when (fieldOrMethod.parameterTypes.size) {
                0 -> fieldOrMethod.invoke(null)
                1 -> fieldOrMethod.invoke(null, realReceiver1 ?: defaultPrimitiveValue(fieldOrMethod.parameterTypes[0]))
                2 -> fieldOrMethod.invoke(null, realReceiver1, realReceiver2 ?: defaultPrimitiveValue(fieldOrMethod.parameterTypes[1]))
                else -> throw AssertionError("delegate method $fieldOrMethod should take 0, 1, or 2 parameters")
            }
            else -> throw AssertionError("delegate field/method $fieldOrMethod neither field nor method")
        }
    } catch (e: IllegalAccessException) {
        throw IllegalPropertyDelegateAccessException(e)
    }
