/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.resolve;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor;
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor;
import org.jetbrains.kotlin.resolve.scopes.RedeclarationHandler;

import static org.jetbrains.kotlin.diagnostics.Errors.CONFLICTING_OVERLOADS;
import static org.jetbrains.kotlin.diagnostics.Errors.REDECLARATION;

public class TraceBasedRedeclarationHandler implements RedeclarationHandler {
    private final BindingTrace trace;

    public TraceBasedRedeclarationHandler(@NotNull BindingTrace trace) {
        this.trace = trace;
    }
    
    @Override
    public void handleRedeclaration(@NotNull DeclarationDescriptor first, @NotNull DeclarationDescriptor second) {
        reportRedeclaration(first);
        reportRedeclaration(second);
    }

    @Override
    public void handleConflictingOverloads(@NotNull CallableMemberDescriptor first, @NotNull CallableMemberDescriptor second) {
        reportConflictingOverloads(first, second.getContainingDeclaration());
        reportConflictingOverloads(second, first.getContainingDeclaration());
    }

    private void reportConflictingOverloads(CallableMemberDescriptor conflicting, DeclarationDescriptor withContainedIn) {
        PsiElement reportElement = DescriptorToSourceUtils.descriptorToDeclaration(conflicting);
        if (reportElement != null) {
            trace.report(CONFLICTING_OVERLOADS.on(reportElement, conflicting, withContainedIn));
        }
        else {
            throw new IllegalStateException("No declaration found for " + conflicting);
        }
    }

    private void reportRedeclaration(DeclarationDescriptor descriptor) {
        PsiElement firstElement = DescriptorToSourceUtils.descriptorToDeclaration(descriptor);
        if (firstElement != null) {
            trace.report(REDECLARATION.on(firstElement, descriptor.getName().asString()));
        }
        else {
            throw new IllegalStateException("No declaration found for " + descriptor);
        }
    }
}
