/*
 * Copyright 2000-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen.range.forLoop

import org.jetbrains.kotlin.codegen.ExpressionCodegen
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.psi.KtDestructuringDeclaration
import org.jetbrains.kotlin.psi.KtDestructuringDeclarationEntry
import org.jetbrains.kotlin.psi.KtForExpression
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.org.objectweb.asm.Label
import org.jetbrains.org.objectweb.asm.Type

abstract class AbstractWithIndexForLoopGenerator(
    protected val codegen: ExpressionCodegen,
    final override val forExpression: KtForExpression,
    protected val loopParameter: KtDestructuringDeclaration,
    protected val rangeCall: ResolvedCall<out CallableDescriptor>
) : ForLoopGenerator {

    protected val bindingContext = codegen.bindingContext
    protected val v = codegen.v!!

    private val loopParameterStartLabel = Label()
    private val bodyEnd = Label()
    private val leaveTasks = arrayListOf<() -> Unit>()

    protected class LoopComponent(val parameterVar: Int, val parameterType: Type, val componentType: Type)

    protected val indexLoopComponent: LoopComponent? = loopParameter.entries.getOrNull(0)?.resolveLoopComponent()
    protected val elementLoopComponent: LoopComponent? = loopParameter.entries.getOrNull(1)?.resolveLoopComponent()

    private fun KtDestructuringDeclarationEntry.resolveLoopComponent(): LoopComponent? {
        val variableDescriptor = bindingContext[BindingContext.VARIABLE, this]

        if (variableDescriptor == null || variableDescriptor.name.isSpecial) return null

        val resolvedCall = bindingContext[BindingContext.COMPONENT_RESOLVED_CALL, this] ?: return null

        val elementType = codegen.asmType(resolvedCall.resultingDescriptor.returnType ?: return null)

        val parameterType = codegen.asmType(variableDescriptor.type)
        val parameterVar = codegen.myFrameMap.enter(variableDescriptor, parameterType)
        scheduleLeaveTask {
            codegen.myFrameMap.leaveTemp(parameterType)
            v.visitLocalVariable(
                variableDescriptor.name.asString(),
                parameterType.descriptor, null,
                loopParameterStartLabel, bodyEnd,
                parameterVar
            )
        }
        return LoopComponent(parameterVar, parameterType, elementType)
    }

    protected fun scheduleLeaveTask(task: () -> Unit) {
        leaveTasks.add(task)
    }

    protected fun createLoopTempVariable(type: Type): Int {
        val varIndex = codegen.myFrameMap.enterTemp(type)
        scheduleLeaveTask { codegen.myFrameMap.leaveTemp(type) }
        return varIndex
    }


    override fun beforeBody() {
        assignLoopParametersNextValues()
        v.mark(loopParameterStartLabel)
    }

    override fun checkEmptyLoop(loopExit: Label) {
        // do nothing
    }

    override fun body() {
        codegen.generateLoopBody(forExpression.body)
    }

    override fun afterBody(loopExit: Label) {
        codegen.markStartLineNumber(forExpression)
        incrementAndCheckPostCondition(loopExit)
        v.mark(bodyEnd)
    }

    override fun afterLoop() {
        for (task in leaveTasks.asReversed()) task()
    }

    protected abstract fun assignLoopParametersNextValues()
    protected abstract fun incrementAndCheckPostCondition(loopExit: Label)

}