/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.debugger.stackFrame

import com.sun.jdi.ObjectReference
import com.sun.jdi.ReferenceType
import com.sun.jdi.Type
import org.jetbrains.kotlin.codegen.AsmUtil

fun getThisName(label: String): String {
    return AsmUtil.THIS + " (@" + label + ")"
}

fun getThisValueLabel(thisValue: ObjectReference): String? {
    val thisType = thisValue.referenceType()
    val unsafeLabel = generateThisLabelUnsafe(thisType) ?: return null
    return checkLabel(unsafeLabel)
}

fun generateThisLabelUnsafe(type: Type?): String? {
    val referenceType = type as? ReferenceType ?: return null
    return referenceType.name().substringAfterLast('.').substringAfterLast('$')
}

fun generateThisLabel(type: Type?): String? {
    return checkLabel(generateThisLabelUnsafe(type) ?: return null)
}

private fun checkLabel(label: String): String? {
    if (label.isEmpty() || label.all { it.isDigit() }) {
        return null
    }

    return label
}