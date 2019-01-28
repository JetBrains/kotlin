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

package org.jetbrains.kotlin.contracts.interpretation

import org.jetbrains.kotlin.contracts.description.expressions.BooleanConstantReference
import org.jetbrains.kotlin.contracts.description.expressions.ConstantReference
import org.jetbrains.kotlin.contracts.model.structure.ESConstant
import org.jetbrains.kotlin.contracts.model.structure.ESConstants

internal class ConstantValuesInterpreter {
    fun interpretConstant(constantReference: ConstantReference, constants: ESConstants): ESConstant? = when (constantReference) {
        BooleanConstantReference.TRUE -> constants.trueValue
        BooleanConstantReference.FALSE -> constants.falseValue
        ConstantReference.NULL -> constants.nullValue
        ConstantReference.NOT_NULL -> constants.notNullValue
        ConstantReference.WILDCARD -> constants.wildcard
        else -> null
    }
}
