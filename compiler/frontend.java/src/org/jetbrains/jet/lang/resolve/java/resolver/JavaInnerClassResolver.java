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

package org.jetbrains.jet.lang.resolve.java.resolver;

import com.google.common.collect.Lists;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiModifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.resolve.java.DescriptorResolverUtils;
import org.jetbrains.jet.lang.resolve.java.DescriptorSearchRule;
import org.jetbrains.jet.lang.resolve.java.JvmAbi;
import org.jetbrains.jet.lang.resolve.java.provider.ClassPsiDeclarationProvider;
import org.jetbrains.jet.lang.resolve.name.FqName;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.jetbrains.jet.lang.resolve.java.provider.DeclarationOrigin.KOTLIN;

public final class JavaInnerClassResolver {

    private JavaClassResolver classResolver;

    public JavaInnerClassResolver() {
    }

    @Inject
    public void setClassResolver(JavaClassResolver classResolver) {
        this.classResolver = classResolver;
    }

    @NotNull
    public List<ClassDescriptor> resolveInnerClasses(
            @NotNull DeclarationDescriptor owner,
            @NotNull ClassPsiDeclarationProvider declarationProvider
    ) {
        if (declarationProvider.isStaticMembers() && declarationProvider.getDeclarationOrigin() == KOTLIN) {
            return resolveInnerClassesOfClassObject(owner, declarationProvider.getPsiClass());
        }

        return resolveInnerClasses(owner, declarationProvider.getPsiClass(), declarationProvider.isStaticMembers());
    }

    @NotNull
    private List<ClassDescriptor> resolveInnerClasses(@NotNull DeclarationDescriptor owner, @NotNull PsiClass psiClass, boolean isStatic) {
        PsiClass[] innerPsiClasses = psiClass.getInnerClasses();
        List<ClassDescriptor> result = new ArrayList<ClassDescriptor>(innerPsiClasses.length);
        for (PsiClass innerPsiClass : innerPsiClasses) {
            if (shouldBeIgnored(owner, psiClass, innerPsiClass, isStatic)) {
                continue;
            }
            ClassDescriptor classDescriptor = resolveInnerClass(innerPsiClass);
            result.add(classDescriptor);
        }
        return result;
    }

    private static boolean shouldBeIgnored(DeclarationDescriptor owner, PsiClass psiClass, PsiClass innerPsiClass, boolean isStatic) {
        if (innerPsiClass.getName().equals(JvmAbi.CLASS_OBJECT_CLASS_NAME)
            || DescriptorResolverUtils.isInnerEnum(innerPsiClass, owner)) {
            return true;
        }
        //TODO: this is a hack that puts interfaces and inner classes of interfaces (which are always static)
        // inside NonStaticMemberScope
        if (innerPsiClass.isInterface() || psiClass.isInterface()) {
            return isStatic;
        }
        else {
            return innerPsiClass.hasModifierProperty(PsiModifier.STATIC) != isStatic;
        }
    }

    private List<ClassDescriptor> resolveInnerClassesOfClassObject(@NotNull DeclarationDescriptor classObject, @NotNull PsiClass psiClass) {
        if (!DescriptorUtils.isClassObject(classObject)) {
            return Collections.emptyList();
        }

        List<ClassDescriptor> result = Lists.newArrayList();
        // If we're a class object, inner enums of our parent need to be put into us
        DeclarationDescriptor containingDeclaration = classObject.getContainingDeclaration();
        for (PsiClass innerPsiClass : psiClass.getInnerClasses()) {
            if (DescriptorResolverUtils.isInnerEnum(innerPsiClass, containingDeclaration)) {
                ClassDescriptor classDescriptor = resolveInnerClass(innerPsiClass);
                result.add(classDescriptor);
            }
        }
        return result;
    }

    @NotNull
    private ClassDescriptor resolveInnerClass(@NotNull PsiClass innerPsiClass) {
        String name = innerPsiClass.getQualifiedName();
        assert name != null : "Inner class has no qualified name";
        ClassDescriptor classDescriptor = classResolver.resolveClass(new FqName(name), DescriptorSearchRule.IGNORE_IF_FOUND_IN_KOTLIN);
        assert classDescriptor != null : "Couldn't resolve class " + name;
        return classDescriptor;
    }
}