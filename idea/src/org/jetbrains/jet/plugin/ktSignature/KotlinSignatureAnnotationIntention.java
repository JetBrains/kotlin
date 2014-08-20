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

import com.intellij.codeInsight.ExternalAnnotationsListener;
import com.intellij.codeInsight.ExternalAnnotationsManager;
import com.intellij.codeInsight.intention.AddAnnotationFix;
import com.intellij.codeInsight.intention.impl.BaseIntentionAction;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Iconable;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.PsiUtil;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.messages.MessageBusConnection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.ConstructorDescriptor;
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor;
import org.jetbrains.jet.lang.descriptors.VariableDescriptor;
import org.jetbrains.jet.lang.resolve.java.JavaDescriptorResolver;
import org.jetbrains.jet.lang.resolve.java.JavaPackage;
import org.jetbrains.jet.lang.resolve.java.structure.impl.JavaFieldImpl;
import org.jetbrains.jet.lang.resolve.java.structure.impl.JavaMethodImpl;
import org.jetbrains.jet.plugin.JetBundle;
import org.jetbrains.jet.plugin.JetIcons;
import org.jetbrains.jet.plugin.caches.resolve.JavaResolveExtension;
import org.jetbrains.jet.renderer.DescriptorRenderer;
import org.jetbrains.jet.renderer.DescriptorRendererBuilder;

import javax.swing.*;

import static org.jetbrains.jet.lang.resolve.java.JvmAnnotationNames.KOTLIN_SIGNATURE;

public class KotlinSignatureAnnotationIntention extends BaseIntentionAction implements Iconable {
    private static final DescriptorRenderer RENDERER = new DescriptorRendererBuilder()
            .setShortNames(true)
            .setModifiers()
            .setWithDefinedIn(false).build();

    @NotNull
    @Override
    public String getFamilyName() {
        return JetBundle.message("add.kotlin.signature.action.family.name");
    }

    @Override
    public Icon getIcon(@IconFlags int flags) {
        return JetIcons.SMALL_LOGO;
    }

    @Override
    public boolean isAvailable(@NotNull Project project, Editor editor, PsiFile file) {
        PsiModifierListOwner annotationOwner = findAnnotationOwner(file, editor);
        if (annotationOwner == null) {
            return false;
        }

        PsiModifierList modifierList = annotationOwner.getModifierList();
        if (modifierList == null || modifierList.hasExplicitModifier(PsiModifier.PRIVATE)) {
            return false;
        }

        if (!PsiUtil.isLanguageLevel5OrHigher(annotationOwner)) return false;

        if (KotlinSignatureUtil.findKotlinSignatureAnnotation(annotationOwner) != null) {
            if (KotlinSignatureUtil.isAnnotationEditable(annotationOwner)) {
                setText(JetBundle.message("edit.kotlin.signature.action.text"));
            }
            else {
                setText(JetBundle.message("view.kotlin.signature.action.text"));
            }
        }
        else {
            setText(JetBundle.message("add.kotlin.signature.action.text"));
        }
        return true;
    }

    @Override
    public void invoke(@NotNull final Project project, final Editor editor, PsiFile file) throws IncorrectOperationException {
        final PsiMember annotatedElement = findAnnotationOwner(file, editor);

        assert annotatedElement != null;

        if (KotlinSignatureUtil.findKotlinSignatureAnnotation(annotatedElement) != null) {
            EditSignatureAction.invokeEditSignature(annotatedElement, editor, null);
            return;
        }

        String signature = getDefaultSignature(project, annotatedElement);

        final MessageBusConnection busConnection = project.getMessageBus().connect();
        busConnection.subscribe(ExternalAnnotationsManager.TOPIC, new ExternalAnnotationsListener.Adapter() {
            @Override
            public void afterExternalAnnotationChanging(@NotNull PsiModifierListOwner owner, @NotNull String annotationFQName, boolean successful) {
                busConnection.disconnect();

                if (successful && owner == annotatedElement && KOTLIN_SIGNATURE.asString().equals(annotationFQName)) {
                    ApplicationManager.getApplication().invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            KotlinSignatureUtil.refreshMarkers(project);
                            EditSignatureAction.invokeEditSignature(annotatedElement, editor, null);
                        }
                    }, ModalityState.NON_MODAL);
                }
            }
        });

        AddAnnotationFix addAnnotationFix = new AddAnnotationFix(
                KOTLIN_SIGNATURE.asString(), annotatedElement, KotlinSignatureUtil
                .signatureToNameValuePairs(annotatedElement.getProject(), signature));
        addAnnotationFix.invoke(project, editor, file);
    }

    private static String renderMember(PsiMember member) {
        PsiClass containingClass = member.getContainingClass();
        assert containingClass != null;
        String qualifiedName = containingClass.getQualifiedName();
        assert qualifiedName != null;

        return member.getName() + " in " + qualifiedName;
    }

    @NotNull
    private static String getDefaultSignature(@NotNull Project project, @NotNull PsiMember psiMember) {
        JavaDescriptorResolver javaDescriptorResolver = JavaResolveExtension.INSTANCE$.get(project);

        if (psiMember instanceof PsiMethod) {
            PsiMethod psiMethod = (PsiMethod) psiMember;
            FunctionDescriptor functionDescriptor = JavaPackage.resolveMethod(javaDescriptorResolver, new JavaMethodImpl(psiMethod));
            assert functionDescriptor != null: "Couldn't find function descriptor for " + renderMember(psiMember);
            return functionDescriptor instanceof ConstructorDescriptor
                    ? getDefaultConstructorAnnotation((ConstructorDescriptor) functionDescriptor)
                    : RENDERER.render(functionDescriptor);
        }

        if (psiMember instanceof PsiField) {
            VariableDescriptor variableDescriptor = JavaPackage.resolveField(javaDescriptorResolver, new JavaFieldImpl((PsiField) psiMember));
            assert variableDescriptor != null: "Couldn't find variable descriptor for field " + renderMember(psiMember);
            return RENDERER.render(variableDescriptor);
        }

        throw new IllegalStateException("PsiMethod or PsiField are expected");
    }

    private static String getDefaultConstructorAnnotation(ConstructorDescriptor constructorDescriptor) {
        return String.format("fun %s%s", constructorDescriptor.getContainingDeclaration().getName(), RENDERER.renderFunctionParameters(constructorDescriptor));
    }

    @Nullable
    private static PsiMember findAnnotationOwner(@NotNull PsiElement file, Editor editor) {
        int offset = editor.getCaretModel().getOffset();
        PsiMember methodMember = findMethod(file, offset);
        if (methodMember != null) {
            return methodMember;
        }

        return findField(file, offset);
    }

    @Nullable
    private static PsiMethod findMethod(@NotNull PsiElement file, int offset) {
        PsiElement element = file.findElementAt(offset);
        PsiMethod res = PsiTreeUtil.getParentOfType(element, PsiMethod.class);
        if (res == null) return null;

        //Not available in method's body
        PsiCodeBlock body = res.getBody();
        if (body != null) {
            TextRange bodyRange = body.getTextRange();
            if (bodyRange != null && bodyRange.getStartOffset() <= offset) {
                return null;
            }
        }
        return res;
    }

    @Nullable
    private static PsiField findField(@NotNull PsiElement file, int offset) {
        return PsiTreeUtil.getParentOfType(file.findElementAt(offset), PsiField.class);
    }
}
