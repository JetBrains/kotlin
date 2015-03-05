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

package org.jetbrains.kotlin.resolve.lazy.data;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.descriptors.ClassKind;
import org.jetbrains.kotlin.psi.JetObjectDeclaration;
import org.jetbrains.kotlin.psi.JetParameter;
import org.jetbrains.kotlin.psi.JetTypeParameterList;
import org.jetbrains.kotlin.resolve.ModifiersChecker;

import java.util.Collections;
import java.util.List;

public class JetObjectInfo extends JetClassOrObjectInfo<JetObjectDeclaration> {
    @NotNull
    private final ClassKind kind;

    protected JetObjectInfo(@NotNull JetObjectDeclaration element) {
        super(element);
        this.kind = element.isObjectLiteral() ? ClassKind.CLASS : ClassKind.OBJECT;
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
        return kind;
    }

    public boolean isDefaultObject() {
        return element.isDefault() && ModifiersChecker.isDefaultModifierAllowed(element);
    }
}
