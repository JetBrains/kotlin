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

package org.jetbrains.kotlin.types.expressions.typeInfoFactory

import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.resolve.calls.context.ResolutionContext
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowInfo
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.expressions.DataFlowAnalyzer
import org.jetbrains.kotlin.types.expressions.JetTypeInfo

/*
 * Functions in this file are intended to create type info instances in different circumstances
 */

public fun createTypeInfo(type: KotlinType?, dataFlowInfo: DataFlowInfo): JetTypeInfo = JetTypeInfo(type, dataFlowInfo)

public fun createTypeInfo(type: KotlinType?, dataFlowInfo: DataFlowInfo, jumpPossible: Boolean, jumpFlowInfo: DataFlowInfo): JetTypeInfo =
        JetTypeInfo(type, dataFlowInfo, jumpPossible, jumpFlowInfo)

public fun createTypeInfo(type: KotlinType?): JetTypeInfo = createTypeInfo(type, DataFlowInfo.EMPTY)

public fun createTypeInfo(type: KotlinType?, context: ResolutionContext<*>): JetTypeInfo = createTypeInfo(type, context.dataFlowInfo)

public fun noTypeInfo(dataFlowInfo: DataFlowInfo): JetTypeInfo = createTypeInfo(null, dataFlowInfo)

public fun noTypeInfo(context: ResolutionContext<*>): JetTypeInfo = noTypeInfo(context.dataFlowInfo)
