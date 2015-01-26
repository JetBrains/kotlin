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

package org.jetbrains.kotlin.kdoc.parser;

import com.intellij.lang.ASTNode;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.idea.JetLanguage;
import org.jetbrains.kotlin.kdoc.psi.impl.KDocElementImpl;

import java.lang.reflect.Constructor;

public class KDocElementType extends IElementType {
    private final Constructor<? extends KDocElementImpl> psiFactory;

    public KDocElementType(String debugName, @NotNull Class<? extends KDocElementImpl> psiClass) {
        super(debugName, JetLanguage.INSTANCE);
        try {
            psiFactory = psiClass != null ? psiClass.getConstructor(ASTNode.class) : null;
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("Must have a constructor with ASTNode");
        }
    }

    public KDocElementImpl createPsi(ASTNode node) {
        assert node.getElementType() == this;

        try {
            return psiFactory.newInstance(node);
        } catch (Exception e) {
            throw new RuntimeException("Error creating psi element for node", e);
        }
    }
}
