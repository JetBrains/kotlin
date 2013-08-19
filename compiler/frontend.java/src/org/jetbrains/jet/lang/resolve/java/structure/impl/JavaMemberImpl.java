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

package org.jetbrains.jet.lang.resolve.java.structure.impl;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMember;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.Visibility;
import org.jetbrains.jet.lang.resolve.java.structure.JavaAnnotation;
import org.jetbrains.jet.lang.resolve.java.structure.JavaClass;
import org.jetbrains.jet.lang.resolve.java.structure.JavaMember;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.Name;

import java.util.Collection;

public abstract class JavaMemberImpl extends JavaElementImpl implements JavaMember {
    protected JavaMemberImpl(@NotNull PsiMember psiMember) {
        super(psiMember);
    }

    @NotNull
    @Override
    public PsiMember getPsi() {
        return (PsiMember) super.getPsi();
    }

    @NotNull
    @Override
    public Name getName() {
        String name = getPsi().getName();
        assert name != null : "Member must have a name: " + getPsi();
        return Name.identifier(name);
    }

    @NotNull
    @Override
    public JavaClass getContainingClass() {
        PsiClass psiClass = getPsi().getContainingClass();
        assert psiClass != null : "Member must have a containing class: " + getPsi();
        return new JavaClassImpl(psiClass);
    }

    @Override
    public boolean isAbstract() {
        return JavaElementUtil.isAbstract(this);
    }

    @Override
    public boolean isStatic() {
        return JavaElementUtil.isStatic(this);
    }

    @Override
    public boolean isFinal() {
        return JavaElementUtil.isFinal(this);
    }

    @NotNull
    @Override
    public Visibility getVisibility() {
        return JavaElementUtil.getVisibility(this);
    }

    @NotNull
    @Override
    public Collection<JavaAnnotation> getAnnotations() {
        return JavaElementUtil.getAnnotations(this);
    }

    @Nullable
    @Override
    public JavaAnnotation findAnnotation(@NotNull FqName fqName) {
        return JavaElementUtil.findAnnotation(this, fqName);
    }
}
