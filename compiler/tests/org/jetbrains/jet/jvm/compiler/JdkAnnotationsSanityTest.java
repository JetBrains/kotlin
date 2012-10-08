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

package org.jetbrains.jet.jvm.compiler;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.intellij.codeInsight.AnnotationUtil;
import com.intellij.codeInsight.ExternalAnnotationsManager;
import com.intellij.openapi.util.io.StreamUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.vfs.VirtualFileVisitor;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiFormatUtil;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.ConfigurationKind;
import org.jetbrains.jet.JetTestUtils;
import org.jetbrains.jet.TestJdkKind;
import org.jetbrains.jet.cli.jvm.compiler.CoreExternalAnnotationsManager;
import org.jetbrains.jet.cli.jvm.compiler.JetCoreEnvironment;
import org.jetbrains.jet.di.InjectorForJavaSemanticServices;
import org.jetbrains.jet.lang.BuiltinsScopeExtensionMode;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.java.JavaDescriptorResolver;
import org.jetbrains.jet.lang.resolve.java.JvmStdlibNames;
import org.jetbrains.jet.lang.resolve.lazy.KotlinTestWithEnvironment;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.resolve.DescriptorRenderer;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JdkAnnotationsSanityTest extends KotlinTestWithEnvironment {
    private VirtualFile kotlinAnnotationsRoot;
    private VirtualFile ideaAnnotationsRoot;

    @Override
    protected JetCoreEnvironment createEnvironment() {
        return JetTestUtils.createEnvironmentWithJdkAndNullabilityAnnotationsFromIdea(
                myTestRootDisposable, ConfigurationKind.JDK_AND_ANNOTATIONS, TestJdkKind.FULL_JDK);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        kotlinAnnotationsRoot = VirtualFileManager.getInstance().findFileByUrl("file://jdk-annotations");
        ideaAnnotationsRoot = VirtualFileManager.getInstance().findFileByUrl("jar://ideaSDK/lib/jdkAnnotations.jar!/");
    }

    public void testNoErrorsInAlternativeSignatures() {
        InjectorForJavaSemanticServices injector = new InjectorForJavaSemanticServices(BuiltinsScopeExtensionMode.ALL,
                                                                                       getProject());

        final BindingContext bindingContext = injector.getBindingTrace().getBindingContext();
        JavaDescriptorResolver javaDescriptorResolver = injector.getJavaDescriptorResolver();

        final Map<DeclarationDescriptor, String> errors = Maps.newHashMap();

        Iterable<FqName> affectedClasses = getAffectedClasses(kotlinAnnotationsRoot);
        AlternativeSignatureErrorFindingVisitor visitor = new AlternativeSignatureErrorFindingVisitor(bindingContext, errors);
        for (FqName javaClass : affectedClasses) {
            ClassDescriptor topLevelClass = javaDescriptorResolver.resolveClass(javaClass);
            NamespaceDescriptor topLevelNamespace = javaDescriptorResolver.resolveNamespace(javaClass);
            assertNotNull("Class has annotation, but it is not found: " + javaClass, topLevelClass);

            topLevelClass.acceptVoid(visitor);

            if (topLevelNamespace != null) {
                topLevelNamespace.acceptVoid(visitor);
            }
        }

        if (!errors.isEmpty()) {
            StringBuilder sb = new StringBuilder("Error(s) in JDK alternative signatures: \n");
            for (Map.Entry<DeclarationDescriptor, String> entry : errors.entrySet()) {
                sb.append(DescriptorRenderer.TEXT.render(entry.getKey())).append(" : ").append(entry.getValue()).append("\n");
            }
            fail(sb.toString());
        }
    }

    private CoreExternalAnnotationsManager createFakeAnnotationsManager(VirtualFile annotationsRoot) {
        CoreExternalAnnotationsManager annotationsManager = new CoreExternalAnnotationsManager(PsiManager.getInstance(getProject()));
        annotationsManager.addExternalAnnotationsRoot(annotationsRoot);
        return annotationsManager;
    }

    public void testAllIdeaJdkAnnotationsAreReflected() {
        JavaPsiFacade javaPsiFacade = JavaPsiFacade.getInstance(getProject());
        GlobalSearchScope allScope = GlobalSearchScope.allScope(getProject());

        final CoreExternalAnnotationsManager kotlinFakeAnnotationsManager = createFakeAnnotationsManager(kotlinAnnotationsRoot);
        final CoreExternalAnnotationsManager ideaFakeAnnotationsManager = createFakeAnnotationsManager(ideaAnnotationsRoot);

        final Set<PsiModifierListOwner> declarationsWithMissingAnnotations = Sets.newLinkedHashSet();

        for (FqName classFqName : getAffectedClasses(ideaAnnotationsRoot)) {
            if (new FqName("org.jdom").equals(classFqName.parent())) continue; // filter unrelated jdom annotations

            PsiClass psiClass = javaPsiFacade.findClass(classFqName.getFqName(), allScope);
            assertNotNull("Class has annotation, but it is not found: " + classFqName, psiClass);

            psiClass.accept(new JavaRecursiveElementVisitor() {
                @Override
                public void visitMethod(PsiMethod method) {
                    super.visitMethod(method);
                    if (method.getReturnType() != null) { // disabled for constructors
                        check(method, method);
                    }
                }

                @Override
                public void visitField(PsiField field) {
                    super.visitField(field);
                    check(field, field);
                }

                @Override
                public void visitParameter(PsiParameter parameter) {
                    super.visitParameter(parameter);
                    PsiMethod method = PsiTreeUtil.getParentOfType(parameter, PsiMethod.class);
                    assert method != null;
                    if (method.getReturnType() != null) { // disabled for constructors
                        check(parameter, method);
                    }
                }

                private void check(@NotNull PsiModifierListOwner ideaOwner, @NotNull PsiModifierListOwner kotlinOwner) {
                    if (hasAnnotation(ideaFakeAnnotationsManager, ideaOwner, AnnotationUtil.NOT_NULL)) {
                        boolean kotlinHasNotNull = hasAnnotation(kotlinFakeAnnotationsManager, kotlinOwner, AnnotationUtil.NOT_NULL);
                        boolean kotlinHasKotlinSignature = hasAnnotation(kotlinFakeAnnotationsManager, kotlinOwner,
                                                                         JvmStdlibNames.KOTLIN_SIGNATURE.getFqName().getFqName());
                        if (kotlinOwner == ideaOwner && kotlinHasNotNull || kotlinHasKotlinSignature) {
                            // good
                        }
                        else {
                            declarationsWithMissingAnnotations.add(kotlinOwner);
                        }
                    }
                }

                private boolean hasAnnotation(
                        @NotNull CoreExternalAnnotationsManager annotationsManager,
                        @NotNull PsiModifierListOwner owner,
                        @NotNull String annotationFqName
                ) {
                    return annotationsManager.findExternalAnnotation(owner, annotationFqName) != null;
                }
            });
        }

        if (!declarationsWithMissingAnnotations.isEmpty()) {
            StringBuilder builder = new StringBuilder("Annotations missing for JDK items:\n");
            for (PsiModifierListOwner declaration : declarationsWithMissingAnnotations) {
                builder.append(PsiFormatUtil.getExternalName(declaration)).append("\n");
            }
            fail(builder.toString());
        }
    }

    private static Iterable<FqName> getAffectedClasses(final VirtualFile root) {
        final Set<FqName> result = Sets.newLinkedHashSet();
        VfsUtilCore.visitChildrenRecursively(root, new VirtualFileVisitor() {
            @Override
            public boolean visitFile(@NotNull VirtualFile file) {
                if (ExternalAnnotationsManager.ANNOTATIONS_XML.equals(file.getName())) {
                    try {
                        String text = StreamUtil.readText(file.getInputStream());
                        Matcher matcher = Pattern.compile("<item name=['\"]([\\w\\d\\.]+)[\\s'\"]").matcher(text);
                        while (matcher.find()) {
                            result.add(new FqName(matcher.group(1)));
                        }
                    }
                    catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
                return true;
            }
        });
        return result;
    }

    private static class AlternativeSignatureErrorFindingVisitor extends DeclarationDescriptorVisitorEmptyBodies<Void, Void> {
        private final BindingContext bindingContext;
        private final Map<DeclarationDescriptor, String> errors;

        public AlternativeSignatureErrorFindingVisitor(BindingContext bindingContext, Map<DeclarationDescriptor, String> errors) {
            this.bindingContext = bindingContext;
            this.errors = errors;
        }

        @Override
        public Void visitNamespaceDescriptor(NamespaceDescriptor descriptor, Void data) {
            return visitDeclarationRecursively(descriptor, descriptor.getMemberScope());
        }

        @Override
        public Void visitClassDescriptor(ClassDescriptor descriptor, Void data) {
            return visitDeclarationRecursively(descriptor, descriptor.getDefaultType().getMemberScope());
        }

        @Override
        public Void visitFunctionDescriptor(FunctionDescriptor descriptor, Void data) {
            return visitDeclaration(descriptor);
        }

        @Override
        public Void visitPropertyDescriptor(PropertyDescriptor descriptor, Void data) {
            return visitDeclaration(descriptor);
        }

        private Void visitDeclaration(@NotNull DeclarationDescriptor descriptor) {
            String error = bindingContext.get(BindingContext.ALTERNATIVE_SIGNATURE_DATA_ERROR, descriptor);
            if (error != null) {
                errors.put(descriptor, error);
            }
            return null;
        }

        private Void visitDeclarationRecursively(@NotNull DeclarationDescriptor descriptor, @NotNull JetScope memberScope) {
            for (DeclarationDescriptor member : memberScope.getAllDescriptors()) {
                member.acceptVoid(this);
            }

            return visitDeclaration(descriptor);
        }
    }
}
