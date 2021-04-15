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
    private val currentFrame get() = frames.last()
    internal val currentFrameOwner get() = currentFrame.currentSubFrameOwner

    fun newFrame(frameOwner: IrElement, irFile: IrFile? = null) {
        val newFrame = SubFrame(frameOwner)
        frames.add(Frame(newFrame, irFile))
    }

    fun newFrame(frameOwner: IrFunction) {
        val newFrame = SubFrame(frameOwner)
        frames.add(Frame(newFrame, frameOwner.fileOrNull))
    }

    fun newSubFrame(frameOwner: IrElement) {
        val newFrame = SubFrame(frameOwner)
        currentFrame.addSubFrame(newFrame)
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
        currentFrame.removeSubFrame()
    }

    fun returnFromFrameWithResult(irReturn: IrReturn) {
        val result = popState()
        var frameOwner = currentFrameOwner
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
                    val tryBlock = currentFrame.dropInstructions()!!.element as IrTry// last instruction in `catch` block is `try`
                    dropSubFrame()
                    pushState(result)
                    addInstruction(SimpleInstruction(irReturn))
                    addInstruction(CompoundInstruction(tryBlock.finallyExpression))
                    return
                }
                else -> {
                    dropSubFrame()
                    if (currentFrame.hasNoSubFrames() && frameOwner != irReturn.returnTargetSymbol.owner) dropFrame()
                    frameOwner = currentFrameOwner
                }
            }
        }

        dropFrame()
        // check that last frame is not a function itself; use case for proxyInterpret
        if (frames.size == 0) newFrame(irReturn) // just stub frame
        pushState(result)
    }

    fun unrollInstructionsForBreakContinue(breakOrContinue: IrBreakContinue) {
        var frameOwner = currentFrameOwner
        while (frameOwner != breakOrContinue.loop) {
            when (frameOwner) {
                is IrTry -> {
                    currentFrame.removeSubFrameWithoutDataPropagation()
                    addInstruction(CompoundInstruction(breakOrContinue))
                    newSubFrame(frameOwner) // will be deleted when interpret 'try'
                    addInstruction(SimpleInstruction(frameOwner))
                    return
                }
                is IrCatch -> {
                    val tryInstruction = currentFrame.dropInstructions()!! // last instruction in `catch` block is `try`
                    currentFrame.removeSubFrameWithoutDataPropagation()
                    addInstruction(CompoundInstruction(breakOrContinue))
                    newSubFrame(tryInstruction.element!!)  // will be deleted when interpret 'try'
                    addInstruction(tryInstruction)
                    return
                }
                else -> {
                    currentFrame.removeSubFrameWithoutDataPropagation()
                    frameOwner = currentFrameOwner
                }
            }
        }

        when (breakOrContinue) {
            is IrBreak -> currentFrame.removeSubFrameWithoutDataPropagation() // drop loop
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
        var frameOwner = currentFrameOwner
        while (frames.isNotEmpty()) {
            val frame = currentFrame
            while (!frame.hasNoSubFrames()) {
                frameOwner = frame.currentSubFrameOwner
                when (frameOwner) {
                    is IrTry -> {
                        dropSubFrame()  // drop all instructions that left
                        newSubFrame(frameOwner)
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

        if (frames.size == 0) newFrame(frameOwner) // just stub frame
        pushState(exception)
    }

    fun hasNoInstructions() = frames.isEmpty() || (frames.size == 1 && frames.first().hasNoInstructions())

    fun addInstruction(instruction: Instruction) {
        currentFrame.addInstruction(instruction)
    }

    fun popInstruction(): Instruction {
        return currentFrame.popInstruction()
    }

    fun pushState(state: State) {
        currentFrame.pushState(state)
    }

    fun popState(): State = currentFrame.popState()
    fun peekState(): State? = currentFrame.peekState()

    fun addVariable(variable: Variable) {
        currentFrame.addVariable(variable)
    }

    fun getState(symbol: IrSymbol): State = currentFrame.getState(symbol)
    fun setState(symbol: IrSymbol, newState: State) = currentFrame.setState(symbol, newState)
    fun containsVariable(symbol: IrSymbol): Boolean = currentFrame.containsVariable(symbol)

    fun storeUpValues(state: StateWithClosure) {
        // TODO save only necessary declarations
        state.upValues.addAll(currentFrame.getAll().toMutableList())
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
