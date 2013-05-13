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
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.intellij.codeHighlighting.Pass;
import com.intellij.codeInsight.daemon.GutterIconNavigationHandler;
import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.codeInsight.daemon.LineMarkerProvider;
import com.intellij.codeInsight.daemon.impl.LineMarkerNavigator;
import com.intellij.codeInsight.daemon.impl.MarkerType;
import com.intellij.codeInsight.hint.HintUtil;
import com.intellij.codeInsight.navigation.NavigationUtil;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.editor.markup.GutterIconRenderer;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.CommonClassNames;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.search.searches.AllOverridingMethodsSearch;
import com.intellij.psi.search.searches.ClassInheritorsSearch;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.util.Function;
import com.intellij.util.NullableFunction;
import com.intellij.util.Processor;
import com.intellij.util.PsiNavigateUtil;
import gnu.trove.THashSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.asJava.LightClassUtil;
import org.jetbrains.jet.lang.descriptors.CallableMemberDescriptor;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.Modality;
import org.jetbrains.jet.lang.psi.*;
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
import java.util.Map;
import java.util.Set;

public class JetLineMarkerProvider implements LineMarkerProvider {
    public static final Icon OVERRIDING_MARK = AllIcons.Gutter.OverridingMethod;
    public static final Icon IMPLEMENTING_MARK = AllIcons.Gutter.ImplementingMethod;
    protected static final Icon OVERRIDDEN_MARK = AllIcons.Gutter.OverridenMethod;
    protected static final Icon IMPLEMENTED_MARK = AllIcons.Gutter.ImplementedMethod;

    private static final MarkerType SUBCLASSED_CLASS = new MarkerType(
            new NullableFunction<PsiElement, String>() {
                @Override
                public String fun(@Nullable PsiElement element) {
                    PsiClass psiClass = getPsiClass(element);
                    return psiClass != null ? MarkerType.getSubclassedClassTooltip(psiClass) : null;
                }
            },

            new LineMarkerNavigator() {
                @Override
                public void browse(@Nullable MouseEvent e, @Nullable PsiElement element) {
                    PsiClass psiClass = getPsiClass(element);
                    if (psiClass != null) {
                        MarkerType.navigateToSubclassedClass(e, psiClass);
                    }
                }
            }
    );

    private static final MarkerType OVERRIDDEN_FUNCTION = new MarkerType(
            new NullableFunction<PsiElement, String>() {
                @Override
                public String fun(@Nullable PsiElement element) {
                    PsiMethod psiMethod = getPsiMethod(element);
                    return psiMethod != null ? MarkerType.getOverriddenMethodTooltip(psiMethod) : null;
                }
            },

            new LineMarkerNavigator() {
                @Override
                public void browse(@Nullable MouseEvent e, @Nullable PsiElement element) {
                    PsiMethod psiMethod = getPsiMethod(element);
                    if (psiMethod != null) {
                        MarkerType.navigateToOverriddenMethod(e, psiMethod);
                    }
                }
            }
    );

    @Nullable
    private static PsiClass getPsiClass(@Nullable PsiElement element) {
        if (element == null) return null;
        if (element instanceof PsiClass) return (PsiClass) element;

        if (!(element instanceof JetClass)) {
            element = element.getParent();
            if (!(element instanceof JetClass)) {
                return null;
            }
        }

        return LightClassUtil.getPsiClass((JetClass) element);
    }

    @Nullable
    private static PsiMethod getPsiMethod(@Nullable PsiElement element) {
        if (element == null) return null;
        if (element instanceof PsiMethod) return (PsiMethod) element;
        if (element.getParent() instanceof JetNamedFunction) return LightClassUtil.getLightClassMethod((JetNamedFunction) element.getParent());
        return null;
    }

    @Override
    public LineMarkerInfo getLineMarkerInfo(@NotNull PsiElement element) {
        JetFile file = (JetFile)element.getContainingFile();
        if (file == null) return null;

        if (!(element instanceof JetNamedFunction || element instanceof JetProperty)) return null;

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
            builder.append("<br/>").append(implementsOrOverrides).append("<br/>");
            builder.append(DescriptorRenderer.HTML.render(overriddenMembers.iterator().next()));
        }
        if (overrideCount > 1) {
            int otherCount = overrideCount - 1;
            builder.append("<br/>and ").append(otherCount).append(" other ").append(StringUtil.pluralize(memberKind, otherCount));
        }

        return builder.toString();
    }

    @Override
    public void collectSlowLineMarkers(@NotNull List<PsiElement> elements, @NotNull Collection<LineMarkerInfo> result) {
        if (elements.isEmpty() || DumbService.getInstance(elements.get(0).getProject()).isDumb()) {
            return;
        }

        Set<JetNamedFunction> functions = Sets.newHashSet();

        for (PsiElement element : elements) {
            if (element instanceof JetClass) {
                collectInheritingClasses((JetClass) element, result);
            }

            if (element instanceof JetNamedFunction) {
                functions.add((JetNamedFunction) element);
            }
        }

        collectOverridingAccessors(functions, result);
    }

    private static void collectInheritingClasses(JetClass element, Collection<LineMarkerInfo> result) {
        boolean isTrait = element.isTrait();
        if (!(isTrait ||
              element.hasModifier(JetTokens.OPEN_KEYWORD) ||
              element.hasModifier(JetTokens.ABSTRACT_KEYWORD))) {
            return;
        }
        PsiClass lightClass = LightClassUtil.getPsiClass(element);
        if (lightClass == null) {
            return;
        }
        PsiClass inheritor = ClassInheritorsSearch.search(lightClass, false).findFirst();
        if (inheritor != null) {
            PsiElement nameIdentifier = element.getNameIdentifier();
            PsiElement anchor = nameIdentifier != null ? nameIdentifier : element;
            Icon mark = isTrait ? IMPLEMENTED_MARK : OVERRIDDEN_MARK;
            result.add(new LineMarkerInfo<PsiElement>(anchor, anchor.getTextOffset(), mark, Pass.UPDATE_OVERRIDEN_MARKERS,
                                                      SUBCLASSED_CLASS.getTooltip(), SUBCLASSED_CLASS.getNavigationHandler()));
        }
    }

    private static boolean isOverridableHeuristic(JetNamedDeclaration declaration) {
        PsiElement parent = declaration.getParent();
        if (parent instanceof JetFile) return false;
        if (parent instanceof JetClassBody && parent.getParent() instanceof JetClass) {
            // Not all open or abstract declarations are actually overridable, but this is heuristic
            return declaration.hasModifier(JetTokens.ABSTRACT_KEYWORD) ||
                   declaration.hasModifier(JetTokens.OPEN_KEYWORD) ||
                   declaration.hasModifier(JetTokens.OVERRIDE_KEYWORD) ||
                   ((JetClass) parent.getParent()).isTrait();
        }

        return false;
    }

    private static boolean isImplemented(JetNamedDeclaration declaration) {
        if (declaration.hasModifier(JetTokens.ABSTRACT_KEYWORD)) return true;

        PsiElement parent = declaration.getParent();
        if (parent instanceof JetClass) {
            return ((JetClass) parent).isTrait();
        }

        return false;
    }

    private static void collectOverridingAccessors(Collection<JetNamedFunction> functions, Collection<LineMarkerInfo> result) {
        final Map<PsiMethod, JetNamedFunction> mappingToJava = Maps.newHashMap();
        for (JetNamedFunction function : functions) {
            if (isOverridableHeuristic(function)) {
                PsiMethod method = LightClassUtil.getLightClassMethod(function);
                if (method != null) {
                    mappingToJava.put(method, function);
                }
            }
        }

        Set<PsiClass> classes = new THashSet<PsiClass>();
        for (PsiMethod method : mappingToJava.keySet()) {
            ProgressManager.checkCanceled();
            PsiClass parentClass = method.getContainingClass();
            if (parentClass != null && !CommonClassNames.JAVA_LANG_OBJECT.equals(parentClass.getQualifiedName())) {
                classes.add(parentClass);
            }
        }

        final Set<JetNamedFunction> overridden = Sets.newHashSet();
        for (PsiClass aClass : classes) {
            AllOverridingMethodsSearch.search(aClass).forEach(new Processor<Pair<PsiMethod, PsiMethod>>() {
                @Override
                public boolean process(Pair<PsiMethod, PsiMethod> pair) {
                    ProgressManager.checkCanceled();

                    PsiMethod superMethod = pair.getFirst();

                    JetNamedFunction function = mappingToJava.get(superMethod);
                    if (function != null) {
                        mappingToJava.remove(superMethod);
                        overridden.add(function);
                    }

                    return !mappingToJava.isEmpty();
                }
            });
        }

        for (JetNamedFunction function : overridden) {
            ProgressManager.checkCanceled();

            PsiElement anchor = function.getNameIdentifier();
            if (anchor == null) anchor = function;

            LineMarkerInfo info = new LineMarkerInfo<PsiElement>(
                    anchor, anchor.getTextOffset(),
                    isImplemented(function) ? IMPLEMENTED_MARK : OVERRIDDEN_MARK,
                    Pass.UPDATE_OVERRIDEN_MARKERS,
                    OVERRIDDEN_FUNCTION.getTooltip(), OVERRIDDEN_FUNCTION.getNavigationHandler(),
                    GutterIconRenderer.Alignment.RIGHT);

            result.add(info);
        }
    }
}
