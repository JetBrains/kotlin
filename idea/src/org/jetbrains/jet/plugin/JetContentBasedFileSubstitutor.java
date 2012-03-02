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
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.impl.compiled.ClsAnnotationImpl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.JetSemanticServices;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.resolve.BindingTraceContext;
import org.jetbrains.jet.lang.resolve.java.JavaDescriptorResolver;
import org.jetbrains.jet.lang.resolve.java.JavaSemanticServices;
import org.jetbrains.jet.resolve.DescriptorRenderer;

import java.util.Arrays;

/**
 * @author Evgeny Gerashchenko
 * @since 2/15/12
 */
public class JetContentBasedFileSubstitutor implements ContentBasedClassFileProcessor {

    @Override
    public boolean isApplicable(Project project, VirtualFile vFile) {
        if (!FileTypeManager.getInstance().isFileOfType(vFile, JavaClassFileType.INSTANCE)) {
            return false;
        }
        PsiClassOwner clsFile = (PsiClassOwner) PsiManager.getInstance(project).findFile(vFile);
        if (clsFile == null) {
            return false;
        }
        // TODO multiple?
        if (clsFile.getClasses().length != 1) {
            throw new AssertionError("Multiple classes in file: " + Arrays.toString(clsFile.getClasses()));
        }
        PsiClass psiClass = clsFile.getClasses()[0];
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

    @NotNull
    @Override
    public String obtainFileText(Project project, VirtualFile file) {
        StringBuilder builder = new StringBuilder();

        JavaSemanticServices jss = new JavaSemanticServices(
                project,
                JetSemanticServices.createSemanticServices(project),
                new BindingTraceContext());
        PsiClassOwner clsFile = (PsiClassOwner) PsiManager.getInstance(project).findFile(file);
        if (clsFile != null) {
            builder.append(PsiBundle.message("psi.decompiled.text.header"));
            builder.append("\n\n");

            String packageName = clsFile.getPackageName();
            if (packageName == null) {
                packageName = "";
            }
            if (packageName.length() > 0) {
                builder.append("package ").append(packageName).append("\n\n");
            }

            PsiClass psiClass = clsFile.getClasses()[0];
            JavaDescriptorResolver jdr = jss.getDescriptorResolver();

            if (psiClass.getName().equals("namespace")) { // TODO better check for namespace
                NamespaceDescriptor nd = jdr.resolveNamespace(packageName);

                if (nd != null) {
                    for (DeclarationDescriptor member : nd.getMemberScope().getAllDescriptors()) {
                        if (member instanceof ClassDescriptor && member.getName().equals("namespace") || member instanceof NamespaceDescriptor) {
                            continue;
                        }
                        appendMemberDescriptor(builder, member);
                    }
                }
            } else {
                ClassDescriptor cd = jdr.resolveClass(psiClass);
                if (cd != null) {
                    builder.append(DescriptorRenderer.COMPACT.render(cd));

                    builder.append(" {\n");

                    for (DeclarationDescriptor member : cd.getDefaultType().getMemberScope().getAllDescriptors()) {
                        if (member.getContainingDeclaration() == cd) {
                            builder.append("    ");
                            appendMemberDescriptor(builder, member);
                        }
                    }

                    builder.append("}");
                }
            }
        }
        return builder.toString();
    }

    private static void appendMemberDescriptor(StringBuilder builder, DeclarationDescriptor member) {
        String decompiledComment = "/* " + PsiBundle.message("psi.decompiled.method.body") + " */";
        builder.append(DescriptorRenderer.COMPACT.render(member));
        if (member instanceof FunctionDescriptor) {
            builder.append(" { ").append(decompiledComment).append(" }");
        } else if (member instanceof PropertyDescriptor) {
            builder.append(" ").append(decompiledComment);
        }
        builder.append("\n\n");
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

    @NotNull
    @Override
    public PsiFile getDecompiledPsiFile(PsiFile psiFile) {
        Project project = psiFile.getProject();
        String text = obtainFileText(project, psiFile.getVirtualFile());
        return PsiFileFactory.getInstance(project).createFileFromText("", JetLanguage.INSTANCE, text);
    }
}

