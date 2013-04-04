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

package org.jetbrains.jet.lang.resolve.java;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiPackage;
import com.intellij.psi.search.DelegatingGlobalSearchScope;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.resolve.java.resolver.JavaAnnotationResolver;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;
import org.jetbrains.jet.plugin.JetFileType;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.util.List;
import java.util.Set;

public class PsiClassFinderImpl implements PsiClassFinder {
    private static final Logger LOG = Logger.getInstance(PsiClassFinderImpl.class);

    @NotNull
    private Project project;

    private GlobalSearchScope javaSearchScope;
    private JavaPsiFacadeKotlinHacks javaFacade;

    @Inject
    public void setProject(@NotNull Project project) {
        this.project = project;
    }

    @PostConstruct
    public void initialize() {
        javaSearchScope = new DelegatingGlobalSearchScope(GlobalSearchScope.allScope(project)) {
            @Override
            public boolean contains(VirtualFile file) {
                return myBaseScope.contains(file) && file.getFileType() != JetFileType.INSTANCE;
            }

            @Override
            public int compare(VirtualFile file1, VirtualFile file2) {
                // TODO: this is a hackish workaround for the following problem:
                // since we are working with the allScope(), if the same class FqName
                // to be on the class path twice, because it is included into different libraries
                // (e.g. junit-4.0.jar is used as a separate library and as a part of idea_full)
                // the two libraries are attached to different modules, the parent compare()
                // can't tell which one comes first, so they can come in random order
                // To fix this, we sort additionally by the full path, to make the ordering deterministic
                // TODO: Delete this hack when proper scopes are used
                int compare = super.compare(file1, file2);
                if (compare == 0) {
                    return Comparing.compare(file1.getPath(), file2.getPath());
                }
                return compare;
            }
        };
        javaFacade = new JavaPsiFacadeKotlinHacks(project);
    }


    @Override
    @Nullable
    public PsiClass findPsiClass(@NotNull FqName qualifiedName, @NotNull RuntimeClassesHandleMode runtimeClassesHandleMode) {
        PsiClass original = javaFacade.findClass(qualifiedName.getFqName(), javaSearchScope);

        if (original != null) {
            String classQualifiedName = original.getQualifiedName();
            FqName actualQualifiedName = classQualifiedName != null ? new FqName(classQualifiedName) : null;
            if (!qualifiedName.equals(actualQualifiedName)) {
                throw new IllegalStateException("requested " + qualifiedName + ", got " + actualQualifiedName);
            }
        }

        if (original instanceof JetJavaMirrorMarker) {
            throw new IllegalStateException("JetJavaMirrorMaker is not possible in resolve.java, resolving: " + qualifiedName);
        }

        if (original == null) {
            return null;
        }

        if (KotlinBuiltIns.BUILT_INS_PACKAGE_FQ_NAME.equals(qualifiedName.parent())) {
            PsiAnnotation assertInvisibleAnnotation = JavaAnnotationResolver.findOwnAnnotation(
                    original, JvmStdlibNames.ASSERT_INVISIBLE_IN_RESOLVER.getFqName().getFqName());

            if (assertInvisibleAnnotation != null) {
                switch (runtimeClassesHandleMode) {
                    case IGNORE:
                        break;
                    case REPORT_ERROR:
                        if (ApplicationManager.getApplication().isInternal()) {
                            LOG.error("classpath is configured incorrectly:" +
                                      " class " + qualifiedName + " from runtime must not be loaded by compiler");
                        }
                        break;
                    default:
                        throw new IllegalStateException("unknown parameter value: " + runtimeClassesHandleMode);
                }
                return null;
            }
        }

        return original;
    }

    @Override
    @Nullable
    public PsiPackage findPsiPackage(@NotNull FqName qualifiedName) {
        return javaFacade.findPackage(qualifiedName.getFqName());
    }

    @NotNull
    @Override
    public List<PsiClass> findPsiClasses(@NotNull PsiPackage psiPackage) {
        return filterDuplicateClasses(psiPackage.getClasses());
    }

    @NotNull
    @Override
    public List<PsiClass> findInnerPsiClasses(@NotNull PsiClass psiClass) {
        return filterDuplicateClasses(psiClass.getInnerClasses());
    }

    private static List<PsiClass> filterDuplicateClasses(PsiClass[] classes) {
        Set<String> addedQualifiedNames = Sets.newHashSet();
        List<PsiClass> filteredClasses = Lists.newArrayList();

        for (PsiClass aClass : classes) {
            String qualifiedName = aClass.getQualifiedName();

            if (qualifiedName != null) {
                if (addedQualifiedNames.add(qualifiedName)) {
                    filteredClasses.add(aClass);
                }
            }
        }

        return filteredClasses;
    }
}
