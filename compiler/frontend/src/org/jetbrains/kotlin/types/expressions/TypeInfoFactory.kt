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

import org.jetbrains.kotlin.psi.JetExpression
import org.jetbrains.kotlin.resolve.calls.context.ResolutionContext
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowInfo
import org.jetbrains.kotlin.types.JetType
import org.jetbrains.kotlin.types.expressions.JetTypeInfo

/**
 * Functions in this file are intended to create type info instances in different circumstances
 */

public fun createTypeInfo(type: JetType?): JetTypeInfo = createTypeInfo(type, DataFlowInfo.EMPTY)

public fun createTypeInfo(dataFlowInfo: DataFlowInfo): JetTypeInfo = JetTypeInfo(null, dataFlowInfo)

public fun createTypeInfo(context: ResolutionContext<*>): JetTypeInfo = createTypeInfo(context.dataFlowInfo)

public fun createTypeInfo(type: JetType?, dataFlowInfo: DataFlowInfo): JetTypeInfo = JetTypeInfo(type, dataFlowInfo)

public fun createTypeInfo(type: JetType?, context: ResolutionContext<*>): JetTypeInfo = createTypeInfo(type, context.dataFlowInfo)

public fun createTypeInfo(type: JetType?, dataFlowInfo: DataFlowInfo, jumpPossible: Boolean, jumpFlowInfo: DataFlowInfo): JetTypeInfo =
        JetTypeInfo(type, dataFlowInfo, jumpPossible, jumpFlowInfo)

public fun createCheckedTypeInfo(type: JetType?, context: ResolutionContext<*>, expression: JetExpression): JetTypeInfo =
        createTypeInfo(type, context).checkType(expression, context)
