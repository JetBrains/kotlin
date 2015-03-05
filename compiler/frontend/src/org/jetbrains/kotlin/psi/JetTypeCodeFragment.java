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

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.JetNodeTypes;
import org.jetbrains.kotlin.builtins.KotlinBuiltIns;
import org.jetbrains.kotlin.types.JetType;

public class JetTypeCodeFragment extends JetCodeFragment {
    public JetTypeCodeFragment(Project project, String name, CharSequence text, PsiElement context) {
        super(project, name, text, null, JetNodeTypes.TYPE_CODE_FRAGMENT, context);
    }

    @Nullable
    public JetType getType() {
        JetElement typeReference = getContentElement();
        if (typeReference instanceof JetTypeReference) {
            //TODO return the actual type
            return KotlinBuiltIns.getInstance().getAnyType();
        }
        return null;
    }

    @Nullable
    @Override
    public JetElement getContentElement() {
        return findChildByClass(JetTypeReference.class);
    }
}
