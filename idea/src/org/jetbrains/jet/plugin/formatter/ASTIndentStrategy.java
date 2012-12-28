/*
 * Copyright 2010-2012 JetBrains s.r.o.
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

package org.jetbrains.jet.plugin.formatter;

import com.intellij.formatting.Indent;
import com.intellij.lang.ASTNode;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class ASTIndentStrategy {
    public static ASTIndentStrategy constIndent(Indent indent) {
        return new ConstIndentStrategy(indent);
    }

    public static PositionStrategy forNode(@Nullable String debugInfo) {
        return new PositionStrategy(debugInfo);
    }

    @Nullable
    public abstract Indent getIndent(@NotNull ASTNode node);

    public static class ConstIndentStrategy extends ASTIndentStrategy {
        private final Indent indent;

        public ConstIndentStrategy(Indent indent) {
            this.indent = indent;
        }

        @Nullable
        @Override
        public Indent getIndent(@NotNull ASTNode node) {
            return indent;
        }
    }

    public static class PositionStrategy extends ASTIndentStrategy {
        private Indent defaultIndent = Indent.getNoneIndent();

        private final List<IElementType> in = new ArrayList<IElementType>();
        private final List<IElementType> notIn = new ArrayList<IElementType>();
        private final List<IElementType> forElement = new ArrayList<IElementType>();
        private final List<IElementType> notForElement = new ArrayList<IElementType>();

        private final String debugInfo;

        public PositionStrategy(@Nullable String debugInfo) {
            this.debugInfo = debugInfo;
        }

        @Override
        public String toString() {
            return "PositionStrategy " + (debugInfo != null ? debugInfo : "No debug info");
        }

        public PositionStrategy set(Indent indent) {
            defaultIndent = indent;
            return this;
        }

        public PositionStrategy in(@NotNull IElementType parentType, @NotNull IElementType... orParentTypes) {
            in.clear();
            in.add(parentType);
            Collections.addAll(in, orParentTypes);
            return this;
        }

        public PositionStrategy notIn(@NotNull IElementType parentType, @NotNull IElementType... orParentTypes) {
            notIn.clear();
            notIn.add(parentType);
            Collections.addAll(notIn, orParentTypes);
            return this;
        }

        public PositionStrategy inAny() {
            in.clear();
            notIn.clear();
            return this;
        }

        public PositionStrategy forType(@NotNull IElementType elementType, @NotNull IElementType... otherTypes) {
            forElement.clear();
            forElement.add(elementType);
            Collections.addAll(forElement, otherTypes);

            return this;
        }

        public PositionStrategy notForType(@NotNull IElementType elementType, @NotNull IElementType... otherTypes) {
            notForElement.clear();
            notForElement.add(elementType);
            Collections.addAll(notForElement, otherTypes);

            return this;
        }

        public PositionStrategy forAny() {
            forElement.clear();
            notForElement.clear();
            return this;
        }

        @Nullable
        @Override
        public Indent getIndent(@NotNull ASTNode node) {
            if (!forElement.isEmpty()) {
                if (!forElement.contains(node.getElementType())) {
                    return null;
                }
            }

            if (notForElement.contains(node.getElementType())) {
                return null;
            }

            ASTNode parent = node.getTreeParent();
            if (parent != null) {
                if (!in.isEmpty()) {
                    if (!in.contains(parent.getElementType())) {
                        return null;
                    }
                }

                if (notIn.contains(parent.getElementType())) {
                    return null;
                }
            }
            else {
                if (!in.isEmpty()) {
                    return null;
                }
            }

            return defaultIndent;
        }
    }
}
