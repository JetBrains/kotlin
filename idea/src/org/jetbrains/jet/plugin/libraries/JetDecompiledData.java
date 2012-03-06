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

package org.jetbrains.jet.plugin.libraries;

import com.intellij.ide.highlighter.JavaClassFileType;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.impl.compiled.ClsAnnotationImpl;
import com.intellij.psi.impl.compiled.ClsElementImpl;
import com.intellij.psi.impl.compiled.ClsFileImpl;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.JetSemanticServices;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.psi.JetDeclaration;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.BindingTraceContext;
import org.jetbrains.jet.lang.resolve.java.JavaDescriptorResolver;
import org.jetbrains.jet.lang.resolve.java.JavaSemanticServices;
import org.jetbrains.jet.resolve.DescriptorRenderer;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Evgeny Gerashchenko
 * @since 3/2/12
 */
@SuppressWarnings("FieldAccessedSynchronizedAndUnsynchronized")
public class JetDecompiledData {
    private JetFile myJetFile;
    private String myText;
    private Map<ClsElementImpl, JetDeclaration> myClsElementsToJetElements = new HashMap<ClsElementImpl, JetDeclaration>();

    private static final Object LOCK = new String("decompiled data lock");
    private static final Key<JetDecompiledData> USER_DATA_KEY = new Key<JetDecompiledData>("USER_DATA_KEY");

    private JetDecompiledData() {
    }

    @NotNull
    public JetFile getJetFile() {
        return myJetFile;
    }

    @NotNull
    public String getText() {
        return myText;
    }

    public JetDeclaration getJetDeclarationByClsElement(ClsElementImpl clsElement) {
        return myClsElementsToJetElements.get(clsElement);
    }

    @Nullable
    public static ClsFileImpl getClsFileIfKotlin(@NotNull Project project, @NotNull VirtualFile vFile) {
        if (!FileTypeManager.getInstance().isFileOfType(vFile, JavaClassFileType.INSTANCE)) {
            return null;
        }
        PsiFile psiFile = PsiManager.getInstance(project).findFile(vFile);
        if (!(psiFile instanceof ClsFileImpl)) {
            return null;
        }
        ClsFileImpl clsFile = (ClsFileImpl) psiFile;
        // TODO multiple?
        if (clsFile.getClasses().length != 1) {
            throw new AssertionError("Multiple classes in file: " + Arrays.toString(clsFile.getClasses()));
        }
        PsiClass psiClass = clsFile.getClasses()[0];
        // TODO add better check
        if (psiClass.getName().equals("namespace")) {
            return clsFile;
        }
        PsiModifierList modifierList = psiClass.getModifierList();
        if (modifierList != null) {
            for (PsiAnnotation annotation : modifierList.getAnnotations()) {
                if (annotation instanceof ClsAnnotationImpl) {
                    if (((ClsAnnotationImpl) annotation).getStub().getText().startsWith("@jet.runtime.typeinfo.JetClass")) {
                        return clsFile;
                    }
                }
            }
        }
        return null;
    }

    @NotNull
    public static JetDecompiledData getDecompiledData(@NotNull ClsFileImpl clsFile) {
        synchronized (LOCK) {
            if (clsFile.getUserData(USER_DATA_KEY) == null) {
                clsFile.putUserData(USER_DATA_KEY, decompileData(clsFile));
            }
            JetDecompiledData decompiledData = clsFile.getUserData(USER_DATA_KEY);
            assert decompiledData != null;
            return decompiledData;
        }
    }

    @NotNull
    private static JetDecompiledData decompileData(@NotNull ClsFileImpl clsFile) {
        JetDecompiledData result = new JetDecompiledData();

        Project project = clsFile.getProject();
        result.buildTextAndTree(project, clsFile.getVirtualFile());

        return result;
    }


    private void buildTextAndTree(Project project, VirtualFile file) {
        StringBuilder builder = new StringBuilder();

        BindingTraceContext trace = new BindingTraceContext();
        JavaSemanticServices jss = new JavaSemanticServices(
                project,
                JetSemanticServices.createSemanticServices(project),
                trace);
        PsiManager psiManager = PsiManager.getInstance(project);
        ClsFileImpl clsFile = (ClsFileImpl) psiManager.findFile(file);
        assert clsFile != null;
        builder.append(PsiBundle.message("psi.decompiled.text.header"));
        builder.append("\n\n");

        String packageName = clsFile.getPackageName();
        if (packageName.length() > 0) {
            builder.append("package ").append(packageName).append("\n\n");
        }

        PsiClass psiClass = clsFile.getClasses()[0];
        JavaDescriptorResolver jdr = jss.getDescriptorResolver();

        Map<PsiElement, TextRange> clsMembersToRanges = new HashMap<PsiElement, TextRange>();

        if (psiClass.getName().equals("namespace")) { // TODO better check for namespace
            NamespaceDescriptor nd = jdr.resolveNamespace(packageName);

            if (nd != null) {
                for (DeclarationDescriptor member : nd.getMemberScope().getAllDescriptors()) {
                    if (member instanceof ClassDescriptor && member.getName().equals("namespace") || member instanceof NamespaceDescriptor) {
                        continue;
                    }
                    PsiElement clsElement = trace.getBindingContext().get(BindingContext.DESCRIPTOR_TO_DECLARATION, member);
                    clsMembersToRanges.put(clsElement, appendMemberDescriptor(builder, member));
                }
            }
        } else {
            ClassDescriptor cd = jdr.resolveClass(psiClass);
            if (cd != null) {
                int classStart = builder.length();

                builder.append(DescriptorRenderer.COMPACT.render(cd));

                builder.append(" {\n");

                for (DeclarationDescriptor member : cd.getDefaultType().getMemberScope().getAllDescriptors()) {
                    if (member.getContainingDeclaration() == cd) {
                        builder.append("    ");
                        PsiElement clsElement = trace.getBindingContext().get(BindingContext.DESCRIPTOR_TO_DECLARATION, member);
                        clsMembersToRanges.put(clsElement, appendMemberDescriptor(builder, member));
                    }
                }

                builder.append("}");

                PsiElement clsClass = trace.getBindingContext().get(BindingContext.DESCRIPTOR_TO_DECLARATION, cd);
                clsMembersToRanges.put(clsClass, new TextRange(classStart, builder.length()));
            }
        }

        myText = builder.toString();
        myJetFile = JetDummyClassFileViewProvider.createJetFile(psiManager, file, myText);
        for (Map.Entry<PsiElement, TextRange> clsMemberToRange : clsMembersToRanges.entrySet()) {
            PsiElement clsMember = clsMemberToRange.getKey();
            assert clsMember instanceof ClsElementImpl;

            TextRange range = clsMemberToRange.getValue();
            JetDeclaration jetDeclaration = PsiTreeUtil.findElementOfClassAtRange(myJetFile, range.getStartOffset(), range.getEndOffset(), JetDeclaration.class);
            assert jetDeclaration != null;
            myClsElementsToJetElements.put((ClsElementImpl) clsMember, jetDeclaration);
        }
    }

    private static TextRange appendMemberDescriptor(StringBuilder builder, DeclarationDescriptor member) {
        String decompiledComment = "/* " + PsiBundle.message("psi.decompiled.method.body") + " */";
        int startOffset = builder.length();
        builder.append(DescriptorRenderer.COMPACT.render(member));
        int endOffset = builder.length();
        if (member instanceof FunctionDescriptor) {
            builder.append(" { ").append(decompiledComment).append(" }");
            endOffset = builder.length();
        } else if (member instanceof PropertyDescriptor) {
            builder.append(" ").append(decompiledComment);
        }
        builder.append("\n\n");
        return new TextRange(startOffset, endOffset);
    }
}
