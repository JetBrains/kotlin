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

package org.jetbrains.jet.lang.resolve.java.resolver;

import com.intellij.psi.PsiClass;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.resolve.java.DescriptorSearchRule;
import org.jetbrains.jet.lang.resolve.java.JvmAbi;
import org.jetbrains.jet.lang.resolve.java.kt.JetClassObjectAnnotation;
import org.jetbrains.jet.lang.resolve.java.provider.ClassPsiDeclarationProvider;
import org.jetbrains.jet.lang.resolve.name.FqName;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class JavaInnerClassResolver {

    private JavaClassResolver classResolver;

    public JavaInnerClassResolver() {
    }

    @Inject
    public void setClassResolver(JavaClassResolver classResolver) {
        this.classResolver = classResolver;
    }

    @NotNull
    public List<ClassDescriptor> resolveInnerClasses(@NotNull ClassPsiDeclarationProvider declarationProvider) {
        if (declarationProvider.isStaticMembers()) {
            return Collections.emptyList();
        }

        return resolveInnerClasses(declarationProvider.getPsiClass());
    }

    @NotNull
    private List<ClassDescriptor> resolveInnerClasses(@NotNull PsiClass psiClass) {
        PsiClass[] innerPsiClasses = psiClass.getInnerClasses();
        List<ClassDescriptor> result = new ArrayList<ClassDescriptor>(innerPsiClasses.length);
        for (PsiClass innerPsiClass : innerPsiClasses) {
            if (shouldBeIgnored(innerPsiClass)) {
                continue;
            }
            ClassDescriptor classDescriptor = resolveInnerClass(innerPsiClass);
            result.add(classDescriptor);
        }
        return result;
    }

    private static boolean shouldBeIgnored(PsiClass innerPsiClass) {
        return JetClassObjectAnnotation.get(innerPsiClass).isDefined();
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