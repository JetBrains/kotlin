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
import org.jetbrains.asm4.tree.AbstractInsnNode;
import org.jetbrains.asm4.tree.FieldInsnNode;
import org.jetbrains.asm4.tree.MethodNode;
import org.jetbrains.jet.codegen.StackValue;

import java.util.Collection;
import java.util.List;

public class FieldRemapper {

    private final String lambdaInternalName;

    protected FieldRemapper parent;

    private final Parameters params;

    public FieldRemapper(@Nullable String lambdaInternalName, @Nullable FieldRemapper parent, @NotNull Parameters methodParams) {
        this.lambdaInternalName = lambdaInternalName;
        this.parent = parent;
        params = methodParams;
    }

    public boolean canProcess(@NotNull String fieldOwner, boolean isFolding) {
        return fieldOwner.equals(getLambdaInternalName());
    }

    @Nullable
    public AbstractInsnNode foldFieldAccessChainIfNeeded(
            @NotNull List<AbstractInsnNode> capturedFieldAccess,
            @NotNull MethodNode node
    ) {
        if (capturedFieldAccess.size() == 1) {
            //just aload
            return null;
        }

        return foldFieldAccessChainIfNeeded(capturedFieldAccess, 1, node);
    }

    @Nullable
    private AbstractInsnNode foldFieldAccessChainIfNeeded(
            @NotNull List<AbstractInsnNode> capturedFieldAccess,
            int currentInstruction,
            @NotNull MethodNode node
    ) {
        AbstractInsnNode transformed = null;
        boolean checkParent = !isRoot() && currentInstruction < capturedFieldAccess.size() - 1;
        if (checkParent) {
            transformed = parent.foldFieldAccessChainIfNeeded(capturedFieldAccess, currentInstruction + 1, node);
        }

        if (transformed == null) {
            //if parent couldn't transform
            FieldInsnNode insnNode = (FieldInsnNode) capturedFieldAccess.get(currentInstruction);
            if (canProcess(insnNode.owner, true)) {
                insnNode.name = "$$$" + insnNode.name;
                insnNode.setOpcode(Opcodes.GETSTATIC);

                AbstractInsnNode next = capturedFieldAccess.get(0);
                while (next != insnNode) {
                    AbstractInsnNode toDelete = next;
                    next = next.getNext();
                    node.instructions.remove(toDelete);
                }

                transformed = capturedFieldAccess.get(capturedFieldAccess.size() - 1);
            }
        }

        return transformed;
    }

    public CapturedParamInfo findField(@NotNull FieldInsnNode fieldInsnNode) {
        return findField(fieldInsnNode, params.getCaptured());
    }

    @Nullable
    protected CapturedParamInfo findField(@NotNull FieldInsnNode fieldInsnNode, @NotNull Collection<CapturedParamInfo> captured) {
        for (CapturedParamInfo valueDescriptor : captured) {
            if (valueDescriptor.getOriginalFieldName().equals(fieldInsnNode.name) && fieldInsnNode.owner.equals(valueDescriptor.getContainingLambdaName())) {
                return valueDescriptor;
            }
        }
        return null;
    }

    public FieldRemapper getParent() {
        return parent;
    }

    public String getLambdaInternalName() {
        return lambdaInternalName;
    }

    public boolean isRoot() {
        return parent == null;
    }

    @Nullable
    public StackValue getFieldForInline(@NotNull FieldInsnNode node, @Nullable StackValue prefix) {
        CapturedParamInfo field = MethodInliner.findCapturedField(node, this);
        return field.getRemapValue();
    }
}
