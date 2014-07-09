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

package org.jetbrains.jet.lang.resolve;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.resolve.scopes.RedeclarationHandler;

import static org.jetbrains.jet.lang.diagnostics.Errors.REDECLARATION;

public class TraceBasedRedeclarationHandler implements RedeclarationHandler {
    private final BindingTrace trace;

    public TraceBasedRedeclarationHandler(@NotNull BindingTrace trace) {
        this.trace = trace;
    }
    
    @Override
    public void handleRedeclaration(@NotNull DeclarationDescriptor first, @NotNull DeclarationDescriptor second) {
        report(first);
        report(second);
    }

    private void report(DeclarationDescriptor descriptor) {
        PsiElement firstElement = DescriptorToSourceUtils.descriptorToDeclaration(descriptor);
        if (firstElement != null) {
            trace.report(REDECLARATION.on(firstElement, descriptor.getName().asString()));
        }
        else {
            throw new IllegalStateException("No declaration found for " + descriptor);
        }
    }
}
