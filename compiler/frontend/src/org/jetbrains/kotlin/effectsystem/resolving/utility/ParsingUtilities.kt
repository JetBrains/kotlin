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

package org.jetbrains.kotlin.effectsystem.resolving.utility

import org.jetbrains.kotlin.descriptors.ReceiverParameterDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.effectsystem.adapters.ValueIdsFactory
import org.jetbrains.kotlin.effectsystem.impls.ESConstant
import org.jetbrains.kotlin.effectsystem.impls.ESVariable
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowValue
import org.jetbrains.kotlin.resolve.calls.smartcasts.IdentifierInfo
import org.jetbrains.kotlin.resolve.constants.ConstantValue

object UtilityParsers {
    val conditionParser = ConditionParser()
    val constantsParser = ConstantsParser()
}

internal fun ValueParameterDescriptor.toESVariable(): ESVariable {
    val dfv = DataFlowValue(IdentifierInfo.Variable(this, DataFlowValue.Kind.STABLE_VALUE, null), type)
    return ESVariable(ValueIdsFactory.dfvBased(dfv), type)
}

internal fun ReceiverParameterDescriptor.extensionReceiverToESVariable(): ESVariable {
    val dfv = DataFlowValue(IdentifierInfo.Receiver(value), type)
    return ESVariable(ValueIdsFactory.dfvBased(dfv), type)
}

internal fun ConstantValue<*>.toESConstant(): ESConstant? = UtilityParsers.constantsParser.parseConstantValue(this)

