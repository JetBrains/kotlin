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

import org.jetbrains.kotlin.diagnostics.DiagnosticFactory0;
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory1;
import org.jetbrains.kotlin.diagnostics.DiagnosticFactory2;
import org.jetbrains.kotlin.diagnostics.Errors;
import org.jetbrains.kotlin.psi.KtElement;
import org.jetbrains.kotlin.psi.KtExpression;
import org.jetbrains.kotlin.types.KotlinType;

import java.util.Collection;

/**
 * Error messages
 */
public interface ComposeErrors {
    DiagnosticFactory0<PsiElement> OPEN_MODEL = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<PsiElement> UNSUPPORTED_MODEL_INHERITANCE = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<KtElement>
            SUSPEND_FUNCTION_USED_AS_SFC = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<PsiElement>
            COMPOSABLE_INVOCATION_IN_NON_COMPOSABLE = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<KtElement>
            INVALID_TYPE_SIGNATURE_SFC = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory0<KtElement>
            NO_COMPOSER_FOUND = DiagnosticFactory0.create(ERROR);
    DiagnosticFactory2<KtElement, KotlinType, String>
            INVALID_COMPOSER_IMPLEMENTATION = DiagnosticFactory2.create(ERROR);
    DiagnosticFactory2<KtExpression, Collection<KotlinType>, Collection<KotlinType>>
            ILLEGAL_ASSIGN_TO_UNIONTYPE = DiagnosticFactory2.create(ERROR);
    DiagnosticFactory0<PsiElement>
            ILLEGAL_TRY_CATCH_AROUND_COMPOSABLE = DiagnosticFactory0.create(ERROR);

    @SuppressWarnings("UnusedDeclaration")
    Object INITIALIZER = new Object() {
        {
            Errors.Initializer.initializeFactoryNames(ComposeErrors.class);
        }
    };

}
