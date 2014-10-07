/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.intellij.codeHighlighting.Pass;
import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.codeInsight.daemon.LineMarkerProvider;
import com.intellij.codeInsight.daemon.impl.LineMarkerNavigator;
import com.intellij.codeInsight.daemon.impl.MarkerType;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.editor.markup.GutterIconRenderer;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.DumbService;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.search.searches.ClassInheritorsSearch;
import com.intellij.util.NullableFunction;
import kotlin.KotlinPackage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.asJava.LightClassUtil;
import org.jetbrains.jet.lang.descriptors.CallableMemberDescriptor;
import org.jetbrains.jet.lang.descriptors.Modality;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.psi.psiUtil.PsiUtilPackage;
import org.jetbrains.jet.lexer.JetTokens;
import org.jetbrains.jet.plugin.ProjectRootsUtil;
import org.jetbrains.jet.plugin.highlighter.markers.MarkersPackage;
import org.jetbrains.jet.plugin.highlighter.markers.ResolveWithParentsResult;
import org.jetbrains.jet.plugin.highlighter.markers.SuperDeclarationMarkerNavigationHandler;
import org.jetbrains.jet.plugin.highlighter.markers.SuperDeclarationMarkerTooltip;

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
                    PsiClass psiClass = MarkersPackage.getPsiClass(element);
                    return psiClass != null ? MarkerType.getSubclassedClassTooltip(psiClass) : null;
                }
            },

            new LineMarkerNavigator() {
                @Override
                public void browse(@Nullable MouseEvent e, @Nullable PsiElement element) {
                    PsiClass psiClass = MarkersPackage.getPsiClass(element);
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
                    PsiMethod psiMethod = MarkersPackage.getPsiMethod(element);
                    return psiMethod != null ? MarkersPackage.getOverriddenMethodTooltip(psiMethod) : null;
                }
            },

            new LineMarkerNavigator() {
                @Override
                public void browse(@Nullable MouseEvent e, @Nullable PsiElement element) {
                    PsiMethod psiMethod = MarkersPackage.getPsiMethod(element);
                    if (psiMethod != null) {
                        MarkersPackage.navigateToOverriddenMethod(e, psiMethod);
                    }
                }
            }
    );

    private static final MarkerType OVERRIDDEN_PROPERTY = new MarkerType(
            new NullableFunction<PsiElement, String>() {
                @Override
                public String fun(@Nullable PsiElement element) {
                    if (element == null) return null;

                    assert element.getParent() instanceof JetProperty : "This tooltip provider should be placed only on identifies in properties";
                    JetProperty property = (JetProperty) element.getParent();

                    return MarkersPackage.getOverriddenPropertyTooltip(property);
                }
            },

            new LineMarkerNavigator() {
                @Override
                public void browse(@Nullable MouseEvent e, @Nullable PsiElement element) {
                    if (element == null) return;

                    assert element.getParent() instanceof JetProperty : "This marker navigator should be placed only on identifies in properties";
                    JetProperty property = (JetProperty) element.getParent();

                    MarkersPackage.navigateToPropertyOverriddenDeclarations(e, property);
                }
            }
    );

    @Override
    public LineMarkerInfo getLineMarkerInfo(@NotNull PsiElement element) {
        // all Kotlin markers are added in slow marker pass
        return null;
    }

    private static boolean isImplementsAndNotOverrides(
            CallableMemberDescriptor descriptor,
            Collection<? extends CallableMemberDescriptor> overriddenMembers
    ) {
        if (descriptor.getModality() == Modality.ABSTRACT) return false;

        for (CallableMemberDescriptor function : overriddenMembers) {
            if (function.getModality() != Modality.ABSTRACT) return false;
        }

        return true;
    }

    @Override
    public void collectSlowLineMarkers(@NotNull List<PsiElement> elements, @NotNull Collection<LineMarkerInfo> result) {
        if (elements.isEmpty()) return;

        PsiElement first = KotlinPackage.first(elements);
        if (DumbService.getInstance(first.getProject()).isDumb() ||
            !ProjectRootsUtil.isInSourceWithGradleCheck(elements.get(0))) {
            return;
        }

        Set<JetNamedFunction> functions = Sets.newHashSet();
        Set<JetProperty> properties = Sets.newHashSet();

        for (PsiElement element : elements) {
            if (element instanceof JetClass) {
                collectInheritedClassMarker((JetClass) element, result);
            }
            else if (element instanceof JetNamedFunction) {
                JetNamedFunction function = (JetNamedFunction) element;

                functions.add(function);
                collectSuperDeclarationMarkers(function, result);
            }
            else if (element instanceof JetProperty) {
                JetProperty property = (JetProperty) element;

                properties.add(property);
                collectSuperDeclarationMarkers(property, result);
            }
        }

        collectOverridingAccessors(functions, result);
        collectOverridingPropertiesAccessors(properties, result);
    }

    private static void collectSuperDeclarationMarkers(JetDeclaration declaration, Collection<LineMarkerInfo> result) {
        assert (declaration instanceof JetNamedFunction || declaration instanceof JetProperty);

        if (!declaration.hasModifier(JetTokens.OVERRIDE_KEYWORD)) return;

        ResolveWithParentsResult resolveWithParents = MarkersPackage.resolveDeclarationWithParents(declaration);
        if (resolveWithParents.getOverriddenDescriptors().isEmpty()) return;

        // NOTE: Don't store descriptors in line markers because line markers are not deleted while editing other files and this can prevent
        // clearing the whole BindingTrace.
        LineMarkerInfo<JetElement> marker = new LineMarkerInfo<JetElement>(
                declaration,
                declaration.getTextOffset(),
                isImplementsAndNotOverrides(resolveWithParents.getDescriptor(), resolveWithParents.getOverriddenDescriptors()) ?
                IMPLEMENTING_MARK :
                OVERRIDING_MARK,
                Pass.UPDATE_OVERRIDEN_MARKERS,
                SuperDeclarationMarkerTooltip.INSTANCE$,
                new SuperDeclarationMarkerNavigationHandler()
        );

        result.add(marker);
    }

    private static void collectInheritedClassMarker(JetClass element, Collection<LineMarkerInfo> result) {
        boolean isTrait = element.isTrait();
        if (!(isTrait || element.hasModifier(JetTokens.OPEN_KEYWORD) || element.hasModifier(JetTokens.ABSTRACT_KEYWORD))) {
            return;
        }

        PsiClass lightClass = LightClassUtil.getPsiClass(element);
        if (lightClass == null) return;

        PsiClass inheritor = ClassInheritorsSearch.search(lightClass, false).findFirst();
        if (inheritor != null) {
            PsiElement nameIdentifier = element.getNameIdentifier();
            PsiElement anchor = nameIdentifier != null ? nameIdentifier : element;
            Icon mark = isTrait ? IMPLEMENTED_MARK : OVERRIDDEN_MARK;
            result.add(new LineMarkerInfo<PsiElement>(anchor, anchor.getTextOffset(), mark, Pass.UPDATE_OVERRIDEN_MARKERS,
                                                      SUBCLASSED_CLASS.getTooltip(), SUBCLASSED_CLASS.getNavigationHandler()));
        }
    }

    public static boolean isImplemented(JetNamedDeclaration declaration) {
        if (declaration.hasModifier(JetTokens.ABSTRACT_KEYWORD)) return true;

        PsiElement parent = declaration.getParent();
        parent = (parent instanceof JetClassBody) ? parent.getParent() : parent;

        if (parent instanceof JetClass) {
            return ((JetClass) parent).isTrait() &&
                   (!(declaration instanceof JetDeclarationWithBody) || !((JetDeclarationWithBody)declaration).hasBody()) &&
                   (!(declaration instanceof JetWithExpressionInitializer) || !((JetWithExpressionInitializer)declaration).hasInitializer());
        }

        return false;
    }

    private static void collectOverridingPropertiesAccessors(Collection<JetProperty> properties, Collection<LineMarkerInfo> result) {
        Map<PsiMethod, JetProperty> mappingToJava = Maps.newHashMap();
        for (JetProperty property : properties) {
            if (PsiUtilPackage.isOverridable(property)) {
                LightClassUtil.PropertyAccessorsPsiMethods accessorsPsiMethods = LightClassUtil.getLightClassPropertyMethods(property);
                for (PsiMethod psiMethod : accessorsPsiMethods) {
                    mappingToJava.put(psiMethod, property);
                }
            }
        }

        Set<PsiClass> classes = MarkersPackage.collectContainingClasses(mappingToJava.keySet());

        for (JetProperty property : MarkersPackage.getOverriddenDeclarations(mappingToJava, classes)) {
            ProgressManager.checkCanceled();

            PsiElement anchor = property.getNameIdentifier();
            if (anchor == null) anchor = property;

            LineMarkerInfo info = new LineMarkerInfo<PsiElement>(
                    anchor, anchor.getTextOffset(),
                    isImplemented(property) ? IMPLEMENTED_MARK : OVERRIDDEN_MARK,
                    Pass.UPDATE_OVERRIDEN_MARKERS,
                    OVERRIDDEN_PROPERTY.getTooltip(),
                    OVERRIDDEN_PROPERTY.getNavigationHandler(),
                    GutterIconRenderer.Alignment.RIGHT);

            result.add(info);
        }
    }

    private static void collectOverridingAccessors(Collection<JetNamedFunction> functions, Collection<LineMarkerInfo> result) {
        Map<PsiMethod, JetNamedFunction> mappingToJava = Maps.newHashMap();
        for (JetNamedFunction function : functions) {
            if (PsiUtilPackage.isOverridable(function)) {
                PsiMethod method = LightClassUtil.getLightClassMethod(function);
                if (method != null) {
                    mappingToJava.put(method, function);
                }
            }
        }

        Set<PsiClass> classes = MarkersPackage.collectContainingClasses(mappingToJava.keySet());

        for (JetNamedFunction function : MarkersPackage.getOverriddenDeclarations(mappingToJava, classes)) {
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
