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
import org.jetbrains.asm4.Type;
import org.jetbrains.asm4.tree.AbstractInsnNode;
import org.jetbrains.asm4.tree.FieldInsnNode;
import org.jetbrains.asm4.tree.MethodNode;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.jetbrains.jet.codegen.inline.MethodInliner.getPreviousNoLabelNoLine;

public class InlineFieldRemapper extends LambdaFieldRemapper {

    private final String oldOwnerType;

    private final String newOwnerType;

    private final Parameters parameters;

    private final Map<String, LambdaInfo> recapturedLambdas;

    public InlineFieldRemapper(String oldOwnerType, String newOwnerType, Parameters parameters, Map<String, LambdaInfo> recapturedLambdas) {
        this.oldOwnerType = oldOwnerType;
        this.newOwnerType = newOwnerType;
        this.parameters = parameters;
        this.recapturedLambdas = recapturedLambdas;
    }

    @Override
    public AbstractInsnNode doTransform(
            MethodNode node, FieldInsnNode fieldInsnNode, CapturedParamInfo capturedField
    ) {
        boolean isRecaptured = isRecapruredLambdaType(fieldInsnNode.owner);

        if (!isRecaptured && capturedField.getLambda() != null) {
            //strict inlining
            return super.doTransform(node, fieldInsnNode, capturedField);
        }

        AbstractInsnNode prev = getPreviousNoLabelNoLine(fieldInsnNode);

        assert prev.getType() == AbstractInsnNode.VAR_INSN || prev.getType() == AbstractInsnNode.FIELD_INSN;
        AbstractInsnNode loadThis = prev;
        assert /*loadThis.var == info.getCapturedVarsSize() - 1 && */loadThis.getOpcode() == Opcodes.ALOAD || loadThis.getOpcode() == Opcodes.GETSTATIC;

        int opcode = Opcodes.GETSTATIC;

        String descriptor = Type.getObjectType(newOwnerType).getDescriptor();

        FieldInsnNode thisStub = new FieldInsnNode(opcode, newOwnerType, "$$$this", descriptor);

        node.instructions.insertBefore(loadThis, thisStub);
        node.instructions.remove(loadThis);

        fieldInsnNode.owner = newOwnerType;
        fieldInsnNode.name = isRecaptured || capturedField.getRecapturedFrom() != null ? LambdaTransformer.getNewFieldName(capturedField.getFieldName()) : capturedField.getFieldName();

        return fieldInsnNode;
    }

    @Override
    public List<CapturedParamInfo> markRecaptured(List<CapturedParamInfo> originalCaptured, LambdaInfo lambda) {
        List<CapturedParamInfo> captured = parameters.getCaptured();
        for (CapturedParamInfo originalField : originalCaptured) {
            for (CapturedParamInfo capturedParamInfo : captured) {
                if (capturedParamInfo.getRecapturedFrom() == lambda) {
                    if (capturedParamInfo.getFieldName().equals(LambdaTransformer.getNewFieldName(originalField.getFieldName()))) {
                        originalField.setRecapturedFrom(lambda);//just mark recaptured
                    }
                }
            }
        }
        return originalCaptured;
    }

    @Override
    public boolean canProcess(String owner, String currentLambdaType) {
        return super.canProcess(owner, currentLambdaType) || isRecapruredLambdaType(owner);
    }

    private boolean isRecapruredLambdaType(String owner) {
        return recapturedLambdas.containsKey(owner);
    }


    @Nullable
    @Override
    public CapturedParamInfo findField(FieldInsnNode fieldInsnNode, Collection<CapturedParamInfo> captured) {
        if (!isRecapruredLambdaType(fieldInsnNode.owner)) {
            return super.findField(fieldInsnNode, captured);
        } else {
            LambdaInfo info = recapturedLambdas.get(fieldInsnNode.owner);
            return super.findField(fieldInsnNode, info.getCapturedVars());
        }
    }
}
