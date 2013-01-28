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

package org.jetbrains.jet.lang.psi;

import com.google.common.collect.Lists;
import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.JetNodeTypes;
import org.jetbrains.jet.lexer.JetToken;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class JetModifierList extends JetElementImpl {
    public JetModifierList(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public void accept(@NotNull JetVisitorVoid visitor) {
        visitor.visitModifierList(this);
    }

    @Override
    public <R, D> R accept(@NotNull JetVisitor<R, D> visitor, D data) {
        return visitor.visitModifierList(this, data);
    }

    @NotNull
    public List<JetAnnotation> getAnnotations() {
        return findChildrenByType(JetNodeTypes.ANNOTATION);
    }

    @NotNull
    public List<JetAnnotationEntry> getAnnotationEntries() {
        List<JetAnnotationEntry> entries = findChildrenByType(JetNodeTypes.ANNOTATION_ENTRY);
        List<JetAnnotationEntry> answer = entries.isEmpty() ? null : Lists.newArrayList(entries);
        for (JetAnnotation annotation : getAnnotations()) {
            if (answer == null) answer = new ArrayList<JetAnnotationEntry>();
            answer.addAll(annotation.getEntries());
        }
        return answer != null ? answer : Collections.<JetAnnotationEntry>emptyList();
    }

    public boolean hasModifier(JetToken token) {
        return getModifierNode(token) != null;
    }

    @Nullable
    public ASTNode getModifierNode(JetToken token) {
        ASTNode node = getNode().getFirstChildNode();
        while (node != null) {
            if (node.getElementType() == token) return node;
            node = node.getTreeNext();
        }
        return null;
    }
}
