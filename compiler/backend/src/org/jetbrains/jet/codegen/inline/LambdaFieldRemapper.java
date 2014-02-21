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
        AbstractInsnNode prev = getPreviousNoLabelNoLine(fieldInsnNode);

        assert prev.getType() == AbstractInsnNode.VAR_INSN || prev.getType() == AbstractInsnNode.FIELD_INSN;
        AbstractInsnNode loadThis = prev;
        int opcode1 = loadThis.getOpcode();
        assert /*loadThis.var == info.getCapturedVarsSize() - 1 && */opcode1 == Opcodes.ALOAD || opcode1 == Opcodes.GETSTATIC;

        int opcode = fieldInsnNode.getOpcode() == Opcodes.GETFIELD ? capturedField.getType().getOpcode(Opcodes.ILOAD) : capturedField.getType().getOpcode(Opcodes.ISTORE);
        VarInsnNode insn = new VarInsnNode(opcode, capturedField.getIndex());

        node.instructions.remove(prev); //remove aload this
        node.instructions.insertBefore(fieldInsnNode, insn);
        node.instructions.remove(fieldInsnNode); //remove aload field

        return insn;
    }

    public List<CapturedParamInfo> markRecaptured(List<CapturedParamInfo> originalCaptured, LambdaInfo lambda) {
        return originalCaptured;
    }


    public boolean canProcess(String owner, String currentLambdaType) {
        return owner.equals(currentLambdaType);
    }

    @Nullable
    public CapturedParamInfo findField(FieldInsnNode fieldInsnNode, Collection<CapturedParamInfo> captured) {
        String name = fieldInsnNode.name;
        CapturedParamInfo result = null;
        for (CapturedParamInfo valueDescriptor : captured) {
            if (valueDescriptor.getFieldName().equals(name)) {
                result = valueDescriptor;
                break;
            }
        }
        return result;
    }
}
