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

package org.jetbrains.jet.lang.resolve.java;

import com.intellij.psi.PsiClass;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.ClassOrNamespaceDescriptor;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor;
import org.jetbrains.jet.lang.descriptors.VariableDescriptor;
import org.jetbrains.jet.lang.resolve.scopes.JetScopeImpl;

import java.util.Collections;
import java.util.Set;

/**
 * @author Stepan Koltsov
 */
public abstract class JavaClassOrPackageScope extends JetScopeImpl {

    @NotNull
    protected final ClassOrNamespaceDescriptor descriptor;
    @NotNull
    protected final JavaSemanticServices semanticServices;
    @Nullable
    protected final PsiClass psiClass;

    protected JavaClassOrPackageScope(@NotNull ClassOrNamespaceDescriptor descriptor, @NotNull JavaSemanticServices semanticServices,
                                      @Nullable PsiClass psiClass) {
        this.descriptor = descriptor;
        this.semanticServices = semanticServices;
        this.psiClass = psiClass;

        JavaDescriptorResolver.checkPsiClassIsNotJet(psiClass);
    }

    @NotNull
    @Override
    public DeclarationDescriptor getContainingDeclaration() {
        return descriptor;
    }

    protected abstract boolean staticMembers();

    @NotNull
    @Override
    public Set<VariableDescriptor> getProperties(@NotNull String name) {
        if (psiClass == null) {
            return Collections.emptySet();
        }

        // TODO: cache
        return semanticServices.getDescriptorResolver().resolveFieldGroupByName(
                descriptor, psiClass, name, staticMembers());
    }

    @NotNull
    @Override
    public Set<FunctionDescriptor> getFunctions(@NotNull String name) {

        if (psiClass == null) {
            return Collections.emptySet();
        }

        return semanticServices.getDescriptorResolver().resolveFunctionGroup(descriptor, psiClass, name, staticMembers());
    }

}
