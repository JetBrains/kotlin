/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.reflect.jvm.internal

import java.lang.reflect.GenericDeclaration
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.lang.reflect.WildcardType
import kotlin.coroutines.Continuation
import kotlin.jvm.internal.KotlinGenericDeclaration
import kotlin.jvm.internal.findMethodBySignature
import kotlin.reflect.KFunction

internal interface ReflectKFunction : ReflectKCallable<Any?>, KFunction<Any?>, KotlinGenericDeclaration {
    val signature: String

    val overridden: Collection<ReflectKFunction>

    override fun findJvmDeclaration(): GenericDeclaration? = container.findMethodBySignature(signature)
}

internal fun ReflectKFunction.extractContinuationArgument(): Type? {
    if (isSuspend) {
        // kotlin.coroutines.Continuation<? super java.lang.String>
        val continuationType = caller.parameterTypes.lastOrNull() as? ParameterizedType
        if (continuationType?.rawType == Continuation::class.java) {
            // ? super java.lang.String
            val wildcard = continuationType.actualTypeArguments.single() as? WildcardType
            // java.lang.String
            return wildcard?.lowerBounds?.first()
        }
    }

    return null
}
