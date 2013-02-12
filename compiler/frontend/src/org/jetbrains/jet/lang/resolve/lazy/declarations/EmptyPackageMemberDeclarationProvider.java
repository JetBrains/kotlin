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

package org.jetbrains.jet.lang.resolve.lazy.declarations;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.JetClassOrObject;
import org.jetbrains.jet.lang.psi.JetDeclaration;
import org.jetbrains.jet.lang.psi.JetNamedFunction;
import org.jetbrains.jet.lang.psi.JetProperty;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.Name;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class EmptyPackageMemberDeclarationProvider implements PackageMemberDeclarationProvider {

    public static final EmptyPackageMemberDeclarationProvider INSTANCE = new EmptyPackageMemberDeclarationProvider();

    private EmptyPackageMemberDeclarationProvider() {}

    @Override
    public boolean isPackageDeclared(@NotNull Name name) {
        return false;
    }

    @Override
    public Collection<FqName> getAllDeclaredPackages() {
        return Collections.emptyList();
    }

    @Override
    public List<JetDeclaration> getAllDeclarations() {
        return Collections.emptyList();
    }

    @NotNull
    @Override
    public Collection<JetNamedFunction> getFunctionDeclarations(@NotNull Name name) {
        return Collections.emptyList();
    }

    @NotNull
    @Override
    public Collection<JetProperty> getPropertyDeclarations(@NotNull Name name) {
        return Collections.emptyList();
    }

    @NotNull
    @Override
    public Collection<JetClassOrObject> getClassOrObjectDeclarations(@NotNull Name name) {
        return Collections.emptyList();
    }
}
