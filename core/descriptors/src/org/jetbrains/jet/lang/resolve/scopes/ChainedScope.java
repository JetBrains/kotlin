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

package org.jetbrains.jet.lang.resolve.scopes;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.TestOnly;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.resolve.name.LabelName;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.utils.Printer;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

public class ChainedScope implements JetScope {
    private final DeclarationDescriptor containingDeclaration;
    private final String debugName;
    private final JetScope[] scopeChain;
    private Collection<DeclarationDescriptor> allDescriptors;
    private List<ReceiverParameterDescriptor> implicitReceiverHierarchy;

    public ChainedScope(DeclarationDescriptor containingDeclaration, JetScope... scopes) {
        this(containingDeclaration, "Untitled chained scope", scopes);
    }

    public ChainedScope(DeclarationDescriptor containingDeclaration, String debugName, JetScope... scopes) {
        this.containingDeclaration = containingDeclaration;
        scopeChain = scopes.clone();

        this.debugName = debugName;
    }

    @Override
    public ClassifierDescriptor getClassifier(@NotNull Name name) {
        for (JetScope scope : scopeChain) {
            ClassifierDescriptor classifier = scope.getClassifier(name);
            if (classifier != null) return classifier;
        }
        return null;
    }

    @Override
    public PackageViewDescriptor getPackage(@NotNull Name name) {
        for (JetScope jetScope : scopeChain) {
            PackageViewDescriptor aPackage = jetScope.getPackage(name);
            if (aPackage != null) {
                return aPackage;
            }
        }
        return null;
    }

    @NotNull
    @Override
    public Set<VariableDescriptor> getProperties(@NotNull Name name) {
        Set<VariableDescriptor> properties = Sets.newLinkedHashSet();
        for (JetScope jetScope : scopeChain) {
            properties.addAll(jetScope.getProperties(name));
        }
        return properties;
    }

    @Override
    public VariableDescriptor getLocalVariable(@NotNull Name name) {
        for (JetScope jetScope : scopeChain) {
            VariableDescriptor variable = jetScope.getLocalVariable(name);
            if (variable != null) {
                return variable;
            }
        }
        return null;
    }

    @NotNull
    @Override
    public Set<FunctionDescriptor> getFunctions(@NotNull Name name) {
        if (scopeChain.length == 0) {
            return Collections.emptySet();
        }

        Set<FunctionDescriptor> result = Sets.newLinkedHashSet();
        for (JetScope jetScope : scopeChain) {
            result.addAll(jetScope.getFunctions(name));
        }
        return result;
    }

    @NotNull
    @Override
    public List<ReceiverParameterDescriptor> getImplicitReceiversHierarchy() {
        if (implicitReceiverHierarchy == null) {
            implicitReceiverHierarchy = Lists.newArrayList();
            for (JetScope jetScope : scopeChain) {
                implicitReceiverHierarchy.addAll(jetScope.getImplicitReceiversHierarchy());
            }
        }
        return implicitReceiverHierarchy;
    }

    @NotNull
    @Override
    public DeclarationDescriptor getContainingDeclaration() {
        return containingDeclaration;
    }

    @NotNull
    @Override
    public Collection<DeclarationDescriptor> getDeclarationsByLabel(@NotNull LabelName labelName) {
        for (JetScope jetScope : scopeChain) {
            Collection<DeclarationDescriptor> declarationsByLabel = jetScope.getDeclarationsByLabel(labelName);
            if (!declarationsByLabel.isEmpty()) return declarationsByLabel; // TODO : merge?
        }
        return Collections.emptyList();
    }

    @NotNull
    @Override
    public Collection<DeclarationDescriptor> getAllDescriptors() {
        if (allDescriptors == null) {
            allDescriptors = Sets.newHashSet();
            for (JetScope scope : scopeChain) {
                allDescriptors.addAll(scope.getAllDescriptors());
            }
        }
        return allDescriptors;
    }

    @NotNull
    @Override
    public Collection<DeclarationDescriptor> getOwnDeclaredDescriptors() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String toString() {
        return debugName;
    }

    @TestOnly
    @Override
    public void printScopeStructure(@NotNull Printer p) {
        p.println(getClass().getSimpleName(), ": ", debugName, " {");
        p.pushIndent();

        for (JetScope scope : scopeChain) {
            scope.printScopeStructure(p);
        }

        p.popIndent();
        p.println("}");
    }
}
