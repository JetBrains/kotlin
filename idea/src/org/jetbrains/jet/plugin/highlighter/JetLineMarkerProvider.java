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

package org.jetbrains.jet.plugin.highlighter;

import com.google.common.collect.Lists;
import com.intellij.codeHighlighting.Pass;
import com.intellij.codeInsight.daemon.GutterIconNavigationHandler;
import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.codeInsight.daemon.LineMarkerProvider;
import com.intellij.codeInsight.daemon.impl.MarkerType;
import com.intellij.codeInsight.hint.HintUtil;
import com.intellij.codeInsight.navigation.NavigationUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.util.IconLoader;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.search.searches.ClassInheritorsSearch;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.util.Function;
import com.intellij.util.PsiNavigateUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.asJava.KotlinLightClass;
import org.jetbrains.jet.asJava.LightClassUtil;
import org.jetbrains.jet.lang.descriptors.CallableMemberDescriptor;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.Modality;
import org.jetbrains.jet.lang.psi.JetClass;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.psi.JetNamedFunction;
import org.jetbrains.jet.lang.psi.JetProperty;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.BindingContextUtils;
import org.jetbrains.jet.lexer.JetTokens;
import org.jetbrains.jet.plugin.codeInsight.JetFunctionPsiElementCellRenderer;
import org.jetbrains.jet.plugin.project.WholeProjectAnalyzerFacade;
import org.jetbrains.jet.renderer.DescriptorRenderer;

import javax.swing.*;
import java.awt.event.MouseEvent;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public class JetLineMarkerProvider implements LineMarkerProvider {
    public static final Icon OVERRIDING_MARK = IconLoader.getIcon("/gutter/overridingMethod.png");
    public static final Icon IMPLEMENTING_MARK = IconLoader.getIcon("/gutter/implementingMethod.png");
    protected static final Icon OVERRIDDEN_MARK = IconLoader.getIcon("/gutter/overridenMethod.png");
    protected static final Icon IMPLEMENTED_MARK = IconLoader.getIcon("/gutter/implementedMethod.png");

    private static final Function<PsiElement, String> SUBCLASSED_CLASS_TOOLTIP_ADAPTER = new Function<PsiElement, String>() {
        @Override
        public String fun(PsiElement element) {
            PsiElement child = getPsiClassFirstChild(element);
            // Java puts its marker on a child of the PsiClass, so we must find a child of our own class too
            return child != null ? MarkerType.SUBCLASSED_CLASS.getTooltip().fun(child) : null;
        }
    };

    private static PsiElement getPsiClassFirstChild(PsiElement element) {
        if (!(element instanceof JetClass)) {
            element = element.getParent();
            if (!(element instanceof JetClass)) {
                return null;
            }
        }
        KotlinLightClass lightClass = LightClassUtil.createLightClass((JetClass) element);
        if (lightClass == null) {
            return null;
        }
        PsiElement[] children = lightClass.getDelegate().getChildren();
        return children.length > 0 ? children[0] : null;
    }

    private static final GutterIconNavigationHandler<PsiElement> SUBCLASSED_CLASS_NAVIGATION_HANDLER = new GutterIconNavigationHandler<PsiElement>() {
        @Override
        public void navigate(MouseEvent e, PsiElement elt) {
            PsiElement child = getPsiClassFirstChild(elt);
            if (child != null) {
                MarkerType.SUBCLASSED_CLASS.getNavigationHandler().navigate(e, child);
            }
        }
    };

    @Override
    public LineMarkerInfo getLineMarkerInfo(@NotNull PsiElement element) {

        JetFile file = (JetFile)element.getContainingFile();
        if (file == null) return null;

        if (!(element instanceof JetNamedFunction || element instanceof JetProperty))     return null;

        BindingContext bindingContext = WholeProjectAnalyzerFacade.analyzeProjectWithCacheOnAFile(file).getBindingContext();

        DeclarationDescriptor descriptor = bindingContext.get(BindingContext.DECLARATION_TO_DESCRIPTOR, element);
        if (!(descriptor instanceof CallableMemberDescriptor)) {
            return null;
        }

        Set<? extends CallableMemberDescriptor> overriddenMembers = ((CallableMemberDescriptor)descriptor).getOverriddenDescriptors();
        if (overriddenMembers.size() == 0) {
            return null;
        }

        boolean allOverriddenAbstract = true;
        for (CallableMemberDescriptor function : overriddenMembers) {
            allOverriddenAbstract &= function.getModality() == Modality.ABSTRACT;
        }

        // NOTE: Don't store descriptors in line markers because line markers are not deleted while editing other files and this can prevent
        // clearing the whole BindingTrace.
        return new LineMarkerInfo<PsiElement>(
                element,
                element.getTextOffset(),
                allOverriddenAbstract ? IMPLEMENTING_MARK : OVERRIDING_MARK,
                Pass.UPDATE_ALL,
                new Function<PsiElement, String>() {
                    @Override
                    public String fun(PsiElement element) {
                        return calculateTooltipString(element);
                    }
                },
                new GutterIconNavigationHandler<PsiElement>() {
                    @Override
                    public void navigate(MouseEvent event, PsiElement elt) {
                        iconNavigatorHandler(event, elt);
                    }
                }
        );
    }

    private static void iconNavigatorHandler(MouseEvent event, PsiElement elt) {
        JetFile file = (JetFile)elt.getContainingFile();
        assert file != null;

        BindingContext bindingContext = WholeProjectAnalyzerFacade.analyzeProjectWithCacheOnAFile(file).getBindingContext();
        DeclarationDescriptor descriptor = bindingContext.get(BindingContext.DECLARATION_TO_DESCRIPTOR, elt);
        if (!(descriptor instanceof CallableMemberDescriptor)) {
            return;
        }

        Set<? extends CallableMemberDescriptor> overriddenMembers = ((CallableMemberDescriptor)descriptor).getOverriddenDescriptors();
        if (overriddenMembers.size() == 0) {
            return;
        }

        if (overriddenMembers.isEmpty()) return;
        List<PsiElement> list = Lists.newArrayList();
        for (CallableMemberDescriptor overriddenMember : overriddenMembers) {
            PsiElement declarationPsiElement = BindingContextUtils.descriptorToDeclaration(bindingContext, overriddenMember);
            list.add(declarationPsiElement);
        }
        if (list.isEmpty()) {
            String myEmptyText = "empty text";
            JComponent renderer = HintUtil.createErrorLabel(myEmptyText);
            JBPopup popup = JBPopupFactory.getInstance().createComponentPopupBuilder(renderer, renderer).createPopup();
            if (event != null) {
                popup.show(new RelativePoint(event));
            }
            return;
        }
        if (list.size() == 1) {
            PsiNavigateUtil.navigate(list.iterator().next());
        }
        else {
            JBPopup popup = NavigationUtil.getPsiElementPopup(PsiUtilCore.toPsiElementArray(list),
                                                              new JetFunctionPsiElementCellRenderer(bindingContext),
                                                              DescriptorRenderer.TEXT.render(descriptor));
            if (event != null) {
                popup.show(new RelativePoint(event));
            }
        }
    }

    private static String calculateTooltipString(PsiElement element) {
        JetFile file = (JetFile)element.getContainingFile();
        if (file == null) return "";

        BindingContext bindingContext = WholeProjectAnalyzerFacade.analyzeProjectWithCacheOnAFile(file).getBindingContext();

        DeclarationDescriptor descriptor = bindingContext.get(BindingContext.DECLARATION_TO_DESCRIPTOR, element);
        if (!(descriptor instanceof CallableMemberDescriptor)) {
            return "";
        }

        Set<? extends CallableMemberDescriptor> overriddenMembers = ((CallableMemberDescriptor)descriptor).getOverriddenDescriptors();
        if (overriddenMembers.size() == 0) {
            return "";
        }

        boolean allOverriddenAbstract = true;
        for (CallableMemberDescriptor function : overriddenMembers) {
            allOverriddenAbstract &= function.getModality() == Modality.ABSTRACT;
        }

        String implementsOrOverrides = allOverriddenAbstract ? "implements" : "overrides";
        String memberKind = element instanceof JetNamedFunction ? "function" : "property";


        StringBuilder builder = new StringBuilder();
        builder.append(DescriptorRenderer.HTML.render(descriptor));
        int overrideCount = overriddenMembers.size();
        if (overrideCount >= 1) {
            builder.append("\n").append(implementsOrOverrides).append("\n");
            builder.append(DescriptorRenderer.HTML.render(overriddenMembers.iterator().next()));
        }
        if (overrideCount > 1) {
            int count = overrideCount - 1;
            builder.append("\nand ").append(count).append(" other ").append(memberKind);
            if (count > 1) {
                builder.append("s");
            }
        }

        return builder.toString();
    }

    @Override
    public void collectSlowLineMarkers(@NotNull List<PsiElement> elements, @NotNull Collection<LineMarkerInfo> result) {

        if (elements.isEmpty() || DumbService.getInstance(elements.get(0).getProject()).isDumb()) {
          return;
        }
        for (PsiElement element : elements) {
            if (element instanceof JetClass) {
                collectInheritingClasses((JetClass)element, result);
            }
        }
    }

    private static void collectInheritingClasses(JetClass element, Collection<LineMarkerInfo> result) {
        boolean isTrait = element.isTrait();
        if (!(isTrait ||
              element.hasModifier(JetTokens.OPEN_KEYWORD) ||
              element.hasModifier(JetTokens.ABSTRACT_KEYWORD))) {
            return;
        }
        PsiClass lightClass = LightClassUtil.createLightClass(element);
        if (lightClass == null) {
            return;
        }
        PsiClass inheritor = ClassInheritorsSearch.search(lightClass, false).findFirst();
        if (inheritor != null) {
            PsiElement nameIdentifier = element.getNameIdentifier();
            PsiElement anchor = nameIdentifier != null ? nameIdentifier : element;
            Icon mark = isTrait ? IMPLEMENTED_MARK : OVERRIDDEN_MARK;
            result.add(new LineMarkerInfo<PsiElement>(anchor, anchor.getTextOffset(), mark, Pass.UPDATE_OVERRIDEN_MARKERS,
                                                      SUBCLASSED_CLASS_TOOLTIP_ADAPTER, SUBCLASSED_CLASS_NAVIGATION_HANDLER));
        }
    }
}
