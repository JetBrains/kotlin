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

package org.jetbrains.jet.plugin.refactoring.changeSignature.usages;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.JetSimpleNameExpression;
import org.jetbrains.jet.plugin.refactoring.changeSignature.JetChangeInfo;
import org.jetbrains.jet.plugin.refactoring.changeSignature.JetParameterInfo;

import static org.jetbrains.jet.lang.psi.PsiPackage.JetPsiFactory;

public class JetParameterUsage extends JetUsageInfo<JetSimpleNameExpression> {
    private final JetParameterInfo parameterInfo;
    private final PsiElement function;
    private final boolean isInherited;

    public JetParameterUsage(@NotNull JetSimpleNameExpression element, JetParameterInfo parameterInfo, PsiElement function, boolean inherited) {
        super(element);
        this.parameterInfo = parameterInfo;
        this.function = function;
        isInherited = inherited;
    }

    @Override
    public boolean processUsage(JetChangeInfo changeInfo, JetSimpleNameExpression element) {
        String newName = parameterInfo.getInheritedName(isInherited, function, changeInfo.getFunctionDescriptor());
        element.replace(JetPsiFactory(element.getProject()).createSimpleName(newName));
        return false;
    }
}
