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

package org.jetbrains.kotlin.effectsystem.adapters

import org.jetbrains.kotlin.builtins.DefaultBuiltIns
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.effectsystem.structure.ESValue
import org.jetbrains.kotlin.effectsystem.impls.ESConstant
import org.jetbrains.kotlin.effectsystem.structure.ConstantID
import org.jetbrains.kotlin.effectsystem.structure.NOT_NULL_ID
import org.jetbrains.kotlin.resolve.calls.smartcasts.*

fun MutableContextInfo.toDataFlowInfo(languageVersionSettings: LanguageVersionSettings): DataFlowInfo {
    var resultDFI = DataFlowInfoFactory.EMPTY

    extractDataFlowStatements(equalValues) { leftDfv, rightValue ->
        val id = rightValue.id
        if (id == NOT_NULL_ID) {
            resultDFI = resultDFI.disequate(leftDfv, DataFlowValue.nullValue(DefaultBuiltIns.Instance), languageVersionSettings)
            return@extractDataFlowStatements
        }

        val rightDfv = when (id) {
            is DataFlowValueID -> id.dfv
            is ConstantID ->
                if ((rightValue as ESConstant).value == null)
                    DataFlowValue.nullValue(DefaultBuiltIns.Instance)
                else
                    DataFlowValue(IdentifierInfo.NO, rightValue.type)
            else -> return@extractDataFlowStatements
        }
        resultDFI = resultDFI.equate(leftDfv, rightDfv, false, languageVersionSettings)
    }

    extractDataFlowStatements(notEqualValues) { leftDfv, rightValue ->
        val id = rightValue.id
        if (id == NOT_NULL_ID) {
            resultDFI = resultDFI.equate(leftDfv, DataFlowValue.nullValue(DefaultBuiltIns.Instance), false, languageVersionSettings)
            return@extractDataFlowStatements
        }

        val rightDfv = when (id) {
            is DataFlowValueID -> id.dfv
            is ConstantID ->
                if ((rightValue as ESConstant).value == null)
                    DataFlowValue.nullValue(DefaultBuiltIns.Instance)
                else
                    DataFlowValue(IdentifierInfo.NO, rightValue.type)
            else -> return@extractDataFlowStatements
        }
        resultDFI = resultDFI.disequate(leftDfv, rightDfv, languageVersionSettings)
    }

    extractDataFlowStatements(subtypes) { leftDfv, type ->
        resultDFI = resultDFI.establishSubtyping(leftDfv, type, languageVersionSettings)
    }

    return resultDFI
}

private fun <D> extractDataFlowStatements(dictionary: Map<ESValue, Set<D>>, callback: (DataFlowValue, D) -> Unit) {
    dictionary.forEach { key, setOfValues ->
        val leftDfv = (key.id as? DataFlowValueID)?.dfv ?: return@forEach
        setOfValues.forEach { callback(leftDfv, it) }
    }
}