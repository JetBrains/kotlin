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
import jet.runtime.typeinfo.JetClass;
import jet.runtime.typeinfo.JetMethod;
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
import org.jetbrains.jet.lang.resolve.java.JvmAbi;
import org.jetbrains.jet.resolve.DescriptorRenderer;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Evgeny Gerashchenko
 * @since 3/2/12
 */
@SuppressWarnings("FieldAccessedSynchronizedAndUnsynchronized")
public class JetDecompiledData {
    private static final String JET_CLASS = JetClass.class.getName();
    private static final String JET_METHOD = JetMethod.class.getName();
    private static final String DECOMPILED_COMMENT = "/* " + PsiBundle.message("psi.decompiled.method.body") + " */";
    private JetFile myJetFile;
    private String myText;
    private Map<ClsElementImpl, JetDeclaration> myClsElementsToJetElements = new HashMap<ClsElementImpl, JetDeclaration>();

    private static final Object LOCK = new String("decompiled data lock");
    private static final Key<JetDecompiledData> USER_DATA_KEY = new Key<JetDecompiledData>("USER_DATA_KEY");
    private final Map<PsiElement,TextRange> myClsMembersToRanges;

    private JetDecompiledData() {
        myClsMembersToRanges = new HashMap<PsiElement, TextRange>();
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
        if (clsFile.getClasses().length != 1) {
            return null;
        }
        PsiClass psiClass = clsFile.getClasses()[0];
        return isKotlinNamespaceClass(psiClass) || isKotlinClass(psiClass) ? clsFile : null;
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

        if (isKotlinNamespaceClass(psiClass)) {
            NamespaceDescriptor nd = jdr.resolveNamespace(packageName);

            if (nd != null) {
                for (DeclarationDescriptor member : nd.getMemberScope().getAllDescriptors()) {
                    if (member instanceof ClassDescriptor || member instanceof NamespaceDescriptor) {
                        continue;
                    }
                    appendDescriptor(builder, member, trace.getBindingContext(), "");
                    builder.append("\n");
                }
            }
        } else {
            ClassDescriptor cd = jdr.resolveClass(psiClass);
            if (cd != null) {
                appendDescriptor(builder, cd, trace.getBindingContext(), "");
            }
        }

        myText = builder.toString();
        myJetFile = JetDummyClassFileViewProvider.createJetFile(psiManager, file, myText);
        for (Map.Entry<PsiElement, TextRange> clsMemberToRange : myClsMembersToRanges.entrySet()) {
            PsiElement clsMember = clsMemberToRange.getKey();
            assert clsMember instanceof ClsElementImpl;

            TextRange range = clsMemberToRange.getValue();
            JetDeclaration jetDeclaration = PsiTreeUtil.findElementOfClassAtRange(myJetFile, range.getStartOffset(), range.getEndOffset(), JetDeclaration.class);
            assert jetDeclaration != null;
            myClsElementsToJetElements.put((ClsElementImpl) clsMember, jetDeclaration);
        }
    }

    private void appendDescriptor(StringBuilder builder, DeclarationDescriptor descriptor, BindingContext bindingContext, String indent) {
        int startOffset = builder.length();
        builder.append(DescriptorRenderer.COMPACT.render(descriptor));
        int endOffset = builder.length();

        if (descriptor instanceof FunctionDescriptor || descriptor instanceof PropertyDescriptor) {
            if (((CallableMemberDescriptor) descriptor).getModality() != Modality.ABSTRACT) {
                if (descriptor instanceof FunctionDescriptor) {
                    builder.append(" { ").append(DECOMPILED_COMMENT).append(" }");
                    endOffset = builder.length();
                } else { // descriptor instanceof PropertyDescriptor
                    if (((PropertyDescriptor) descriptor).getModality() != Modality.ABSTRACT) {
                        builder.append(" ").append(DECOMPILED_COMMENT);
                    }
                }
            }
        } else if (descriptor instanceof ClassDescriptor) {
            builder.append(" {\n");
            ClassDescriptor classDescriptor = (ClassDescriptor) descriptor;
            boolean firstPassed = false;
            String subindent = indent + "    ";
            if (classDescriptor.getClassObjectDescriptor() != null) {
                firstPassed = true;
                builder.append(subindent).append("class ");
                appendDescriptor(builder, classDescriptor.getClassObjectDescriptor(), bindingContext, subindent);
            }
            for (DeclarationDescriptor member : classDescriptor.getDefaultType().getMemberScope().getAllDescriptors()) {
                if (member.getContainingDeclaration() == descriptor) {
                    if (firstPassed) {
                        builder.append("\n");
                    } else {
                        firstPassed = true;
                    }
                    builder.append(subindent);
                    appendDescriptor(builder, member, bindingContext, subindent);
                }
            }
            builder.append(indent).append("}");
            endOffset = builder.length();
        }

        builder.append("\n");
        PsiElement clsMember = bindingContext.get(BindingContext.DESCRIPTOR_TO_DECLARATION, descriptor);
        if (clsMember != null) {
            myClsMembersToRanges.put(clsMember, new TextRange(startOffset, endOffset));
        }
    }

    private static boolean hasAnnotation(PsiModifierListOwner modifierListOwner, String qualifiedName) {
        PsiModifierList modifierList = modifierListOwner.getModifierList();
        if (modifierList != null) {
            for (PsiAnnotation annotation : modifierList.getAnnotations()) {
                if (annotation instanceof ClsAnnotationImpl) {
                    if (qualifiedName.equals(annotation.getQualifiedName())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static boolean isKotlinClass(PsiClass psiClass) {
        return hasAnnotation(psiClass, JET_CLASS);
    }

    private static boolean isKotlinNamespaceClass(PsiClass psiClass) {
        if (JvmAbi.PACKAGE_CLASS.equals(psiClass.getName()) && !isKotlinClass(psiClass)) {
            for (PsiMethod method : psiClass.getMethods()) {
                if (hasAnnotation(method, JET_METHOD)) {
                    return true;
                }
            }
        }
        return false;
    }
}
