/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.api

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.psi.KtElement
import java.util.*
import kotlin.reflect.KClass

class InvalidFirElementTypeException(
    actualFirClass: KClass<*>?,
    ktElement: KtElement?,
    expectedFirClasses: List<KClass<*>>,
) : IllegalStateException() {
    override val message: String = buildString {
        if (ktElement != null) {
            append("For $ktElement with text `${ktElement.text}`, ")
        }
        val message = when (expectedFirClasses.size) {
            0 -> "Unexpected element of type:"
            1 -> "The element of type ${expectedFirClasses.single()} expected, but"
            else -> "One of [${expectedFirClasses.joinToString()}] element types expected, but"
        }
        append(if (ktElement == null) message else message.replaceFirstChar { it.lowercase(Locale.getDefault()) })
        if (actualFirClass != null) {
            append(" ${actualFirClass.simpleName} found")
        } else {
            append(" no element found")
        }
    }
}


fun throwUnexpectedFirElementError(
    firElement: Any?,
    ktElement: KtElement? = null,
    vararg expectedFirClasses: KClass<*>
): Nothing {
    throw InvalidFirElementTypeException(firElement?.let { it::class }, ktElement, expectedFirClasses.toList())
}