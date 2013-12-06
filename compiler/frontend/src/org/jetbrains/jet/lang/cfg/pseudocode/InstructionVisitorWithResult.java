/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

package org.jetbrains.jet.lang.cfg.pseudocode;

public abstract class InstructionVisitorWithResult<R> {
    public abstract R visitInstruction(Instruction instruction);

    public R visitReadValue(ReadValueInstruction instruction) {
        return visitInstructionWithNext(instruction);
    }

    public R visitLocalFunctionDeclarationInstruction(LocalFunctionDeclarationInstruction instruction) {
        return visitInstructionWithNext(instruction);
    }

    public R visitVariableDeclarationInstruction(VariableDeclarationInstruction instruction) {
        return visitInstructionWithNext(instruction);
    }

    public R visitUnconditionalJump(UnconditionalJumpInstruction instruction) {
        return visitJump(instruction);
    }

    public R visitConditionalJump(ConditionalJumpInstruction instruction) {
        return visitJump(instruction);
    }

    public R visitReturnValue(ReturnValueInstruction instruction) {
        return visitJump(instruction);
    }

    public R visitReturnNoValue(ReturnNoValueInstruction instruction) {
        return visitJump(instruction);
    }

    public R visitThrowExceptionInstruction(ThrowExceptionInstruction instruction) {
        return visitJump(instruction);
    }

    public R visitNondeterministicJump(NondeterministicJumpInstruction instruction) {
        return visitInstruction(instruction);
    }

    public R visitUnsupportedElementInstruction(UnsupportedElementInstruction instruction) {
        return visitInstructionWithNext(instruction);
    }

    public R visitSubroutineExit(SubroutineExitInstruction instruction) {
        return visitInstruction(instruction);
    }

    public R visitSubroutineSink(SubroutineSinkInstruction instruction) {
        return visitInstruction(instruction);
    }

    public R visitJump(AbstractJumpInstruction instruction) {
        return visitInstruction(instruction);
    }

    public R visitInstructionWithNext(InstructionWithNext instruction) {
        return visitInstruction(instruction);
    }

    public R visitSubroutineEnter(SubroutineEnterInstruction instruction) {
        return visitInstructionWithNext(instruction);
    }

    public R visitWriteValue(WriteValueInstruction instruction) {
        return visitInstructionWithNext(instruction);
    }

    public R visitLoadUnitValue(LoadUnitValueInstruction instruction) {
        return visitInstructionWithNext(instruction);
    }

    public R visitCallInstruction(CallInstruction instruction) {
        return visitInstructionWithNext(instruction);
    }

    public R visitCompilationErrorInstruction(CompilationErrorInstruction instruction) {
        return visitInstructionWithNext(instruction);
    }

    public R visitMarkInstruction(MarkInstruction instruction) {
        return visitInstructionWithNext(instruction);
    }

}
