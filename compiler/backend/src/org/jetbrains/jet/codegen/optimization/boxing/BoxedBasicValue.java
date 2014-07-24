/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.codegen.optimization.boxing;

import com.intellij.openapi.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.codegen.AsmUtil;
import org.jetbrains.org.objectweb.asm.Type;
import org.jetbrains.org.objectweb.asm.tree.AbstractInsnNode;
import org.jetbrains.org.objectweb.asm.tree.analysis.BasicValue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class BoxedBasicValue extends BasicValue {
    private final Set<AbstractInsnNode> associatedInsns = new HashSet<AbstractInsnNode>();
    private final Set<Pair<AbstractInsnNode, Type>> unboxingWithCastInsns = new HashSet<Pair<AbstractInsnNode, Type>>();
    private final AbstractInsnNode boxingInsn;
    private final Set<Integer> associatedVariables = new HashSet<Integer>();
    private final Set<BoxedBasicValue> mergedWith = new HashSet<BoxedBasicValue>();
    private final Type primitiveType;
    private final ProgressionIteratorBasicValue progressionIterator;
    private boolean isSafeToRemove = true;

    public BoxedBasicValue(
            @NotNull Type boxedType,
            @NotNull AbstractInsnNode boxingInsn,
            @Nullable ProgressionIteratorBasicValue progressionIterator
    ) {
        super(boxedType);
        this.primitiveType = AsmUtil.unboxType(boxedType);
        this.boxingInsn = boxingInsn;
        this.progressionIterator = progressionIterator;
    }

    @Override
    public boolean equals(Object o) {
        return this == o;
    }

    public boolean typeEquals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;

        BoxedBasicValue that = (BoxedBasicValue) o;

        return getType().equals(that.getType());
    }

    @Override
    public int hashCode() {
        return System.identityHashCode(this);
    }

    public List<AbstractInsnNode> getAssociatedInsns() {
        return new ArrayList<AbstractInsnNode>(associatedInsns);
    }

    public void addInsn(AbstractInsnNode insnNode) {
        associatedInsns.add(insnNode);
    }

    public void addVariableIndex(int index) {
        associatedVariables.add(index);
    }

    public List<Integer> getVariablesIndexes() {
        return new ArrayList<Integer>(associatedVariables);
    }

    public Type getPrimitiveType() {
        return primitiveType;
    }

    public void addMergedWith(@NotNull BoxedBasicValue value) {
        mergedWith.add(value);
    }

    @NotNull
    public Iterable<BoxedBasicValue> getMergedWith() {
        return mergedWith;
    }

    public void markAsUnsafeToRemove() {
        isSafeToRemove = false;
    }

    public boolean isSafeToRemove() {
        return isSafeToRemove;
    }

    public boolean isDoubleSize() {
        return getPrimitiveType().getSize() == 2;
    }

    @NotNull
    public AbstractInsnNode getBoxingInsn() {
        return boxingInsn;
    }

    public boolean isFromProgressionIterator() {
        return progressionIterator != null;
    }

    @Nullable
    public ProgressionIteratorBasicValue getProgressionIterator() {
        return progressionIterator;
    }

    public void addUnboxingWithCastTo(@NotNull AbstractInsnNode insn, @NotNull Type type) {
        unboxingWithCastInsns.add(Pair.create(insn, type));
    }

    @NotNull
    public Set<Pair<AbstractInsnNode, Type>> getUnboxingWithCastInsns() {
        return unboxingWithCastInsns;
    }
}
