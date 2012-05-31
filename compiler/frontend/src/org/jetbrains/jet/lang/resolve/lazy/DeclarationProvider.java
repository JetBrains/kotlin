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

package org.jetbrains.jet.lang.resolve.lazy;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.psi.JetClassOrObject;
import org.jetbrains.jet.lang.psi.JetDeclaration;
import org.jetbrains.jet.lang.psi.JetNamedFunction;
import org.jetbrains.jet.lang.psi.JetProperty;
import org.jetbrains.jet.lang.resolve.name.Name;

import java.util.List;

/**
* @author abreslav
*/
public interface DeclarationProvider {
    List<JetDeclaration> getAllDeclarations();

    @NotNull
    List<JetNamedFunction> getFunctionDeclarations(@NotNull Name name);

    @NotNull
    List<JetProperty> getPropertyDeclarations(@NotNull Name name);

    @Nullable
    JetClassOrObject getClassOrObjectDeclaration(@NotNull Name name);

    boolean isPackageDeclared(@NotNull Name name);
}
