/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.internal.compat.arguments

import org.jetbrains.kotlin.cli.common.arguments.CommonToolArguments
import kotlin.reflect.KMutableProperty
import kotlin.reflect.KProperty
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.jvm.jvmName

/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

internal fun <T> CommonToolArguments.setUsingReflection(propertyName: String, value: T) {
    this::class.declaredMemberProperties.filterIsInstance<KMutableProperty<T>>().firstOrNull { it.name == propertyName }
        ?.let { property: KMutableProperty<T> ->
            property.setter.call(this, value)
        } ?: throw NoSuchMethodError("No property found with name $propertyName in ${this::class.jvmName}")
}

internal fun <T> CommonToolArguments.getUsingReflection(propertyName: String): T {
    return this::class.declaredMemberProperties.filterIsInstance<KProperty<T>>()
        .firstOrNull { it.name == propertyName }
        ?.let { property: KProperty<T> ->
            property.getter.call(this)
        } ?: throw NoSuchMethodError("No property found with name $propertyName in ${this::class.jvmName}")
}