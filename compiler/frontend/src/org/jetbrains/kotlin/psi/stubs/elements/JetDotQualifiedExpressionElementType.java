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

package org.jetbrains.kotlin.psi.stubs.elements;

import com.intellij.lang.ASTNode;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.psi.JetDotQualifiedExpression;
import org.jetbrains.kotlin.psi.JetImportDirective;
import org.jetbrains.kotlin.psi.JetPackageDirective;

public class JetDotQualifiedExpressionElementType extends JetPlaceHolderStubElementType<JetDotQualifiedExpression> {
    public JetDotQualifiedExpressionElementType(@NotNull @NonNls String debugName) {
        super(debugName, JetDotQualifiedExpression.class);
    }

    @Override
    public boolean shouldCreateStub(ASTNode node) {
        return PsiTreeUtil.getParentOfType(node.getPsi(), JetImportDirective.class, JetPackageDirective.class) != null;
    }
}
