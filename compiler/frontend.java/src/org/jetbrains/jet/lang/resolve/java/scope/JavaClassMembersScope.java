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

package org.jetbrains.jet.lang.resolve.java.scope;

import com.google.common.collect.Maps;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiModifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.ClassOrNamespaceDescriptor;
import org.jetbrains.jet.lang.descriptors.ClassifierDescriptor;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.resolve.java.DescriptorSearchRule;
import org.jetbrains.jet.lang.resolve.java.JavaSemanticServices;
import org.jetbrains.jet.lang.resolve.java.provider.ClassPsiDeclarationProvider;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.LabelName;
import org.jetbrains.jet.lang.resolve.name.Name;

import java.util.Collection;
import java.util.Map;

/**
 * Class static of instance members.
 *
 * @author abreslav
 */
public class JavaClassMembersScope extends JavaBaseScope {
    @NotNull
    private final Map<Name, ClassifierDescriptor> classifiers = Maps.newHashMap();
    @NotNull
    private final ClassPsiDeclarationProvider classPsiDeclarationProvider;

    public JavaClassMembersScope(
            @NotNull ClassOrNamespaceDescriptor descriptor,
            @NotNull JavaSemanticServices semanticServices,
            @NotNull ClassPsiDeclarationProvider resolverScopeData) {
        super(descriptor, semanticServices, resolverScopeData);
        this.classPsiDeclarationProvider = resolverScopeData;
    }

    @NotNull
    @Override
    public Collection<DeclarationDescriptor> getDeclarationsByLabel(LabelName labelName) {
        throw new UnsupportedOperationException(); // TODO
    }

    @Override
    public ClassifierDescriptor getClassifier(@NotNull Name name) {
        ClassifierDescriptor classifierDescriptor = classifiers.get(name);
        if (classifierDescriptor == null) {
            classifierDescriptor = doGetClassifierDescriptor(name);
            classifiers.put(name, classifierDescriptor);
        }
        return classifierDescriptor;
    }

    private ClassifierDescriptor doGetClassifierDescriptor(Name name) {
        // TODO : suboptimal, walk the list only once
        for (PsiClass innerClass : classPsiDeclarationProvider.getPsiClass().getAllInnerClasses()) {
            if (name.getName().equals(innerClass.getName())) {
                if (innerClass.hasModifierProperty(PsiModifier.STATIC) != classPsiDeclarationProvider.isStaticMembers()) return null;
                ClassDescriptor classDescriptor = semanticServices.getDescriptorResolver()
                        .resolveClass(new FqName(innerClass.getQualifiedName()), DescriptorSearchRule.IGNORE_IF_FOUND_IN_KOTLIN);
                if (classDescriptor != null) {
                    return classDescriptor;
                }
            }
        }
        return null;
    }
}
