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

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.psi.JetNamespaceHeader;
import org.jetbrains.jet.lang.resolve.lazy.data.JetClassLikeInfo;
import org.jetbrains.jet.lang.resolve.name.FqName;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

public class FileBasedDeclarationProviderFactory implements DeclarationProviderFactory {

    private final Collection<JetFile> allFiles;

    private final Multimap<FqName, JetFile> filesByPackage = HashMultimap.create();
    private final Set<FqName> declaredPackages = Sets.newHashSet();
    private final Map<FqName, PackageMemberDeclarationProvider> packageDeclarationProviders = Maps.newHashMap();
    //private final Map<JetClassOrObject, ClassMemberDeclarationProvider> classMemberDeclarationProviders = Maps.newHashMap();

    private final Predicate<FqName> isPackageDeclaredExternally;

    private boolean indexed = false;

    public FileBasedDeclarationProviderFactory(@NotNull Collection<JetFile> files) {
        this(files, Predicates.<FqName>alwaysFalse());
    }

    public FileBasedDeclarationProviderFactory(@NotNull Collection<JetFile> files, Predicate<FqName> isPackageDeclaredExternally) {
        this.allFiles = files;
        this.isPackageDeclaredExternally = isPackageDeclaredExternally;
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
        declaredPackages.add(name);
        if (!name.isRoot()) {
            addMeAndParentPackages(name.parent());
        }
    }

    /*package*/ boolean isPackageDeclaredExplicitly(@NotNull FqName packageFqName) {
        createIndex();
        return declaredPackages.contains(packageFqName);
    }

    /*package*/ boolean isPackageDeclared(@NotNull FqName packageFqName) {
        return isPackageDeclaredExplicitly(packageFqName) || isPackageDeclaredExternally.apply(packageFqName);
    }

    /*package*/ Collection<FqName> getAllDeclaredSubPackagesOf(@NotNull final FqName parent) {
        return Collections2.filter(declaredPackages, new Predicate<FqName>() {
            @Override
            public boolean apply(FqName fqName) {
                return !fqName.isRoot() && fqName.parent().equals(parent);
            }
        });
    }

    @Override
    public PackageMemberDeclarationProvider getPackageMemberDeclarationProvider(@NotNull FqName packageFqName) {
        createIndex();

        PackageMemberDeclarationProvider declarationProvider = packageDeclarationProviders.get(packageFqName);
        if (declarationProvider != null) {
            return declarationProvider;
        }

        if (!isPackageDeclaredExplicitly(packageFqName)) {
            if (isPackageDeclaredExternally.apply(packageFqName)) {
                return EmptyPackageMemberDeclarationProvider.INSTANCE;
            }
            return null;
        }

        FileBasedPackageMemberDeclarationProvider provider =
                new FileBasedPackageMemberDeclarationProvider(packageFqName, this, filesByPackage.get(packageFqName));
        packageDeclarationProviders.put(packageFqName, provider);

        return provider;
    }

    @NotNull
    @Override
    public ClassMemberDeclarationProvider getClassMemberDeclarationProvider(@NotNull JetClassLikeInfo classLikeInfo) {
        createIndex();

        //JetClassOrObject classOrObject = classLikeInfo.getCorrespondingClassOrObject();
        //if (classOrObject != null) {
        //    ClassMemberDeclarationProvider declarationProvider = classMemberDeclarationProviders.get(classOrObject);
        //    if (declarationProvider != null) {
        //        return declarationProvider;
        //    }
        //}

        if (!filesByPackage.containsKey(classLikeInfo.getContainingPackageFqName())) {
            throw new IllegalStateException("This factory doesn't know about this class: " + classLikeInfo);
        }

        ClassMemberDeclarationProvider provider = new PsiBasedClassMemberDeclarationProvider(classLikeInfo);
        //if (classOrObject != null) {
        //    classMemberDeclarationProviders.put(classOrObject, provider);
        //}

        return provider;
    }
}
