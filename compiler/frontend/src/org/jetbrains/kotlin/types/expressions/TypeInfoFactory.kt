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

import org.jetbrains.kotlin.resolve.calls.context.ResolutionContext
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowInfo
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.expressions.KotlinTypeInfo

/*
 * Functions in this file are intended to create type info instances in different circumstances
 */

fun createTypeInfo(type: KotlinType?, dataFlowInfo: DataFlowInfo): KotlinTypeInfo = KotlinTypeInfo(type, dataFlowInfo)

fun createTypeInfo(type: KotlinType?, dataFlowInfo: DataFlowInfo, jumpPossible: Boolean, jumpFlowInfo: DataFlowInfo): KotlinTypeInfo =
        KotlinTypeInfo(type, dataFlowInfo, jumpPossible, jumpFlowInfo)

fun createTypeInfo(type: KotlinType?): KotlinTypeInfo = createTypeInfo(type, DataFlowInfo.EMPTY)

fun createTypeInfo(type: KotlinType?, context: ResolutionContext<*>): KotlinTypeInfo = createTypeInfo(type, context.dataFlowInfo)

fun noTypeInfo(dataFlowInfo: DataFlowInfo): KotlinTypeInfo = createTypeInfo(null, dataFlowInfo)

fun noTypeInfo(context: ResolutionContext<*>): KotlinTypeInfo = noTypeInfo(context.dataFlowInfo)