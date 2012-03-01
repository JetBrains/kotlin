/*
 * Copyright 2000-2012 JetBrains s.r.o.
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

package org.jetbrains.jet.plugin;

import com.intellij.ide.highlighter.JavaClassFileType;
import com.intellij.lang.Language;
import com.intellij.openapi.fileTypes.ContentBasedClassFileProcessor;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.fileTypes.SyntaxHighlighter;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.util.io.StreamUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.impl.PsiManagerImpl;
import com.intellij.psi.impl.compiled.ClassFileStubBuilder;
import com.intellij.psi.impl.compiled.ClsAnnotationImpl;
import com.intellij.psi.impl.compiled.ClsFileImpl;
import com.intellij.psi.impl.java.stubs.ClsStubPsiFactory;
import com.intellij.psi.impl.java.stubs.PsiJavaFileStub;
import com.intellij.psi.impl.java.stubs.impl.PsiJavaFileStubImpl;
import com.intellij.psi.stubs.PsiClassHolderFileStub;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.JetSemanticServices;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.resolve.BindingTraceContext;
import org.jetbrains.jet.lang.resolve.java.JavaDescriptorResolver;
import org.jetbrains.jet.lang.resolve.java.JavaSemanticServices;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.resolve.DescriptorRenderer;

import java.io.IOException;
import java.util.Arrays;

/**
 * @author Evgeny Gerashchenko
 * @since 2/15/12
 */
public class JetContentBasedFileSubstitutor implements ContentBasedClassFileProcessor {

    @Override
    public boolean isApplicable(Project project, VirtualFile vFile) {
        return isKotlinClass(project, vFile);
    }

    @NotNull
    @Override
    public String obtainFileText(Project project, VirtualFile file) {
        StringBuilder builder = new StringBuilder();

        JavaSemanticServices jss = new JavaSemanticServices(
                project,
                JetSemanticServices.createSemanticServices(project),
                new BindingTraceContext());
        PsiJavaFileStub js = getJavaStub(project, file);
        if (js != null) {
            if (js.getPackageName() != null && js.getPackageName().length() > 0) {
                builder.append("package ").append(js.getPackageName()).append("\n\n");
            }

            PsiClass psiClass = js.getClasses()[0];
            JavaDescriptorResolver jdr = jss.getDescriptorResolver();
            ClassDescriptor cd = jdr.resolveClass(psiClass);
            if (cd != null) {
                builder.append(DescriptorRenderer.COMPACT.render(cd));

                builder.append(" {\n");

                JetScope memberScope = cd.getDefaultType().getMemberScope();
                for (DeclarationDescriptor member : memberScope.getAllDescriptors()) {
                    if (member.getContainingDeclaration() == cd) {
                        builder.append("    ").append(DescriptorRenderer.COMPACT.render(member)).append("\n");
                    }
                }

                builder.append("}");
            }
        }
        return builder.toString();
    }

    @Override
    public Language obtainLanguageForFile(VirtualFile file) {
        return null;
    }

    @NotNull
    @Override
    public SyntaxHighlighter createHighlighter(Project project, VirtualFile vFile) {
        return new JetHighlighter();
    }

    private static boolean isKotlinClass(Project project, VirtualFile file) {
        if (!FileTypeManager.getInstance().isFileOfType(file, JavaClassFileType.INSTANCE)) {
            return false;
        }
        PsiJavaFileStub javaStub = getJavaStub(project, file);
        if (javaStub == null) {
            return false;
        }
        // TODO multiple?
        if (javaStub.getClasses().length != 1) {
            throw new AssertionError("Multiple classes in file: " + Arrays.toString(javaStub.getClasses()));
        }
        PsiClass psiClass = javaStub.getClasses()[0];
        // TODO add better check
        if (psiClass.getName().equals("namespace")) {
            return true;
        }
        PsiModifierList modifierList = psiClass.getModifierList();
        if (modifierList != null) {
            for (PsiAnnotation annotation : modifierList.getAnnotations()) {
                if (annotation instanceof ClsAnnotationImpl) {
                    if (((ClsAnnotationImpl) annotation).getStub().getText().startsWith("@jet.runtime.typeinfo.JetClass")) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static PsiJavaFileStub getJavaStub(Project project, VirtualFile file) {
        try {
            byte[] bytes = StreamUtil.loadFromStream(file.getInputStream());
            final PsiJavaFileStubImpl stub = (PsiJavaFileStubImpl) new ClassFileStubBuilder().buildStubTree(file, bytes, project);
            if (stub == null) {
                return null;
            }
            stub.setPsiFactory(new ClsStubPsiFactory()); // TODO is it needed?
            PsiManagerImpl manager = (PsiManagerImpl) PsiManager.getInstance(project);
            ClsFileImpl fakeFile = new ClsFileImpl(manager, new ClassFileViewProvider(manager, file)) {
                @NotNull
                @Override
                public PsiClassHolderFileStub getStub() {
                    return stub;
                }
            };

            fakeFile.setPhysical(false);
            stub.setPsi(fakeFile);
            return stub;
        } catch (IOException e) {
            return null;
        }
    }


}

