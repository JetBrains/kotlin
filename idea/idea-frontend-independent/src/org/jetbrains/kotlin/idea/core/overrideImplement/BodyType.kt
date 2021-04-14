/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.core.overrideImplement

sealed class BodyType(val requiresReturn: Boolean = true) {
    object NO_BODY : BodyType()
    object EMPTY_OR_TEMPLATE : BodyType(requiresReturn = false)
    object FROM_TEMPLATE : BodyType(requiresReturn = false)
    object SUPER : BodyType()
    object QUALIFIED_SUPER : BodyType()

    class Delegate(val receiverName: String) : BodyType()

    fun effectiveBodyType(canBeEmpty: Boolean): BodyType = if (!canBeEmpty && this == EMPTY_OR_TEMPLATE) FROM_TEMPLATE else this
}

