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

package org.jetbrains.jet.plugin.refactoring.changeSignature;

import com.intellij.lang.Language;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.intellij.refactoring.changeSignature.ChangeInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor;
import org.jetbrains.jet.lang.descriptors.Visibilities;
import org.jetbrains.jet.lang.descriptors.Visibility;
import org.jetbrains.jet.lang.psi.JetFunction;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;
import org.jetbrains.jet.lexer.JetTokens;
import org.jetbrains.jet.plugin.JetLanguage;

import java.util.List;

public class JetChangeInfo implements ChangeInfo {
    private final JetFunctionPlatformDescriptor oldDescriptor;
    private String newName;
    private final JetType newReturnType;
    private String newReturnTypeText;
    private Visibility newVisibility;
    private final List<JetParameterInfo> newParameters;
    private final PsiElement context;
    private final JetGeneratedInfo generatedInfo;
    private Boolean parameterNamesChanged;

    public JetChangeInfo(
            JetFunctionPlatformDescriptor oldDescriptor,
            String newName,
            JetType newReturnType,
            String newReturnTypeText,
            Visibility newVisibility,
            List<JetParameterInfo> newParameters,
            PsiElement context,
            JetGeneratedInfo generatedInfo
    ) {
        this.oldDescriptor = oldDescriptor;
        this.newName = newName;
        this.newReturnType = newReturnType;
        this.newReturnTypeText = newReturnTypeText;
        this.newVisibility = newVisibility;
        this.newParameters = newParameters;
        this.context = context;
        this.generatedInfo = generatedInfo;
    }

    public String getNewSignature(@Nullable JetFunction inheritedFunction, boolean isInherited) {
        StringBuilder buffer = new StringBuilder();

        if (isConstructor()) {
            buffer.append(newName);

            if (newVisibility != null && newVisibility != Visibilities.PUBLIC)
                buffer.append(' ').append(newVisibility.toString()).append(' ');
        }
        else {
            if (newVisibility != null && newVisibility != Visibilities.INTERNAL)
                buffer.append(newVisibility.toString()).append(' ');

            buffer.append(JetTokens.FUN_KEYWORD).append(' ').append(newName);
        }

        buffer.append(getNewParametersSignature(inheritedFunction, isInherited, buffer.length()));

        if (newReturnType != null && !KotlinBuiltIns.getInstance().isUnit(newReturnType) && !isConstructor())
            buffer.append(": ").append(newReturnTypeText);

        return buffer.toString();
    }

    public String getNewParametersSignature(PsiElement inheritedFunction, boolean isInherited, int indentLength) {
        StringBuilder buffer = new StringBuilder("(");
        String indent = StringUtil.repeatSymbol(' ', indentLength + 1);

        for (int i = 0; i < newParameters.size(); i++) {
            JetParameterInfo parameterInfo = newParameters.get(i);
            if (i > 0) {
                buffer.append(",");
                buffer.append("\n");
                buffer.append(indent);
            }

            buffer.append(parameterInfo.getDeclarationSignature(isInherited, inheritedFunction, oldDescriptor));
        }

        buffer.append(")");
        return buffer.toString();
    }

    private boolean innerParameterSetOrOrderChanged() {
        if (newParameters.size() != oldDescriptor.getParametersCount())
            return true;

        for (int i = 0; i < newParameters.size(); i ++) {
            if (newParameters.get(i).getOldIndex() != i)
                return true;
        }

        return false;
    }

    @NotNull
    @Override
    public JetParameterInfo[] getNewParameters() {
        return newParameters.toArray(new JetParameterInfo[newParameters.size()]);
    }

    public void setNewParameter(int index, JetParameterInfo parameterInfo) {
        newParameters.set(index, parameterInfo);
    }

    public void addParameter(JetParameterInfo parameterInfo) {
        newParameters.add(parameterInfo);
    }

    @Override
    public boolean isParameterSetOrOrderChanged() {
        if (parameterNamesChanged == null)
            parameterNamesChanged = innerParameterSetOrOrderChanged();

        return parameterNamesChanged;
    }

    @Override
    public boolean isParameterTypesChanged() {
        return true;
    }

    @Override
    public boolean isParameterNamesChanged() {
        return true;
    }

    @Override
    public boolean isGenerateDelegate() {
        return false;
    }

    public void setNewName(String newName) {
        this.newName = newName;
    }

    @Override
    public boolean isNameChanged() {
        return !newName.equals(oldDescriptor.getName());
    }

    public boolean isVisibilityChanged() {
        return !newVisibility.equals(oldDescriptor.getVisibility());
    }

    public Visibility getNewVisibility() {
        return newVisibility;
    }

    public void setNewVisibility(Visibility newVisibility) {
        this.newVisibility = newVisibility;
    }

    @Override
    public PsiElement getMethod() {
        return oldDescriptor.getMethod();
    }

    @Nullable
    public FunctionDescriptor getOldDescriptor() {
        return oldDescriptor.getDescriptor();
    }

    public JetFunctionPlatformDescriptor getFunctionDescriptor() {
        return oldDescriptor;
    }

    public boolean isConstructor() {
        return oldDescriptor.isConstructor();
    }

    public String getNewReturnTypeText() {
        return newReturnTypeText;
    }

    public void setNewReturnTypeText(String newReturnTypeText) {
        this.newReturnTypeText = newReturnTypeText;
    }

    @Override
    public boolean isReturnTypeChanged() {
        return !newReturnTypeText.equals(oldDescriptor.getReturnTypeText());
    }

    @Nullable
    public String getOldName() {
        PsiElement function = oldDescriptor.getMethod();
        return function instanceof JetFunction ? ((JetFunction) function).getName() : null;
    }

    @Override
    public String getNewName() {
        return newName;
    }

    public JetGeneratedInfo getGeneratedInfo() {
        return generatedInfo;
    }

    public PsiElement getContext() {
        return context;
    }

    @Override
    public Language getLanguage() {
        return JetLanguage.INSTANCE;
    }
}
