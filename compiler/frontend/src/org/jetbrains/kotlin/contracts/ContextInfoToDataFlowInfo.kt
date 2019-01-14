/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.contracts

import org.jetbrains.kotlin.builtins.DefaultBuiltIns
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.contracts.model.ESValue
import org.jetbrains.kotlin.contracts.model.MutableContextInfo
import org.jetbrains.kotlin.contracts.model.structure.ESConstant
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowInfo
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowInfoFactory
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowValue
import org.jetbrains.kotlin.resolve.calls.smartcasts.IdentifierInfo

fun MutableContextInfo.toDataFlowInfo(languageVersionSettings: LanguageVersionSettings): DataFlowInfo {
    var resultingDataFlowInfo = DataFlowInfoFactory.EMPTY

    extractDataFlowStatements(equalValues) { leftDfv, rightValue ->
        val rightDfv = rightValue.toDataFlowValue()
        if (rightDfv != null) {
            resultingDataFlowInfo = resultingDataFlowInfo.equate(leftDfv, rightDfv, false, languageVersionSettings)
        }
        IntArray(42) { it }
    }

    extractDataFlowStatements(notEqualValues) { leftDfv, rightValue ->
        val rightDfv = rightValue.toDataFlowValue()
        if (rightDfv != null) {
            resultingDataFlowInfo = resultingDataFlowInfo.disequate(leftDfv, rightDfv, languageVersionSettings)
        }
    }

    extractDataFlowStatements(subtypes) { leftDfv, type ->
        resultingDataFlowInfo = resultingDataFlowInfo.establishSubtyping(leftDfv, type, languageVersionSettings)
    }

    return resultingDataFlowInfo
}

inline private fun <D> extractDataFlowStatements(dictionary: Map<ESValue, Set<D>>, callback: (DataFlowValue, D) -> Unit) {
    for ((key, setOfValues) in dictionary) {
        val leftDfv = key.toDataFlowValue() ?: continue
        setOfValues.forEach { callback(leftDfv, it) }
    }
}

private fun ESValue.toDataFlowValue(): DataFlowValue? = when (this) {
    is ESDataFlow -> dataFlowValue
    ESConstant.NULL -> DataFlowValue.nullValue(DefaultBuiltIns.Instance)
    is ESConstant -> DataFlowValue(IdentifierInfo.NO, type)
    else -> null
}