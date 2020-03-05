/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.debugger.test

import com.intellij.debugger.actions.MethodSmartStepTarget
import com.intellij.debugger.engine.*
import com.intellij.debugger.engine.evaluation.EvaluationContextImpl
import com.intellij.debugger.impl.DebuggerContextImpl
import com.intellij.debugger.impl.JvmSteppingCommandProvider
import com.intellij.debugger.impl.PositionUtil
import com.intellij.execution.process.ProcessOutputTypes
import com.sun.jdi.request.StepRequest
import org.jetbrains.kotlin.idea.debugger.stepping.*
import org.jetbrains.kotlin.idea.debugger.stepping.filter.KotlinOrdinaryMethodFilter
import org.jetbrains.kotlin.idea.debugger.stepping.filter.KotlinLambdaMethodFilter
import org.jetbrains.kotlin.idea.debugger.stepping.smartStepInto.KotlinLambdaSmartStepTarget
import org.jetbrains.kotlin.idea.debugger.stepping.smartStepInto.KotlinMethodSmartStepTarget
import org.jetbrains.kotlin.idea.debugger.stepping.smartStepInto.KotlinSmartStepIntoHandler
import org.jetbrains.kotlin.idea.debugger.test.util.SteppingInstruction
import org.jetbrains.kotlin.idea.debugger.test.util.SteppingInstructionKind
import org.jetbrains.kotlin.idea.debugger.test.util.renderSourcePosition
import org.jetbrains.kotlin.idea.util.application.runReadAction
import org.jetbrains.kotlin.psi.psiUtil.getElementTextWithContext
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstance

abstract class KotlinDescriptorTestCaseWithStepping : KotlinDescriptorTestCase() {
    private val dp: DebugProcessImpl
        get() = debugProcess ?: throw AssertionError("createLocalProcess() should be called before getDebugProcess()")

    @Volatile
    private var myEvaluationContext: EvaluationContextImpl? = null
    val evaluationContext get() = myEvaluationContext!!

    @Volatile
    private var myDebuggerContext: DebuggerContextImpl? = null
    protected open val debuggerContext get() = myDebuggerContext!!

    @Volatile
    private var myCommandProvider: KotlinSteppingCommandProvider? = null
    private val commandProvider get() = myCommandProvider!!

    private fun initContexts(suspendContext: SuspendContextImpl) {
        myEvaluationContext = createEvaluationContext(suspendContext)
        myDebuggerContext = createDebuggerContext(suspendContext)
        myCommandProvider = JvmSteppingCommandProvider.EP_NAME.extensions.firstIsInstance<KotlinSteppingCommandProvider>()
    }

    internal fun process(instructions: List<SteppingInstruction>) {
        instructions.forEach(this::process)
    }

    internal fun doOnBreakpoint(action: SuspendContextImpl.() -> Unit) {
        super.onBreakpoint {
            try {
                initContexts(it)
                it.printContext()
                it.action()
            } catch (e: AssertionError) {
                throw e
            } catch (e: Throwable) {
                e.printStackTrace()
                resume(it)
            }
        }
    }

    internal fun finish() {
        doOnBreakpoint {
            resume(this)
        }
    }

    private fun SuspendContextImpl.doStepInto(ignoreFilters: Boolean, smartStepFilter: MethodFilter?) {
        val stepIntoCommand =
            runReadAction { commandProvider.getStepIntoCommand(this, ignoreFilters, smartStepFilter, StepRequest.STEP_LINE) }
                ?: dp.createStepIntoCommand(this, ignoreFilters, smartStepFilter)

        dp.managerThread.schedule(stepIntoCommand)
    }

    private fun SuspendContextImpl.doStepOut() {
        val stepOutCommand = runReadAction { commandProvider.getStepOutCommand(this, debuggerContext) }
            ?: dp.createStepOutCommand(this)

        dp.managerThread.schedule(stepOutCommand)
    }

    private fun SuspendContextImpl.doStepOver(ignoreBreakpoints: Boolean = false) {
        val stepOverCommand = runReadAction { commandProvider.getStepOverCommand(this, ignoreBreakpoints, debuggerContext) }
            ?: dp.createStepOverCommand(this, ignoreBreakpoints)

        dp.managerThread.schedule(stepOverCommand)
    }

    private fun process(instruction: SteppingInstruction) {
        fun loop(count: Int, block: SuspendContextImpl.() -> Unit) {
            repeat(count) {
                doOnBreakpoint(block)
            }
        }

        when (instruction.kind) {
            SteppingInstructionKind.StepInto -> loop(instruction.arg) { doStepInto(false, null) }
            SteppingInstructionKind.StepOut -> loop(instruction.arg) { doStepOut() }
            SteppingInstructionKind.StepOver -> loop(instruction.arg) { doStepOver() }
            SteppingInstructionKind.ForceStepOver -> loop(instruction.arg) { doStepOver(ignoreBreakpoints = true) }
            SteppingInstructionKind.SmartStepInto -> loop(instruction.arg) { doSmartStepInto() }
            SteppingInstructionKind.SmartStepIntoByIndex -> doOnBreakpoint { doSmartStepInto(instruction.arg) }
            SteppingInstructionKind.Resume -> loop(instruction.arg) { resume(this) }
        }
    }

    private fun SuspendContextImpl.doSmartStepInto(chooseFromList: Int = 0) {
        this.doSmartStepInto(chooseFromList, false)
    }

    private fun SuspendContextImpl.printContext() {
        runReadAction {
            if (this.frameProxy == null) {
                return@runReadAction println("Context thread is null", ProcessOutputTypes.SYSTEM)
            }

            val sourcePosition = PositionUtil.getSourcePosition(this)
            println(renderSourcePosition(sourcePosition), ProcessOutputTypes.SYSTEM)
        }
    }

    private fun SuspendContextImpl.doSmartStepInto(chooseFromList: Int, ignoreFilters: Boolean) {
        val filters = createSmartStepIntoFilters()
        if (chooseFromList == 0) {
            filters.forEach {
                dp.managerThread!!.schedule(dp.createStepIntoCommand(this, ignoreFilters, it))
            }
        } else {
            try {
                dp.managerThread!!.schedule(dp.createStepIntoCommand(this, ignoreFilters, filters[chooseFromList - 1]))
            } catch (e: IndexOutOfBoundsException) {
                val elementText = runReadAction { debuggerContext.sourcePosition.elementAt.getElementTextWithContext() }
                throw AssertionError("Couldn't find smart step into command at: \n$elementText", e)
            }
        }
    }

    private fun createSmartStepIntoFilters() = runReadAction {
        val position = debuggerContext.sourcePosition

        val stepTargets = KotlinSmartStepIntoHandler().findSmartStepTargets(position)
        stepTargets.mapNotNull { stepTarget ->
            when (stepTarget) {
                is KotlinLambdaSmartStepTarget -> KotlinLambdaMethodFilter(stepTarget)
                is KotlinMethodSmartStepTarget -> KotlinOrdinaryMethodFilter(stepTarget)
                is MethodSmartStepTarget -> BasicStepMethodFilter(stepTarget.method, stepTarget.getCallingExpressionLines())
                else -> null
            }
        }
    }
}