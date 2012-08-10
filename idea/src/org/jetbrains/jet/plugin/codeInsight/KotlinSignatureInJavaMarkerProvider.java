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

package org.jetbrains.jet.plugin.codeInsight;

import com.intellij.codeHighlighting.Pass;
import com.intellij.codeInsight.daemon.GutterIconNavigationHandler;
import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.codeInsight.daemon.LineMarkerProvider;
import com.intellij.codeInsight.navigation.NavigationUtil;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.util.Function;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.resolve.java.JavaDescriptorResolver;
import org.jetbrains.jet.lang.resolve.java.JvmStdlibNames;
import org.jetbrains.jet.plugin.JetIcons;

import java.awt.event.MouseEvent;
import java.util.Collection;
import java.util.List;

/**
 * @author Evgeny Gerashchenko
 * @since 3 August 2012
 */
public class KotlinSignatureInJavaMarkerProvider implements LineMarkerProvider {
    private static final Function<PsiElement,String> TOOLTIP_PROVIDER = new Function<PsiElement, String>() {
        @Override
        public String fun(PsiElement element) {
            PsiAnnotation annotation = findKotlinSignatureAnnotation(element);
            assert annotation != null;
            String signature = getKotlinSignature(annotation);
            return "Alternative Kotlin signature is available for this method:\n"
                   + signature;
        }
    };

    private static final GutterIconNavigationHandler<PsiMethod> NAVIGATION_HANDLER = new GutterIconNavigationHandler<PsiMethod>() {
        @Override
        public void navigate(MouseEvent e, PsiMethod element) {
            if (e.getClickCount() != 1 || e.getButton() != MouseEvent.BUTTON1) return;

            PsiAnnotation annotation = findKotlinSignatureAnnotation(element);
            assert annotation != null;
            if (annotation.getContainingFile() == element.getContainingFile()) {
                // not external, go to
                for (PsiNameValuePair pair : annotation.getParameterList().getAttributes()) {
                    if (pair.getName() == null || "value".equals(pair.getName())) {
                        PsiAnnotationMemberValue value = pair.getValue();
                        if (value != null) {
                            PsiElement firstChild = value.getFirstChild();
                            VirtualFile virtualFile = value.getContainingFile().getVirtualFile();
                            if (firstChild != null && firstChild.getNode().getElementType() == JavaTokenType.STRING_LITERAL
                                && virtualFile != null) {
                                new OpenFileDescriptor(value.getProject(), virtualFile, value.getTextOffset() + 1).navigate(true);
                            }
                            else {
                                NavigationUtil.activateFileWithPsiElement(value);
                            }
                        }
                    }
                }
            }
            else {
                // TODO check if annotations are editable

                new EditSignatureBalloon(element, getKotlinSignature(annotation)).show(e);
            }
        }
    };

    static final String KOTLIN_SIGNATURE_ANNOTATION = JvmStdlibNames.KOTLIN_SIGNATURE.getFqName().getFqName();

    @Nullable
    private static PsiAnnotation findKotlinSignatureAnnotation(@NotNull PsiElement element) {
        if (!(element instanceof PsiMethod)) return null;
        PsiMethod annotationOwner = element.getOriginalElement() instanceof PsiMethod
                                    ? (PsiMethod) element.getOriginalElement()
                                    : (PsiMethod) element;
        PsiAnnotation annotation =
                JavaDescriptorResolver.findAnnotation(annotationOwner, KOTLIN_SIGNATURE_ANNOTATION);
        if (annotation == null) return null;
        if (annotation.getParameterList().getAttributes().length == 0) return null;
        return annotation;
    }

    @NotNull
    private static String getKotlinSignature(@NotNull PsiAnnotation kotlinSignatureAnnotation) {
        PsiNameValuePair pair = kotlinSignatureAnnotation.getParameterList().getAttributes()[0];
        PsiAnnotationMemberValue value = pair.getValue();
        return value == null ? "null" : value instanceof PsiLiteralExpression
                                        ? ((PsiLiteralExpression) value).getValue().toString()
                                        : value.getText();
    }

    @Override
    @Nullable
    public LineMarkerInfo getLineMarkerInfo(@NotNull PsiElement element) {
        if (findKotlinSignatureAnnotation(element) != null) {
            return new LineMarkerInfo<PsiMethod>((PsiMethod) element, element.getTextOffset(), JetIcons.SMALL_LOGO, Pass.UPDATE_ALL,
                                                 TOOLTIP_PROVIDER, NAVIGATION_HANDLER);
        }
        return null;
    }

    @Override
    public void collectSlowLineMarkers(@NotNull List<PsiElement> elements, @NotNull Collection<LineMarkerInfo> result) {
    }
}
