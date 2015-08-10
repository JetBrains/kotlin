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

package org.jetbrains.kotlin.psi;

import com.intellij.lang.ASTNode;
import com.intellij.psi.stubs.IStubElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.psi.stubs.KotlinStubWithFqName;
import org.jetbrains.kotlin.psi.stubs.elements.JetStubElementTypes;

import java.util.Collections;
import java.util.List;

public abstract class JetTypeParameterListOwnerStub<T extends KotlinStubWithFqName<?>> extends JetNamedDeclarationStub<T> implements JetTypeParameterListOwner {
    public JetTypeParameterListOwnerStub(@NotNull T stub, @NotNull IStubElementType nodeType) {
        super(stub, nodeType);
    }

    public JetTypeParameterListOwnerStub(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    @Nullable
    public JetTypeParameterList getTypeParameterList() {
        return getStubOrPsiChild(JetStubElementTypes.TYPE_PARAMETER_LIST);
    }

    @Override
    @Nullable
    public JetTypeConstraintList getTypeConstraintList() {
        return getStubOrPsiChild(JetStubElementTypes.TYPE_CONSTRAINT_LIST);
    }

    @Override
    @NotNull
    public List<JetTypeConstraint> getTypeConstraints() {
        JetTypeConstraintList typeConstraintList = getTypeConstraintList();
        if (typeConstraintList == null) {
            return Collections.emptyList();
        }
        return typeConstraintList.getConstraints();
    }

    @Override
    @NotNull
    public List<JetTypeParameter> getTypeParameters() {
        JetTypeParameterList list = getTypeParameterList();
        if (list == null) return Collections.emptyList();

        return list.getParameters();
    }
}
