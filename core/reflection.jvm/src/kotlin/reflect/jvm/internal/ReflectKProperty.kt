/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.reflect.jvm.internal

import java.lang.reflect.Field
import kotlin.reflect.KProperty

internal interface ReflectKProperty<out V> : ReflectKCallable<V>, KProperty<V> {
    val signature: String

    val javaField: Field?
}

internal val ReflectKProperty<*>.isLocalDelegated: Boolean
    get() = KDeclarationContainerImpl.LOCAL_PROPERTY_SIGNATURE.matches(signature)
