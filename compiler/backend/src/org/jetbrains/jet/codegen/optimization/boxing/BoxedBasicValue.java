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

import org.jetbrains.org.objectweb.asm.Type;
import org.jetbrains.org.objectweb.asm.tree.AbstractInsnNode;
import org.jetbrains.org.objectweb.asm.tree.analysis.BasicValue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class BoxedBasicValue extends BasicValue {
    private final HashSet<AbstractInsnNode> associatedInsns = new HashSet<AbstractInsnNode>();
    private final AbstractInsnNode boxingInsn;
    private final Type boxedType;
    private boolean wasUnboxed = false;

    public BoxedBasicValue(Type type, Type boxedType, AbstractInsnNode insnNode) {
        super(type);
        this.boxedType = boxedType;
        associatedInsns.add(insnNode);
        boxingInsn = insnNode;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BoxedBasicValue that = (BoxedBasicValue) o;

        if (!getType().equals(that.getType())) return false;

        return (boxingInsn == ((BoxedBasicValue) o).boxingInsn);
    }

    public List<AbstractInsnNode> getAssociatedInsns() {
        return new ArrayList<AbstractInsnNode>(associatedInsns);
    }

    public void addInsn(AbstractInsnNode insnNode) {
        associatedInsns.add(insnNode);
    }

    public Type getBoxedType() {
        return boxedType;
    }

    public boolean wasUnboxed() {
        return wasUnboxed;
    }

    public void setWasUnboxed(boolean wasUnboxed) {
        this.wasUnboxed = wasUnboxed;
    }
}
