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

package org.jetbrains.jet.jvm.compiler;

import com.google.common.collect.Sets;
import com.intellij.codeInsight.AnnotationUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
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
import org.jetbrains.jet.lang.resolve.java.JvmStdlibNames;
import org.jetbrains.jet.lang.resolve.lazy.KotlinTestWithEnvironment;
import org.jetbrains.jet.lang.resolve.name.FqName;

import java.util.Set;

public class IdeaJdkAnnotationsReflectedTest extends KotlinTestWithEnvironment {
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

    @Override
    protected void tearDown() throws Exception {
        ideaAnnotationsRoot = null;
        kotlinAnnotationsRoot = null;
        super.tearDown();
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

        for (FqName classFqName : JdkAnnotationsValidityTest.getAffectedClasses("jar://ideaSDK/lib/jdkAnnotations.jar!/")) {
            if (new FqName("org.jdom").equals(classFqName.parent())) continue; // filter unrelated jdom annotations
            if (new FqName("java.util.concurrent.TransferQueue").equals(classFqName)) continue; // filter JDK7-specific class

            PsiClass psiClass = javaPsiFacade.findClass(classFqName.asString(), allScope);
            assertNotNull("Class has annotation, but it is not found: " + classFqName, psiClass);

            psiClass.accept(new JavaRecursiveElementVisitor() {
                @Override
                public void visitMethod(PsiMethod method) {
                    super.visitMethod(method);
                    if (method.getReturnType() != null) { // disabled for constructors
                        checkAndReport(method);
                    }
                }

                @Override
                public void visitField(PsiField field) {
                    super.visitField(field);
                    checkAndReport(field);
                }

                @Override
                public void visitParameter(PsiParameter parameter) {
                    super.visitParameter(parameter);
                    PsiMethod method = PsiTreeUtil.getParentOfType(parameter, PsiMethod.class);
                    assert method != null;
                    if (method.getReturnType() != null) { // disabled for constructors
                        if (!check(parameter, method, AnnotationsKind.KOTLIN_SIGNATURE) &&
                                !check(parameter, parameter, AnnotationsKind.NOT_NULL)) {
                            declarationsWithMissingAnnotations.add(parameter);
                        }
                    }
                }

                private void checkAndReport(@NotNull PsiModifierListOwner annotationOwner) {
                    if (!check(annotationOwner, annotationOwner, AnnotationsKind.ANY)) {
                        declarationsWithMissingAnnotations.add(annotationOwner);
                    }
                }

                private boolean check(
                        @NotNull PsiModifierListOwner ideaOwner,
                        @NotNull PsiModifierListOwner kotlinOwner,
                        @NotNull AnnotationsKind annotationsKind
                )
                {
                    return !hasAnnotationInIdea(ideaOwner) || hasAnnotationInKotlin(kotlinOwner, annotationsKind);
                }

                private boolean hasAnnotationInIdea(@NotNull PsiModifierListOwner owner) {
                    return hasAnnotation(ideaFakeAnnotationsManager, owner, AnnotationUtil.NOT_NULL);
                }

                private boolean hasAnnotationInKotlin(@NotNull PsiModifierListOwner owner, @NotNull AnnotationsKind annotationsKind) {
                    for (String name : annotationsKind.annotationNames) {
                        if (hasAnnotation(kotlinFakeAnnotationsManager, owner, name)) {
                            return true;
                        }
                    }
                    return false;
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
                builder.append(PsiFormatUtil.getExternalName(declaration) + " " + declaration.getText()).append("\n");
            }
            fail(builder.toString());
        }
    }

    private enum AnnotationsKind {
        KOTLIN_SIGNATURE(JvmStdlibNames.KOTLIN_SIGNATURE.getFqName().asString()),
        NOT_NULL(AnnotationUtil.NOT_NULL),
        ANY(AnnotationUtil.NOT_NULL, JvmStdlibNames.KOTLIN_SIGNATURE.getFqName().asString());

        public final String[] annotationNames;

        private AnnotationsKind(@NotNull String... annotationNames) {
            this.annotationNames = annotationNames;
        }
    }
}
