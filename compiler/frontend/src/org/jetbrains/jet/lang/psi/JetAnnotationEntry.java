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

package org.jetbrains.jet.lang.psi;

import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.JetNodeTypes;

import java.util.Collections;
import java.util.List;

/**
 * @author max
 */
public class JetAnnotationEntry extends JetElementImpl implements JetCallElement {
    public JetAnnotationEntry(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public void accept(@NotNull JetVisitorVoid visitor) {
        visitor.visitAnnotationEntry(this);
    }

    @Override
    public <R, D> R accept(@NotNull JetVisitor<R, D> visitor, D data) {
        return visitor.visitAnnotationEntry(this, data);
    }


    @Nullable @IfNotParsed
    public JetTypeReference getTypeReference() {
        JetConstructorCalleeExpression calleeExpression = getCalleeExpression();
        if (calleeExpression == null) {
            return null;
        }
        return calleeExpression.getTypeReference();
    }

    @Override
    public JetConstructorCalleeExpression getCalleeExpression() {
        return (JetConstructorCalleeExpression) findChildByType(JetNodeTypes.CONSTRUCTOR_CALLEE);
    }

    @Override
    public JetValueArgumentList getValueArgumentList() {
        return (JetValueArgumentList) findChildByType(JetNodeTypes.VALUE_ARGUMENT_LIST);
    }

    @NotNull
    @Override
    public List<? extends ValueArgument> getValueArguments() {
        JetValueArgumentList list = getValueArgumentList();
        return list != null ? list.getArguments() : Collections.<JetValueArgument>emptyList();
    }

    @NotNull
    @Override
    public List<JetExpression> getFunctionLiteralArguments() {
        return Collections.emptyList();
    }

    @NotNull
    @Override
    public List<JetTypeProjection> getTypeArguments() {
        JetTypeArgumentList typeArgumentList = getTypeArgumentList();
        if (typeArgumentList == null) {
            return Collections.emptyList();
        }
        return typeArgumentList.getArguments();
    }

    @Override
    public JetTypeArgumentList getTypeArgumentList() {
        JetTypeReference typeReference = getTypeReference();
        if (typeReference == null) {
            return null;
        }
        JetTypeElement typeElement = typeReference.getTypeElement();
        if (typeElement instanceof JetUserType) {
            JetUserType userType = (JetUserType) typeElement;
            return userType.getTypeArgumentList();
        }
        return null;
    }

}
