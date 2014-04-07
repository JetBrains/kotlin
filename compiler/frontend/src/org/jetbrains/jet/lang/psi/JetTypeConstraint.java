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

import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.psi.stubs.PsiJetTypeConstraintStub;
import org.jetbrains.jet.lang.psi.stubs.elements.JetStubElementTypes;
import org.jetbrains.jet.lexer.JetTokens;

public class JetTypeConstraint extends JetElementImplStub<PsiJetTypeConstraintStub> {
    public JetTypeConstraint(@NotNull ASTNode node) {
        super(node);
    }

    public JetTypeConstraint(@NotNull PsiJetTypeConstraintStub stub) {
        super(stub, JetStubElementTypes.TYPE_CONSTRAINT);
    }

    @Override
    public <R, D> R accept(@NotNull JetVisitor<R, D> visitor, D data) {
        return visitor.visitTypeConstraint(this, data);
    }

    public boolean isClassObjectConstraint() {
        PsiJetTypeConstraintStub stub = getStub();
        if (stub != null) {
            return stub.isClassObjectConstraint();
        }
        return findChildByType(JetTokens.CLASS_KEYWORD) != null &&
                findChildByType(JetTokens.OBJECT_KEYWORD) != null;
    }

    @Nullable @IfNotParsed
    public JetSimpleNameExpression getSubjectTypeParameterName() {
        return getStubOrPsiChild(JetStubElementTypes.REFERENCE_EXPRESSION);
    }

    @Nullable @IfNotParsed
    public JetTypeReference getBoundTypeReference() {
        return getStubOrPsiChild(JetStubElementTypes.TYPE_REFERENCE);
    }
}
