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
import com.intellij.codeInsight.daemon.GutterIconNavigationHandler;
import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.codeInsight.daemon.LineMarkerProvider;
import com.intellij.codeInsight.daemon.impl.GutterIconTooltipHelper;
import com.intellij.codeInsight.daemon.impl.LineMarkerNavigator;
import com.intellij.codeInsight.daemon.impl.MarkerType;
import com.intellij.codeInsight.daemon.impl.PsiElementListNavigator;
import com.intellij.codeInsight.hint.HintUtil;
import com.intellij.codeInsight.navigation.NavigationUtil;
import com.intellij.icons.AllIcons;
import com.intellij.ide.util.DefaultPsiElementCellRenderer;
import com.intellij.ide.util.PsiClassListCellRenderer;
import com.intellij.openapi.editor.markup.GutterIconRenderer;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.PsiElementProcessor;
import com.intellij.psi.search.PsiElementProcessorAdapter;
import com.intellij.psi.search.searches.AllOverridingMethodsSearch;
import com.intellij.psi.search.searches.ClassInheritorsSearch;
import com.intellij.psi.search.searches.OverridingMethodsSearch;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.util.*;
import gnu.trove.THashSet;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.asJava.LightClassUtil;
import org.jetbrains.jet.lang.descriptors.CallableMemberDescriptor;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.Modality;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.DescriptorToSourceUtils;
import org.jetbrains.jet.lang.resolve.OverrideResolver;
import org.jetbrains.jet.lexer.JetTokens;
import org.jetbrains.jet.plugin.JetBundle;
import org.jetbrains.jet.plugin.JetPluginUtil;
import org.jetbrains.jet.plugin.caches.resolve.ResolvePackage;
import org.jetbrains.jet.plugin.codeInsight.JetFunctionPsiElementCellRenderer;
import org.jetbrains.jet.plugin.project.AnalyzerFacadeWithCache;
import org.jetbrains.jet.plugin.search.ideaExtensions.KotlinDefinitionsSearcher;
import org.jetbrains.jet.renderer.DescriptorRenderer;

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

        BindingContext bindingContext = AnalyzerFacadeWithCache.getContextForElement((JetElement) element);
        DeclarationDescriptor descriptor = bindingContext.get(BindingContext.DECLARATION_TO_DESCRIPTOR, element);

        if (!(descriptor instanceof CallableMemberDescriptor)) {
            return null;
        }

        Set<? extends CallableMemberDescriptor> overriddenMembers = OverrideResolver.getDirectlyOverriddenDeclarations(
                (CallableMemberDescriptor) descriptor);
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

        BindingContext bindingContext = AnalyzerFacadeWithCache.getContextForElement((JetElement) elt);
        DeclarationDescriptor descriptor = bindingContext.get(BindingContext.DECLARATION_TO_DESCRIPTOR, elt);
        if (!(descriptor instanceof CallableMemberDescriptor)) {
            return;
        }

        Set<CallableMemberDescriptor> overriddenMembers = OverrideResolver
                .getDirectlyOverriddenDeclarations((CallableMemberDescriptor) descriptor);
        if (overriddenMembers.size() == 0) {
            return;
        }

        if (overriddenMembers.isEmpty()) return;
        List<PsiElement> list = Lists.newArrayList();
        for (CallableMemberDescriptor overriddenMember : overriddenMembers) {
            PsiElement declarationPsiElement = DescriptorToSourceUtils.descriptorToDeclaration(overriddenMember);
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
                                                              DescriptorRenderer.FQ_NAMES_IN_TYPES.render(descriptor));
            if (event != null) {
                popup.show(new RelativePoint(event));
            }
        }
    }

    private static String calculateTooltipString(PsiElement element) {
        JetFile file = (JetFile)element.getContainingFile();
        if (file == null) return "";

        BindingContext bindingContext = ResolvePackage.getBindingContext(file);

        DeclarationDescriptor descriptor = bindingContext.get(BindingContext.DECLARATION_TO_DESCRIPTOR, element);
        if (!(descriptor instanceof CallableMemberDescriptor)) {
            return "";
        }

        Set<CallableMemberDescriptor> overriddenMembers = OverrideResolver
                .getDirectlyOverriddenDeclarations((CallableMemberDescriptor) descriptor);
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
        if (elements.isEmpty() ||
            DumbService.getInstance(elements.get(0).getProject()).isDumb() ||
            !JetPluginUtil.isInSource(elements.get(0)) ||
            JetPluginUtil.isKtFileInGradleProjectInWrongFolder(elements.get(0))) {
            return;
        }

        Set<JetNamedFunction> functions = Sets.newHashSet();
        Set<JetProperty> properties = Sets.newHashSet();

        for (PsiElement element : elements) {
            if (element instanceof JetClass) {
                collectInheritingClasses((JetClass) element, result);
            }

            if (element instanceof JetNamedFunction) {
                functions.add((JetNamedFunction) element);
            }

            if (element instanceof JetProperty) {
                properties.add((JetProperty) element);
            }
        }

        collectOverridingAccessors(functions, result);
        collectOverridingPropertiesAccessors(properties, result);
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

        Set<PsiClass> classes = collectContainingClasses(mappingToJava.keySet());

        for (JetProperty property : getOverriddenDeclarations(mappingToJava, classes)) {
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

        Set<PsiClass> classes = collectContainingClasses(mappingToJava.keySet());

        for (JetNamedFunction function : getOverriddenDeclarations(mappingToJava, classes)) {
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

    private static Set<PsiClass> collectContainingClasses(Collection<PsiMethod> methods) {
        Set<PsiClass> classes = new THashSet<PsiClass>();
        for (PsiMethod method : methods) {
            ProgressManager.checkCanceled();
            PsiClass parentClass = method.getContainingClass();
            if (parentClass != null && !CommonClassNames.JAVA_LANG_OBJECT.equals(parentClass.getQualifiedName())) {
                classes.add(parentClass);
            }
        }
        return classes;
    }

    private static <T> Set<T> getOverriddenDeclarations(final Map<PsiMethod, T> mappingToJava, Set<PsiClass> classes) {
        final Set<T> overridden = Sets.newHashSet();
        for (PsiClass aClass : classes) {
            AllOverridingMethodsSearch.search(aClass).forEach(new Processor<Pair<PsiMethod, PsiMethod>>() {
                @Override
                public boolean process(Pair<PsiMethod, PsiMethod> pair) {
                    ProgressManager.checkCanceled();

                    PsiMethod superMethod = pair.getFirst();

                    T declaration = mappingToJava.get(superMethod);
                    if (declaration != null) {
                        mappingToJava.remove(superMethod);
                        overridden.add(declaration);
                    }

                    return !mappingToJava.isEmpty();
                }
            });
        }

        return overridden;
    }
}
