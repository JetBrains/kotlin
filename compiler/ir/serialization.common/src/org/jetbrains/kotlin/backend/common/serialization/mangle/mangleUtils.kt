/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization.mangle

import org.jetbrains.kotlin.name.FqName

fun <T> Iterable<T>.collectForMangler(builder: StringBuilder, params: MangleConstant, collect: StringBuilder.(T) -> Unit) {
    var first = true

    builder.append(params.prefix)

    var addSeparator = true

    for (e in this) {
        if (first) {
            first = false
        } else if (addSeparator) {
            builder.append(params.separator)
        }

        val l = builder.length
        builder.collect(e)
        addSeparator = l < builder.length
    }

    if (!addSeparator) {
        if (builder.last() == params.separator) {
            // avoid signatures like foo(Int;)
            builder.deleteCharAt(builder.lastIndex)
        }
    }

    builder.append(params.suffix)
}

val publishedApiAnnotation = FqName("kotlin.PublishedApi")
