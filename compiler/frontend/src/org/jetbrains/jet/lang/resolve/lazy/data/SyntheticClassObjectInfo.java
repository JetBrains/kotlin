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

package org.jetbrains.jet.lang.resolve.lazy.data;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.ClassKind;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.lazy.descriptors.LazyClassDescriptor;
import org.jetbrains.jet.lang.resolve.name.FqName;

import java.util.Collections;
import java.util.List;

public class SyntheticClassObjectInfo implements JetClassLikeInfo {
    private final JetClassLikeInfo classInfo;
    private final LazyClassDescriptor classDescriptor;

    public SyntheticClassObjectInfo(@NotNull JetClassLikeInfo classInfo, @NotNull LazyClassDescriptor classDescriptor) {
        this.classInfo = classInfo;
        this.classDescriptor = classDescriptor;
    }

    @NotNull
    public LazyClassDescriptor getClassDescriptor() {
        return classDescriptor;
    }


    @NotNull
    @Override
    public FqName getContainingPackageFqName() {
        return classInfo.getContainingPackageFqName();
    }

    @Nullable
    @Override
    public JetModifierList getModifierList() {
        return null;
    }

    @Nullable
    @Override
    public JetClassObject getClassObject() {
        return null;
    }

    @NotNull
    @Override
    public List<JetClassObject> getClassObjects() {
        return Collections.emptyList();
    }

    @NotNull
    @Override
    public PsiElement getScopeAnchor() {
        return classInfo.getScopeAnchor();
    }

    @Nullable
    @Override
    public JetClassOrObject getCorrespondingClassOrObject() {
        return null;
    }

    @Nullable
    @Override
    public JetTypeParameterList getTypeParameterList() {
        return null;
    }

    @NotNull
    @Override
    public List<? extends JetParameter> getPrimaryConstructorParameters() {
        return Collections.emptyList();
    }

    @NotNull
    @Override
    public ClassKind getClassKind() {
        return ClassKind.CLASS_OBJECT;
    }

    @NotNull
    @Override
    public List<JetDeclaration> getDeclarations() {
        // There can be no declarations in a synthetic class object, all its members are fake overrides
        return Collections.emptyList();
    }

    @Override
    public String toString() {
        return "class object of " + classInfo;
    }
}
