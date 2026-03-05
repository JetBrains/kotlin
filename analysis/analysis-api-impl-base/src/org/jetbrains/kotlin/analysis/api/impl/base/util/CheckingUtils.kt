/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.impl.base.util

import org.jetbrains.kotlin.analysis.api.KaImplementationDetail
import org.jetbrains.kotlin.utils.exceptions.errorWithAttachment
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

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

@KaImplementationDetail
@OptIn(ExperimentalContracts::class)
inline fun <reified T> requireIsInstance(obj: Any) {
    contract {
        returns() implies (obj is T)
    }
    require(obj is T) { "Expected ${T::class} instead of ${obj::class} for $obj" }
}

@KaImplementationDetail
@OptIn(ExperimentalContracts::class)
inline fun <reified T> checkIsInstance(obj: Any) {
    contract {
        returns() implies (obj is T)
    }
    check(obj is T) { "Expected ${T::class} instead of ${obj::class} for $obj" }
}