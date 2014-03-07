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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.asm4.Opcodes;
import org.jetbrains.asm4.Type;
import org.jetbrains.asm4.tree.AbstractInsnNode;
import org.jetbrains.asm4.tree.FieldInsnNode;
import org.jetbrains.asm4.tree.MethodNode;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public class RegeneratedLambdaFieldRemapper extends LambdaFieldRemapper {

    private final String oldOwnerType;

    private final String newOwnerType;

    private final Parameters parameters;

    private final Map<String, LambdaInfo> recapturedLambdas;

    public RegeneratedLambdaFieldRemapper(
            String oldOwnerType,
            String newOwnerType,
            Parameters parameters,
            Map<String, LambdaInfo> recapturedLambdas,
            LambdaFieldRemapper remapper
    ) {
        super(oldOwnerType, remapper, parameters);
        this.oldOwnerType = oldOwnerType;
        this.newOwnerType = newOwnerType;
        this.parameters = parameters;
        this.recapturedLambdas = recapturedLambdas;
    }

    @Override
    public AbstractInsnNode doTransform(
            MethodNode node, FieldInsnNode fieldInsnNode, CapturedParamInfo capturedField
    ) {
        boolean isRecaptured = isRecapturedLambdaType(fieldInsnNode.owner);

        if (!isRecaptured && capturedField.getLambda() != null) {
            //strict inlining
            return super.doTransform(node, fieldInsnNode, capturedField);
        }

        AbstractInsnNode loadThis = getPreviousThis(fieldInsnNode);

        int opcode = Opcodes.GETSTATIC;

        String descriptor = Type.getObjectType(newOwnerType).getDescriptor();

        //HACK: it would be reverted again to ALOAD 0 later
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
        return super.canProcess(owner, currentLambdaType) || isRecapturedLambdaType(owner);
    }

    private boolean isRecapturedLambdaType(String owner) {
        return recapturedLambdas.containsKey(owner);
    }

    @Nullable
    @Override
    public CapturedParamInfo findField(FieldInsnNode fieldInsnNode, Collection<CapturedParamInfo> captured) {
        if (isRecapturedLambdaType(fieldInsnNode.owner)) {
            LambdaInfo info = recapturedLambdas.get(fieldInsnNode.owner);
            return super.findField(fieldInsnNode, info.getCapturedVars());
        }
        else {
            return super.findField(fieldInsnNode, captured);
        }
    }

    @Override
    public boolean shouldPatch(@NotNull FieldInsnNode node) {
        //parent is inlined so we need patch instruction chain
        return shouldPatchByMe(node) || parent.shouldPatch(node);
    }

    private boolean shouldPatchByMe(@NotNull FieldInsnNode node) {
        //parent is inlined so we need patch instruction chain
        //aloading inlined this
        return parent.isRoot() && node.owner.equals(getLambdaInternalName()) && node.name.equals("this$0");
    }

    @NotNull
    @Override
    public AbstractInsnNode patch(@NotNull FieldInsnNode fieldInsnNode, @NotNull MethodNode node) {
        if (!shouldPatchByMe(fieldInsnNode)) {
            return parent.patch(fieldInsnNode, node);
        }
        //parent is inlined so we need patch instruction chain
        AbstractInsnNode previous = fieldInsnNode.getPrevious();
        AbstractInsnNode nextInstruction = fieldInsnNode.getNext();
        if (!(nextInstruction instanceof FieldInsnNode)) {
            throw new IllegalStateException(
                    "Instruction after inlined one should be field access: " + nextInstruction);
        }
        if (!(previous instanceof FieldInsnNode)) {
            throw new IllegalStateException("Instruction before inlined one should be field access: " + previous);
        }
        FieldInsnNode next = (FieldInsnNode) nextInstruction;
        node.instructions.remove(next.getPrevious());
        next.owner = Type.getType(((FieldInsnNode) previous).desc).getInternalName();
        next.name = node.name.equals("this$0") ? node.name : LambdaTransformer.getNewFieldName(next.name);

        return next;
    }
}
