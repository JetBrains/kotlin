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
import org.jetbrains.jet.lang.resolve.name.Name;

/**
 * @author abreslav
 */
public class JetLabelQualifiedExpression extends JetExpression {

    public JetLabelQualifiedExpression(@NotNull ASTNode node) {
        super(node);
    }
    
    @Nullable
    public JetSimpleNameExpression getTargetLabel() {
        JetContainerNode qualifier = (JetContainerNode) findChildByType(JetNodeTypes.LABEL_QUALIFIER);
        if (qualifier == null) return null;
        return (JetSimpleNameExpression) qualifier.findChildByType(JetNodeTypes.LABEL_REFERENCE);
    }

    @Nullable @IfNotParsed
    public JetExpression getLabeledExpression() {
        return findChildByClass(JetExpression.class);
    }

    @Nullable
    public String getLabelName() {
        JetSimpleNameExpression labelElement = getTargetLabel();
        assert labelElement == null || labelElement.getText().startsWith("@");
        return labelElement == null ? null : labelElement.getText().substring(1);
    }
}
