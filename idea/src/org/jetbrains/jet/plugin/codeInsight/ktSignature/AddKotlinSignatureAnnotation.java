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

package org.jetbrains.jet.plugin.codeInsight.ktSignature;

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
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.messages.MessageBusConnection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.di.InjectorForJavaSemanticServices;
import org.jetbrains.jet.lang.BuiltinsScopeExtensionMode;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.ConstructorDescriptor;
import org.jetbrains.jet.lang.descriptors.NamespaceDescriptor;
import org.jetbrains.jet.lang.descriptors.SimpleFunctionDescriptor;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.java.JavaDescriptorResolver;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.plugin.JetBundle;
import org.jetbrains.jet.plugin.JetIcons;
import org.jetbrains.jet.resolve.DescriptorRenderer;

import javax.swing.*;

import static org.jetbrains.jet.plugin.codeInsight.ktSignature.KotlinSignatureUtil.*;

/**
 * @author Evgeny Gerashchenko
 * @since 16 Aug 2012
 */
public class AddKotlinSignatureAnnotation extends BaseIntentionAction implements Iconable {
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
    public Icon getIcon(@IconFlags int flags) {
        return JetIcons.SMALL_LOGO;
    }

    @Override
    public boolean isAvailable(@NotNull Project project, Editor editor, PsiFile file) {
        PsiMethod method = findMethod(file, editor.getCaretModel().getOffset());
        if (method != null) {
            if (findKotlinSignatureAnnotation(method) != null) return false;
            if (method.getModifierList().hasExplicitModifier(PsiModifier.PRIVATE)) return false;
            return createFix(method, "").isAvailable(project, editor, file);
        }

        PsiField field = findField(file, editor.getCaretModel().getOffset());
        if (field != null) {
            if (findKotlinSignatureAnnotation(field) != null) return false;
            PsiModifierList modifierList = field.getModifierList();
            if (modifierList == null || modifierList.hasExplicitModifier(PsiModifier.PRIVATE)) return false;
            return createFix(field, "").isAvailable(project, editor, file);
        }

        return false;
    }

    @Override
    public void invoke(@NotNull final Project project, final Editor editor, PsiFile file) throws IncorrectOperationException {
        PsiMethod method = findMethod(file, editor.getCaretModel().getOffset());
        PsiField field = findField(file, editor.getCaretModel().getOffset());

        assert (method != null || field != null);

        String signature = method != null ?
                           getDefaultSignature(project, (PsiMethod) method.getOriginalElement()) :
                           getDefaultSignature(project, (PsiField) field.getOriginalElement());

        final PsiModifierListOwner annotatedElement = method != null ? method : field;

        final MessageBusConnection busConnection = project.getMessageBus().connect();
        busConnection.subscribe(ExternalAnnotationsManager.TOPIC, new ExternalAnnotationsListener.Adapter() {
            @Override
            public void afterExternalAnnotationChanging(@NotNull PsiModifierListOwner owner, @NotNull String annotationFQName,
                    boolean successful) {
                busConnection.disconnect();

                if (successful && owner == annotatedElement && KOTLIN_SIGNATURE_ANNOTATION.equals(annotationFQName)) {
                    ApplicationManager.getApplication().invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            refreshMarkers(project);
                            EditSignatureAction.invokeEditSignature(annotatedElement, editor, null);
                        }
                    }, ModalityState.NON_MODAL);
                }
            }
        });
        createFix(annotatedElement, signature).invoke(project, editor, file);
    }

    @NotNull
    private static AddAnnotationFix createFix(@NotNull PsiModifierListOwner annotatedElement, @NotNull String signature) {
        return new AddAnnotationFix(KOTLIN_SIGNATURE_ANNOTATION, annotatedElement, signatureToNameValuePairs(annotatedElement.getProject(), signature));
    }

    @NotNull
    private static String getDefaultSignature(@NotNull Project project, @NotNull PsiMethod method) {
        InjectorForJavaSemanticServices injector = new InjectorForJavaSemanticServices(BuiltinsScopeExtensionMode.ALL, project);
        JavaDescriptorResolver javaDescriptorResolver = injector.getJavaDescriptorResolver();

        PsiClass containingClass = method.getContainingClass();
        assert containingClass != null;
        String qualifiedName = containingClass.getQualifiedName();
        assert qualifiedName != null;
        FqName classFqName = new FqName(qualifiedName);

        if (method.getReturnType() == null) {
            // For constructor
            ClassDescriptor classDescriptor = javaDescriptorResolver.resolveClass(classFqName);
            assert classDescriptor != null: "Couldn't resolve class descriptor for " + classFqName;
            classDescriptor.getConstructors();

            ConstructorDescriptor constructorDescriptor = injector.getBindingTrace().getBindingContext().get(BindingContext.CONSTRUCTOR, method);
            assert constructorDescriptor != null: "Couldn't find constructor descriptor for " + method.getName() + " in " + classFqName;
            return getDefaultConstructorAnnotation(constructorDescriptor, classFqName);
        }

        if (method.getModifierList().hasModifierProperty(PsiModifier.STATIC)) {
            NamespaceDescriptor namespaceDescriptor = javaDescriptorResolver.resolveNamespace(classFqName);
            assert namespaceDescriptor != null: "Couldn't resolve namespace descriptor for " + classFqName;
            namespaceDescriptor.getMemberScope().getFunctions(Name.identifier(method.getName()));
        }
        else {
            ClassDescriptor classDescriptor = javaDescriptorResolver.resolveClass(classFqName);
            assert classDescriptor != null: "Couldn't resolve class descriptor for " + classFqName;
            classDescriptor.getDefaultType().getMemberScope().getFunctions(Name.identifier(method.getName()));
        }

        SimpleFunctionDescriptor functionDescriptor = injector.getBindingTrace().getBindingContext().get(BindingContext.FUNCTION, method);
        assert functionDescriptor != null: "Couldn't find function descriptor for " + method.getName() + " in " + classFqName;
        return RENDERER.render(functionDescriptor);
    }

    private static String getDefaultConstructorAnnotation(ConstructorDescriptor constructorDescriptor, FqName classFqName) {
        return String.format("fun %s%s", classFqName.shortName(), RENDERER.renderFunctionParameters(constructorDescriptor));
    }

    @NotNull
    private static String getDefaultSignature(@NotNull Project project, @NotNull PsiField field) {
        InjectorForJavaSemanticServices injector = new InjectorForJavaSemanticServices(BuiltinsScopeExtensionMode.ALL, project);
        JavaDescriptorResolver javaDescriptorResolver = injector.getJavaDescriptorResolver();

        PsiClass containingClass = field.getContainingClass();
        assert containingClass != null;
        String qualifiedName = containingClass.getQualifiedName();
        assert qualifiedName != null;
        FqName classFqName = new FqName(qualifiedName);

        PsiModifierList modifierList = field.getModifierList();
        if (modifierList != null && modifierList.hasModifierProperty(PsiModifier.STATIC)) {
            NamespaceDescriptor namespaceDescriptor = javaDescriptorResolver.resolveNamespace(classFqName);
            assert namespaceDescriptor != null: "Couldn't resolve namespace descriptor for " + classFqName;
            namespaceDescriptor.getMemberScope().getProperties(Name.identifier(field.getName()));
        }
        else {
            ClassDescriptor classDescriptor = javaDescriptorResolver.resolveClass(classFqName);
            assert classDescriptor != null: "Couldn't resolve class descriptor for " + classFqName;
            classDescriptor.getDefaultType().getMemberScope().getProperties(Name.identifier(field.getName()));
        }

        // TODO: Generate default string for the field
        return "";
    }

    @Nullable
    private static PsiMethod findMethod(@NotNull PsiFile file, int offset) {
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
    private static PsiField findField(@NotNull PsiFile file, int offset) {
        return PsiTreeUtil.getParentOfType(file.findElementAt(offset), PsiField.class);
    }
}
