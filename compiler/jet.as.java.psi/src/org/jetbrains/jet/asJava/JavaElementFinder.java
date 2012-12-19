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

package org.jetbrains.jet.asJava;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.Sets;
import com.intellij.openapi.extensions.Extensions;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.*;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElementFinder;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiPackage;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.SmartList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.codegen.NamespaceCodegen;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.java.JavaPsiFacadeKotlinHacks;
import org.jetbrains.jet.lang.resolve.java.JetFilesProvider;
import org.jetbrains.jet.lang.resolve.java.JvmAbi;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.util.QualifiedNamesUtil;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;

import static org.jetbrains.jet.codegen.CodegenUtil.getLocalNameForObject;

public class JavaElementFinder extends PsiElementFinder implements JavaPsiFacadeKotlinHacks.KotlinFinderMarker {

    @NotNull
    public static JavaElementFinder getInstance(@NotNull Project project) {
        PsiElementFinder[] extensions = Extensions.getArea(project).getExtensionPoint(PsiElementFinder.EP_NAME).getExtensions();
        for (PsiElementFinder extension : extensions) {
            if (extension instanceof JavaElementFinder) {
                return (JavaElementFinder) extension;
            }
        }
        throw new IllegalStateException(JavaElementFinder.class.getSimpleName() + " is not found for project " + project);
    }

    private final Project project;
    private final PsiManager psiManager;
    private final LightClassGenerationSupport lightClassGenerationSupport;

    private final WeakHashMap<GlobalSearchScope, Collection<JetFile>> jetFiles = new WeakHashMap<GlobalSearchScope, Collection<JetFile>>();

    public JavaElementFinder(
            @NotNull Project project,
            @NotNull LightClassGenerationSupport lightClassGenerationSupport
    ) {
        this.project = project;
        psiManager = PsiManager.getInstance(project);
        this.lightClassGenerationSupport = lightClassGenerationSupport;

        // Monitoring for files instead of collecting them each time
        VirtualFileManager.getInstance().addVirtualFileListener(new VirtualFileListener() {
            @Override
            public void fileCreated(VirtualFileEvent event) {
                invalidateJetFilesCache();
            }

            @Override
            public void fileDeleted(VirtualFileEvent event) {
                invalidateJetFilesCache();
            }

            @Override
            public void fileMoved(VirtualFileMoveEvent event) {
                invalidateJetFilesCache();
            }

            @Override
            public void fileCopied(VirtualFileCopyEvent event) {
                invalidateJetFilesCache();
            }

            @Override
            public void propertyChanged(VirtualFilePropertyEvent event) {}

            @Override
            public void contentsChanged(VirtualFileEvent event) {}

            @Override
            public void beforePropertyChange(VirtualFilePropertyEvent event) {}

            @Override
            public void beforeContentsChange(VirtualFileEvent event) {}

            @Override
            public void beforeFileDeletion(VirtualFileEvent event) {}

            @Override
            public void beforeFileMovement(VirtualFileMoveEvent event) {}
        });
    }

    @Override
    public PsiClass findClass(@NotNull String qualifiedName, @NotNull GlobalSearchScope scope) {
        final PsiClass[] allClasses = findClasses(qualifiedName, scope);
        return allClasses.length > 0 ? allClasses[0] : null;
    }

    @NotNull
    @Override
    public PsiClass[] findClasses(@NotNull String qualifiedNameString, @NotNull GlobalSearchScope scope) {
        if (!FqName.isValid(qualifiedNameString)) {
            return PsiClass.EMPTY_ARRAY;
        }

        List<PsiClass> answer = new SmartList<PsiClass>();

        FqName qualifiedName = new FqName(qualifiedNameString);

        findClassesAndObjects(qualifiedName, scope, answer);

        if (JvmAbi.PACKAGE_CLASS.equals(qualifiedName.shortName().getName())) {
            findPackageClass(qualifiedName.parent(), scope, answer);
        }

        return answer.toArray(new PsiClass[answer.size()]);
    }

    // Finds explicitly declared classes and objects, not package classes
    private void findClassesAndObjects(FqName qualifiedName, GlobalSearchScope scope, List<PsiClass> answer) {
        Collection<JetClassOrObject> classOrObjectDeclarations =
                lightClassGenerationSupport.findClassOrObjectDeclarations(qualifiedName, scope);

        for (JetClassOrObject declaration : classOrObjectDeclarations) {
            if (!(declaration instanceof JetEnumEntry)) {
                JetLightClass lightClass = JetLightClass.create(psiManager, (JetFile) declaration.getContainingFile(), qualifiedName);
                if (lightClass != null) {
                    answer.add(lightClass);
                }
            }
        }
    }

    private void findPackageClass(FqName qualifiedName, GlobalSearchScope scope, List<PsiClass> answer) {
        FqName packageClassName = QualifiedNamesUtil.combine(qualifiedName, Name.identifier(JvmAbi.PACKAGE_CLASS));
        Collection<JetFile> filesForPackage = lightClassGenerationSupport.findFilesForPackage(qualifiedName, scope);

        if (!filesForPackage.isEmpty() && NamespaceCodegen.shouldGenerateNSClass(filesForPackage)) {
            // TODO This is wrong, but mimics the previous behavior. Will fix
            JetFile someFile = filesForPackage.iterator().next();
            JetLightClass lightClass = JetLightClass.create(psiManager, someFile, packageClassName);
            if (lightClass != null) {
                answer.add(lightClass);
            }
        }
    }

    @Nullable
    private static String getLocalName(JetDeclaration declaration) {
        String given = declaration.getName();
        if (given != null) return given;

        if (declaration instanceof JetObjectDeclaration) {
            return getLocalNameForObject((JetObjectDeclaration) declaration);
        }

        return null;
    }

    @NotNull
    @Override
    public Set<String> getClassNames(@NotNull PsiPackage psiPackage, @NotNull GlobalSearchScope scope) {
        FqName packageFQN = new FqName(psiPackage.getQualifiedName());

        Collection<JetClassOrObject> declarations = lightClassGenerationSupport.findClassOrObjectDeclarationsInPackage(packageFQN, scope);

        Set<String> answer = Sets.newHashSet();
        answer.add(JvmAbi.PACKAGE_CLASS);

        for (JetClassOrObject declaration : declarations) {
            String name = declaration.getName();
            if (name != null) {
                answer.add(name);
            }
        }

        return answer;
    }

    @Override
    public PsiPackage findPackage(@NotNull String qualifiedNameString) {
        if (!FqName.isValid(qualifiedNameString)) {
            return null;
        }

        FqName fqName = new FqName(qualifiedNameString);

        // allScope() because the contract says that the whole project
        GlobalSearchScope allScope = GlobalSearchScope.allScope(project);
        if (lightClassGenerationSupport.packageExists(fqName, allScope)) {
            return new JetLightPackage(psiManager, fqName, allScope);
        }

        return null;
    }

    @NotNull
    @Override
    public PsiPackage[] getSubPackages(@NotNull PsiPackage psiPackage, @NotNull final GlobalSearchScope scope) {
        FqName packageFQN = new FqName(psiPackage.getQualifiedName());

        Collection<FqName> subpackages = lightClassGenerationSupport.getSubPackages(packageFQN, scope);

        Collection<PsiPackage> answer = Collections2.transform(subpackages, new Function<FqName, PsiPackage>() {
            @Override
            public PsiPackage apply(@Nullable FqName input) {
                return new JetLightPackage(psiManager, input, scope);
            }
        });

        return answer.toArray(new PsiPackage[answer.size()]);
    }

    @NotNull
    @Override
    public PsiClass[] getClasses(@NotNull PsiPackage psiPackage, @NotNull GlobalSearchScope scope) {
        List<PsiClass> answer = new SmartList<PsiClass>();
        FqName packageFQN = new FqName(psiPackage.getQualifiedName());

        findPackageClass(packageFQN, scope, answer);

        Collection<JetClassOrObject> declarations = lightClassGenerationSupport.findClassOrObjectDeclarationsInPackage(packageFQN, scope);
        for (JetClassOrObject declaration : declarations) {
            String localName = getLocalName(declaration);
            if (localName != null) {
                JetLightClass aClass = JetLightClass.create(psiManager, (JetFile) declaration.getContainingFile(),
                                                            QualifiedNamesUtil.combine(packageFQN, Name.identifier(localName)));
                if (aClass != null) {
                    answer.add(aClass);
                }
            }
        }

        return answer.toArray(new PsiClass[answer.size()]);
    }

    private synchronized void invalidateJetFilesCache() {
        jetFiles.clear();
    }

    @Deprecated
    private synchronized Collection<JetFile> collectProjectJetFiles(final Project project, @NotNull final GlobalSearchScope scope) {
        Collection<JetFile> cachedFiles = jetFiles.get(scope);

        if (cachedFiles == null) {
            cachedFiles = JetFilesProvider.getInstance(project).allInScope(scope);
            jetFiles.put(scope, cachedFiles);
        }

        return cachedFiles;
    }
}

