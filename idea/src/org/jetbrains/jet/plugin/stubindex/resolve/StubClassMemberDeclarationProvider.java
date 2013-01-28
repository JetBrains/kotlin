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

package org.jetbrains.jet.plugin.stubindex.resolve;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.JetNamedFunction;
import org.jetbrains.jet.lang.psi.JetProperty;
import org.jetbrains.jet.lang.resolve.lazy.ClassMemberDeclarationProvider;
import org.jetbrains.jet.lang.resolve.lazy.data.JetClassLikeInfo;
import org.jetbrains.jet.lang.resolve.name.Name;

import java.util.Collection;

public class StubClassMemberDeclarationProvider extends AbstractStubDeclarationProvider implements ClassMemberDeclarationProvider {
    @NotNull
    @Override
    public JetClassLikeInfo getOwnerInfo() {
        // TODO:
        return null;
    }

    @NotNull
    @Override
    public Collection<JetNamedFunction> getFunctionDeclarations(@NotNull Name name) {
        // TODO:
        return null;
    }

    @NotNull
    @Override
    public Collection<JetProperty> getPropertyDeclarations(@NotNull Name name) {
        // TODO:
        return null;
    }
}
