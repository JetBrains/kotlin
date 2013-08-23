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

package org.jetbrains.jet.lang.resolve.java.structure;

import com.intellij.psi.PsiModifier;
import com.intellij.psi.PsiModifierList;
import com.intellij.psi.PsiModifierListOwner;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.Visibilities;
import org.jetbrains.jet.lang.descriptors.Visibility;
import org.jetbrains.jet.lang.resolve.java.JavaVisibilities;

import java.util.Collection;
import java.util.Collections;

import static org.jetbrains.jet.lang.resolve.java.structure.JavaElementCollectionFromPsiArrayUtil.annotations;

public abstract class JavaModifierListOwnerImpl extends JavaElementImpl implements JavaModifierListOwner {
    protected JavaModifierListOwnerImpl(@NotNull PsiModifierListOwner psiModifierListOwner) {
        super(psiModifierListOwner);
    }

    @NotNull
    @Override
    public PsiModifierListOwner getPsi() {
        return (PsiModifierListOwner) super.getPsi();
    }

    @Override
    public boolean isAbstract() {
        return getPsi().hasModifierProperty(PsiModifier.ABSTRACT);
    }

    @Override
    public boolean isStatic() {
        return getPsi().hasModifierProperty(PsiModifier.STATIC);
    }

    @Override
    public boolean isFinal() {
        return getPsi().hasModifierProperty(PsiModifier.FINAL);
    }

    @NotNull
    @Override
    public Visibility getVisibility() {
        if (getPsi().hasModifierProperty(PsiModifier.PUBLIC)) {
            return Visibilities.PUBLIC;
        }
        if (getPsi().hasModifierProperty(PsiModifier.PRIVATE)) {
            return Visibilities.PRIVATE;
        }
        if (getPsi().hasModifierProperty(PsiModifier.PROTECTED)) {
            return isStatic() ? JavaVisibilities.PROTECTED_STATIC_VISIBILITY : JavaVisibilities.PROTECTED_AND_PACKAGE;
        }
        return JavaVisibilities.PACKAGE_VISIBILITY;
    }

    @NotNull
    @Override
    public Collection<JavaAnnotation> getAnnotations() {
        PsiModifierList modifierList = getPsi().getModifierList();
        if (modifierList != null) {
            return annotations(modifierList.getAnnotations());
        }
        return Collections.emptyList();
    }
}
