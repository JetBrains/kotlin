/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.load.java.structure.impl;

import com.intellij.psi.PsiAnnotationOwner;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiDocCommentOwner;
import com.intellij.psi.PsiMember;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.descriptors.Visibility;
import org.jetbrains.kotlin.load.java.structure.JavaAnnotation;
import org.jetbrains.kotlin.load.java.structure.JavaClass;
import org.jetbrains.kotlin.load.java.structure.JavaMember;
import org.jetbrains.kotlin.load.java.structure.impl.source.JavaElementPsiSource;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.name.Name;

import java.util.Collection;

public abstract class JavaMemberImpl<Psi extends PsiMember> extends JavaElementImpl<Psi>
        implements JavaMember, JavaAnnotationOwnerImpl, JavaModifierListOwnerImpl {
    protected JavaMemberImpl(JavaElementPsiSource<Psi> psiMember) {
        super(psiMember);
    }

    @Nullable
    @Override
    public PsiAnnotationOwner getAnnotationOwnerPsi() {
        return getPsi().getModifierList();
    }

    @NotNull
    @Override
    public Name getName() {
        String name = getPsi().getName();
        assert name != null && Name.isValidIdentifier(name) : "Member must have a name: " + getPsi().getText();
        return Name.identifier(name);
    }

    @Override
    public final boolean isFromSource() {
        return true;
    }

    @NotNull
    @Override
    public JavaClass getContainingClass() {
        PsiClass psiClass = getPsi().getContainingClass();
        assert psiClass != null : "Member must have a containing class: " + getPsi();
        return new JavaClassImpl(createPsiSource(psiClass));
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
        return JavaElementUtil.getRegularAndExternalAnnotations(this, getSourceFactory());
    }

    @Nullable
    @Override
    public JavaAnnotation findAnnotation(@NotNull FqName fqName) {
        return JavaElementUtil.findAnnotation(this, fqName, getSourceFactory());
    }

    @Override
    public boolean isDeprecatedInJavaDoc() {
        PsiMember psi = getPsi();
        return psi instanceof PsiDocCommentOwner && ((PsiDocCommentOwner) psi).isDeprecated();
    }
}
