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

import com.intellij.codeInsight.ExternalAnnotationsManager;
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.*;
import com.intellij.psi.impl.compiled.ClsElementImpl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.resolve.java.resolver.PsiBasedExternalAnnotationResolver;

import static org.jetbrains.jet.lang.resolve.java.JvmAnnotationNames.KOTLIN_SIGNATURE;
import static org.jetbrains.jet.lang.resolve.java.JvmAnnotationNames.OLD_KOTLIN_SIGNATURE;

class KotlinSignatureUtil {
    private KotlinSignatureUtil() {
    }

    @NotNull
    static PsiModifierListOwner getAnnotationOwner(@NotNull PsiElement element) {
        PsiModifierListOwner annotationOwner = element.getOriginalElement() instanceof PsiModifierListOwner
                                               ? (PsiModifierListOwner) element.getOriginalElement()
                                               : (PsiModifierListOwner) element;
        if (!annotationOwner.isPhysical()) {
            // this is fake PsiFile which is mirror for ClsFile without sources
            PsiCompiledElement compiledElement = element.getUserData(ClsElementImpl.COMPILED_ELEMENT);
            if (compiledElement instanceof PsiModifierListOwner) {
                return (PsiModifierListOwner) compiledElement;
            }
        }
        return annotationOwner;
    }

    @NotNull
    static String getKotlinSignature(@NotNull PsiAnnotation kotlinSignatureAnnotation) {
        PsiNameValuePair pair = kotlinSignatureAnnotation.getParameterList().getAttributes()[0];
        PsiAnnotationMemberValue value = pair.getValue();
        if (value == null) {
            return "null";
        }
        else if (value instanceof PsiLiteralExpression) {
            Object valueObject = ((PsiLiteralExpression) value).getValue();
            return valueObject == null ? "null" : StringUtil.unescapeStringCharacters(valueObject.toString());
        }
        else {
            return value.getText();
        }
    }

    @NotNull
    static PsiNameValuePair[] signatureToNameValuePairs(@NotNull Project project, @NotNull String signature) {
        return JavaPsiFacade.getElementFactory(project).createAnnotationFromText(
                "@" + KOTLIN_SIGNATURE + "(value=\"" + StringUtil.escapeStringCharacters(signature) + "\")", null)
                .getParameterList().getAttributes();
    }

    @Nullable
    static PsiAnnotation findKotlinSignatureAnnotation(@NotNull PsiElement element) {
        if (!(element instanceof PsiModifierListOwner)) return null;
        PsiModifierListOwner annotationOwner = getAnnotationOwner(element);
        PsiModifierList list = annotationOwner.getModifierList();

        PsiAnnotation annotation = list == null ? null : list.findAnnotation(KOTLIN_SIGNATURE.asString());
        if (annotation == null) {
            annotation = PsiBasedExternalAnnotationResolver.findExternalAnnotation(annotationOwner, KOTLIN_SIGNATURE);
            if (annotation == null) {
                annotation = list == null ? null : list.findAnnotation(OLD_KOTLIN_SIGNATURE.asString());
                if (annotation == null) {
                    annotation = PsiBasedExternalAnnotationResolver.findExternalAnnotation(annotationOwner, OLD_KOTLIN_SIGNATURE);
                }
            }
        }

        if (annotation == null) return null;
        if (annotation.getParameterList().getAttributes().length == 0) return null;
        return annotation;
    }

    static void refreshMarkers(@NotNull Project project) {
        DaemonCodeAnalyzer.getInstance(project).restart();
    }

    static boolean isAnnotationEditable(@NotNull PsiElement element) {
        PsiModifierListOwner annotationOwner = getAnnotationOwner(element);
        PsiAnnotation annotation = findKotlinSignatureAnnotation(element);
        assert annotation != null;
        if (annotation.getContainingFile() == annotationOwner.getContainingFile()) {
            return annotation.isWritable();
        } else {
            ExternalAnnotationsManager annotationsManager = ExternalAnnotationsManager.getInstance(element.getProject());
            //noinspection ConstantConditions
            return annotationsManager.isExternalAnnotationWritable(annotationOwner, annotation.getQualifiedName());
        }
    }
}
