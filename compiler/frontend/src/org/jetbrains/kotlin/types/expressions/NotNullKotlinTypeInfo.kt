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

package org.jetbrains.kotlin.types.expressions

import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowInfo
import org.jetbrains.kotlin.types.KotlinType


/**
 * KotlinTypeInfo with not null type
 * @see KotlinTypeInfo
 */
class NotNullKotlinTypeInfo @JvmOverloads constructor(
        val type: KotlinType,
        val dataFlowInfo: DataFlowInfo,
        val jumpOutPossible: Boolean = false,
        val jumpFlowInfo: DataFlowInfo = dataFlowInfo
) {

    fun kotlinTypeInfo() = KotlinTypeInfo(type, dataFlowInfo, jumpOutPossible, jumpFlowInfo)

    fun clearType() = KotlinTypeInfo(null, dataFlowInfo, jumpOutPossible, jumpFlowInfo)

    fun replaceType(type: KotlinType?) = KotlinTypeInfo(type, dataFlowInfo, jumpOutPossible, jumpFlowInfo)

    fun replaceType(type: KotlinType) = NotNullKotlinTypeInfo(type, dataFlowInfo, jumpOutPossible, jumpFlowInfo)

    fun replaceJumpOutPossible(jumpOutPossible: Boolean) =
            if (jumpOutPossible == this.jumpOutPossible) this else NotNullKotlinTypeInfo(type, dataFlowInfo, jumpOutPossible, jumpFlowInfo)

    fun replaceJumpFlowInfo(jumpFlowInfo: DataFlowInfo) =
            if (jumpFlowInfo == this.jumpFlowInfo) this else NotNullKotlinTypeInfo(type, dataFlowInfo, jumpOutPossible, jumpFlowInfo)

    fun replaceDataFlowInfo(dataFlowInfo: DataFlowInfo) = when (this.dataFlowInfo) {
        dataFlowInfo -> this
        jumpFlowInfo -> NotNullKotlinTypeInfo(type, dataFlowInfo, jumpOutPossible, dataFlowInfo)
        else -> NotNullKotlinTypeInfo(type, dataFlowInfo, jumpOutPossible, jumpFlowInfo)
    }
}