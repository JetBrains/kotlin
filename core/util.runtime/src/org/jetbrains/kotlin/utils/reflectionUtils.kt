/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.utils

import java.lang.reflect.Constructor
import java.lang.reflect.Modifier
import java.lang.reflect.Type

private val Constructor<*>.genericParameterTypesWithoutEnclosingThis: Array<Type>
    get() {
        val genericParameterTypes = genericParameterTypes
        val parameterTypes = parameterTypes
        /**
         * When constructor has no generic signature, `Class::getGenericParameterTypes` simply returns `Class::parameterTypes`.
         * Otherwise, it returns the specified generic signature.
         *
         * For an inner class, it's generic signature (whenever it exists) DOES NOT contain enclosing `this` parameter,
         * yet the descriptor DOES.
         *
         * As Java Reflection API does not provide a direct way to tell, what Class::getGenericParameterTypes actually returned,
         * we distinguish these cases relying on the arrays lengths.
         *
         * See KT-17763 for the details.
         */
        return if (genericParameterTypes.size == parameterTypes.size) {
            val clazz = declaringClass
            if (!Modifier.isStatic(clazz.modifiers) && clazz.declaringClass != null) {
                genericParameterTypes.drop(1).toTypedArray()
            } else {
                genericParameterTypes
            }
        } else {
            genericParameterTypes
        }
    }

private val Constructor<*>.outerClassForInnerClassConstructor: Type?
    get() =
        declaringClass.let { klass ->
            val outerClass = klass.declaringClass
            if (outerClass != null && !Modifier.isStatic(klass.modifiers)) outerClass else null
        }


val Constructor<*>.genericParameterTypesWithEnclosingThis: Array<Type>
    get() {
        val outerThis = outerClassForInnerClassConstructor
        return if (outerThis != null) arrayOf(
            outerThis,
            *genericParameterTypesWithoutEnclosingThis
        ) else genericParameterTypesWithoutEnclosingThis
    }