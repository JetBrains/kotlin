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

package org.jetbrains.jet.plugin.versions;

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.projectRoots.SdkModificator;
import com.intellij.openapi.roots.AnnotationOrderRootType;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.JarFileSystem;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.ProjectScope;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.indexing.FileBasedIndex;
import com.intellij.util.indexing.ID;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.resolve.java.AbiVersionUtil;
import org.jetbrains.jet.lang.resolve.java.PackageClassUtils;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.utils.PathUtil;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Set;

import static com.intellij.util.PathUtil.getLocalFile;
import static com.intellij.util.PathUtil.getLocalPath;

public class KotlinRuntimeLibraryUtil {
    private KotlinRuntimeLibraryUtil() {}

    @NotNull
    public static Collection<VirtualFile> getLibraryRootsWithAbiIncompatibleKotlinClasses(@NotNull Project project) {
        ID<Integer,Void> id = KotlinAbiVersionIndex.INSTANCE.getName();
        Collection<Integer> abiVersions = FileBasedIndex.getInstance().getAllKeys(id, project);
        Set<Integer> badAbiVersions = Sets.newHashSet(Collections2.filter(abiVersions, new Predicate<Integer>() {
            @Override
            public boolean apply(Integer abiVersion) {
                return !AbiVersionUtil.isAbiVersionCompatible(abiVersion);
            }
        }));
        Set<VirtualFile> badRoots = Sets.newHashSet();
        ProjectFileIndex fileIndex = ProjectFileIndex.SERVICE.getInstance(project);

        for (Integer version : badAbiVersions) {
            Collection<VirtualFile> indexedFiles = FileBasedIndex.getInstance().getContainingFiles(id, version, ProjectScope.getLibrariesScope(project));

            for (VirtualFile indexedFile : indexedFiles) {
                VirtualFile libraryRoot = fileIndex.getClassRootForFile(indexedFile);
                assert libraryRoot != null : "Only library roots were requested, " +
                                             "and only class files should be indexed with KotlinAbiVersionIndex key. " +
                                             "File: " + libraryRoot;
                badRoots.add(getLocalFile(libraryRoot));
            }
        }

        return badRoots;
    }

    public static void addJdkAnnotations(@NotNull Module module) {
        addAnnotations(module, PathUtil.getKotlinPathsForIdeaPlugin().getJdkAnnotationsPath());
    }

    public static void addAndroidSdkAnnotations(@NotNull Module module) {
        addAnnotations(module, PathUtil.getKotlinPathsForIdeaPlugin().getAndroidSdkAnnotationsPath());
    }

    private static void addAnnotations(@NotNull Module module, @NotNull File annotationsPath) {
        Sdk sdk = ModuleRootManager.getInstance(module).getSdk();
        if (sdk == null) {
            return;
        }
        if (annotationsPath.exists()) {
            VirtualFile jdkAnnotationsJar = LocalFileSystem.getInstance().findFileByIoFile(annotationsPath);
            if (jdkAnnotationsJar != null) {
                SdkModificator modificator = sdk.getSdkModificator();
                modificator.addRoot(JarFileSystem.getInstance().getJarRootForLocalFile(jdkAnnotationsJar),
                                    AnnotationOrderRootType.getInstance());
                modificator.commitChanges();
            }
        }
    }

    public static boolean jdkAnnotationsArePresent(@NotNull Module module) {
        return areAnnotationsPresent(module, PathUtil.JDK_ANNOTATIONS_JAR);
    }

    public static boolean androidSdkAnnotationsArePresent(@NotNull Module module) {
        return areAnnotationsPresent(module, PathUtil.ANDROID_SDK_ANNOTATIONS_JAR);
    }

    private static boolean areAnnotationsPresent(@NotNull Module module, @NotNull final String jarFileName) {
        Sdk sdk = ModuleRootManager.getInstance(module).getSdk();
        if (sdk == null) return false;
        return ContainerUtil.exists(sdk.getRootProvider().getFiles(AnnotationOrderRootType.getInstance()),
                                    new Condition<VirtualFile>() {
                                        @Override
                                        public boolean value(VirtualFile file) {
                                            return jarFileName.equals(file.getName());
                                        }
                                    });
    }

    @Nullable
    public static PsiClass getKotlinRuntimeMarkerClass(@NotNull GlobalSearchScope scope) {
        FqName kotlinPackageFqName = FqName.topLevel(Name.identifier("kotlin"));
        String kotlinPackageClassFqName = PackageClassUtils.getPackageClassFqName(kotlinPackageFqName).asString();

        ImmutableList<String> candidateClassNames = ImmutableList.of(
                kotlinPackageClassFqName,
                // For older versions
                "kotlin.namespace",
                // For really old versions
                "jet.JetObject"
        );

        for (String className : candidateClassNames) {
            PsiClass psiClass = JavaPsiFacade.getInstance(scope.getProject()).findClass(className, scope);
            if (psiClass != null) {
                return psiClass;
            }
        }
        return null;
    }

    public static void updateRuntime(@NotNull final Project project, @NotNull final Runnable jarNotFoundHandler) {
        ApplicationManager.getApplication().invokeLater(new Runnable() {
            @Override
            public void run() {
                File runtimePath = PathUtil.getKotlinPathsForIdeaPlugin().getRuntimePath();
                if (!runtimePath.exists()) {
                    jarNotFoundHandler.run();
                    return;
                }

                VirtualFile localJar = getLocalKotlinRuntimeJar(project);
                assert localJar != null;

                replaceFile(runtimePath, localJar);
            }
        });
    }

    @Nullable
    private static VirtualFile getKotlinRuntimeJar(@NotNull Project project) {
        PsiClass markerClass = getKotlinRuntimeMarkerClass(ProjectScope.getAllScope(project));
        if (markerClass == null) return null;

        VirtualFile virtualFile = markerClass.getContainingFile().getVirtualFile();
        if (virtualFile == null) return null;

        ProjectFileIndex projectFileIndex = ProjectFileIndex.SERVICE.getInstance(project);
        return projectFileIndex.getClassRootForFile(virtualFile);
    }

    @Nullable
    public static VirtualFile getLocalKotlinRuntimeJar(@NotNull Project project) {
        VirtualFile kotlinRuntimeJar = getKotlinRuntimeJar(project);
        if (kotlinRuntimeJar == null) return null;

        VirtualFile localJarFile = JarFileSystem.getInstance().getVirtualFileForJar(kotlinRuntimeJar);
        if (localJarFile != null) {
            return localJarFile;
        }
        return kotlinRuntimeJar;
    }

    static void replaceFile(File updatedFile, VirtualFile replacedJarFile) {
        try {
            VirtualFile replacedFile = getLocalFile(replacedJarFile);

            String localPath = getLocalPath(replacedFile);
            assert localPath != null : "Should be called for replacing valid root file: " + replacedJarFile;

            File libraryJarPath = new File(localPath);

            if (FileUtil.filesEqual(updatedFile, libraryJarPath)) {
                throw new IllegalArgumentException("Shouldn't be called for updating same file: " + updatedFile);
            }

            FileUtil.copy(updatedFile, libraryJarPath);
            replacedFile.refresh(false, true);
        }
        catch (IOException e) {
            throw new AssertionError(e);
        }
    }
}
