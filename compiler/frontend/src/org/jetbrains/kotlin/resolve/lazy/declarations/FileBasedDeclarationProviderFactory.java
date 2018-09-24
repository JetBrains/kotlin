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

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import kotlin.collections.CollectionsKt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.psi.KtFile;
import org.jetbrains.kotlin.resolve.lazy.data.KtClassLikeInfo;
import org.jetbrains.kotlin.storage.NotNullLazyValue;
import org.jetbrains.kotlin.storage.StorageManager;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class FileBasedDeclarationProviderFactory extends AbstractDeclarationProviderFactory  {

    private static class Index {
        private final Multimap<FqName, KtFile> filesByPackage = LinkedHashMultimap.create();
        private final Set<FqName> declaredPackages = new HashSet<>();
    }

    private final StorageManager storageManager;
    private final NotNullLazyValue<Index> index;

    public FileBasedDeclarationProviderFactory(@NotNull StorageManager storageManager, @NotNull Collection<KtFile> files) {
        super(storageManager);
        this.storageManager = storageManager;
        this.index = storageManager.createLazyValue(() -> computeFilesByPackage(files));
    }

    @NotNull
    private static Index computeFilesByPackage(@NotNull Collection<KtFile> files) {
        Index index = new Index();
        for (KtFile file : files) {
            FqName packageFqName = file.getPackageFqName();
            addMeAndParentPackages(index, packageFqName);
            index.filesByPackage.put(packageFqName, file);
        }
        return index;
    }

    private static void addMeAndParentPackages(@NotNull Index index, @NotNull FqName name) {
        index.declaredPackages.add(name);
        if (!name.isRoot()) {
            addMeAndParentPackages(index, name.parent());
        }
    }

    @Override
    public boolean packageExists(@NotNull FqName packageFqName) {
        return index.invoke().declaredPackages.contains(packageFqName);
    }

    /*package*/ Collection<FqName> getAllDeclaredSubPackagesOf(@NotNull FqName parent) {
        return CollectionsKt.filter(index.invoke().declaredPackages, fqName -> !fqName.isRoot() && fqName.parent().equals(parent));
    }

    @Nullable
    @Override
    protected PackageMemberDeclarationProvider createPackageMemberDeclarationProvider(@NotNull FqName packageFqName) {
        if (packageExists(packageFqName)) {
            return new FileBasedPackageMemberDeclarationProvider(
                    storageManager, packageFqName, this, index.invoke().filesByPackage.get(packageFqName));
        }

        return null;
    }

    @NotNull
    @Override
    public ClassMemberDeclarationProvider getClassMemberDeclarationProvider(@NotNull KtClassLikeInfo classLikeInfo) {
        if (!index.invoke().filesByPackage.containsKey(classLikeInfo.getContainingPackageFqName())) {
            throw new IllegalStateException("This factory doesn't know about this class: " + classLikeInfo);
        }

        return new PsiBasedClassMemberDeclarationProvider(storageManager, classLikeInfo);
    }
}
