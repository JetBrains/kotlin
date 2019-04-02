/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.types.model

fun KotlinTypeMarker.typeConstructor(context: TypeSystemContext): TypeConstructorMarker =
    with(context) { typeConstructor() }

fun TypeConstructorMarker.isIntegerLiteralTypeConstructor(context: TypeSystemContext): Boolean =
    with(context) { isIntegerLiteralTypeConstructor() }
