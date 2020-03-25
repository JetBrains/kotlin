/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.debugger.coroutine.proxy

import com.sun.jdi.ArrayReference
import com.sun.jdi.ClassType
import com.sun.jdi.ObjectReference
import com.sun.jdi.StringReference
import org.jetbrains.kotlin.idea.debugger.evaluate.DefaultExecutionContext

data class FieldVariable(val fieldName: String, val variableName: String) {
    companion object {
        fun extractFromContinuation(context: DefaultExecutionContext, continuation: ObjectReference, debugMetadataKtType: ClassType?) : List<FieldVariable> {
            val metadataType = debugMetadataKtType ?: context.findCoroutineMetadataType() ?: return emptyList()
            val rawSpilledVariables =
                context.invokeMethodAsArray(
                    metadataType,
                    "getSpilledVariableFieldMapping",
                    "(Lkotlin/coroutines/jvm/internal/BaseContinuationImpl;)[Ljava/lang/String;",
                    continuation
                ) ?: return emptyList()

            context.keepReference(rawSpilledVariables)

            val length = rawSpilledVariables.length() / 2
            val fieldVariables = ArrayList<FieldVariable>()
            for (index in 0 until length) {
                fieldVariables.add(getFieldVariableName(rawSpilledVariables, index) ?: continue)
            }
            return fieldVariables
        }


        private fun getFieldVariableName(rawSpilledVariables: ArrayReference, index: Int): FieldVariable? {
            val fieldName = (rawSpilledVariables.getValue(2 * index) as? StringReference)?.value() ?: return null
            val variableName = (rawSpilledVariables.getValue(2 * index + 1) as? StringReference)?.value() ?: return null
            return FieldVariable(fieldName, variableName)
        }
    }
}

