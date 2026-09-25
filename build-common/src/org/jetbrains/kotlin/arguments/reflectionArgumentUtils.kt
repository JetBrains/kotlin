/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.arguments

import java.lang.reflect.Modifier
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.KVisibility
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.javaField

fun <T : Any> collectProperties(kClass: KClass<T>, inheritedOnly: Boolean): List<KProperty1<T, Any?>> {
    val properties = ArrayList(kClass.memberProperties)
    if (inheritedOnly) {
        properties.removeAll(kClass.declaredMemberProperties)
    }
    return properties.filter { property ->
        property.visibility == KVisibility.PUBLIC
                && (property.javaField?.modifiers?.let { Modifier.isTransient(it) } != true)
                && (!property.isAbstract)
    }
}
