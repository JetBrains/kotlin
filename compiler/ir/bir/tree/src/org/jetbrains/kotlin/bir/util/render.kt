/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.bir.util

import org.jetbrains.kotlin.bir.BirElement
import org.jetbrains.kotlin.bir.types.*

fun BirElement.render(): String = TODO()

fun BirElement.dump(): String = TODO()

fun BirType.render(): String = TODO()

fun BirSimpleType.render(): String = (this as BirType).render()

fun BirTypeArgument.render(): String =
    when (this) {
        is BirStarProjection -> "*"
        is BirTypeProjection -> "$variance ${type.render()}"
    }