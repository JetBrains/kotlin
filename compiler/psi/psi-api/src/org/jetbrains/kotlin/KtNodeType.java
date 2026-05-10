/*
 * Copyright 2010-2026 JetBrains s.r.o.
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

package org.jetbrains.kotlin;

import com.intellij.lang.ASTNode;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.idea.KotlinLanguage;
import org.jetbrains.kotlin.psi.KtElement;
import org.jetbrains.kotlin.psi.KtElementImpl;

import java.lang.reflect.Constructor;
import java.util.function.Function;

public class KtNodeType extends IElementType {
    private final Function<ASTNode, KtElement> myPsiFactory;

    @Deprecated  // Deprecated in the favor of second constructor, should not be used
    public KtNodeType(@NotNull @NonNls String debugName, Class<? extends KtElement> psiClass) {
        this(debugName, createFactory(psiClass));
    }

    public KtNodeType(@NotNull @NonNls String debugName, Function<ASTNode, KtElement> psiFactory) {
        super(debugName, KotlinLanguage.INSTANCE);
        myPsiFactory = psiFactory;
    }

    public KtElement createPsi(ASTNode node) {
        assert node.getElementType() == this;
        if (myPsiFactory == null) {
            return new KtElementImpl(node);
        }
        return myPsiFactory.apply(node);
    }

    private static Function<ASTNode, KtElement> createFactory(Class<? extends KtElement> psiClass) {
        if (psiClass == null) {
            return null;
        }

        Constructor<? extends KtElement> constructor;
        try {
            constructor = psiClass.getConstructor(ASTNode.class);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("Must have a constructor with ASTNode", e);
        }

        return node -> {
            try {
                return constructor.newInstance(node);
            } catch (Exception e) {
                throw new RuntimeException("Error creating psi element for node", e);
            }
        };
    }

    public static class KtLeftBoundNodeType extends KtNodeType {
        @SuppressWarnings("unused")
        @Deprecated  // Deprecated in the favor of second constructor, should not be used
        public KtLeftBoundNodeType(@NotNull @NonNls String debugName, Class<? extends KtElement> psiClass) {
            super(debugName, psiClass);
        }

        public KtLeftBoundNodeType(@NotNull @NonNls String debugName, Function<ASTNode, KtElement> psiFactory) {
            super(debugName, psiFactory);
        }

        @Override
        public boolean isLeftBound() {
            return true;
        }
    }
}
