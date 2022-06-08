/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.utils.errors

public fun unexpectedElementError(elementName: String, element: Any?): Nothing {
    error("Unexpected $elementName ${element?.let { it::class.simpleName }}")
}

public inline fun <reified ELEMENT> unexpectedElementError(element: Any?): Nothing {
    unexpectedElementError(ELEMENT::class.simpleName ?: ELEMENT::class.java.name, element)
}