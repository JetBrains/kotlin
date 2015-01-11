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

package org.jetbrains.kotlin.psi;

import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.JetNodeTypes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class JetAnnotatedExpression extends JetExpressionImpl implements JetAnnotated {
    public JetAnnotatedExpression(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public <R, D> R accept(@NotNull JetVisitor<R, D> visitor, D data) {
        return visitor.visitAnnotatedExpression(this, data);
    }

    @Nullable
    public JetExpression getBaseExpression() {
        return findChildByClass(JetExpression.class);
    }

    @Override
    @NotNull
    public List<JetAnnotation> getAnnotations() {
        return findChildrenByType(JetNodeTypes.ANNOTATION);
    }

    @Override
    @NotNull
    public List<JetAnnotationEntry> getAnnotationEntries() {
        List<JetAnnotationEntry> answer = null;
        for (JetAnnotation annotation : getAnnotations()) {
            if (answer == null) answer = new ArrayList<JetAnnotationEntry>();
            answer.addAll(annotation.getEntries());
        }
        return answer != null ? answer : Collections.<JetAnnotationEntry>emptyList();
    }
}
