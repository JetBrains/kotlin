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

package org.jetbrains.jet.plugin.refactoring.safeDelete;

import com.intellij.refactoring.safeDelete.usageInfo.SafeDeleteReferenceSimpleDeleteUsageInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.plugin.project.AnalyzerFacadeWithCache;

public class SafeDeleteImportDirectiveUsageInfo extends SafeDeleteReferenceSimpleDeleteUsageInfo {
    public SafeDeleteImportDirectiveUsageInfo(@NotNull JetImportDirective importDirective, @NotNull JetDeclaration declaration) {
        super(importDirective, declaration, isSafeToDelete(declaration, importDirective));
    }

    private static boolean isSafeToDelete(@NotNull JetDeclaration declaration, @NotNull JetImportDirective importDirective) {
        JetExpression importExpr = importDirective.getImportedReference();

        JetReferenceExpression importReference = null;
        if (importExpr instanceof JetSimpleNameExpression) {
            importReference = (JetReferenceExpression) importExpr;
        }
        else if (importExpr instanceof JetDotQualifiedExpression) {
            JetExpression selector = ((JetDotQualifiedExpression) importExpr).getSelectorExpression();
            if (selector instanceof JetSimpleNameExpression) {
                importReference = (JetReferenceExpression) selector;
            }
        }
        if (importReference == null) return false;

        BindingContext bindingContext =
                AnalyzerFacadeWithCache.analyzeFileWithCache((JetFile) declaration.getContainingFile()).getBindingContext();

        DeclarationDescriptor referenceDescriptor = bindingContext.get(BindingContext.REFERENCE_TARGET, importReference);

        DeclarationDescriptor declarationDescriptor = declaration instanceof JetObjectDeclaration
                ? bindingContext.get(BindingContext.OBJECT_DECLARATION, ((JetObjectDeclaration) declaration).getNameAsDeclaration())
                : bindingContext.get(BindingContext.DECLARATION_TO_DESCRIPTOR, declaration);

        return referenceDescriptor == declarationDescriptor;
    }
}
