/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization.mangle

import org.jetbrains.kotlin.name.FqName

internal fun <T> Collection<T>.collect(builder: StringBuilder, params: MangleConstant, collect: StringBuilder.(T) -> Unit) {
    var first = true

    builder.append(params.prefix)

    for (e in this) {
        if (first) {
            first = false
        } else {
            builder.append(params.separator)
        }

        builder.collect(e)
    }

    builder.append(params.suffix)
}

internal val publishedApiAnnotation = FqName("kotlin.PublishedApi")
