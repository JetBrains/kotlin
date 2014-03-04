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

package org.jetbrains.jet.codegen.inline;

import org.jetbrains.annotations.Nullable;
import org.jetbrains.asm4.Opcodes;
import org.jetbrains.asm4.tree.AbstractInsnNode;
import org.jetbrains.asm4.tree.FieldInsnNode;
import org.jetbrains.asm4.tree.MethodNode;
import org.jetbrains.asm4.tree.VarInsnNode;

import java.util.Collection;
import java.util.List;

import static org.jetbrains.jet.codegen.inline.MethodInliner.getPreviousNoLabelNoLine;

public class LambdaFieldRemapper {

    public AbstractInsnNode doTransform(MethodNode node, FieldInsnNode fieldInsnNode, CapturedParamInfo capturedField) {
        AbstractInsnNode loadThis = getPreviousThis(fieldInsnNode);

        int opcode = fieldInsnNode.getOpcode() == Opcodes.GETFIELD ? capturedField.getType().getOpcode(Opcodes.ILOAD) : capturedField.getType().getOpcode(Opcodes.ISTORE);
        VarInsnNode newInstruction = new VarInsnNode(opcode, capturedField.getIndex());

        node.instructions.remove(loadThis); //remove aload this
        node.instructions.insertBefore(fieldInsnNode, newInstruction);
        node.instructions.remove(fieldInsnNode); //remove aload field

        return newInstruction;
    }

    protected static AbstractInsnNode getPreviousThis(FieldInsnNode fieldInsnNode) {
        AbstractInsnNode loadThis = getPreviousNoLabelNoLine(fieldInsnNode);

        assert loadThis.getType() == AbstractInsnNode.VAR_INSN || loadThis.getType() == AbstractInsnNode.FIELD_INSN :
                "Field access instruction should go after load this but goes after " + loadThis;
        assert loadThis.getOpcode() == Opcodes.ALOAD || loadThis.getOpcode() == Opcodes.GETSTATIC :
                "This should be loaded by ALOAD or GETSTATIC but " + loadThis.getOpcode();
        return loadThis;
    }

    public List<CapturedParamInfo> markRecaptured(List<CapturedParamInfo> originalCaptured, LambdaInfo lambda) {
        return originalCaptured;
    }


    public boolean canProcess(String owner, String currentLambdaType) {
        return owner.equals(currentLambdaType);
    }

    @Nullable
    public CapturedParamInfo findField(FieldInsnNode fieldInsnNode, Collection<CapturedParamInfo> captured) {
        for (CapturedParamInfo valueDescriptor : captured) {
            if (valueDescriptor.getFieldName().equals(fieldInsnNode.name)) {
                return valueDescriptor;
            }
        }
        return null;
    }
}
