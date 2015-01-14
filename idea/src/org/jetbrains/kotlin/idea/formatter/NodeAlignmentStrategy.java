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

package org.jetbrains.kotlin.idea.formatter;

import com.intellij.formatting.Alignment;
import com.intellij.formatting.alignment.AlignmentStrategy;
import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class NodeAlignmentStrategy {

    private static final NodeAlignmentStrategy NULL_STRATEGY = fromTypes(AlignmentStrategy.wrap(null));

    /** @return shared strategy instance that returns <code>null</code> all the time */
    public static NodeAlignmentStrategy getNullStrategy() {
        return NULL_STRATEGY;
    }

    public static NodeAlignmentStrategy fromTypes(AlignmentStrategy strategy) {
        return new AlignmentStrategyWrapper(strategy);
    }

    @Nullable
    public abstract Alignment getAlignment(@NotNull ASTNode node);

    public static class AlignmentStrategyWrapper extends NodeAlignmentStrategy {
        private final AlignmentStrategy internalStrategy;

        public AlignmentStrategyWrapper(@NotNull AlignmentStrategy internalStrategy) {
            this.internalStrategy = internalStrategy;
        }

        @Nullable
        @Override
        public Alignment getAlignment(@NotNull ASTNode node) {
            ASTNode parent = node.getTreeParent();
            if (parent != null) {
                return internalStrategy.getAlignment(parent.getElementType(), node.getElementType());
            }

            return internalStrategy.getAlignment(node.getElementType());
        }
    }
}
