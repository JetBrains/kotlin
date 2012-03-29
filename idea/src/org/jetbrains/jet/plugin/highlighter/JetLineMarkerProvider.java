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

package org.jetbrains.jet.plugin.highlighter;

import com.google.common.collect.Lists;
import com.intellij.codeHighlighting.Pass;
import com.intellij.codeInsight.daemon.GutterIconNavigationHandler;
import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.codeInsight.daemon.LineMarkerProvider;
import com.intellij.codeInsight.hint.HintUtil;
import com.intellij.codeInsight.navigation.NavigationUtil;
import com.intellij.ide.util.DefaultPsiElementCellRenderer;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.util.IconLoader;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiUtilBase;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.util.Function;
import com.intellij.util.PsiNavigateUtil;
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor;
import org.jetbrains.jet.lang.descriptors.Modality;
import org.jetbrains.jet.lang.descriptors.SimpleFunctionDescriptor;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.psi.JetNamedFunction;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.plugin.compiler.WholeProjectAnalyzerFacade;
import org.jetbrains.jet.resolve.DescriptorRenderer;

import javax.swing.*;
import java.awt.event.MouseEvent;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * @author abreslav
 */
public class JetLineMarkerProvider implements LineMarkerProvider {
    public static final Icon OVERRIDING_MARK = IconLoader.getIcon("/gutter/overridingMethod.png");
    public static final Icon IMPLEMENTING_MARK = IconLoader.getIcon("/gutter/implementingMethod.png");

    @Override
    public LineMarkerInfo getLineMarkerInfo(PsiElement element) {
        JetFile file = (JetFile)element.getContainingFile();
        if (file == null) return null;

        if (!(element instanceof JetNamedFunction))     return null;
        JetNamedFunction jetFunction = (JetNamedFunction)element;

        final BindingContext bindingContext = WholeProjectAnalyzerFacade.analyzeProjectWithCacheOnAFile(file);

        final SimpleFunctionDescriptor functionDescriptor = bindingContext.get(BindingContext.FUNCTION, jetFunction);
        if (functionDescriptor == null) return null;
        final Set<? extends FunctionDescriptor> overriddenFunctions = functionDescriptor.getOverriddenDescriptors();
        if (overriddenFunctions.size() == 0) return null;

        boolean allOverriddenAbstract = true;
        for (FunctionDescriptor function : overriddenFunctions) {
            allOverriddenAbstract &= function.getModality() == Modality.ABSTRACT;
        }

        final String implementsOrOverrides = allOverriddenAbstract ? "implements" : "overrides";

        return new LineMarkerInfo<JetNamedFunction>(
                jetFunction,
                jetFunction.getTextOffset(),
                allOverriddenAbstract ? IMPLEMENTING_MARK : OVERRIDING_MARK,
                Pass.UPDATE_ALL,
                new Function<JetNamedFunction, String>() {
                    @Override
                    public String fun(JetNamedFunction jetFunction) {
                        StringBuilder builder = new StringBuilder();
                        builder.append(DescriptorRenderer.HTML.render(functionDescriptor));
                        int overrideCount = overriddenFunctions.size();
                        if (overrideCount >= 1) {
                            builder.append(" ").append(implementsOrOverrides).append(" ");
                            builder.append(DescriptorRenderer.HTML.render(overriddenFunctions.iterator().next()));
                        }
                        if (overrideCount > 1) {
                            int count = overrideCount - 1;
                            builder.append(" and ").append(count).append(" other function");
                            if (count > 1) {
                                builder.append("s");
                            }
                        }

                        return builder.toString();
                    }
                },
                new GutterIconNavigationHandler<JetNamedFunction>() {
                    @Override
                    public void navigate(MouseEvent event, JetNamedFunction elt) {
                        if (overriddenFunctions.isEmpty()) return;
                        final List<PsiElement> list = Lists.newArrayList();
                        for (FunctionDescriptor overriddenFunction : overriddenFunctions) {
                            PsiElement declarationPsiElement = bindingContext.get(BindingContext.DESCRIPTOR_TO_DECLARATION, overriddenFunction);
                            list.add(declarationPsiElement);
                        }
                        if (list.isEmpty()) {
                            String myEmptyText = "empty text";
                            final JComponent renderer = HintUtil.createErrorLabel(myEmptyText);
                            final JBPopup popup = JBPopupFactory.getInstance().createComponentPopupBuilder(renderer, renderer).createPopup();
                            if (event != null) {
                                popup.show(new RelativePoint(event));
                            }
                            return;
                        }
                        if (list.size() == 1) {
                            PsiNavigateUtil.navigate(list.iterator().next());
                        }
                        else {
                            final JBPopup popup = NavigationUtil.getPsiElementPopup(PsiUtilBase.toPsiElementArray(list), new DefaultPsiElementCellRenderer() {
                                        @Override
                                        public String getElementText(PsiElement element) {
                                            if (element instanceof JetNamedFunction) {
                                                JetNamedFunction function = (JetNamedFunction) element;
                                                return DescriptorRenderer.HTML.render(bindingContext.get(BindingContext.FUNCTION, function));
                                            }
                                            return super.getElementText(element);
                                        }
                                    }, DescriptorRenderer.HTML.render(functionDescriptor));
                            if (event != null) {
                                popup.show(new RelativePoint(event));
                            }
                        }
                    }
                }
        );
    }

    @Override
    public void collectSlowLineMarkers(List<PsiElement> elements, Collection<LineMarkerInfo> result) {
    }
}
