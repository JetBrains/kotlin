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

import org.jetbrains.kotlin.effectsystem.structure.ConstantID
import org.jetbrains.kotlin.effectsystem.structure.ESValueID
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowValue

class DataFlowValueID(val dfv: DataFlowValue) : ESValueID {
    override fun equals(other: Any?): Boolean = other is DataFlowValueID && dfv == other.dfv

    override fun hashCode(): Int = dfv.hashCode()

    override fun toString(): String = dfv.identifierInfo.toString()
}

object ValueIdsFactory {
    fun idForConstant(value: Any?): ESValueID = ConstantID(value)
    fun dfvBased(dfv: DataFlowValue): ESValueID = DataFlowValueID(dfv)
}