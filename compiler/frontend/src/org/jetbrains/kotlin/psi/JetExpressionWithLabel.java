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
import org.jetbrains.jet.JetNodeTypes;

public class JetExpressionWithLabel extends JetExpressionImpl {

    public JetExpressionWithLabel(@NotNull ASTNode node) {
        super(node);
    }
    
    @Nullable
    public JetSimpleNameExpression getTargetLabel() {
        JetContainerNode qualifier = (JetContainerNode) findChildByType(JetNodeTypes.LABEL_QUALIFIER);
        if (qualifier == null) return null;
        return (JetSimpleNameExpression) qualifier.findChildByType(JetNodeTypes.LABEL);
    }

    @Nullable
    public String getLabelName() {
        JetSimpleNameExpression labelElement = getTargetLabel();
        assert labelElement == null || labelElement.getText().startsWith("@");
        return labelElement == null ? null : labelElement.getText().substring(1);
    }

    @Override
    public <R, D> R accept(@NotNull JetVisitor<R, D> visitor, D data) {
        return visitor.visitExpressionWithLabel(this, data);
    }
}
