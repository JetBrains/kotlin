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

import com.intellij.psi.PsiNameIdentifierOwner;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.name.Name;

import java.util.List;

public interface JetClassOrObject extends PsiNameIdentifierOwner, JetDeclarationContainer, JetElement, JetModifierListOwner, JetNamedDeclaration {
    @Nullable
    JetDelegationSpecifierList getDelegationSpecifierList();

    @NotNull
    List<JetDelegationSpecifier> getDelegationSpecifiers();

    @NotNull
    List<JetClassInitializer> getAnonymousInitializers();

    @Override
    @Nullable
    Name getNameAsName();

    @Override
    @Nullable
    JetModifierList getModifierList();

    @Nullable
    JetObjectDeclarationName getNameAsDeclaration();

    @Nullable
    JetClassBody getBody();

    boolean isTopLevel();

    boolean isLocal();
}
