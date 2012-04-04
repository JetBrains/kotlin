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
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.util.Function;
import com.intellij.util.PsiNavigateUtil;
import org.jetbrains.jet.lang.descriptors.CallableMemberDescriptor;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.Modality;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.psi.JetNamedFunction;
import org.jetbrains.jet.lang.psi.JetProperty;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.plugin.project.WholeProjectAnalyzerFacade;
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
    public LineMarkerInfo getLineMarkerInfo(final PsiElement element) {
        JetFile file = (JetFile)element.getContainingFile();
        if (file == null) return null;

        if (!(element instanceof JetNamedFunction || element instanceof JetProperty))     return null;

        final BindingContext bindingContext = WholeProjectAnalyzerFacade.analyzeProjectWithCacheOnAFile(file).getBindingContext();

        final DeclarationDescriptor descriptor =
            bindingContext.get(BindingContext.DECLARATION_TO_DESCRIPTOR, element);
        if (!(descriptor instanceof CallableMemberDescriptor)) return null;
        final Set<? extends CallableMemberDescriptor> overriddenMembers = ((CallableMemberDescriptor)descriptor).getOverriddenDescriptors();
        if (overriddenMembers.size() == 0) return null;

        boolean allOverriddenAbstract = true;
        for (CallableMemberDescriptor function : overriddenMembers) {
            allOverriddenAbstract &= function.getModality() == Modality.ABSTRACT;
        }

        final String implementsOrOverrides = allOverriddenAbstract ? "implements" : "overrides";
        final String memberKind = element instanceof JetNamedFunction ? "function" : "property";

        return new LineMarkerInfo<PsiElement>(
                element,
                element.getTextOffset(),
                allOverriddenAbstract ? IMPLEMENTING_MARK : OVERRIDING_MARK,
                Pass.UPDATE_ALL,
                new Function<PsiElement, String>() {
                    @Override
                    public String fun(PsiElement element) {
                        StringBuilder builder = new StringBuilder();
                        builder.append(DescriptorRenderer.HTML.render(descriptor));
                        int overrideCount = overriddenMembers.size();
                        if (overrideCount >= 1) {
                            builder.append(" ").append(implementsOrOverrides).append(" ");
                            builder.append(DescriptorRenderer.HTML.render(overriddenMembers.iterator().next()));
                        }
                        if (overrideCount > 1) {
                            int count = overrideCount - 1;
                            builder.append(" and ").append(count).append(" other ").append(memberKind);
                            if (count > 1) {
                                builder.append("s");
                            }
                        }

                        return builder.toString();
                    }
                },
                new GutterIconNavigationHandler<PsiElement>() {
                    @Override
                    public void navigate(MouseEvent event, PsiElement elt) {
                        if (overriddenMembers.isEmpty()) return;
                        final List<PsiElement> list = Lists.newArrayList();
                        for (CallableMemberDescriptor overriddenMember : overriddenMembers) {
                            PsiElement declarationPsiElement = bindingContext.get(BindingContext.DESCRIPTOR_TO_DECLARATION, overriddenMember);
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
                            final JBPopup popup = NavigationUtil.getPsiElementPopup(PsiUtilCore.toPsiElementArray(list), new DefaultPsiElementCellRenderer() {
                                        @Override
                                        public String getElementText(PsiElement element) {
                                            if (element instanceof JetNamedFunction) {
                                                JetNamedFunction function = (JetNamedFunction) element;
                                                return DescriptorRenderer.HTML.render(bindingContext.get(BindingContext.FUNCTION, function));
                                            }
                                            return super.getElementText(element);
                                        }
                                    }, DescriptorRenderer.HTML.render(descriptor));
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
