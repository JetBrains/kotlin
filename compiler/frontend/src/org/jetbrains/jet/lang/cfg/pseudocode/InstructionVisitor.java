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

public class InstructionVisitor {
    public void visitReadValue(ReadValueInstruction instruction) {
        visitInstructionWithNext(instruction);
    }

    public void visitLocalFunctionDeclarationInstruction(LocalFunctionDeclarationInstruction instruction) {
        visitInstructionWithNext(instruction);
    }

    public void visitVariableDeclarationInstruction(VariableDeclarationInstruction instruction) {
        visitInstructionWithNext(instruction);
    }

    public void visitUnconditionalJump(UnconditionalJumpInstruction instruction) {
        visitJump(instruction);
    }

    public void visitConditionalJump(ConditionalJumpInstruction instruction) {
        visitJump(instruction);
    }

    public void visitReturnValue(ReturnValueInstruction instruction) {
        visitJump(instruction);
    }

    public void visitReturnNoValue(ReturnNoValueInstruction instruction) {
        visitJump(instruction);
    }

    public void visitThrowExceptionInstruction(ThrowExceptionInstruction instruction) {
        visitJump(instruction);
    }

    public void visitNondeterministicJump(NondeterministicJumpInstruction instruction) {
        visitInstruction(instruction);
    }

    public void visitUnsupportedElementInstruction(UnsupportedElementInstruction instruction) {
        visitInstructionWithNext(instruction);
    }

    public void visitSubroutineExit(SubroutineExitInstruction instruction) {
        visitInstruction(instruction);
    }

    public void visitSubroutineSink(SubroutineSinkInstruction instruction) {
        visitInstruction(instruction);
    }

    public void visitJump(AbstractJumpInstruction instruction) {
        visitInstruction(instruction);
    }

    public void visitInstructionWithNext(InstructionWithNext instruction) {
        visitInstruction(instruction);
    }

    public void visitInstruction(Instruction instruction) {
    }

    public void visitSubroutineEnter(SubroutineEnterInstruction instruction) {
        visitInstructionWithNext(instruction);
    }

    public void visitWriteValue(WriteValueInstruction instruction) {
        visitInstructionWithNext(instruction);
    }

    public void visitLoadUnitValue(LoadUnitValueInstruction instruction) {
        visitInstructionWithNext(instruction);
    }

    public void visitCallInstruction(CallInstruction instruction) {
        visitInstructionWithNext(instruction);
    }

    public void visitCompilationErrorInstruction(CompilationErrorInstruction instruction) {
        visitInstructionWithNext(instruction);
    }

    public void visitMarkInstruction(MarkInstruction instruction) {
        visitInstructionWithNext(instruction);
    }

}
