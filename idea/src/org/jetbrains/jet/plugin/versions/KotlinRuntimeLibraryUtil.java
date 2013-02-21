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
import com.intellij.facet.impl.DefaultFacetsProvider;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.projectRoots.SdkModificator;
import com.intellij.openapi.roots.AnnotationOrderRootType;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.roots.libraries.LibraryTable;
import com.intellij.openapi.util.Condition;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.JarFileSystem;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.packaging.impl.elements.ManifestFileUtil;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.ProjectScope;
import com.intellij.util.Processor;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.indexing.FileBasedIndex;
import com.intellij.util.indexing.ID;
import com.intellij.util.text.UniqueNameGenerator;
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
import java.util.jar.Attributes;
import java.util.jar.JarFile;

import static org.jetbrains.jet.plugin.project.JsModuleDetector.isJsModule;

public class KotlinRuntimeLibraryUtil {
    public static final String LIBRARY_NAME = "KotlinRuntime";
    public static final String KOTLIN_RUNTIME_JAR = "kotlin-runtime.jar";
    public static final String UNKNOWN_VERSION = "UNKNOWN";

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
        final Collection<VirtualFile> badRoots = Sets.newHashSet();
        final ProjectFileIndex fileIndex = ProjectFileIndex.SERVICE.getInstance(project);
        FileBasedIndex.getInstance().getFilesWithKey(
                id,
                badAbiVersions,
                new Processor<VirtualFile>() {
                    @Override
                    public boolean process(VirtualFile file) {
                        assert file != null;
                        if (!file.isValid()) return true;
                        VirtualFile root = fileIndex.getClassRootForFile(file);
                        if (root != null) {
                            VirtualFile jarFile = JarFileSystem.getInstance().getVirtualFileForJar(root);
                            badRoots.add(jarFile != null ? jarFile : root);
                        }
                        else {
                            badRoots.add(file);
                        }
                        return true;
                    }
                },
                ProjectScope.getLibrariesScope(project)
        );

        return badRoots;
    }

    public static void addJdkAnnotations(@NotNull Module module) {
        Sdk sdk = ModuleRootManager.getInstance(module).getSdk();
        if (sdk == null) {
            return;
        }
        File annotationsIoFile = PathUtil.getKotlinPathsForIdeaPlugin().getJdkAnnotationsPath();
        if (annotationsIoFile.exists()) {
            VirtualFile jdkAnnotationsJar = LocalFileSystem.getInstance().findFileByIoFile(annotationsIoFile);
            if (jdkAnnotationsJar != null) {
                SdkModificator modificator = sdk.getSdkModificator();
                modificator.addRoot(JarFileSystem.getInstance().getJarRootForLocalFile(jdkAnnotationsJar),
                                    AnnotationOrderRootType.getInstance());
                modificator.commitChanges();
            }
        }
    }

    public static boolean jdkAnnotationsArePresent(@NotNull Module module) {
        Sdk sdk = ModuleRootManager.getInstance(module).getSdk();
        if (sdk == null) return false;
        return ContainerUtil.exists(sdk.getRootProvider().getFiles(AnnotationOrderRootType.getInstance()),
                                    new Condition<VirtualFile>() {
                                        @Override
                                        public boolean value(VirtualFile file) {
                                            return PathUtil.JDK_ANNOTATIONS_JAR.equals(file.getName());
                                        }
                                    });
    }

    public static boolean isModuleAlreadyConfigured(Module module) {
        return isMavenModule(module) || isJsModule(module) || isWithJavaModule(module);
    }

    private static boolean isMavenModule(@NotNull Module module) {
        // This constant could be acquired from MavenProjectsManager, but we don't want to depend on the Maven plugin...
        // See MavenProjectsManager.isMavenizedModule()
        return "true".equals(module.getOptionValue("org.jetbrains.idea.maven.project.MavenProjectsManager.isMavenModule"));
    }

    private static boolean isWithJavaModule(Module module) {
        // Can find a reference to kotlin class in module scope
        GlobalSearchScope scope = module.getModuleWithDependenciesAndLibrariesScope(false);

        return getKotlinRuntimeMarkerClass(scope) != null;
    }

    @Nullable
    private static PsiClass getKotlinRuntimeMarkerClass(@NotNull GlobalSearchScope scope) {
        FqName kotlinPackageFqName = FqName.topLevel(Name.identifier("kotlin"));
        String kotlinPackageClassFqName = PackageClassUtils.getPackageClassFqName(kotlinPackageFqName).getFqName();

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

    public static boolean isLibraryCanBeUsedAsJavaRuntime(@Nullable Library library) {
        if (library == null) {
            return false;
        }

        for (VirtualFile root : library.getFiles(OrderRootType.CLASSES)) {
            if (root.getName().equals(KOTLIN_RUNTIME_JAR)) {
                return true;
            }
        }

        return false;
    }

    private static String getUniqueLibraryName(final String baseName, final LibraryTable.ModifiableModel model) {
        return UniqueNameGenerator.generateUniqueName(baseName, "", "", " (", ")", new Condition<String>() {
            @Override
            public boolean value(String s) {
                return model.getLibraryByName(s) == null;
            }
        });
    }

    public static void updateRuntime(
            @NotNull final Project project,
            @NotNull final Runnable jarNotFoundHandler
    ) {
        ApplicationManager.getApplication().invokeLater(new Runnable() {
            @Override
            public void run() {
                File runtimePath = PathUtil.getKotlinPathsForIdeaPlugin().getRuntimePath();
                if (!runtimePath.exists()) {
                    jarNotFoundHandler.run();
                    return;
                }
                VirtualFile runtimeJar = getLocalKotlinRuntimeJar(project);
                assert runtimeJar != null;

                try {
                    FileUtil.copy(runtimePath, new File(runtimeJar.getPath()));
                }
                catch (IOException e) {
                    throw new AssertionError(e);
                }
                runtimeJar.refresh(true, true);
            }
        });
    }

    @Nullable
    public static String getRuntimeVersion(@NotNull Project project) {
        VirtualFile kotlinRuntimeJar = getKotlinRuntimeJar(project);
        if (kotlinRuntimeJar == null) return null;
        VirtualFile manifestFile = kotlinRuntimeJar.findFileByRelativePath(JarFile.MANIFEST_NAME);
        if (manifestFile != null) {
            Attributes attributes = ManifestFileUtil.readManifest(manifestFile).getMainAttributes();
            if (attributes.containsKey(Attributes.Name.IMPLEMENTATION_VERSION)) {
                return attributes.getValue(Attributes.Name.IMPLEMENTATION_VERSION);
            }
        }
        return UNKNOWN_VERSION;
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
}
