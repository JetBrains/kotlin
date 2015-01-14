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

import java.util.List;

public class JetCatchClause extends JetElementImpl {
    public JetCatchClause(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public <R, D> R accept(@NotNull JetVisitor<R, D> visitor, D data) {
        return visitor.visitCatchSection(this, data);
    }

    @Nullable @IfNotParsed
    public JetParameterList getParameterList() {
        return (JetParameterList) findChildByType(JetNodeTypes.VALUE_PARAMETER_LIST);
    }

    @Nullable @IfNotParsed
    public JetParameter getCatchParameter() {
        JetParameterList list = getParameterList();
        if (list == null) return null;
        List<JetParameter> parameters = list.getParameters();
        return parameters.size() == 1 ? parameters.get(0) : null;
    }


    @Nullable @IfNotParsed
    public JetExpression getCatchBody() {
        return findChildByClass(JetExpression.class);
    }
}
