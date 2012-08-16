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

import com.intellij.codeInsight.intention.AddAnnotationFix;
import com.intellij.codeInsight.intention.impl.BaseIntentionAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiCodeBlock;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.di.InjectorForJavaSemanticServices;
import org.jetbrains.jet.lang.BuiltinsScopeExtensionMode;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.SimpleFunctionDescriptor;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.java.JavaDescriptorResolver;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.plugin.JetBundle;
import org.jetbrains.jet.resolve.DescriptorRenderer;

/**
 * @author Evgeny Gerashchenko
 * @since 16 Aug 2012
 */
public class AddKotlinSignatureAnnotation extends BaseIntentionAction {
    private static final DescriptorRenderer RENDERER = new DescriptorRenderer() {
        @Override
        protected boolean shouldRenderDefinedIn() {
            return false;
        }

        @Override
        public String renderType(JetType type) {
            return renderTypeWithShortNames(type);
        }

        @Override
        protected boolean shouldRenderModifiers() {
            return false;
        }
    };

    public AddKotlinSignatureAnnotation() {
        setText(JetBundle.message("add.kotlin.signature.action.text"));
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return JetBundle.message("add.kotlin.signature.action.family.name");
    }

    @Override
    public boolean isAvailable(@NotNull Project project, Editor editor, PsiFile file) {
        PsiMethod method = findMethod(file, editor.getCaretModel().getOffset());
        if (method == null) return false;
        if (KotlinSignatureInJavaMarkerProvider.findKotlinSignatureAnnotation(method) != null) return false;
        return createFix(method, "").isAvailable(project, editor, file);
    }

    @Override
    public void invoke(@NotNull Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
        PsiMethod method = (PsiMethod) findMethod(file, editor.getCaretModel().getOffset()).getOriginalElement();
        String signature = getDefaultSignature(project, method);
        if (signature == null) {
            return;
        }
        createFix(method, signature).invoke(project, editor, file);
        KotlinSignatureInJavaMarkerProvider.refresh(project);
    }

    @NotNull
    private static AddAnnotationFix createFix(@NotNull PsiMethod method, @NotNull String signature) {
        return new AddAnnotationFix(KotlinSignatureInJavaMarkerProvider.KOTLIN_SIGNATURE_ANNOTATION, method,
                                    EditSignatureBalloon.signatureToNameValuePairs(method.getProject(), signature));
    }

    private static String getDefaultSignature(@NotNull Project project, @NotNull PsiMethod method) {
        InjectorForJavaSemanticServices injector = new InjectorForJavaSemanticServices(BuiltinsScopeExtensionMode.ALL, project);
        JavaDescriptorResolver javaDescriptorResolver = injector.getJavaDescriptorResolver();
        FqName classFqName = new FqName(method.getContainingClass().getQualifiedName());
        ClassDescriptor classDescriptor = javaDescriptorResolver.resolveClass(classFqName);
        if (classDescriptor == null) return null;
        classDescriptor.getDefaultType().getMemberScope().getFunctions(Name.identifier(method.getName()));
        SimpleFunctionDescriptor functionDescriptor = injector.getBindingTrace().getBindingContext().get(BindingContext.FUNCTION, method);
        assert functionDescriptor != null: "Couldn't find function descriptor for " + method.getName() + " in " + classFqName;
        return RENDERER.render(functionDescriptor);
    }

    private static PsiMethod findMethod(PsiFile file, int offset) {
        PsiElement element = file.findElementAt(offset);
        PsiMethod res = PsiTreeUtil.getParentOfType(element, PsiMethod.class);
        if (res == null) return null;

        //Not available in method's body
        PsiCodeBlock body = res.getBody();
        if (body == null) return null;
        TextRange textRange = body.getTextRange();
        if (textRange == null || textRange.getStartOffset() <= offset) return null;

        return res;
    }
}
