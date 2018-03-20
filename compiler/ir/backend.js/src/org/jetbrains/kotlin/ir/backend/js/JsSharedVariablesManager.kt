/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js

import org.jetbrains.kotlin.backend.common.descriptors.SharedVariablesManager
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.expressions.IrSetVariable

class JsSharedVariablesManager(builtIns: KotlinBuiltIns): SharedVariablesManager {
    override fun createSharedVariableDescriptor(variableDescriptor: VariableDescriptor): VariableDescriptor {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun defineSharedValue(sharedVariableDescriptor: VariableDescriptor, originalDeclaration: IrVariable): IrStatement {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getSharedValue(sharedVariableDescriptor: VariableDescriptor, originalGet: IrGetValue): IrExpression {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun setSharedValue(sharedVariableDescriptor: VariableDescriptor, originalSet: IrSetVariable): IrExpression {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}
