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

package org.jetbrains.jet.plugin.stubindex.resolve;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.resolve.lazy.ClassMemberDeclarationProvider;
import org.jetbrains.jet.lang.resolve.lazy.DeclarationProviderFactory;
import org.jetbrains.jet.lang.resolve.lazy.PackageMemberDeclarationProvider;
import org.jetbrains.jet.lang.resolve.lazy.data.JetClassLikeInfo;
import org.jetbrains.jet.lang.resolve.name.FqName;

/**
 * @author Nikolay Krasko
 */
public class StubDeclarationProviderFactory implements DeclarationProviderFactory {
    @NotNull
    @Override
    public ClassMemberDeclarationProvider getClassMemberDeclarationProvider(@NotNull JetClassLikeInfo classLikeInfo) {
        // TODO:
        return null;
    }

    @Override
    public PackageMemberDeclarationProvider getPackageMemberDeclarationProvider(@NotNull FqName packageFqName) {
        // TODO:
        return null;
    }
}
