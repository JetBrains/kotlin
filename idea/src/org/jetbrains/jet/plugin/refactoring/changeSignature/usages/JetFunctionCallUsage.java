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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.jetbrains.jet.lang.psi.PsiPackage.JetPsiFactory;

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
                callee.replace(JetPsiFactory(getProject()).createSimpleName(changeInfo.getNewName()));
        }

        if (arguments != null) {
            List<JetValueArgument> oldArguments = arguments.getArguments();

            if (changeInfo.isParameterSetOrOrderChanged())
                arguments.replace(generateNewArgumentList(changeInfo, oldArguments));
            else
                changeArgumentNames(changeInfo, oldArguments);
        }

        return true;
    }

    private JetValueArgumentList generateNewArgumentList(JetChangeInfo changeInfo, List<JetValueArgument> oldArguments) {
        boolean isNamedCall = oldArguments.size() > 1 && oldArguments.get(0).getArgumentName() != null;
        StringBuilder parametersBuilder = new StringBuilder("(");
        boolean isFirst = true;

        for (JetParameterInfo parameterInfo : changeInfo.getNewParameters()) {
            if (isFirst)
                isFirst = false;
            else
                parametersBuilder.append(',');

            String defaultValueText = parameterInfo.getDefaultValueText();

            if (isNamedCall) {
                String newName = parameterInfo.getInheritedName(isInherited, function, changeInfo.getFunctionDescriptor());
                parametersBuilder.append(newName).append('=');
            }

            parametersBuilder.append(defaultValueText.isEmpty() ? '0' : defaultValueText);
        }

        parametersBuilder.append(')');
        JetValueArgumentList newArguments = JetPsiFactory(getProject()).createCallArguments(parametersBuilder.toString());

        Map<Integer, JetValueArgument> argumentMap = getParamIndexToArgumentMap(changeInfo, oldArguments);
        int argIndex = 0;

        for (JetValueArgument newArgument : newArguments.getArguments()) {
            JetParameterInfo parameterInfo = changeInfo.getNewParameters()[argIndex++];
            JetValueArgument oldArgument = argumentMap.get(parameterInfo.getOldIndex());

            if (oldArgument != null) {
                JetValueArgumentName argumentName = oldArgument.getArgumentName();
                JetSimpleNameExpression argumentNameExpression = argumentName != null ? argumentName.getReferenceExpression() : null;
                changeArgumentName(changeInfo, argumentNameExpression, parameterInfo);
                newArgument.replace(oldArgument);
            }
            else if (parameterInfo.getDefaultValueText().isEmpty())
                newArgument.delete();
        }

        return newArguments;
    }

    private static Map<Integer, JetValueArgument> getParamIndexToArgumentMap(JetChangeInfo changeInfo, List<JetValueArgument> oldArguments) {
        Map<Integer, JetValueArgument> argumentMap = new HashMap<Integer, JetValueArgument>();

        for (int i = 0; i < oldArguments.size(); i++) {
            JetValueArgument argument = oldArguments.get(i);
            JetValueArgumentName argumentName = argument.getArgumentName();
            JetSimpleNameExpression argumentNameExpression = argumentName != null ? argumentName.getReferenceExpression() : null;
            String oldParameterName = argumentNameExpression != null ? argumentNameExpression.getReferencedName() : null;

            if (oldParameterName != null) {
                Integer oldParameterIndex = changeInfo.getOldParameterIndex(oldParameterName);

                if (oldParameterIndex != null)
                    argumentMap.put(oldParameterIndex, argument);
            }
            else
                argumentMap.put(i, argument);
        }

        return argumentMap;
    }

    private void changeArgumentNames(JetChangeInfo changeInfo, List<JetValueArgument> oldArguments) {
        for (JetValueArgument argument : oldArguments) {
            JetValueArgumentName argumentName = argument.getArgumentName();
            JetSimpleNameExpression argumentNameExpression = argumentName != null ? argumentName.getReferenceExpression() : null;

            if (argumentNameExpression != null) {
                Integer oldParameterIndex = changeInfo.getOldParameterIndex(argumentNameExpression.getReferencedName());

                if (oldParameterIndex != null) {
                    JetParameterInfo parameterInfo = changeInfo.getNewParameters()[oldParameterIndex];
                    changeArgumentName(changeInfo, argumentNameExpression, parameterInfo);
                }
            }
        }
    }

    private void changeArgumentName(JetChangeInfo changeInfo, JetSimpleNameExpression argumentNameExpression, JetParameterInfo parameterInfo) {
        PsiElement identifier = argumentNameExpression != null ? argumentNameExpression.getIdentifier() : null;

        if (identifier != null) {
            String newName = parameterInfo.getInheritedName(isInherited, function, changeInfo.getFunctionDescriptor());
            identifier.replace(JetPsiFactory(getProject()).createIdentifier(newName));
        }
    }
}
