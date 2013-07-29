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
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.ConstructorDescriptor;
import org.jetbrains.jet.lang.resolve.java.JavaDescriptorResolver;
import org.jetbrains.jet.lang.resolve.java.structure.JavaClass;
import org.jetbrains.jet.lang.resolve.name.FqName;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.jetbrains.jet.lang.resolve.java.DescriptorSearchRule.IGNORE_KOTLIN_SOURCES;

public final class JavaClassNonStaticMembersScope extends JavaClassMembersScope {
    private Collection<ConstructorDescriptor> constructors = null;
    private ConstructorDescriptor primaryConstructor = null;
    @NotNull
    private final ClassDescriptor descriptor;
    @NotNull
    private final JavaClass javaClass;
    private final boolean staticMembersOfPsiClass;

    public JavaClassNonStaticMembersScope(
            @NotNull ClassDescriptor descriptor,
            @NotNull JavaClass javaClass,
            boolean staticMembersOfPsiClass,
            @NotNull JavaDescriptorResolver javaDescriptorResolver
    ) {
        super(descriptor, MembersProvider.forClass(javaClass, staticMembersOfPsiClass), javaDescriptorResolver);
        this.descriptor = descriptor;
        this.javaClass = javaClass;
        this.staticMembersOfPsiClass = staticMembersOfPsiClass;
    }


    @NotNull
    public Collection<ConstructorDescriptor> getConstructors() {
        initConstructorsIfNeeded();
        return constructors;
    }

    @Nullable
    public ConstructorDescriptor getPrimaryConstructor() {
        initConstructorsIfNeeded();
        return primaryConstructor;
    }

    private void initConstructorsIfNeeded() {
        if (constructors == null) {
            constructors = javaDescriptorResolver.resolveConstructors(javaClass, descriptor);

            for (ConstructorDescriptor constructor : constructors) {
                if (constructor.isPrimary()) {
                    if (primaryConstructor != null) {
                        throw new IllegalStateException(
                                "Class has more than one primary constructor: " + primaryConstructor + "\n" + constructor);
                    }
                    primaryConstructor = constructor;
                }
            }
        }
    }

    @NotNull
    @Override
    protected Collection<ClassDescriptor> computeInnerClasses() {
        if (staticMembersOfPsiClass) {
            return Collections.emptyList();
        }

        Collection<JavaClass> innerClasses = javaClass.getInnerClasses();
        List<ClassDescriptor> result = new ArrayList<ClassDescriptor>(innerClasses.size());
        for (JavaClass innerClass : innerClasses) {
            result.add(resolveInnerClass(innerClass));
        }
        return result;
    }

    @NotNull
    private ClassDescriptor resolveInnerClass(@NotNull JavaClass innerClass) {
        String name = innerClass.getFqName();
        assert name != null : "Inner class has no qualified name: " + innerClass;
        ClassDescriptor classDescriptor = javaDescriptorResolver.resolveClass(new FqName(name), IGNORE_KOTLIN_SOURCES);
        assert classDescriptor != null : "Couldn't resolve inner class " + name;
        return classDescriptor;
    }
}
