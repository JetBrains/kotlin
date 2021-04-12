/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.interpreter.stack

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.interpreter.CompoundInstruction
import org.jetbrains.kotlin.ir.interpreter.Instruction
import org.jetbrains.kotlin.ir.interpreter.SimpleInstruction
import org.jetbrains.kotlin.ir.interpreter.state.State
import org.jetbrains.kotlin.ir.interpreter.state.StateWithClosure
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.util.fileOrNull

internal class CallStack {
    private val frames = mutableListOf<Frame>()
    private fun getCurrentFrame() = frames.last()
    internal fun getCurrentFrameOwner() = frames.last().currentSubFrameOwner

    fun newFrame(frameOwner: IrElement, instructions: List<Instruction>, irFile: IrFile? = null) {
        val newFrame = SubFrame(instructions.toMutableList(), frameOwner)
        frames.add(Frame(newFrame, irFile))
    }

    fun newFrame(frameOwner: IrFunction, instructions: List<Instruction>) {
        val newFrame = SubFrame(instructions.toMutableList(), frameOwner)
        frames.add(Frame(newFrame, frameOwner.fileOrNull))
    }

    fun newSubFrame(frameOwner: IrElement, instructions: List<Instruction>) {
        val newFrame = SubFrame(instructions.toMutableList(), frameOwner)
        getCurrentFrame().addSubFrame(newFrame)
    }

    fun dropFrame() {
        frames.removeLast()
    }

    fun dropFrameAndCopyResult() {
        val result = peekState() ?: return dropFrame()
        popState()
        dropFrame()
        pushState(result)
    }

    fun dropSubFrame() {
        getCurrentFrame().removeSubFrame()
    }

    fun returnFromFrameWithResult(irReturn: IrReturn) {
        val result = popState()
        var frameOwner = getCurrentFrameOwner()
        while (frameOwner != irReturn.returnTargetSymbol.owner) {
            when (frameOwner) {
                is IrTry -> {
                    dropSubFrame()
                    pushState(result)
                    addInstruction(SimpleInstruction(irReturn))
                    addInstruction(CompoundInstruction(frameOwner.finallyExpression))
                    return
                }
                is IrCatch -> {
                    val tryBlock = getCurrentFrame().dropInstructions()!!.element as IrTry// last instruction in `catch` block is `try`
                    dropSubFrame()
                    pushState(result)
                    addInstruction(SimpleInstruction(irReturn))
                    addInstruction(CompoundInstruction(tryBlock.finallyExpression))
                    return
                }
                else -> {
                    dropSubFrame()
                    if (getCurrentFrame().hasNoSubFrames() && frameOwner != irReturn.returnTargetSymbol.owner) dropFrame()
                    frameOwner = getCurrentFrameOwner()
                }
            }
        }

        dropFrame()
        // check that last frame is not a function itself; use case for proxyInterpret
        if (frames.size == 0) newFrame(irReturn, emptyList()) // just stub frame
        pushState(result)
    }

    fun unrollInstructionsForBreakContinue(breakOrContinue: IrBreakContinue) {
        var frameOwner = getCurrentFrameOwner()
        while (frameOwner != breakOrContinue.loop) {
            when (frameOwner) {
                is IrTry -> {
                    getCurrentFrame().removeSubFrameWithoutDataPropagation()
                    addInstruction(CompoundInstruction(breakOrContinue))
                    newSubFrame(frameOwner, listOf(SimpleInstruction(frameOwner))) // will be deleted when interpret 'try'
                    return
                }
                is IrCatch -> {
                    val tryInstruction = getCurrentFrame().dropInstructions()!! // last instruction in `catch` block is `try`
                    getCurrentFrame().removeSubFrameWithoutDataPropagation()
                    addInstruction(CompoundInstruction(breakOrContinue))
                    newSubFrame(tryInstruction.element!!, listOf(tryInstruction))  // will be deleted when interpret 'try'
                    return
                }
                else -> {
                    getCurrentFrame().removeSubFrameWithoutDataPropagation()
                    frameOwner = getCurrentFrameOwner()
                }
            }
        }

        when (breakOrContinue) {
            is IrBreak -> getCurrentFrame().removeSubFrameWithoutDataPropagation() // drop loop
            else -> if (breakOrContinue.loop is IrDoWhileLoop) {
                addInstruction(SimpleInstruction(breakOrContinue.loop))
                addInstruction(CompoundInstruction(breakOrContinue.loop.condition))
            } else {
                addInstruction(CompoundInstruction(breakOrContinue.loop))
            }
        }
    }

    fun dropFramesUntilTryCatch() {
        val exception = popState()
        var frameOwner = getCurrentFrameOwner()
        while (frames.isNotEmpty()) {
            val frame = getCurrentFrame()
            while (!frame.hasNoSubFrames()) {
                frameOwner = frame.currentSubFrameOwner
                when (frameOwner) {
                    is IrTry -> {
                        dropSubFrame()  // drop all instructions that left
                        newSubFrame(frameOwner, listOf())
                        addInstruction(SimpleInstruction(frameOwner)) // to evaluate finally at the end
                        frameOwner.catches.reversed().forEach { addInstruction(CompoundInstruction(it)) }
                        pushState(exception)
                        return
                    }
                    is IrCatch -> {
                        // in case of exception in catch, drop everything except of last `try` instruction
                        addInstruction(frame.dropInstructions()!!)
                        pushState(exception)
                        return
                    }
                    else -> frame.removeSubFrameWithoutDataPropagation()
                }
            }
            dropFrame()
        }

        if (frames.size == 0) newFrame(frameOwner, emptyList()) // just stub frame
        pushState(exception)
    }

    fun hasNoInstructions() = frames.isEmpty() || (frames.size == 1 && frames.first().hasNoInstructions())

    fun addInstruction(instruction: Instruction) {
        getCurrentFrame().addInstruction(instruction)
    }

    fun popInstruction(): Instruction {
        return getCurrentFrame().popInstruction()
    }

    fun pushState(state: State) {
        getCurrentFrame().pushState(state)
    }

    fun popState(): State = getCurrentFrame().popState()
    fun peekState(): State? = getCurrentFrame().peekState()

    fun addVariable(variable: Variable) {
        getCurrentFrame().addVariable(variable)
    }

    fun getVariable(symbol: IrSymbol): Variable = getCurrentFrame().getVariable(symbol)
    fun containsVariable(symbol: IrSymbol): Boolean = getCurrentFrame().containsVariable(symbol)

    fun storeUpValues(state: StateWithClosure) {
        // TODO save only necessary declarations
        state.upValues.addAll(getCurrentFrame().getAll().toMutableList())
    }

    fun loadUpValues(state: StateWithClosure) {
        state.upValues.forEach { addVariable(it) }
    }

    fun copyUpValuesFromPreviousFrame() {
        frames[frames.size - 2].getAll().forEach { if (!containsVariable(it.symbol)) addVariable(it) }
    }

    fun getStackTrace(): List<String> {
        return frames.map { it.toString() }.filter { it != Frame.NOT_DEFINED }
    }

    fun getFileAndPositionInfo(): String {
        return frames[frames.size - 2].getFileAndPositionInfo()
    }

    fun getStackCount(): Int = frames.size
}
