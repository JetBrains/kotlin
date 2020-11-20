/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.calls

import org.jetbrains.kotlin.types.UnwrappedType
import org.jetbrains.kotlin.types.checker.SimpleClassicTypeSystemContext

fun NewCommonSuperTypeCalculator.commonSuperType(types: List<UnwrappedType>): UnwrappedType {
    return SimpleClassicTypeSystemContext.commonSuperType(types) as UnwrappedType
}
