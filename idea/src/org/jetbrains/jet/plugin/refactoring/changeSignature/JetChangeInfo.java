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
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.PsiType;
import com.intellij.refactoring.changeSignature.*;
import com.intellij.usageView.UsageInfo;
import com.intellij.util.Function;
import com.intellij.util.VisibilityUtil;
import com.intellij.util.containers.ContainerUtil;
import kotlin.Function1;
import kotlin.KotlinPackage;
import kotlin.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.asJava.AsJavaPackage;
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor;
import org.jetbrains.jet.lang.descriptors.ValueParameterDescriptor;
import org.jetbrains.jet.lang.descriptors.Visibilities;
import org.jetbrains.jet.lang.descriptors.Visibility;
import org.jetbrains.jet.lang.psi.JetFunction;
import org.jetbrains.jet.lang.psi.JetFunctionLiteral;
import org.jetbrains.jet.lang.resolve.DescriptorToSourceUtils;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.TypeSubstitutor;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;
import org.jetbrains.jet.lexer.JetTokens;
import org.jetbrains.jet.plugin.JetLanguage;
import org.jetbrains.jet.plugin.caches.resolve.ResolvePackage;
import org.jetbrains.jet.plugin.refactoring.changeSignature.usages.JetFunctionDefinitionUsage;
import org.jetbrains.jet.plugin.util.IdeDescriptorRenderers;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JetChangeInfo implements ChangeInfo {
    private final JetMethodDescriptor oldDescriptor;
    private String newName;
    private final JetType newReturnType;
    private String newReturnTypeText;
    private Visibility newVisibility;
    private final List<JetParameterInfo> newParameters;
    private final PsiElement context;
    private final JetGeneratedInfo generatedInfo;
    private Boolean parameterNamesChanged;
    private Map<String, Integer> oldNameToParameterIndex;
    private boolean primaryMethodUpdated;
    @Nullable
    private JavaChangeInfo javaChangeInfo;
    private final PsiMethod originalPsiMethod;

    public JetChangeInfo(
            JetMethodDescriptor oldDescriptor,
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
        this.originalPsiMethod = getCurrentPsiMethod();
    }

    @Nullable
    private PsiMethod getCurrentPsiMethod() {
        List<PsiMethod> psiMethods = AsJavaPackage.toLightMethods(getMethod());
        assert psiMethods.size() <= 1 : "Multiple light methods: " + getMethod().getText();
        return KotlinPackage.firstOrNull(psiMethods);
    }

    public String getNewSignature(@NotNull JetFunctionDefinitionUsage inheritedFunction) {
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

        buffer.append(getNewParametersSignature(inheritedFunction, buffer.length()));

        if (newReturnType != null && !KotlinBuiltIns.isUnit(newReturnType) && !isConstructor())
            buffer.append(": ").append(newReturnTypeText);

        return buffer.toString();
    }

    public boolean isRefactoringTarget(FunctionDescriptor inheritedFunctionDescriptor) {
        return inheritedFunctionDescriptor != null &&
               getMethod() == DescriptorToSourceUtils.descriptorToDeclaration(inheritedFunctionDescriptor);
    }

    public String getNewParametersSignature(
            @NotNull JetFunctionDefinitionUsage inheritedFunction,
            int indentLength
    ) {
        boolean isLambda = inheritedFunction.getDeclaration() instanceof JetFunctionLiteral;
        if (isLambda && newParameters.size() == 1 && !newParameters.get(0).requiresExplicitType(inheritedFunction)) {
            return newParameters.get(0).getDeclarationSignature(0, inheritedFunction);
        }

        StringBuilder buffer = new StringBuilder("(");
        String indent = StringUtil.repeatSymbol(' ', indentLength + 1);

        for (int i = 0; i < newParameters.size(); i++) {
            JetParameterInfo parameterInfo = newParameters.get(i);
            if (i > 0) {
                buffer.append(",");
                buffer.append(indent);
            }

            buffer.append(parameterInfo.getDeclarationSignature(i, inheritedFunction));
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

    private Map<String, Integer> initOldNameToParameterIndex() {
        Map<String, Integer> map = new HashMap<String, Integer>();
        FunctionDescriptor descriptor = oldDescriptor.getDescriptor();

        if (descriptor != null) {
            List<ValueParameterDescriptor> parameters = descriptor.getValueParameters();

            for (int i = 0; i < parameters.size(); i++) {
                ValueParameterDescriptor oldParameter = parameters.get(i);
                map.put(oldParameter.getName().asString(), i);
            }
        }

        return map;
    }

    public int getNewParametersCount() {
        return newParameters.size();
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

    public void removeParameter(int index) {
        newParameters.remove(index);
    }

    @Override
    public boolean isParameterSetOrOrderChanged() {
        if (parameterNamesChanged == null)
            parameterNamesChanged = innerParameterSetOrOrderChanged();

        return parameterNamesChanged;
    }

    @Nullable
    public Integer getOldParameterIndex(String oldParameterName) {
        if (oldNameToParameterIndex == null)
            oldNameToParameterIndex = initOldNameToParameterIndex();

        return oldNameToParameterIndex.get(oldParameterName);
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

    public JetMethodDescriptor getFunctionDescriptor() {
        return oldDescriptor;
    }

    public boolean isConstructor() {
        return oldDescriptor.isConstructor();
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

    @NotNull
    public Collection<UsageInfo> getAffectedFunctions() {
        return oldDescriptor.getAffectedFunctions();
    }

    public String renderReturnType(@NotNull JetFunctionDefinitionUsage inheritedFunction) {
        TypeSubstitutor typeSubstitutor = inheritedFunction.getOrCreateTypeSubstitutor();
        if (typeSubstitutor == null) return newReturnTypeText;

        FunctionDescriptor currentBaseFunction = inheritedFunction.getBaseFunction().getCurrentFunctionDescriptor();
        if (currentBaseFunction == null) return newReturnTypeText;

        JetType originalType = currentBaseFunction.getReturnType();
        return ChangeSignaturePackage.renderTypeWithSubstitution(originalType, typeSubstitutor, newReturnTypeText, false);
    }

    @Nullable
    public JavaChangeInfo getOrCreateJavaChangeInfo() {
        if (javaChangeInfo == null) {
            final PsiMethod currentPsiMethod = getCurrentPsiMethod();
            if (originalPsiMethod == null || currentPsiMethod == null) return null;

            /*
             * When primaryMethodUpdated is false, changes to the primary Kotlin declaration are already confirmed, but not yet applied.
             * It means that originalPsiMethod has already expired, but new one can't be created until Kotlin declaration is updated
             * (signified by primaryMethodUpdated being true). It means we can't know actual PsiType, visibility, etc.
             * to use in JavaChangeInfo. However they are not actually used at this point since only parameter count and order matters here
             * So we resort to this hack and pass around "default" type (void) and visibility (package-local)
             */
            String javaVisibility = primaryMethodUpdated
                                    ? VisibilityUtil.getVisibilityModifier(currentPsiMethod.getModifierList())
                                    : PsiModifier.PACKAGE_LOCAL;

            JetParameterInfo[] newParameters = getNewParameters();
            ParameterInfoImpl[] newJavaParameters = ContainerUtil.map2Array(
                    KotlinPackage.withIndices(newParameters),
                    new ParameterInfoImpl[newParameters.length],
                    new Function<Pair<? extends Integer, ? extends JetParameterInfo>, ParameterInfoImpl>() {
                        @Override
                        public ParameterInfoImpl fun(Pair<? extends Integer, ? extends JetParameterInfo> pair) {
                            JetParameterInfo info = pair.getSecond();
                            PsiType type = primaryMethodUpdated
                                           ? currentPsiMethod.getParameterList().getParameters()[pair.getFirst()].getType()
                                           : PsiType.VOID;
                            return new ParameterInfoImpl(info.getOldIndex(), info.getName(), type, info.getDefaultValueText());
                        }
                    }
            );

            PsiType returnType = primaryMethodUpdated ? currentPsiMethod.getReturnType() : PsiType.VOID;

            javaChangeInfo = new ChangeSignatureProcessor(
                    getMethod().getProject(), originalPsiMethod, false, javaVisibility, getNewName(), returnType, newJavaParameters
            ).getChangeInfo();
            javaChangeInfo.updateMethod(currentPsiMethod);
        }

        return javaChangeInfo;
    }

    public void primaryMethodUpdated() {
        primaryMethodUpdated = true;
        javaChangeInfo = null;
    }

    @NotNull
    public static JetChangeInfo fromJavaChangeInfo(
            @NotNull ChangeInfo javaChangeInfo,
            @NotNull JetMethodDescriptor originalChangeSignatureDescriptor
    ) {
        PsiMethod method = (PsiMethod) javaChangeInfo.getMethod();

        FunctionDescriptor functionDescriptor = ResolvePackage.getJavaMethodDescriptor(method);
        final List<ValueParameterDescriptor> parameterDescriptors = functionDescriptor.getValueParameters();

        //noinspection ConstantConditions
        final List<ValueParameterDescriptor> originalParameterDescriptors =
                originalChangeSignatureDescriptor.getDescriptor().getValueParameters();


        List<JetParameterInfo> newParameters = KotlinPackage.map(
                KotlinPackage.withIndices(javaChangeInfo.getNewParameters()),
                new Function1<Pair<? extends Integer, ? extends ParameterInfo>, JetParameterInfo>() {
                    @Override
                    public JetParameterInfo invoke(Pair<? extends Integer, ? extends ParameterInfo> pair) {
                        ParameterInfo info = pair.getSecond();
                        int oldIndex = info.getOldIndex();
                        JetType currentType = parameterDescriptors.get(pair.getFirst()).getType();
                        JetType originalType = oldIndex >= 0
                                               ? originalParameterDescriptors.get(oldIndex).getType()
                                               : currentType;

                        JetParameterInfo jetParameterInfo = new JetParameterInfo(oldIndex, info.getName(), originalType, null, null);
                        jetParameterInfo.setDefaultValueText(info.getDefaultValue());
                        jetParameterInfo.setTypeText(IdeDescriptorRenderers.SOURCE_CODE.renderType(currentType));
                        return jetParameterInfo;
                    }
                }
        );

        JetType returnType = functionDescriptor.getReturnType();
        String returnTypeText = returnType != null ? IdeDescriptorRenderers.SOURCE_CODE.renderType(returnType) : "";

        return new JetChangeInfo(
                originalChangeSignatureDescriptor,
                javaChangeInfo.getNewName(),
                returnType,
                returnTypeText,
                functionDescriptor.getVisibility(),
                newParameters,
                method,
                new JetGeneratedInfo()
        );
    }
}
