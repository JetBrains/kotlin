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

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.JetClassOrObject;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.psi.JetNamespaceHeader;
import org.jetbrains.jet.lang.resolve.name.FqName;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * @author abreslav
 */
public class FileBasedDeclarationProviderFactory implements DeclarationProviderFactory {

    private final Collection<JetFile> allFiles;

    private final Multimap<FqName, JetFile> filesByPackage = HashMultimap.create();
    private final Set<FqName> definedPackages = Sets.newHashSet();
    private final Map<FqName, PackageMemberDeclarationProvider> packageDeclarationProviders = Maps.newHashMap();
    private final Map<JetClassOrObject, ClassMemberDeclarationProvider> classMemberDeclarationProviders = Maps.newHashMap();

    private boolean indexed = false;

    public FileBasedDeclarationProviderFactory(@NotNull Collection<JetFile> files) {
        this.allFiles = files;
    }

    private void createIndex() {
        if (indexed) return;
        indexed = true;

        for (JetFile file : allFiles) {
            JetNamespaceHeader header = file.getNamespaceHeader();
            if (header == null) {
                throw new IllegalArgumentException("Scripts are not supported");
            }

            FqName packageFqName = new FqName(header.getQualifiedName());
            addMeAndParentPackages(packageFqName);
            filesByPackage.put(packageFqName, file);
        }
    }

    private void addMeAndParentPackages(@NotNull FqName name) {
        definedPackages.add(name);
        if (!name.isRoot()) {
            addMeAndParentPackages(name.parent());
        }
    }

    /*package*/ boolean isPackageDeclared(@NotNull FqName packageFqName) {
        createIndex();
        return definedPackages.contains(packageFqName);
    }

    @Override
    public PackageMemberDeclarationProvider getPackageMemberDeclarationProvider(@NotNull FqName packageFqName) {
        createIndex();

        PackageMemberDeclarationProvider declarationProvider = packageDeclarationProviders.get(packageFqName);
        if (declarationProvider != null) {
            return declarationProvider;
        }

        if (!isPackageDeclared(packageFqName)) return null;

        FileBasedPackageMemberDeclarationProvider provider =
                new FileBasedPackageMemberDeclarationProvider(packageFqName, this, filesByPackage.get(packageFqName));
        packageDeclarationProviders.put(packageFqName, provider);

        return provider;
    }

    @NotNull
    @Override
    public ClassMemberDeclarationProvider getClassMemberDeclarationProvider(@NotNull JetClassOrObject jetClassOrObject) {
        createIndex();

        ClassMemberDeclarationProvider declarationProvider = classMemberDeclarationProviders.get(jetClassOrObject);
        if (declarationProvider != null) {
            return declarationProvider;
        }

        JetFile file = (JetFile) jetClassOrObject.getContainingFile();

        if (!filesByPackage.containsKey(new FqName(file.getNamespaceHeader().getQualifiedName()))) {
            throw new IllegalStateException("This factory doesn't know about this class: " + jetClassOrObject);
        }

        ClassMemberDeclarationProvider provider = new PsiBasedClassMemberDeclarationProvider(jetClassOrObject);
        classMemberDeclarationProviders.put(jetClassOrObject, provider);

        return provider;
    }
}
