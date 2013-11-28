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

package org.jetbrains.jet.plugin.ktSignature;

import com.intellij.codeHighlighting.Pass;
import com.intellij.codeInsight.daemon.GutterIconNavigationHandler;
import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.codeInsight.daemon.LineMarkerProvider;
import com.intellij.icons.AllIcons;
import com.intellij.ide.DataManager;
import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.editor.markup.GutterIconRenderer;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.*;
import com.intellij.util.Function;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.di.InjectorForJavaDescriptorResolver;
import org.jetbrains.jet.di.InjectorForJavaDescriptorResolverUtil;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.PackageFragmentDescriptor;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.BindingTrace;
import org.jetbrains.jet.lang.resolve.DelegatingBindingTrace;
import org.jetbrains.jet.lang.resolve.java.JavaBindingContext;
import org.jetbrains.jet.lang.resolve.java.JavaDescriptorResolver;
import org.jetbrains.jet.lang.resolve.java.descriptor.JavaClassDescriptor;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.plugin.JetIcons;
import org.jetbrains.jet.plugin.caches.resolve.KotlinCacheManager;
import org.jetbrains.jet.plugin.caches.resolve.KotlinDeclarationsCache;
import org.jetbrains.jet.plugin.project.ProjectStructureUtil;
import org.jetbrains.jet.plugin.project.TargetPlatform;

import java.awt.event.MouseEvent;
import java.util.Collection;
import java.util.List;

import static org.jetbrains.jet.lang.resolve.java.DescriptorSearchRule.IGNORE_KOTLIN_SOURCES;

public class KotlinSignatureInJavaMarkerProvider implements LineMarkerProvider {
    private static final String SHOW_MARKERS_PROPERTY = "kotlin.signature.markers.enabled";

    private static final GutterIconNavigationHandler<PsiModifierListOwner> NAVIGATION_HANDLER = new GutterIconNavigationHandler<PsiModifierListOwner>() {
        @Override
        public void navigate(MouseEvent e, PsiModifierListOwner element) {
            if (UIUtil.isActionClick(e, MouseEvent.MOUSE_RELEASED)) {
                new EditSignatureAction(element).actionPerformed(DataManager.getInstance().getDataContext(e.getComponent()), e.getPoint());
            }
        }
    };

    @Override
    @Nullable
    public LineMarkerInfo getLineMarkerInfo(@NotNull PsiElement element) {
        return null;
    }

    @Override
    public void collectSlowLineMarkers(@NotNull List<PsiElement> elements, @NotNull Collection<LineMarkerInfo> result) {
        if (elements.isEmpty()) {
            return;
        }

        Project project = elements.get(0).getProject();
        if (!isMarkersEnabled(project)) {
            return;
        }

        if (!ProjectStructureUtil.hasJvmKotlinModules(project)) {
            return;
        }

        Module module = ModuleUtilCore.findModuleForPsiElement(elements.get(0));
        if (module != null && !ProjectStructureUtil.isUsedInKotlinJavaModule(module)) {
            return;
        }

        BindingTrace trace = createDelegatingTrace(project);
        InjectorForJavaDescriptorResolver injector = InjectorForJavaDescriptorResolverUtil.create(project, trace);

        JavaDescriptorResolver javaDescriptorResolver = injector.getJavaDescriptorResolver();

        for (PsiElement element : elements) {
            if (!(element instanceof PsiMember)) {
                continue;
            }

            PsiMember member = (PsiMember) element;
            if (member.hasModifierProperty(PsiModifier.PRIVATE)) {
                continue;
            }

            DeclarationDescriptor memberDescriptor = getDescriptorForMember(javaDescriptorResolver, member, trace);

            if (memberDescriptor == null) continue;

            List<String> errors = trace.get(JavaBindingContext.LOAD_FROM_JAVA_SIGNATURE_ERRORS, memberDescriptor);
            boolean hasSignatureAnnotation = KotlinSignatureUtil.findKotlinSignatureAnnotation(element) != null;

            if (errors != null || hasSignatureAnnotation) {
                result.add(new MyLineMarkerInfo((PsiModifierListOwner) element, errors, hasSignatureAnnotation));
            }
        }
    }

    @NotNull
    static BindingTrace createDelegatingTrace(@NotNull Project project) {
        KotlinDeclarationsCache declarationsCache = KotlinCacheManager.getInstance(project).getDeclarationsFromProject(TargetPlatform.JVM);
        BindingContext bindingContext = declarationsCache.getBindingContext();
        return new DelegatingBindingTrace(bindingContext, "wrapped context of declarations cache");
    }

    @Nullable
    private static DeclarationDescriptor getDescriptorForMember(
            @NotNull JavaDescriptorResolver javaDescriptorResolver,
            @NotNull PsiMember member,
            @NotNull BindingTrace trace
    ) {
        PsiClass containingClass = member.getContainingClass();
        if (containingClass == null) { // e.g., type parameter
            return null;
        }

        String qualifiedName = containingClass.getQualifiedName();
        if (qualifiedName == null) {
            // Trying to get line markers for anonymous or local class
            return null;
        }

        FqName classFqName = new FqName(qualifiedName);
        JetScope memberScope = getScopeForMember(javaDescriptorResolver, classFqName, member);

        if (memberScope == null) {
            return null;
        }
        return getDescriptorForMember(member, memberScope, trace);
    }

    @Nullable
    private static JetScope getScopeForMember(
            @NotNull JavaDescriptorResolver javaDescriptorResolver,
            @NotNull FqName classFqName,
            @NotNull PsiMember member
    ) {
        if (member.hasModifierProperty(PsiModifier.STATIC)) {
            PackageFragmentDescriptor packageFragment = javaDescriptorResolver.getPackageFragmentProvider().getOrCreatePackage(classFqName);
            if (packageFragment == null) {
                return null;
            }

            return packageFragment.getMemberScope();
        }
        else {
            ClassDescriptor klass = javaDescriptorResolver.resolveClass(classFqName, IGNORE_KOTLIN_SOURCES);
            if (klass == null) {
                return null;
            }

            return klass.getDefaultType().getMemberScope();
        }
    }

    @Nullable
    private static DeclarationDescriptor getDescriptorForMember(
            @NotNull PsiMember member,
            @NotNull JetScope memberScope,
            @NotNull BindingTrace trace
    ) {
        if (!(member instanceof PsiMethod) && !(member instanceof PsiField)) {
            return null;
        }

        String memberNameAsString = member.getName();
        assert memberNameAsString != null: "No name for member: \n" + member.getText();

        Name name = Name.identifier(memberNameAsString);
        if (member instanceof PsiMethod) {
            if (((PsiMethod) member).isConstructor()) {
                DeclarationDescriptor container = memberScope.getContainingDeclaration();
                assert container instanceof JavaClassDescriptor : container + "\n" + memberScope;
                ((JavaClassDescriptor) container).getConstructors();
            }
            else {
                memberScope.getFunctions(name);
            }
        }
        else {
            memberScope.getProperties(name);
        }

        PsiModifierListOwner annotationOwner = KotlinSignatureUtil.getAnnotationOwner(member);
        return trace.get(BindingContext.DECLARATION_TO_DESCRIPTOR, annotationOwner);
    }

    public static boolean isMarkersEnabled(@NotNull Project project) {
        return PropertiesComponent.getInstance(project).getBoolean(SHOW_MARKERS_PROPERTY, true);
    }

    public static void setMarkersEnabled(@NotNull Project project, boolean value) {
        PropertiesComponent.getInstance(project).setValue(SHOW_MARKERS_PROPERTY, Boolean.toString(value));
        KotlinSignatureUtil.refreshMarkers(project);
    }

    private static class MyLineMarkerInfo extends LineMarkerInfo<PsiModifierListOwner> {
        public MyLineMarkerInfo(PsiModifierListOwner element, @Nullable List<String> errors, boolean hasAnnotation) {
            super(element, element.getTextOffset(), errors != null ? AllIcons.Ide.Error : JetIcons.SMALL_LOGO, Pass.UPDATE_OVERRIDEN_MARKERS,
                  new TooltipProvider(errors), hasAnnotation ? NAVIGATION_HANDLER : null);
        }

        @Nullable
        @Override
        public GutterIconRenderer createGutterRenderer() {
            return new LineMarkerGutterIconRenderer<PsiModifierListOwner>(this) {
                @Nullable
                @Override
                public ActionGroup getPopupMenuActions() {
                    PsiModifierListOwner element = getElement();
                    assert element != null;

                    return new DefaultActionGroup(new EditSignatureAction(element), new DeleteSignatureAction(element));
                }
            };
        }
    }

    private static class TooltipProvider implements Function<PsiElement, String> {
        private final @Nullable List<String> errors;

        private TooltipProvider(@Nullable List<String> errors) {
            this.errors = errors;
        }

        @Nullable
        @Override
        public String fun(PsiElement element) {
            PsiAnnotation annotation = KotlinSignatureUtil.findKotlinSignatureAnnotation(element);

            if (annotation == null) return errorsString();

            String signature = KotlinSignatureUtil.getKotlinSignature(annotation);
            String text = "Alternative Kotlin signature is available for this method:\n" + StringUtil.escapeXml(signature);
            if (errors == null) {
                return text;
            }
            return text + "\nIt has the following " + StringUtil.pluralize("error", errors.size()) + ":\n" + errorsString();
        }

        @NotNull
        private String errorsString() {
            assert errors != null;
            return StringUtil.escapeXml(StringUtil.join(errors, "\n"));
        }
    }
}
