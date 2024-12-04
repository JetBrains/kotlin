/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.interpreter.stack

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.interpreter.Instruction
import org.jetbrains.kotlin.ir.interpreter.handleAndDropResult
import org.jetbrains.kotlin.ir.interpreter.pushCompoundInstruction
import org.jetbrains.kotlin.ir.interpreter.pushSimpleInstruction
import org.jetbrains.kotlin.ir.interpreter.state.State
import org.jetbrains.kotlin.ir.interpreter.state.StateWithClosure
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.util.fileOrNull

internal class CallStack {
    private val frames = ArrayDeque<Frame>()
    private val currentFrame get() = frames.last()
    internal val currentFrameOwner get() = currentFrame.currentSubFrameOwner

    fun newFrame(frameOwner: IrElement, irFile: IrFile? = null) {
        frames.add(Frame(frameOwner, irFile))
    }

    fun newFrame(frameOwner: IrFunction) {
        frames.add(Frame(frameOwner, frameOwner.fileOrNull))
    }

    fun newSubFrame(frameOwner: IrElement) {
        currentFrame.addSubFrame(frameOwner)
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
        val returnTarget = irReturn.returnTargetSymbol.owner
        var frameOwner = currentFrameOwner
        while (frameOwner != returnTarget) {
            when (frameOwner) {
                is IrTry -> {
                    dropSubFrame()
                    pushState(result)
                    pushSimpleInstruction(irReturn)
                    frameOwner.finallyExpression?.handleAndDropResult(this)
                    return
                }
                is IrCatch -> {
                    val tryBlock = currentFrame.dropInstructions()!!.element as IrTry// last instruction in `catch` block is `try`
                    dropSubFrame()
                    pushState(result)
                    pushSimpleInstruction(irReturn)
                    tryBlock.finallyExpression?.handleAndDropResult(this)
                    return
                }
                else -> {
                    dropSubFrame()
                    if (currentFrame.hasNoSubFrames() && frameOwner != returnTarget) dropFrame()
                    frameOwner = currentFrameOwner
                }
            }
        }

        currentFrame.dropInstructions()
        pushSimpleInstruction(returnTarget)
        if (returnTarget !is IrConstructor) pushState(result)
    }

    fun unrollInstructionsForBreakContinue(breakOrContinue: IrBreakContinue) {
        var frameOwner = currentFrameOwner
        while (frameOwner != breakOrContinue.loop) {
            when (frameOwner) {
                is IrTry -> {
                    currentFrame.removeSubFrameWithoutDataPropagation()
                    pushCompoundInstruction(breakOrContinue)
                    newSubFrame(frameOwner) // will be deleted when interpret 'try'
                    pushSimpleInstruction(frameOwner)
                    return
                }
                is IrCatch -> {
                    val tryInstruction = currentFrame.dropInstructions()!! // last instruction in `catch` block is `try`
                    currentFrame.removeSubFrameWithoutDataPropagation()
                    pushCompoundInstruction(breakOrContinue)
                    newSubFrame(tryInstruction.element!!)  // will be deleted when interpret 'try'
                    pushInstruction(tryInstruction)
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
                pushSimpleInstruction(breakOrContinue.loop)
                pushCompoundInstruction(breakOrContinue.loop.condition)
            } else {
                pushCompoundInstruction(breakOrContinue.loop)
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
                        pushSimpleInstruction(frameOwner) // to evaluate finally at the end
                        frameOwner.catches.reversed().forEach { pushCompoundInstruction(it) }
                        pushState(exception)
                        return
                    }
                    is IrCatch -> {
                        // in case of exception in catch, drop everything except of last `try` instruction
                        pushInstruction(frame.dropInstructions()!!)
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
    fun pushInstruction(instruction: Instruction) = currentFrame.pushInstruction(instruction)
    fun popInstruction(): Instruction = currentFrame.popInstruction()

    fun pushState(state: State) = currentFrame.pushState(state)
    fun popState(): State = currentFrame.popState()
    fun peekState(): State? = currentFrame.peekState()

    fun storeState(symbol: IrSymbol, state: State?) = currentFrame.storeState(symbol, state)
    private fun storeState(symbol: IrSymbol, variable: Variable) = currentFrame.storeState(symbol, variable)
    fun containsStateInMemory(symbol: IrSymbol): Boolean = currentFrame.containsStateInMemory(symbol)
    fun loadState(symbol: IrSymbol): State = currentFrame.loadState(symbol)
    fun rewriteState(symbol: IrSymbol, newState: State) = currentFrame.rewriteState(symbol, newState)

    // TODO save only necessary declarations
    fun storeUpValues(state: StateWithClosure) = currentFrame.copyMemoryInto(state)
    fun loadUpValues(state: StateWithClosure) = state.upValues.forEach { (symbol, variable) -> storeState(symbol, variable) }
    fun copyUpValuesFromPreviousFrame() = frames[frames.size - 2].copyMemoryInto(currentFrame)

    fun getStackTrace(): List<String> = frames.map { it.toString() }.filter { it != Frame.NOT_DEFINED }
    fun getFileAndPositionInfo(): String = frames[frames.size - 2].getFileAndPositionInfo()
    fun getStackCount(): Int = frames.size
}
