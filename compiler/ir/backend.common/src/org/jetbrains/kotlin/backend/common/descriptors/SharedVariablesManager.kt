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

package org.jetbrains.kotlin.backend.common.descriptors

import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.expressions.IrSetVariable

interface SharedVariablesManager {
    fun createSharedVariableDescriptor(variableDescriptor: VariableDescriptor): VariableDescriptor
    fun defineSharedValue(sharedVariableDescriptor: VariableDescriptor, originalDeclaration: IrVariable): IrStatement
    fun getSharedValue(sharedVariableDescriptor: VariableDescriptor, originalGet: IrGetValue): IrExpression
    fun setSharedValue(sharedVariableDescriptor: VariableDescriptor, originalSet: IrSetVariable): IrExpression
}