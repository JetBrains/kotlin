/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.utils.errors

import org.jetbrains.kotlin.utils.exceptions.errorWithAttachment

@Deprecated("Unintentionally exposed implementation detail. Do not use", level = DeprecationLevel.ERROR)
public fun unexpectedElementError(elementName: String, element: Any?): Nothing {
    errorWithAttachment("Unexpected $elementName ${element?.let { it::class.simpleName }}") {
        withEntry(elementName, element) { element.toString() }
    }
}

@Deprecated("Unintentionally exposed implementation detail. Do not use", level = DeprecationLevel.ERROR)
public inline fun <reified ELEMENT> unexpectedElementError(element: Any?): Nothing {
    @Suppress("DEPRECATION_ERROR")
    unexpectedElementError(ELEMENT::class.simpleName ?: ELEMENT::class.java.name, element)
}

