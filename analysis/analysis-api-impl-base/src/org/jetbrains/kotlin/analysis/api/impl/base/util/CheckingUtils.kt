/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.util

import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.utils.exceptions.errorWithAttachment

@KaImplementationDetail
fun unexpectedElementError(elementName: String, element: Any?): Nothing {
    errorWithAttachment("Unexpected $elementName ${element?.let { it::class.simpleName }}") {
        withEntry(elementName, element) { element.toString() }
    }
}

@KaImplementationDetail
inline fun <reified ELEMENT> unexpectedElementError(element: Any?): Nothing {
    unexpectedElementError(ELEMENT::class.simpleName ?: ELEMENT::class.java.name, element)
}