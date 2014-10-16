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

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.refactoring.changeSignature.ParameterInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.psi.JetExpression;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.psi.JetFunction;
import org.jetbrains.jet.lang.psi.JetParameter;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lexer.JetTokens;
import org.jetbrains.jet.plugin.project.AnalyzerFacadeWithCache;
import org.jetbrains.jet.renderer.DescriptorRenderer;

import java.util.List;

public class JetParameterInfo implements ParameterInfo {
    private String name = "";
    private final int oldIndex;
    private JetType type;
    private String typeText;
    private String defaultValueText = "";
    private JetValVar valOrVar;
    @Nullable private JetExpression defaultValue;

    public JetParameterInfo(int oldIndex, String name, JetType type, @Nullable JetExpression defaultValue, @Nullable ASTNode valOrVar) {
        this.oldIndex = oldIndex;
        this.name = name;
        this.type = type;
        this.typeText = getOldTypeText();
        this.defaultValue = defaultValue;

        if (valOrVar == null)
            this.valOrVar = JetValVar.None;
        else if (valOrVar.getElementType() == JetTokens.VAL_KEYWORD)
            this.valOrVar = JetValVar.Val;
        else if (valOrVar.getElementType() == JetTokens.VAR_KEYWORD)
            this.valOrVar = JetValVar.Var;
        else
            throw new IllegalArgumentException("Unknown val/var token: " + valOrVar.getText());
    }

    public JetParameterInfo(String name, JetType type) {
        this(-1, name, type, null, null);
    }

    public JetParameterInfo(int index) {
        oldIndex = index;
        typeText = "";
        valOrVar = JetValVar.None;
    }

    @Override
    public String getName() {
        return name;
    }

    public String getInheritedName(boolean isInherited, @Nullable PsiElement inheritedFunction, @NotNull JetMethodDescriptor baseFunction) {
        if (!(inheritedFunction instanceof JetFunction))
            return name;

        JetFunction inheritedJetFunction = (JetFunction) inheritedFunction;
        List<JetParameter> inheritedParameters = inheritedJetFunction.getValueParameters();

        if (!isInherited || oldIndex < 0 || oldIndex >= baseFunction.getParametersCount() || oldIndex >= inheritedParameters.size())
            return name;

        JetParameterInfo oldParam = baseFunction.getParameters().get(oldIndex);
        JetParameter inheritedParam = inheritedParameters.get(oldIndex);
        String inheritedParamName = inheritedParam.getName();

        if (oldParam.getName().equals(inheritedParamName)) {
            BindingContext bindingContext = AnalyzerFacadeWithCache.getContextForElement(inheritedJetFunction);
            JetScope parametersScope = JetChangeSignatureUsageProcessor.getFunctionBodyScope(inheritedJetFunction, bindingContext);

            if (parametersScope != null && parametersScope.getLocalVariable(Name.identifier(name)) == null)
                return name;
            else
                return inheritedParamName;
        }
        else
            return inheritedParamName;
    }

    @Override
    public int getOldIndex() {
        return oldIndex;
    }

    public boolean isNewParameter() {
        return oldIndex == -1;
    }

    @Nullable
    @Override
    public String getDefaultValue() {
        return null;
    }

    @Override
    public void setName(String name) {
        this.name = name != null ? name : "";
    }

    @Override
    public boolean isUseAnySingleVariable() {
        return false;
    }

    @Override
    public void setUseAnySingleVariable(boolean b) {
        throw new UnsupportedOperationException();
    }

    private String getOldTypeText() {
        return DescriptorRenderer.SHORT_NAMES_IN_TYPES.renderType(type);
    }

    @Override
    public String getTypeText() {
        return typeText;
    }

    public void setTypeText(String typeText) {
        this.typeText = typeText;
    }

    public boolean isTypeChanged() {
        return !getOldTypeText().equals(typeText);
    }

    public String getDefaultValueText() {
        return defaultValueText;
    }

    public void setDefaultValueText(String defaultValueText) {
        this.defaultValueText = defaultValueText;
    }

    public JetValVar getValOrVar() {
        return valOrVar != null ? valOrVar : JetValVar.None;
    }

    public void setValOrVar(JetValVar valOrVar) {
        this.valOrVar = valOrVar;
    }

    public JetType getType() {
        return type;
    }

    public String getDeclarationSignature(boolean isInherited, PsiElement inheritedFunction, JetMethodDescriptor baseFunction) {
        StringBuilder buffer = new StringBuilder();
        JetValVar valVar = getValOrVar();

        if (valVar != JetValVar.None)
            buffer.append(valVar.toString()).append(' ');

        buffer.append(getInheritedName(isInherited, inheritedFunction, baseFunction));
        buffer.append(": ").append(getTypeText());

        if (defaultValue != null)
            buffer.append(" = ").append(defaultValue.getText());

        return buffer.toString();
    }
}
