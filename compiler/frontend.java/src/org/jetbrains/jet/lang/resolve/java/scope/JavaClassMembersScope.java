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

package org.jetbrains.jet.lang.resolve.java.scope;

import com.intellij.psi.PsiClass;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.resolve.java.JavaDescriptorResolver;
import org.jetbrains.jet.lang.resolve.java.provider.NamedMembers;
import org.jetbrains.jet.lang.resolve.name.LabelName;
import org.jetbrains.jet.lang.resolve.name.Name;

import java.util.*;

public abstract class JavaClassMembersScope extends JavaBaseScope {
    @NotNull
    protected final PsiClass psiClass;

    private Map<Name, ClassDescriptor> innerClassesMap = null;

    protected JavaClassMembersScope(
            @NotNull ClassOrNamespaceDescriptor descriptor,
            @NotNull PsiClass psiClass,
            @NotNull MembersProvider membersProvider,
            @NotNull JavaDescriptorResolver javaDescriptorResolver
    ) {
        super(descriptor, javaDescriptorResolver, membersProvider);
        this.psiClass = psiClass;
    }

    @NotNull
    @Override
    public Collection<DeclarationDescriptor> getDeclarationsByLabel(LabelName labelName) {
        throw new UnsupportedOperationException(); // TODO
    }


    @NotNull
    @Override
    protected Set<FunctionDescriptor> computeFunctionDescriptor(@NotNull Name name) {
        NamedMembers members = membersProvider.get(name);
        if (members == null) {
            return Collections.emptySet();
        }
        return javaDescriptorResolver.resolveFunctionGroupForClass(members, descriptor, psiClass);
    }

    @NotNull
    private Map<Name, ClassDescriptor> getInnerClassesMap() {
        if (innerClassesMap == null) {
            Collection<ClassDescriptor> innerClasses = getInnerClasses();
            innerClassesMap = new HashMap<Name, ClassDescriptor>();
            for (ClassDescriptor innerClass : innerClasses) {
                innerClassesMap.put(innerClass.getName(), innerClass);
            }
        }
        return innerClassesMap;
    }

    @Override
    public ClassDescriptor getObjectDescriptor(@NotNull Name name) {
        ClassDescriptor innerClass = getInnerClassesMap().get(name);
        if (innerClass != null && innerClass.getKind().isObject()) {
            return innerClass;
        }
        return null;
    }

    @Override
    public ClassifierDescriptor getClassifier(@NotNull Name name) {
        ClassDescriptor innerClass = getInnerClassesMap().get(name);
        if (innerClass == null || innerClass.getKind().isObject()) {
            return null;
        }
        return innerClass;
    }
}
