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

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.*;
import com.intellij.psi.impl.compiled.ClsAnnotationImpl;
import com.intellij.psi.impl.compiled.ClsElementImpl;
import com.intellij.psi.impl.compiled.ClsFileImpl;
import com.intellij.psi.util.PsiTreeUtil;
import jet.runtime.typeinfo.JetClass;
import jet.runtime.typeinfo.JetMethod;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.psi.JetDeclaration;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.BindingTraceContext;
import org.jetbrains.jet.lang.resolve.FqName;
import org.jetbrains.jet.lang.resolve.java.JavaDescriptorResolver;
import org.jetbrains.jet.lang.resolve.java.JavaSemanticServices;
import org.jetbrains.jet.lang.resolve.java.JvmAbi;
import org.jetbrains.jet.resolve.DescriptorRenderer;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Evgeny Gerashchenko
 * @since 3/11/12
 */
class DecompiledDataFactory {
    private static final String JET_CLASS = JetClass.class.getName();
    private static final String JET_METHOD = JetMethod.class.getName();
    private static final String DECOMPILED_COMMENT = "/* " + PsiBundle.message("psi.decompiled.method.body") + " */";

    private StringBuilder myBuilder = new StringBuilder();
    private ClsFileImpl myClsFile;
    private BindingContext myBindingContext;
    private final Map<PsiElement, TextRange> myClsMembersToRanges = new HashMap<PsiElement, TextRange>();

    private Map<ClsElementImpl, JetDeclaration> myClsElementsToJetElements = new HashMap<ClsElementImpl, JetDeclaration>();
    private JavaDescriptorResolver myJavaDescriptorResolver;

    private DecompiledDataFactory(ClsFileImpl clsFile) {
        myClsFile = clsFile;
        Project project = myClsFile.getProject();
        JavaSemanticServices jss = new JavaSemanticServices(project, new BindingTraceContext());
        myBindingContext = jss.getTrace().getBindingContext();
        myJavaDescriptorResolver = jss.getDescriptorResolver();
    }

    @NotNull
    static JetDecompiledData createDecompiledData(@NotNull ClsFileImpl clsFile) {
        return new DecompiledDataFactory(clsFile).build();
    }

    private JetDecompiledData build() {
        myBuilder.append(PsiBundle.message("psi.decompiled.text.header"));
        myBuilder.append("\n\n");

        String packageName = myClsFile.getPackageName();
        if (packageName.length() > 0) {
            myBuilder.append("package ").append(packageName).append("\n\n");
        }

        PsiClass psiClass = myClsFile.getClasses()[0];

        if (isKotlinNamespaceClass(psiClass)) {
            NamespaceDescriptor nd = myJavaDescriptorResolver.resolveNamespace(new FqName(packageName));

            if (nd != null) {
                for (DeclarationDescriptor member : nd.getMemberScope().getAllDescriptors()) {
                    if (member instanceof ClassDescriptor || member instanceof NamespaceDescriptor) {
                        continue;
                    }
                    appendDescriptor(member, "");
                    myBuilder.append("\n");
                }
            }
        } else {
            ClassDescriptor cd = myJavaDescriptorResolver.resolveClass(psiClass);
            if (cd != null) {
                appendDescriptor(cd, "");
            }
        }

        JetFile jetFile = JetDummyClassFileViewProvider.createJetFile(myClsFile.getManager(), myClsFile.getVirtualFile(), myBuilder.toString());
        for (Map.Entry<PsiElement, TextRange> clsMemberToRange : myClsMembersToRanges.entrySet()) {
            PsiElement clsMember = clsMemberToRange.getKey();
            assert clsMember instanceof ClsElementImpl;

            TextRange range = clsMemberToRange.getValue();
            JetDeclaration jetDeclaration = PsiTreeUtil.findElementOfClassAtRange(jetFile, range.getStartOffset(), range.getEndOffset(), JetDeclaration.class);
            assert jetDeclaration != null;
            myClsElementsToJetElements.put((ClsElementImpl) clsMember, jetDeclaration);
        }

        return new JetDecompiledData(jetFile, myClsElementsToJetElements);
    }

    private void appendDescriptor(DeclarationDescriptor descriptor, String indent) {
        int startOffset = myBuilder.length();
        myBuilder.append(DescriptorRenderer.COMPACT.render(descriptor));
        int endOffset = myBuilder.length();

        if (descriptor instanceof FunctionDescriptor || descriptor instanceof PropertyDescriptor) {
            if (((CallableMemberDescriptor) descriptor).getModality() != Modality.ABSTRACT) {
                if (descriptor instanceof FunctionDescriptor) {
                    myBuilder.append(" { ").append(DECOMPILED_COMMENT).append(" }");
                    endOffset = myBuilder.length();
                } else { // descriptor instanceof PropertyDescriptor
                    if (((PropertyDescriptor) descriptor).getModality() != Modality.ABSTRACT) {
                        myBuilder.append(" ").append(DECOMPILED_COMMENT);
                    }
                }
            }
        } else if (descriptor instanceof ClassDescriptor) {
            myBuilder.append(" {\n");
            ClassDescriptor classDescriptor = (ClassDescriptor) descriptor;
            boolean firstPassed = false;
            String subindent = indent + "    ";
            if (classDescriptor.getClassObjectDescriptor() != null) {
                firstPassed = true;
                myBuilder.append(subindent).append("class ");
                appendDescriptor(classDescriptor.getClassObjectDescriptor(), subindent);
            }
            for (DeclarationDescriptor member : classDescriptor.getDefaultType().getMemberScope().getAllDescriptors()) {
                if (member.getContainingDeclaration() == descriptor) {
                    if (firstPassed) {
                        myBuilder.append("\n");
                    } else {
                        firstPassed = true;
                    }
                    myBuilder.append(subindent);
                    appendDescriptor(member, subindent);
                }
            }
            myBuilder.append(indent).append("}");
            endOffset = myBuilder.length();
        }

        myBuilder.append("\n");
        PsiElement clsMember = myBindingContext.get(BindingContext.DESCRIPTOR_TO_DECLARATION, descriptor);
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

    static boolean isKotlinClass(PsiClass psiClass) {
        return hasAnnotation(psiClass, JET_CLASS);
    }

    static boolean isKotlinNamespaceClass(PsiClass psiClass) {
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
