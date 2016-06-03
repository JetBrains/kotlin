/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.codegen.inline;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.codegen.StackValue;
import org.jetbrains.org.objectweb.asm.Opcodes;
import org.jetbrains.org.objectweb.asm.tree.AbstractInsnNode;
import org.jetbrains.org.objectweb.asm.tree.FieldInsnNode;
import org.jetbrains.org.objectweb.asm.tree.MethodNode;

import java.util.Collection;
import java.util.List;

public class FieldRemapper {
    protected FieldRemapper parent;
    private final String lambdaInternalName;
    private final Parameters params;

    public FieldRemapper(@Nullable String lambdaInternalName, @Nullable FieldRemapper parent, @NotNull Parameters methodParams) {
        this.lambdaInternalName = lambdaInternalName;
        this.parent = parent;
        this.params = methodParams;
    }

    protected boolean canProcess(@NotNull String fieldOwner, @NotNull String fieldName, boolean isFolding) {
        return fieldOwner.equals(getLambdaInternalName()) &&
               //don't process general field of anonymous objects
               InlineCodegenUtil.isCapturedFieldName(fieldName);
    }

    @Nullable
    public AbstractInsnNode foldFieldAccessChainIfNeeded(@NotNull List<AbstractInsnNode> capturedFieldAccess, @NotNull MethodNode node) {
        if (capturedFieldAccess.size() == 1) {
            //just aload
            return null;
        }

        return foldFieldAccessChainIfNeeded(capturedFieldAccess, 1, node);
    }

    //TODO: seems that this method is redundant but it added from safety purposes before new milestone
    public boolean processNonAload0FieldAccessChains(boolean isInlinedLambda) {
        return false;
    }

    @Nullable
    private AbstractInsnNode foldFieldAccessChainIfNeeded(
            @NotNull List<AbstractInsnNode> capturedFieldAccess,
            int currentInstruction,
            @NotNull MethodNode node
    ) {
        boolean checkParent = !isRoot() && currentInstruction < capturedFieldAccess.size() - 1;
        if (checkParent) {
            AbstractInsnNode transformed = parent.foldFieldAccessChainIfNeeded(capturedFieldAccess, currentInstruction + 1, node);
            if (transformed != null) {
                return transformed;
            }
        }

        FieldInsnNode insnNode = (FieldInsnNode) capturedFieldAccess.get(currentInstruction);
        if (canProcess(insnNode.owner, insnNode.name, true)) {
            insnNode.name = "$$$" + insnNode.name;
            insnNode.setOpcode(Opcodes.GETSTATIC);

            AbstractInsnNode next = capturedFieldAccess.get(0);
            while (next != insnNode) {
                AbstractInsnNode toDelete = next;
                next = next.getNext();
                node.instructions.remove(toDelete);
            }

            return capturedFieldAccess.get(capturedFieldAccess.size() - 1);
        }

        return null;
    }

    @Nullable
    public CapturedParamInfo findField(@NotNull FieldInsnNode fieldInsnNode) {
        return findField(fieldInsnNode, params.getCaptured());
    }

    @Nullable
    protected CapturedParamInfo findField(@NotNull FieldInsnNode fieldInsnNode, @NotNull Collection<CapturedParamInfo> captured) {
        for (CapturedParamInfo valueDescriptor : captured) {
            if (valueDescriptor.getOriginalFieldName().equals(fieldInsnNode.name) &&
                valueDescriptor.getContainingLambdaName().equals(fieldInsnNode.owner)) {
                return valueDescriptor;
            }
        }
        return null;
    }

    @NotNull
    public FieldRemapper getParent() {
        return parent;
    }

    public String getLambdaInternalName() {
        return lambdaInternalName;
    }

    public String getNewLambdaInternalName() {
        return lambdaInternalName;
    }

    public boolean isRoot() {
        return parent == null;
    }

    @Nullable
    public StackValue getFieldForInline(@NotNull FieldInsnNode node, @Nullable StackValue prefix) {
        return MethodInliner.findCapturedField(node, this).getRemapValue();
    }

    public boolean isInsideInliningLambda() {
        return !isRoot() && parent.isInsideInliningLambda();
    }
}
