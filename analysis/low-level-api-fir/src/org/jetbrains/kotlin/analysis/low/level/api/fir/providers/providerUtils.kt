/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.providers

import org.jetbrains.kotlin.analysis.low.level.api.fir.stubBased.deserialization.DeserializedContainerSourceWithJvmClassName
import org.jetbrains.kotlin.analysis.low.level.api.fir.stubBased.deserialization.JvmStubDeserializedBuiltInsContainerSource
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.load.kotlin.FacadeClassSource
import org.jetbrains.kotlin.load.kotlin.KotlinJvmBinarySourceElement
import org.jetbrains.kotlin.resolve.jvm.JvmClassName
import java.util.*

internal fun <T: Any> Optional<T>.getOrNull(): T? = orElse(null)

fun FirCallableSymbol<*>.jvmClassNameIfDeserialized(): JvmClassName? {
    return when (val containerSource = fir.containerSource) {
        is JvmStubDeserializedBuiltInsContainerSource -> containerSource.facadeClassName
        is FacadeClassSource -> containerSource.facadeClassName ?: containerSource.className
        is DeserializedContainerSourceWithJvmClassName -> containerSource.className
        is KotlinJvmBinarySourceElement -> JvmClassName.byClassId(containerSource.binaryClass.classId)
        else -> null
    }
}
