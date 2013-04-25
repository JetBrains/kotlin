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

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.JetNodeTypes;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;
import org.jetbrains.jet.lexer.JetTokens;

public class JetTypeCodeFragmentImpl extends JetCodeFragmentImpl implements JetTypeCodeFragment {
    public JetTypeCodeFragmentImpl(Project project, String name, CharSequence text, PsiElement context) {
        super(project, name, text, JetNodeTypes.TYPE_CODE_FRAGMENT, context);
    }

    @Nullable
    @Override
    public JetType getType() {
        JetType type = null;

        for (PsiElement child : getChildren()) {
            IElementType elementType = child.getNode().getElementType();

            if (elementType == JetNodeTypes.TYPE_CODE_FRAGMENT) {
                for (PsiElement grChild : child.getChildren()) {
                    if (grChild instanceof JetTypeReference) {
                        if (!grChild.getText().isEmpty())
                            //TODO return the actual type
                            type = KotlinBuiltIns.getInstance().getAnyType();
                    }
                    else if (!JetTokens.WHITE_SPACE_OR_COMMENT_BIT_SET.contains(elementType))
                        return null;
                }
            }
            else if (!JetTokens.WHITE_SPACE_OR_COMMENT_BIT_SET.contains(elementType))
                return null;
        }

        return type;
    }
}
