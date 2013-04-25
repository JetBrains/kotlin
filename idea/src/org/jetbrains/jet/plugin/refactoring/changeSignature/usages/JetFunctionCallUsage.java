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
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.plugin.refactoring.changeSignature.JetChangeInfo;
import org.jetbrains.jet.plugin.refactoring.changeSignature.JetParameterInfo;

import java.util.List;

public class JetFunctionCallUsage extends JetUsageInfo<JetCallElement> {
    private final PsiElement function;
    private final boolean isInherited;

    public JetFunctionCallUsage(@NotNull JetCallElement element, @NotNull PsiElement function, boolean isInherited) {
        super(element);
        this.function = function;
        this.isInherited = isInherited;
    }

    @Override
    public boolean processUsage(JetChangeInfo changeInfo, JetCallElement element) {
        JetValueArgumentList arguments = element.getValueArgumentList();

        if (changeInfo.isNameChanged()) {
            JetExpression callee = element.getCalleeExpression();

            if (callee instanceof JetSimpleNameExpression)
                callee.replace(JetPsiFactory.createSimpleName(getProject(), changeInfo.getNewName()));
        }

        if (arguments != null) {
            List<JetValueArgument> oldArguments = arguments.getArguments();

            if (changeInfo.isParameterSetOrOrderChanged()) {
                StringBuilder parametersBuilder = new StringBuilder("(");
                boolean isFirst = true;

                for (JetParameterInfo parameterInfo : changeInfo.getNewParameters()) {
                    if (isFirst)
                        isFirst = false;
                    else
                        parametersBuilder.append(',');

                    String defaultValueText = parameterInfo.getDefaultValueText();
                    parametersBuilder.append(defaultValueText.isEmpty() ? '0' : defaultValueText);
                }

                parametersBuilder.append(')');
                JetValueArgumentList newArguments = JetPsiFactory.createCallArguments(getProject(), parametersBuilder.toString());
                int argIndex = 0;

                for (JetValueArgument newArgument : newArguments.getArguments()) {
                    JetParameterInfo parameterInfo = changeInfo.getNewParameters()[argIndex++];
                    int oldIndex = parameterInfo.getOldIndex();

                    if (oldIndex >= 0 && oldIndex < oldArguments.size())
                        newArgument.replace(oldArguments.get(oldIndex));
                    else if (parameterInfo.getDefaultValueText().isEmpty())
                        newArgument.delete();
                }

                arguments.replace(newArguments);
            }
            else {
                for (JetParameterInfo parameterInfo : changeInfo.getNewParameters()) {
                    JetValueArgument argument = parameterInfo.getOldIndex() < oldArguments.size() ? oldArguments.get(parameterInfo.getOldIndex()) : null;
                    JetValueArgumentName argumentName = argument != null ? argument.getArgumentName() : null;
                    JetSimpleNameExpression argumentNameExpression = argumentName != null ? argumentName.getReferenceExpression() : null;
                    PsiElement identifier = argumentNameExpression != null ? argumentNameExpression.getIdentifier() : null;

                    if (identifier != null) {
                        String newName = parameterInfo.getInheritedName(isInherited, function, changeInfo.getFunctionDescriptor());
                        identifier.replace(JetPsiFactory.createIdentifier(getProject(), newName));
                    }
                }
            }
        }

        return true;
    }
}
