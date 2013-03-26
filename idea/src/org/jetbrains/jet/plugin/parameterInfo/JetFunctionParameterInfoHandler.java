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

package org.jetbrains.jet.plugin.parameterInfo;

import com.intellij.codeInsight.CodeInsightBundle;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.lang.ASTNode;
import com.intellij.lang.parameterInfo.*;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiReference;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ArrayUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.BindingContextUtils;
import org.jetbrains.jet.lang.resolve.JetVisibilityChecker;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.checker.JetTypeChecker;
import org.jetbrains.jet.lexer.JetTokens;
import org.jetbrains.jet.plugin.codeInsight.TipsManager;
import org.jetbrains.jet.plugin.project.AnalyzerFacadeWithCache;
import org.jetbrains.jet.renderer.DescriptorRenderer;

import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * User: Alefas
 * Date: 17.01.12
 */
public class JetFunctionParameterInfoHandler implements
        ParameterInfoHandlerWithTabActionSupport<JetValueArgumentList, Object, JetValueArgument> {
    public final static Color GREEN_BACKGROUND = new Color(231, 254, 234);

    @NotNull
    @Override
    public JetValueArgument[] getActualParameters(@NotNull JetValueArgumentList arguments) {
        List<JetValueArgument> argumentList = arguments.getArguments();
        return argumentList.toArray(new JetValueArgument[argumentList.size()]);
    }

    @NotNull
    @Override
    public IElementType getActualParameterDelimiterType() {
        return JetTokens.COMMA;
    }

    @NotNull
    @Override
    public IElementType getActualParametersRBraceType() {
        return JetTokens.RBRACE;
    }

    @NotNull
    @Override
    public Set<Class> getArgumentListAllowedParentClasses() {
        return Collections.singleton((Class) JetCallElement.class);
    }

    @NotNull
    @Override
    public Set<? extends Class> getArgListStopSearchClasses() {
        return Collections.singleton(JetFunction.class);
    }

    @NotNull
    @Override
    public Class<JetValueArgumentList> getArgumentListClass() {
        return JetValueArgumentList.class;
    }

    @Override
    public boolean couldShowInLookup() {
        return true;
    }

    @Override
    public Object[] getParametersForLookup(LookupElement item, ParameterInfoContext context) {
        return ArrayUtil.EMPTY_OBJECT_ARRAY; //todo: ?
    }

    @Override
    public Object[] getParametersForDocumentation(Object p, ParameterInfoContext context) {
        return ArrayUtil.EMPTY_OBJECT_ARRAY; //todo: ?
    }

    @Override
    public JetValueArgumentList findElementForParameterInfo(CreateParameterInfoContext context) {
        return findCall(context);
    }

    @Override
    public void showParameterInfo(@NotNull JetValueArgumentList element, CreateParameterInfoContext context) {
        context.showHint(element, element.getTextRange().getStartOffset(), this);
    }

    @Override
    public JetValueArgumentList findElementForUpdatingParameterInfo(UpdateParameterInfoContext context) {
        return findCallAndUpdateContext(context);
    }

    @Override
    public void updateParameterInfo(@NotNull JetValueArgumentList argumentList, UpdateParameterInfoContext context) {
        if (context.getParameterOwner() != argumentList) context.removeHint();
        int offset = context.getOffset();
        ASTNode child = argumentList.getNode().getFirstChildNode();
        int i = 0;
        while (child != null && child.getStartOffset() < offset) {
            if (child.getElementType() == JetTokens.COMMA) ++i;
            child = child.getTreeNext();
        }
        context.setCurrentParameter(i);
    }

    @Override
    public String getParameterCloseChars() {
        return ParameterInfoUtils.DEFAULT_PARAMETER_CLOSE_CHARS;
    }

    @Override
    public boolean tracksParameterIndex() {
        return true;
    }
    
    private static String renderParameter(ValueParameterDescriptor parameter, boolean named, BindingContext bindingContext) {
        StringBuilder builder = new StringBuilder();
        if (named) builder.append("[");
        if (parameter.getVarargElementType() != null) {
            builder.append("vararg ");
        }
        builder.append(parameter.getName()).append(": ").
                append(DescriptorRenderer.SHORT_NAMES_IN_TYPES.renderType(getActualParameterType(parameter)));
        if (parameter.hasDefaultValue()) {
            PsiElement parameterDeclaration = BindingContextUtils.descriptorToDeclaration(bindingContext, parameter);
            builder.append(" = ").append(getDefaultExpressionString(parameterDeclaration));
        }
        if (named) builder.append("]");
        return builder.toString();
    }

    private static String getDefaultExpressionString(@Nullable PsiElement parameterDeclaration) {
        if (parameterDeclaration instanceof JetParameter) {
            JetExpression defaultValue = ((JetParameter) parameterDeclaration).getDefaultValue();
            if (defaultValue != null) {
                String defaultExpression = defaultValue.getText();
                if (defaultExpression.length() <= 32) {
                    return defaultExpression;
                }

                if (defaultValue instanceof JetConstantExpression || defaultValue instanceof JetStringTemplateExpression) {
                    if (defaultExpression.startsWith("\"")) {
                        return "\"...\"";
                    }
                    else if (defaultExpression.startsWith("\'")) {
                        return "\'...\'";
                    }
                }
            }
        }
        return "...";
    }

    private static JetType getActualParameterType(ValueParameterDescriptor descriptor) {
        JetType paramType = descriptor.getType();
        if (descriptor.getVarargElementType() != null) paramType = descriptor.getVarargElementType();
        return paramType;
    }

    @Override
    public void updateUI(Object descriptor, ParameterInfoUIContext context) {
        //todo: when we will have ability to pass Array as vararg, implement such feature here too?
        if (context == null || context.getParameterOwner() == null || !context.getParameterOwner().isValid()) {
            return;
        }

        PsiElement parameterOwner = context.getParameterOwner();
        if (!(parameterOwner instanceof JetValueArgumentList)) {
            return;
        }

        JetValueArgumentList argumentList = (JetValueArgumentList) parameterOwner;

        if (!(descriptor instanceof FunctionDescriptor)) {
            context.setUIComponentEnabled(false);
            return;
        }

        FunctionDescriptor functionDescriptor = (FunctionDescriptor) descriptor;

        JetFile file = (JetFile) argumentList.getContainingFile();
        BindingContext bindingContext = AnalyzerFacadeWithCache.analyzeFileWithCache(file).getBindingContext();
        List<ValueParameterDescriptor> valueParameters = functionDescriptor.getValueParameters();
        List<JetValueArgument> valueArguments = argumentList.getArguments();

        StringBuilder builder = new StringBuilder();
        int currentParameterIndex = context.getCurrentParameterIndex();
        int boldStartOffset = -1;
        int boldEndOffset = -1;
        boolean isGrey = false;
        boolean isDeprecated = false; //todo: add deprecation check

        boolean[] usedIndexes = new boolean[valueParameters.size()];
        boolean namedMode = false;
        Arrays.fill(usedIndexes, false);

        if ((currentParameterIndex >= valueParameters.size() && (valueParameters.size() > 0 || currentParameterIndex > 0)) &&
            (valueParameters.size() == 0 || valueParameters.get(valueParameters.size() - 1).getVarargElementType() == null)) {
            isGrey = true;
        }

        if (valueParameters.size() == 0) {
            builder.append(CodeInsightBundle.message("parameter.info.no.parameters"));
        }

        for (int i = 0; i < valueParameters.size(); ++i) {
            if (i != 0) {
                builder.append(", ");
            }

            boolean highlightParameter = i == currentParameterIndex ||
                    (!namedMode && i < currentParameterIndex && valueParameters.get(valueParameters.size() - 1).getVarargElementType() != null);

            if (highlightParameter) {
                boldStartOffset = builder.length();
            }

            if (!namedMode) {
                if (valueArguments.size() > i) {
                    JetValueArgument argument = valueArguments.get(i);
                    if (argument.isNamed()) {
                        namedMode = true;
                    }
                    else {
                        ValueParameterDescriptor param = valueParameters.get(i);
                        builder.append(renderParameter(param, false, bindingContext));
                        if (i < currentParameterIndex) {
                            if (argument.getArgumentExpression() != null) {
                                //check type
                                JetType paramType = getActualParameterType(param);
                                JetType exprType = bindingContext.get(BindingContext.EXPRESSION_TYPE, argument.getArgumentExpression());
                                if (exprType != null && !JetTypeChecker.INSTANCE.isSubtypeOf(exprType, paramType)) {
                                    isGrey = true;
                                }
                            }
                            else {
                                isGrey = true;
                            }
                        }
                        usedIndexes[i] = true;
                    }
                }
                else {
                    ValueParameterDescriptor param = valueParameters.get(i);
                    builder.append(renderParameter(param, false, bindingContext));
                }
            }
            if (namedMode) {
                boolean takeAnyArgument = true;
                if (valueArguments.size() > i) {
                    JetValueArgument argument = valueArguments.get(i);
                    if (argument.isNamed()) {
                        for (int j = 0; j < valueParameters.size(); ++j) {
                            JetSimpleNameExpression referenceExpression = argument.getArgumentName().getReferenceExpression();
                            ValueParameterDescriptor param = valueParameters.get(j);
                            if (referenceExpression != null && !usedIndexes[j] &&
                                param.getName().equals(referenceExpression.getReferencedNameAsName())) {
                                takeAnyArgument = false;
                                usedIndexes[j] = true;
                                builder.append(renderParameter(param, true, bindingContext));
                                if (i < currentParameterIndex) {
                                    if (argument.getArgumentExpression() != null) {
                                        //check type
                                        JetType paramType = getActualParameterType(param);
                                        JetType exprType = bindingContext.get(BindingContext.EXPRESSION_TYPE, argument.getArgumentExpression());
                                        if (exprType != null && !JetTypeChecker.INSTANCE.isSubtypeOf(exprType, paramType)) isGrey = true;
                                    }
                                    else {
                                        isGrey = true;
                                    }
                                }
                                break;
                            }
                        }
                    }
                }

                if (takeAnyArgument) {
                    if (i < currentParameterIndex) {
                        isGrey = true;
                    }

                    for (int j = 0; j < valueParameters.size(); ++j) {
                        ValueParameterDescriptor param = valueParameters.get(j);
                        if (!usedIndexes[j]) {
                            usedIndexes[j] = true;
                            builder.append(renderParameter(param, true, bindingContext));
                            break;
                        }
                    }
                }
            }

            if (highlightParameter) {
                boldEndOffset = builder.length();
            }
        }

        Color color = getBackgroundColor(context, argumentList, functionDescriptor, bindingContext);

        if (builder.toString().isEmpty()) {
            context.setUIComponentEnabled(false);
        }
        else {
            context.setupUIComponentPresentation(builder.toString(), boldStartOffset, boldEndOffset, isGrey,
                                                 isDeprecated, false, color);
        }
    }

    private static Color getBackgroundColor(
            ParameterInfoUIContext context,
            JetValueArgumentList argumentList,
            FunctionDescriptor functionDescriptor,
            BindingContext bindingContext
    ) {
        JetSimpleNameExpression callNameExpression = getCallSimpleNameExpression(argumentList);
        if (callNameExpression != null) {
            // Mark with green background the variant with resolved call
            DeclarationDescriptor declarationDescriptor = bindingContext.get(BindingContext.REFERENCE_TARGET, callNameExpression);
            if (declarationDescriptor != null) {
                if (declarationDescriptor == functionDescriptor) {
                    return GREEN_BACKGROUND;
                }
            }
        }

        return context.getDefaultParameterColor();
    }


    private static JetValueArgumentList findCall(CreateParameterInfoContext context) {
        //todo: calls to this constructors, when we will have auxiliary constructors
        PsiFile file = context.getFile();
        if (!(file instanceof JetFile)) {
            return null;
        }

        JetValueArgumentList argumentList = PsiTreeUtil.getParentOfType(file.findElementAt(context.getOffset()), JetValueArgumentList.class);
        if (argumentList == null) {
            return null;
        }

        JetSimpleNameExpression callNameExpression = getCallSimpleNameExpression(argumentList);
        if (callNameExpression == null) {
            return null;
        }

        PsiReference[] references = callNameExpression.getReferences();
        if (references.length == 0) {
            return null;
        }

        BindingContext bindingContext = AnalyzerFacadeWithCache.analyzeFileWithCache((JetFile) file).getBindingContext();
        JetScope scope = bindingContext.get(BindingContext.RESOLUTION_SCOPE, callNameExpression);
        DeclarationDescriptor placeDescriptor = null;
        if (scope != null) {
            placeDescriptor = scope.getContainingDeclaration();
        }

        Collection<DeclarationDescriptor> variants = TipsManager.getReferenceVariants(callNameExpression, bindingContext);
        Name refName = callNameExpression.getReferencedNameAsName();

        Collection<DeclarationDescriptor> itemsToShow = new ArrayList<DeclarationDescriptor>();
        for (DeclarationDescriptor variant : variants) {
            if (variant instanceof FunctionDescriptor) {
                FunctionDescriptor functionDescriptor = (FunctionDescriptor) variant;
                if (functionDescriptor.getName().equals(refName)) {
                    //todo: renamed functions?
                    if (placeDescriptor != null && !JetVisibilityChecker.isVisible(placeDescriptor, functionDescriptor)) {
                        continue;
                    }
                    itemsToShow.add(functionDescriptor);
                }
            }
            else if (variant instanceof ClassDescriptor) {
                ClassDescriptor classDescriptor = (ClassDescriptor) variant;
                if (classDescriptor.getName().equals(refName)) {
                    //todo: renamed classes?
                    for (ConstructorDescriptor constructorDescriptor : classDescriptor.getConstructors()) {
                        if (placeDescriptor != null && !JetVisibilityChecker.isVisible(placeDescriptor, constructorDescriptor)) {
                            continue;
                        }
                        itemsToShow.add(constructorDescriptor);
                    }
                }
            }
        }

        context.setItemsToShow(ArrayUtil.toObjectArray(itemsToShow));
        return argumentList;
    }

    @Nullable
    private static JetSimpleNameExpression getCallSimpleNameExpression(JetValueArgumentList argumentList) {
        if (!(argumentList.getParent() instanceof JetCallElement)) {
            return null;
        }

        JetCallElement callExpression = (JetCallElement)argumentList.getParent();
        JetExpression calleeExpression = callExpression.getCalleeExpression();
        if (calleeExpression == null) {
            return null;
        }

        if (calleeExpression instanceof JetSimpleNameExpression) {
            return (JetSimpleNameExpression) calleeExpression;
        }
        else if (calleeExpression instanceof JetConstructorCalleeExpression) {
            JetConstructorCalleeExpression constructorCalleeExpression = (JetConstructorCalleeExpression) calleeExpression;
            if (constructorCalleeExpression.getConstructorReferenceExpression() instanceof JetSimpleNameExpression) {
                return (JetSimpleNameExpression) constructorCalleeExpression.getConstructorReferenceExpression();
            }
        }

        return null;
    }

    private static JetValueArgumentList findCallAndUpdateContext(UpdateParameterInfoContext context) {
        PsiFile file = context.getFile();
        PsiElement element = file.findElementAt(context.getOffset());
        if (element == null) return null;
        PsiElement parent = element.getParent();
        while (parent != null && !(parent instanceof JetValueArgumentList)) {
            element = element.getParent();
            parent = parent.getParent();
        }
        if (parent == null) return null;
        JetValueArgumentList argumentList = (JetValueArgumentList) parent;
        if (element instanceof JetValueArgument) {
            JetValueArgument arg = (JetValueArgument) element;
            int i = argumentList.getArguments().indexOf(arg);
            context.setCurrentParameter(i);
            context.setHighlightedParameter(arg);
        }
        return argumentList;
    }
}
