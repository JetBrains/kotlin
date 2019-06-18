/*
 * Copyright 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package androidx.compose.plugins.kotlin.analysis;

import static org.jetbrains.kotlin.diagnostics.Severity.ERROR;
import static org.jetbrains.kotlin.diagnostics.Severity.WARNING;

import com.intellij.psi.PsiElement;

import org.jetbrains.kotlin.descriptors.DeclarationDescriptor;
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor;
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory0;
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory1;
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory2;
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory3;
import org.jetbrains.kotlin.diagnostics.Errors;
import org.jetbrains.kotlin.psi.KtElement;
import org.jetbrains.kotlin.psi.KtExpression;
import org.jetbrains.kotlin.psi.KtReferenceExpression;
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall;
import org.jetbrains.kotlin.types.KotlinType;

import java.util.Collection;

/**
 * Error messages
 */
public interface ComposeErrors {
    DiagnosticFactory0<PsiElement> DUPLICATE_ATTRIBUTE = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<PsiElement> OPEN_COMPONENT = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<PsiElement> OPEN_MODEL = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<PsiElement> UNSUPPORTED_MODEL_INHERITANCE = DiagnosticFactory0.create(ERROR);

    DiagnosticFactory3<KtElement, Collection<DeclarationDescriptor>, String, KotlinType>
            UNRESOLVED_ATTRIBUTE_KEY = DiagnosticFactory3.create(ERROR);
    DiagnosticFactory2<KtElement, Collection<DeclarationDescriptor>, String>
            UNRESOLVED_ATTRIBUTE_KEY_UNKNOWN_TYPE = DiagnosticFactory2.create(ERROR);
    DiagnosticFactory2<KtElement, KotlinType, Collection<KotlinType>>
            MISMATCHED_ATTRIBUTE_TYPE = DiagnosticFactory2.create(ERROR);
    DiagnosticFactory2<KtElement, KotlinType, Collection<KotlinType>>
            MISMATCHED_INFERRED_ATTRIBUTE_TYPE = DiagnosticFactory2.create(ERROR);
    DiagnosticFactory1<KtElement, Collection<KotlinType>>
            UNRESOLVED_CHILDREN = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory1<KtElement, SimpleFunctionDescriptor>
            MISMATCHED_ATTRIBUTE_TYPE_NO_SINGLE_PARAM_SETTER_FNS = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory1<KtElement, Collection<DeclarationDescriptor>>
            MISSING_REQUIRED_ATTRIBUTES = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory2<KtElement, KotlinType, Collection<KotlinType>>
            INVALID_TAG_TYPE = DiagnosticFactory2.create(ERROR);
    DiagnosticFactory0<KtElement>
            CALLABLE_RECURSION_DETECTED = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory1<KtElement, Collection<String>>
            AMBIGUOUS_ATTRIBUTES_DETECTED = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory1<KtElement, Collection<KotlinType>>
            INVALID_TAG_DESCRIPTOR = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory1<KtElement, String>
            SVC_INVOCATION = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory2<KtElement, String, DeclarationDescriptor>
            NON_COMPOSABLE_INVOCATION = DiagnosticFactory2.create(WARNING);
    DiagnosticFactory0<KtElement>
            SUSPEND_FUNCTION_USED_AS_SFC = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<PsiElement>
            KTX_IN_NON_COMPOSABLE = DiagnosticFactory0.create(WARNING);
    DiagnosticFactory0<PsiElement>
            COMPOSABLE_INVOCATION_IN_NON_COMPOSABLE = DiagnosticFactory0.create(WARNING);
    DiagnosticFactory0<KtElement>
            INVALID_TYPE_SIGNATURE_SFC = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory1<KtElement, KtReferenceExpression>
            UNRESOLVED_TAG = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory0<KtElement>
            NO_COMPOSER_FOUND = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory2<KtElement, KotlinType, String>
            INVALID_COMPOSER_IMPLEMENTATION = DiagnosticFactory2.create(ERROR);
    DiagnosticFactory1<KtElement, Collection<ResolvedCall<?>>>
            AMBIGUOUS_KTX_CALL = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory1<KtElement, String>
            CHILDREN_ATTR_USED_AS_BODY_AND_KEYED_ATTRIBUTE = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory0<KtElement>
            CHILDREN_PROVIDED_BUT_NO_CHILDREN_DECLARED = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory1<KtElement, KotlinType>
            MISSING_REQUIRED_CHILDREN = DiagnosticFactory1.create(ERROR);
    DiagnosticFactory2<KtExpression, Collection<KotlinType>, Collection<KotlinType>>
            ILLEGAL_ASSIGN_TO_UNIONTYPE = DiagnosticFactory2.create(ERROR);

    @SuppressWarnings("UnusedDeclaration")
    Object INITIALIZER = new Object() {
        {
            Errors.Initializer.initializeFactoryNames(ComposeErrors.class);
        }
    };

}
