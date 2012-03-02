/*
 * Copyright 2010-2012 JetBrains s.r.o.
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
import com.intellij.util.ArrayUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.compiler.TipsManager;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.JetVisibilityChecker;
import org.jetbrains.jet.lang.resolve.java.AnalyzerFacadeForJVM;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.checker.JetTypeChecker;
import org.jetbrains.jet.lexer.JetTokens;
import org.jetbrains.jet.resolve.DescriptorRenderer;

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
    
    private static String renderParameter(ValueParameterDescriptor descriptor, boolean named, BindingContext bindingContext) {
        StringBuilder builder = new StringBuilder();
        if (named) builder.append("[");
        if (descriptor.getVarargElementType() != null) {
            builder.append("vararg ");
        }
        builder.append(descriptor.getName()).append(": ").
                append(DescriptorRenderer.TEXT.renderType(getActualParameterType(descriptor)));
        if (descriptor.hasDefaultValue()) {
            PsiElement element = bindingContext.get(BindingContext.DESCRIPTOR_TO_DECLARATION, descriptor);
            String defaultExpression = "?";
            if (element instanceof JetParameter) {
                JetParameter parameter = (JetParameter) element;
                JetExpression defaultValue = parameter.getDefaultValue();
                if (defaultValue != null) {
                    if (defaultValue instanceof JetConstantExpression) {
                        JetConstantExpression constantExpression = (JetConstantExpression) defaultValue;
                        defaultExpression = constantExpression.getText();
                        if (defaultExpression.length() > 10) {
                            if (defaultExpression.startsWith("\"")) defaultExpression = "\"...\"";
                            else if (defaultExpression.startsWith("\'")) defaultExpression = "\'...\'";
                            else defaultExpression = defaultExpression.substring(0, 7) + "...";
                        }
                    }
                }
            }
            builder.append(" = ").append(defaultExpression);
        }
        if (named) builder.append("]");
        return builder.toString();
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
        if (parameterOwner instanceof JetValueArgumentList) {
            JetValueArgumentList argumentList = (JetValueArgumentList) parameterOwner;
            if (descriptor instanceof FunctionDescriptor) {
                JetFile file = (JetFile) argumentList.getContainingFile();
                BindingContext bindingContext =
                        AnalyzerFacadeForJVM.analyzeFileWithCache(file, AnalyzerFacadeForJVM.SINGLE_DECLARATION_PROVIDER);
                FunctionDescriptor functionDescriptor = (FunctionDescriptor) descriptor;
                StringBuilder builder = new StringBuilder();
                List<ValueParameterDescriptor> valueParameters = functionDescriptor.getValueParameters();
                List<JetValueArgument> valueArguments = argumentList.getArguments();
                int currentParameterIndex = context.getCurrentParameterIndex();
                int boldStartOffset = -1;
                int boldEndOffset = -1;
                boolean isGrey = false;
                boolean isDeprecated = false; //todo: add deprecation check
                Color color = context.getDefaultParameterColor();
                PsiElement parent = argumentList.getParent();
                if (parent instanceof JetCallElement) {
                    JetCallElement callExpression = (JetCallElement) parent;
                    JetExpression calleeExpression = callExpression.getCalleeExpression();
                    JetSimpleNameExpression refExpression = null;
                    if (calleeExpression instanceof JetSimpleNameExpression) {
                        refExpression = (JetSimpleNameExpression) calleeExpression; 
                    } else if (calleeExpression instanceof JetConstructorCalleeExpression) {
                        JetConstructorCalleeExpression constructorCalleeExpression = (JetConstructorCalleeExpression) calleeExpression;
                        if (constructorCalleeExpression.getConstructorReferenceExpression() instanceof JetSimpleNameExpression) {
                            refExpression = (JetSimpleNameExpression) constructorCalleeExpression.getConstructorReferenceExpression();
                        }
                    }
                    if (refExpression != null) {
                        DeclarationDescriptor declarationDescriptor = bindingContext.get(BindingContext.REFERENCE_TARGET, refExpression);
                        if (declarationDescriptor != null) {
                            if (declarationDescriptor == functionDescriptor) {
                               color = GREEN_BACKGROUND;
                            }
                        }
                    }
                }
                
                boolean[] usedIndexes = new boolean[valueParameters.size()];
                boolean namedMode = false;
                Arrays.fill(usedIndexes, false);
                if ((currentParameterIndex >= valueParameters.size() && (valueParameters.size() > 0 ||
                                                                         currentParameterIndex > 0)) &&
                    (valueParameters.size() == 0 || valueParameters.get(valueParameters.size() - 1).getVarargElementType() == null)) {
                    isGrey = true;
                }
                if (valueParameters.size() == 0) builder.append(CodeInsightBundle.message("parameter.info.no.parameters"));
                for (int i = 0; i < valueParameters.size(); ++i) {
                    if (i != 0) builder.append(", ");
                    boolean highlightParameter = 
                            i == currentParameterIndex || (!namedMode && i < currentParameterIndex &&
                                                           valueParameters.get(valueParameters.size() - 1).
                                                                   getVarargElementType() != null);
                    if (highlightParameter) boldStartOffset = builder.length();
                    if (!namedMode) {
                        if (valueArguments.size() > i) {
                            JetValueArgument argument = valueArguments.get(i);
                            if (argument.isNamed()) {
                                namedMode = true;
                            } else {
                                ValueParameterDescriptor param = valueParameters.get(i);
                                builder.append(renderParameter(param, false, bindingContext));
                                if (i < currentParameterIndex) {
                                    if (argument.getArgumentExpression() != null) {
                                        //check type
                                        JetType paramType = getActualParameterType(param);
                                        JetType exprType = bindingContext.get(BindingContext.EXPRESSION_TYPE, argument.getArgumentExpression());
                                        if (exprType != null && !JetTypeChecker.INSTANCE.isSubtypeOf(exprType, paramType)) isGrey = true;
                                    }
                                    else isGrey = true;
                                }
                                usedIndexes[i] = true;
                            }
                        } else {
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
                                        param.getName().equals(referenceExpression.getReferencedName())) {
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
                                            else isGrey = true;
                                        }
                                        break;
                                    }
                                }
                            }
                        }
                        
                        if (takeAnyArgument) {
                            if (i < currentParameterIndex) isGrey = true;
                            
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
                    if (highlightParameter) boldEndOffset = builder.length();
                }
                if (builder.toString().isEmpty()) context.setUIComponentEnabled(false);
                else context.setupUIComponentPresentation(builder.toString(), boldStartOffset, boldEndOffset, isGrey,
                                                          isDeprecated, false, color);
            } else context.setUIComponentEnabled(false);
        }
    }

    private static JetValueArgumentList findCall(CreateParameterInfoContext context) {
        //todo: calls to this constructors, when we will have auxiliary constructors
        PsiFile file = context.getFile();
        if (!(file instanceof JetFile)) return null;
        PsiElement element = file.findElementAt(context.getOffset());
        while (element != null && !(element instanceof JetValueArgumentList)) {
            element = element.getParent();
        }
        if (element == null) return null;
        JetValueArgumentList argumentList = (JetValueArgumentList) element;
        JetCallElement callExpression;
        if (element.getParent() instanceof JetCallElement) {
            callExpression = (JetCallElement) element.getParent();
        } else return null;
        BindingContext bindingContext = AnalyzerFacadeForJVM.analyzeFileWithCache((JetFile) file,
                                                                                  AnalyzerFacadeForJVM.SINGLE_DECLARATION_PROVIDER);
        JetExpression calleeExpression = callExpression.getCalleeExpression();
        if (calleeExpression == null) return null;
        JetSimpleNameExpression refExpression = null;
        if (calleeExpression instanceof JetSimpleNameExpression) {
            refExpression = (JetSimpleNameExpression) calleeExpression;
        } else if (calleeExpression instanceof JetConstructorCalleeExpression) {
            JetConstructorCalleeExpression constructorCalleeExpression = (JetConstructorCalleeExpression) calleeExpression;
            if (constructorCalleeExpression.getConstructorReferenceExpression() instanceof JetSimpleNameExpression) {
                refExpression = (JetSimpleNameExpression) constructorCalleeExpression.getConstructorReferenceExpression();
            }
        }
        if (refExpression != null) {
            JetScope scope = bindingContext.get(BindingContext.RESOLUTION_SCOPE, refExpression);
            DeclarationDescriptor placeDescriptor = null;
            if (scope != null) {
                placeDescriptor = scope.getContainingDeclaration();
            }
            Collection<DeclarationDescriptor> variants = TipsManager.getReferenceVariants(refExpression, bindingContext);
            String refName = refExpression.getReferencedName();
            PsiReference[] references = refExpression.getReferences();
            if (references.length == 0) return null;
            ArrayList<DeclarationDescriptor> itemsToShow = new ArrayList<DeclarationDescriptor>();
            for (DeclarationDescriptor variant : variants) {
                if (variant instanceof FunctionDescriptor) {
                    FunctionDescriptor functionDescriptor = (FunctionDescriptor) variant;
                    if (functionDescriptor.getName().equals(refName)) {
                        //todo: renamed functions?
                        if (placeDescriptor != null && !JetVisibilityChecker.isVisible(placeDescriptor, functionDescriptor)) continue;
                        itemsToShow.add(functionDescriptor);
                    }
                } else if (variant instanceof ClassDescriptor) {
                   ClassDescriptor classDescriptor = (ClassDescriptor) variant;
                    if (classDescriptor.getName().equals(refName)) {
                        //todo: renamed classes?
                        for (ConstructorDescriptor constructorDescriptor : classDescriptor.getConstructors()) {
                            if (placeDescriptor != null && !JetVisibilityChecker.isVisible(placeDescriptor, constructorDescriptor)) continue;
                            itemsToShow.add(constructorDescriptor);
                        }
                    }
                }
            }
            context.setItemsToShow(ArrayUtil.toObjectArray(itemsToShow));
            return argumentList;
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
