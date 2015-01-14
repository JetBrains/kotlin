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

package org.jetbrains.kotlin.idea.refactoring.changeSignature.usages;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.idea.refactoring.changeSignature.JetChangeInfo;
import org.jetbrains.kotlin.idea.refactoring.changeSignature.JetParameterInfo;
import org.jetbrains.kotlin.psi.JetSimpleNameExpression;

import static org.jetbrains.kotlin.psi.PsiPackage.JetPsiFactory;

public class JetParameterUsage extends JetUsageInfo<JetSimpleNameExpression> {
    private final JetParameterInfo parameterInfo;
    private final JetFunctionDefinitionUsage containingFunction;

    public JetParameterUsage(
            @NotNull JetSimpleNameExpression element,
            @NotNull JetParameterInfo parameterInfo,
            @NotNull JetFunctionDefinitionUsage containingFunction
    ) {
        super(element);
        this.parameterInfo = parameterInfo;
        this.containingFunction = containingFunction;
    }

    @Override
    public boolean processUsage(JetChangeInfo changeInfo, JetSimpleNameExpression element) {
        String newName = parameterInfo.getInheritedName(containingFunction);
        element.replace(JetPsiFactory(element.getProject()).createSimpleName(newName));
        return false;
    }
}
