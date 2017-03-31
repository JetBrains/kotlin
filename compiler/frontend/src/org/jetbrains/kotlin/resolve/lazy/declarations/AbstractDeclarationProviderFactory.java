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

package org.jetbrains.kotlin.resolve.lazy.declarations;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.psi.KtFile;
import org.jetbrains.kotlin.storage.MemoizedFunctionToNullable;
import org.jetbrains.kotlin.storage.StorageManager;

public abstract class AbstractDeclarationProviderFactory implements DeclarationProviderFactory {
    private final MemoizedFunctionToNullable<FqName, PackageMemberDeclarationProvider> packageDeclarationProviders;

    public AbstractDeclarationProviderFactory(@NotNull StorageManager storageManager) {
        this.packageDeclarationProviders =
                storageManager.createMemoizedFunctionWithNullableValues(this::createPackageMemberDeclarationProvider);
    }

    @Nullable
    protected abstract PackageMemberDeclarationProvider createPackageMemberDeclarationProvider(@NotNull FqName name);

    @Override
    public PackageMemberDeclarationProvider getPackageMemberDeclarationProvider(@NotNull FqName packageFqName) {
        return packageDeclarationProviders.invoke(packageFqName);
    }

    @Override
    public void diagnoseMissingPackageFragment(KtFile file) {
        throw new IllegalStateException("Cannot find package fragment for file " + file.getName() + " with package " + file.getPackageFqName());
    }
}
