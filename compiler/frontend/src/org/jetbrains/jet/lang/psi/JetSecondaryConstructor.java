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
import org.jetbrains.jet.lexer.JetTokens;

import java.util.Collections;
import java.util.List;

/**
 * @author max
 */
public class JetSecondaryConstructor extends JetDeclaration implements JetDeclarationWithBody, JetStatementExpression {
    public JetSecondaryConstructor(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public void accept(@NotNull JetVisitorVoid visitor) {
        visitor.visitConstructor(this);
    }

    @Override
    public <R, D> R accept(@NotNull JetVisitor<R, D> visitor, D data) {
        return visitor.visitConstructor(this, data);
    }

    @Nullable @IfNotParsed
    public JetParameterList getParameterList() {
        return (JetParameterList) findChildByType(JetNodeTypes.VALUE_PARAMETER_LIST);
    }

    @Override
    @NotNull
    public List<JetParameter> getValueParameters() {
        JetParameterList list = getParameterList();
        return list != null ? list.getParameters() : Collections.<JetParameter>emptyList();
    }

    @Nullable
    public JetInitializerList getInitializerList() {
        return (JetInitializerList) findChildByType(JetNodeTypes.INITIALIZER_LIST);
    }

    @NotNull
    public List<JetDelegationSpecifier> getInitializers() {
        JetInitializerList list = getInitializerList();
        return list != null ? list.getInitializers() : Collections.<JetDelegationSpecifier>emptyList();
    }

    @Override
    public JetExpression getBodyExpression() {
        return findChildByClass(JetExpression.class);
    }

    @Override
    public boolean hasBlockBody() {
        return findChildByType(JetTokens.EQ) == null;
    }

    @Override
    public boolean hasDeclaredReturnType() {
        return true;
    }

    @NotNull
    @Override
    public JetElement asElement() {
        return this;
    }

    public ASTNode getNameNode() {
        return getNode().findChildByType(JetTokens.THIS_KEYWORD);
    }
}
