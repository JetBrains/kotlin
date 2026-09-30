/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.analysis.checkers

import org.jetbrains.kotlin.fir.SessionHolder
import org.jetbrains.kotlin.fir.types.ConeKotlinType

// A flexible primitive type like `Int!` is its Java box, like `java.lang.Integer`, so it is a value class only if its platform class is.
context(sessionHolder: SessionHolder)
fun ConeKotlinType.isValueClassOrPrimitive(): Boolean =
    if (isFlexiblePrimitive()) isValueClass(sessionHolder.session)
    else isBoundedByPrimitiveOrValueClass(sessionHolder.session)
