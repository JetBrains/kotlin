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

/*
 * @author max
 */
package org.jetbrains.jet.asJava;

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
import org.jetbrains.jet.codegen.JetTypeMapper;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.FqName;
import org.jetbrains.jet.lang.resolve.java.JvmAbi;
import org.jetbrains.jet.util.QualifiedNamesUtil;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;

public class JavaElementFinder extends PsiElementFinder {
    private final Project project;
    private final PsiManager psiManager;

    private WeakHashMap<GlobalSearchScope, List<JetFile>> jetFiles = new WeakHashMap<GlobalSearchScope, List<JetFile>>();

    public JavaElementFinder(Project project) {
        this.project = project;
        psiManager = PsiManager.getInstance(project);

        // Monitoring for files instead of collecting them each time
        VirtualFileManager.getInstance().addVirtualFileListener(new VirtualFileAdapter() {
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
        FqName qualifiedName = new FqName(qualifiedNameString);

        // Backend searches for java.lang.String. Will fail with SOE if continue
        if (qualifiedName.getFqName().startsWith("java.")) return PsiClass.EMPTY_ARRAY;

        List<PsiClass> answer = new SmartList<PsiClass>();
        final List<JetFile> filesInScope = collectProjectJetFiles(project, scope);
        for (JetFile file : filesInScope) {
            final FqName packageName = JetPsiUtil.getFQName(file);
            if (packageName != null && qualifiedName.getFqName().startsWith(packageName.getFqName())) {
                if (qualifiedName.equals(QualifiedNamesUtil.combine(packageName, JvmAbi.PACKAGE_CLASS))) {
                    answer.add(new JetLightClass(psiManager, file, qualifiedName));
                }
                else {
                    for (JetDeclaration declaration : file.getDeclarations()) {
                        scanClasses(answer, declaration, qualifiedName, packageName, file);
                    }
                }
            }
        }
        return answer.toArray(new PsiClass[answer.size()]);
    }

    private void scanClasses(List<PsiClass> answer, JetDeclaration declaration, FqName qualifiedName, FqName containerFqn, JetFile file) {
        if (declaration instanceof JetClassOrObject) {
            String localName = getLocalName(declaration);
            if (localName != null) {
                FqName fqn = QualifiedNamesUtil.combine(containerFqn, localName);
                if (qualifiedName.equals(fqn)) {
                    answer.add(new JetLightClass(psiManager, file, qualifiedName));
                }
                else {
                    for (JetDeclaration child : ((JetClassOrObject) declaration).getDeclarations()) {
                        scanClasses(answer, child, qualifiedName, fqn, file);
                    }
                }
            }
        }
        else if (declaration instanceof JetClassObject) {
            scanClasses(answer, ((JetClassObject) declaration).getObjectDeclaration(), qualifiedName, containerFqn, file);
        }
    }

    @Nullable
    private static String getLocalName(JetDeclaration declaration) {
        String given = declaration.getName();
        if (given != null) return given;

        if (declaration instanceof JetObjectDeclaration) {
            return JetTypeMapper.getLocalNameForObject((JetObjectDeclaration) declaration);
        }

        return null;
    }

    @Override
    public Set<String> getClassNames(@NotNull PsiPackage psiPackage, @NotNull GlobalSearchScope scope) {
        Set<String> answer = new HashSet<String>();

        FqName packageFQN = new FqName(psiPackage.getQualifiedName());
        for (JetFile psiFile : collectProjectJetFiles(project, GlobalSearchScope.allScope(project))) {
            if (packageFQN.equals(JetPsiUtil.getFQName(psiFile))) {
                answer.add(JvmAbi.PACKAGE_CLASS);
                for (JetDeclaration declaration : psiFile.getDeclarations()) {
                    if (declaration instanceof JetClassOrObject) {
                        answer.add(getLocalName(declaration));
                    }
                }
            }
        }

        return answer;
    }

    @Override
    public PsiPackage findPackage(@NotNull String qualifiedNameString) {
        FqName fqName = new FqName(qualifiedNameString);

        final List<JetFile> psiFiles = collectProjectJetFiles(project, GlobalSearchScope.allScope(project));

        for (JetFile psiFile : psiFiles) {
            if (QualifiedNamesUtil.isSubpackageOf(JetPsiUtil.getFQName(psiFile), fqName)) {
                return new JetLightPackage(psiManager, fqName, psiFile.getNamespaceHeader());
            }
        }

        return null;
    }

    @NotNull
    @Override
    public PsiPackage[] getSubPackages(@NotNull PsiPackage psiPackage, @NotNull GlobalSearchScope scope) {
        final List<JetFile> psiFiles = collectProjectJetFiles(project, GlobalSearchScope.allScope(project));

        Set<PsiPackage> answer = new HashSet<PsiPackage>();

        for (JetFile psiFile : psiFiles) {
            FqName jetRootNamespace = JetPsiUtil.getFQName(psiFile);

            final FqName subPackageFQN = QualifiedNamesUtil.plusOneSegment(new FqName(psiPackage.getQualifiedName()), jetRootNamespace);
            if (subPackageFQN != null) {
                answer.add(new JetLightPackage(psiManager, subPackageFQN, psiFile.getNamespaceHeader()));
            }
        }

        return answer.toArray(new PsiPackage[answer.size()]);
    }

    @NotNull
    @Override
    public PsiClass[] getClasses(@NotNull PsiPackage psiPackage, @NotNull GlobalSearchScope scope) {
        List<PsiClass> answer = new SmartList<PsiClass>();
        final List<JetFile> filesInScope = collectProjectJetFiles(project, scope);
        FqName packageFQN = new FqName(psiPackage.getQualifiedName());
        for (JetFile file : filesInScope) {
            if (packageFQN.equals(JetPsiUtil.getFQName(file))) {
                answer.add(new JetLightClass(psiManager, file, QualifiedNamesUtil.combine(packageFQN, JvmAbi.PACKAGE_CLASS)));
                for (JetDeclaration declaration : file.getDeclarations()) {
                    if (declaration instanceof JetClassOrObject) {
                        String localName = getLocalName(declaration);
                        if (localName != null) {
                            answer.add(new JetLightClass(psiManager, file, QualifiedNamesUtil.combine(packageFQN, localName)));
                        }
                    }
                }
            }
        }

        return answer.toArray(new PsiClass[answer.size()]);
    }

    private synchronized void invalidateJetFilesCache() {
        jetFiles.clear();
    }

    private synchronized List<JetFile> collectProjectJetFiles(final Project project, @NotNull final GlobalSearchScope scope) {
        List<JetFile> cachedFiles = jetFiles.get(scope);
        
        if (cachedFiles == null) {
            cachedFiles = JetFileUtil.collectJetFiles(project, scope);
             jetFiles.put(scope, cachedFiles);
        }

        return cachedFiles;
    }
}

