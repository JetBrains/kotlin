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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.resolve.java.JavaClassClassResolutionFacade;
import org.jetbrains.jet.lang.resolve.java.provider.ClassPsiDeclarationProvider;
import org.jetbrains.jet.lang.resolve.name.LabelName;
import org.jetbrains.jet.lang.resolve.name.Name;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public abstract class JavaClassMembersScope extends JavaBaseScope {
    @NotNull
    protected final ClassPsiDeclarationProvider declarationProvider;

    private Map<Name, ClassDescriptor> innerClassesMap = null;

    protected JavaClassMembersScope(
            @NotNull ClassOrPackageDescriptor descriptor,
            @NotNull ClassPsiDeclarationProvider declarationProvider,
            @NotNull JavaClassClassResolutionFacade classResolutionFacade
    ) {
        super(descriptor, classResolutionFacade, declarationProvider);
        this.declarationProvider = declarationProvider;
    }

    @NotNull
    @Override
    public Collection<DeclarationDescriptor> getDeclarationsByLabel(LabelName labelName) {
        throw new UnsupportedOperationException(); // TODO
    }


    @NotNull
    @Override
    protected Set<FunctionDescriptor> computeFunctionDescriptor(@NotNull Name name) {
        return javaDescriptorResolver.resolveFunctionGroup(name, declarationProvider, descriptor);
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

    @NotNull
    @Override
    protected Collection<ClassDescriptor> computeInnerClasses() {
        return javaDescriptorResolver.resolveInnerClasses(declarationProvider);
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
