/*
 * Copyright 2010-2015 JetBrains s.r.o.
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
 * Stores simultaneously type of current expression together with data flow info
 * and jump point data flow info, together with information about possible jump outside. For example:
 * do {
 * x!!.foo()
 * if (bar()) break;
 * y!!.gav()
 * } while (bis())
 * At the end current data flow info is x != null && y != null, but jump data flow info is x != null only.
 * Both break and continue are counted as possible jump outside of a loop, but return is not.
 */
class KotlinTypeInfo @JvmOverloads constructor(
        val type: KotlinType?,
        val dataFlowInfo: DataFlowInfo,
        val jumpOutPossible: Boolean = false,
        val jumpFlowInfo: DataFlowInfo = dataFlowInfo
) {

    fun clearType() = replaceType(null)

    // NB: do not compare type with this.type because this comparison is complex and unstable
    fun replaceType(type: KotlinType?) = KotlinTypeInfo(type, dataFlowInfo, jumpOutPossible, jumpFlowInfo)

    fun replaceJumpOutPossible(jumpOutPossible: Boolean) =
            if (jumpOutPossible == this.jumpOutPossible) this else KotlinTypeInfo(type, dataFlowInfo, jumpOutPossible, jumpFlowInfo)

    fun replaceJumpFlowInfo(jumpFlowInfo: DataFlowInfo) =
            if (jumpFlowInfo == this.jumpFlowInfo) this else KotlinTypeInfo(type, dataFlowInfo, jumpOutPossible, jumpFlowInfo)

    fun replaceDataFlowInfo(dataFlowInfo: DataFlowInfo) = when (this.dataFlowInfo) {
        // Nothing changed
        dataFlowInfo -> this
        // Jump info is the same as data flow info: change both
        jumpFlowInfo -> KotlinTypeInfo(type, dataFlowInfo, jumpOutPossible, dataFlowInfo)
        // Jump info is not the same: change data flow info only
        else -> KotlinTypeInfo(type, dataFlowInfo, jumpOutPossible, jumpFlowInfo)
    }
}