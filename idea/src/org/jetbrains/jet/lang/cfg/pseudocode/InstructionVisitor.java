package org.jetbrains.jet.lang.cfg.pseudocode;

/**
 * @author abreslav
 */
public class InstructionVisitor {
    public void visitReadValue(ReadValueInstruction instruction) {
        visitInstructionWithNext(instruction);
    }

    public void visitFunctionLiteralValue(FunctionLiteralValueInstruction instruction) {
        visitReadValue(instruction);
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

    public void visitNondeterministicJump(NondeterministicJumpInstruction instruction) {
        visitJump(instruction);
    }

    public void visitUnsupportedElementInstruction(UnsupportedElementInstruction instruction) {
        visitInstructionWithNext(instruction);
    }

    public void visitSubroutineExit(SubroutineExitInstruction instruction) {
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

    public void visitWriteValue(WriteValueInstruction writeValueInstruction) {
        visitInstructionWithNext(writeValueInstruction);
    }

    public void visitReadUnitValue(ReadUnitValueInstruction instruction) {
        visitInstructionWithNext(instruction);
    }
}
