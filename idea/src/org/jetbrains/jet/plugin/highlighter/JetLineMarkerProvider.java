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

import com.google.common.collect.*;
import com.intellij.codeHighlighting.Pass;
import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.codeInsight.daemon.LineMarkerProvider;
import com.intellij.codeInsight.daemon.impl.GutterIconTooltipHelper;
import com.intellij.codeInsight.daemon.impl.LineMarkerNavigator;
import com.intellij.codeInsight.daemon.impl.MarkerType;
import com.intellij.codeInsight.daemon.impl.PsiElementListNavigator;
import com.intellij.icons.AllIcons;
import com.intellij.ide.util.DefaultPsiElementCellRenderer;
import com.intellij.ide.util.PsiClassListCellRenderer;
import com.intellij.openapi.editor.markup.GutterIconRenderer;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.DumbService;
import com.intellij.psi.NavigatablePsiElement;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.PsiElementProcessor;
import com.intellij.psi.search.PsiElementProcessorAdapter;
import com.intellij.psi.search.searches.ClassInheritorsSearch;
import com.intellij.psi.search.searches.OverridingMethodsSearch;
import com.intellij.util.*;
import kotlin.KotlinPackage;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.asJava.LightClassUtil;
import org.jetbrains.jet.lang.descriptors.CallableMemberDescriptor;
import org.jetbrains.jet.lang.descriptors.Modality;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lexer.JetTokens;
import org.jetbrains.jet.plugin.JetBundle;
import org.jetbrains.jet.plugin.ProjectRootsUtil;
import org.jetbrains.jet.plugin.highlighter.markers.MarkersPackage;
import org.jetbrains.jet.plugin.highlighter.markers.ResolveWithParentsResult;
import org.jetbrains.jet.plugin.highlighter.markers.SuperDeclarationMarkerNavigationHandler;
import org.jetbrains.jet.plugin.highlighter.markers.SuperDeclarationMarkerTooltip;
import org.jetbrains.jet.plugin.search.ideaExtensions.KotlinDefinitionsSearcher;

import javax.swing.*;
import java.awt.event.MouseEvent;
import java.util.*;

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

                    PsiElementProcessor.CollectElementsWithLimit<PsiClass> processor = new PsiElementProcessor.CollectElementsWithLimit<PsiClass>(5);
                    Processor<PsiMethod> consumer = new AdapterProcessor<PsiMethod, PsiClass>(
                            new CommonProcessors.UniqueProcessor<PsiClass>(new PsiElementProcessorAdapter<PsiClass>(processor)),
                            new Function<PsiMethod, PsiClass>() {
                                @Override
                                public PsiClass fun(PsiMethod method) {
                                    return method.getContainingClass();
                                }
                            });

                    for (PsiMethod method : LightClassUtil.getLightClassPropertyMethods(property)) {
                        if (!processor.isOverflow()) {
                            OverridingMethodsSearch.search(method, true).forEach(consumer);
                        }
                    }

                    boolean isImplemented = isImplemented(property);
                    if (processor.isOverflow()) {
                        return isImplemented ?
                               JetBundle.message("property.is.implemented.too.many") :
                               JetBundle.message("property.is.overridden.too.many");
                    }

                    List<PsiClass> collectedClasses = Lists.newArrayList(processor.getCollection());
                    if (collectedClasses.isEmpty()) return null;

                    Collections.sort(collectedClasses, new PsiClassListCellRenderer().getComparator());

                    String start = isImplemented ?
                                   JetBundle.message("property.is.implemented.header") :
                                   JetBundle.message("property.is.overridden.header");

                    @NonNls String pattern = "&nbsp;&nbsp;&nbsp;&nbsp;{0}";
                    return GutterIconTooltipHelper.composeText(collectedClasses, start, pattern);
                }
            },

            new LineMarkerNavigator() {
                @Override
                public void browse(@Nullable MouseEvent e, @Nullable final PsiElement element) {
                    if (element == null) return;

                    assert element.getParent() instanceof JetProperty : "This marker navigator should be placed only on identifies in properties";
                    JetProperty property = (JetProperty) element.getParent();

                    if (DumbService.isDumb(element.getProject())) {
                        DumbService.getInstance(element.getProject()).showDumbModeNotification("Navigation to overriding classes is not possible during index update");
                        return;
                    }

                    final LightClassUtil.PropertyAccessorsPsiMethods psiPropertyMethods =
                            LightClassUtil.getLightClassPropertyMethods((JetProperty) element.getParent());

                    final CommonProcessors.CollectUniquesProcessor<PsiElement> elementProcessor = new CommonProcessors.CollectUniquesProcessor<PsiElement>();
                    Runnable jetPsiMethodProcessor = new Runnable() {
                        @Override
                        public void run() {
                            KotlinDefinitionsSearcher.processPropertyImplementationsMethods(psiPropertyMethods, GlobalSearchScope.allScope(element.getProject()), elementProcessor);
                        }
                    };

                    if (!ProgressManager.getInstance().runProcessWithProgressSynchronously(
                            jetPsiMethodProcessor,
                            MarkerType.SEARCHING_FOR_OVERRIDING_METHODS, true,
                            element.getProject(),
                            e != null ? (JComponent) e.getComponent() : null)) {
                        return;
                    }

                    DefaultPsiElementCellRenderer renderer = new DefaultPsiElementCellRenderer();
                    List<PsiElement> elements = Ordering.from(renderer.getComparator()).sortedCopy(elementProcessor.getResults());

                    NavigatablePsiElement[] navigatableElements = Iterables.toArray(
                            Iterables.filter(elements, NavigatablePsiElement.class), NavigatablePsiElement.class);

                    PsiElementListNavigator.openTargets(e, navigatableElements,
                                                        JetBundle.message("navigation.title.overriding.property", property.getName()),
                                                        JetBundle.message("navigation.findUsages.title.overriding.property", property.getName()),
                                                        renderer);
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

    private static void collectOverridingPropertiesAccessors(Collection<JetProperty> properties, Collection<LineMarkerInfo> result) {
        Map<PsiMethod, JetProperty> mappingToJava = Maps.newHashMap();
        for (JetProperty property : properties) {
            if (isOverridableHeuristic(property)) {
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
                    OVERRIDDEN_PROPERTY.getTooltip(), OVERRIDDEN_PROPERTY.getNavigationHandler(),
                    GutterIconRenderer.Alignment.RIGHT);

            result.add(info);
        }
    }

    private static void collectOverridingAccessors(Collection<JetNamedFunction> functions, Collection<LineMarkerInfo> result) {
        Map<PsiMethod, JetNamedFunction> mappingToJava = Maps.newHashMap();
        for (JetNamedFunction function : functions) {
            if (isOverridableHeuristic(function)) {
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
