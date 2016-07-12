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

package org.jetbrains.kotlin.idea.configuration;

import com.intellij.codeInsight.CodeInsightUtilCore;
import com.intellij.ide.actions.OpenFileAction;
import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.openapi.application.Result;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ContentEntry;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.SourceFolder;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.WritingAccessProvider;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.search.FileTypeIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.xml.XmlFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.idea.maven.dom.MavenDomUtil;
import org.jetbrains.idea.maven.dom.model.*;
import org.jetbrains.idea.maven.model.MavenId;
import org.jetbrains.idea.maven.project.MavenProjectsManager;
import org.jetbrains.idea.maven.utils.MavenArtifactScope;
import org.jetbrains.jps.model.java.JavaSourceRootType;
import org.jetbrains.kotlin.idea.KotlinPluginUtil;
import org.jetbrains.kotlin.idea.framework.ui.ConfigureDialogWithModulesAndVersion;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public abstract class KotlinMavenConfigurator implements KotlinProjectConfigurator {
    public static final String NAME = "maven";

    private static final String GROUP_ID = "org.jetbrains.kotlin";
    private static final String MAVEN_PLUGIN_ID = "kotlin-maven-plugin";
    private static final String KOTLIN_VERSION_PROPERTY = "kotlin.version";

    private static final String PROCESS_TEST_SOURCES_PHASE = "process-test-sources";
    private static final String PROCESS_SOURCES_PHASE = "process-sources";
    private static final String TEST_COMPILE_PHASE = "test-compile";
    private static final String COMPILE_PHASE = "compile";
    private static final String TEST_COMPILE_GOAL = "test-compile";
    private static final String COMPILE_GOAL = "compile";
    private static final String TEST_COMPILE_EXECUTION_ID = "test-compile";
    private static final String COMPILE_EXECUTION_ID = "compile";

    private final String stdlibArtifactId;
    private final String testArtifactId;
    private final boolean addJunit;
    private final String name;
    private final String presentableText;

    protected KotlinMavenConfigurator(@NotNull String stdlibArtifactId, @Nullable String testArtifactId, boolean addJunit, @NotNull String name, @NotNull String presentableText) {
        this.stdlibArtifactId = stdlibArtifactId;
        this.testArtifactId = testArtifactId;
        this.addJunit = addJunit;
        this.name = name;
        this.presentableText = presentableText;
    }

    @Override
    public boolean isApplicable(@NotNull Module module) {
        return KotlinPluginUtil.isMavenModule(module);
    }

    @NotNull
    @Override
    public String getPresentableText() {
        return presentableText;
    }

    @NotNull
    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean isConfigured(@NotNull Module module) {
        if (isKotlinModule(module)) {
            return true;
        }

        PsiFile psi = findModulePomFile(module);
        if (psi == null
            || !psi.isValid()
            || !(psi instanceof XmlFile)
            || psi.getVirtualFile() == null
            || MavenDomUtil.getMavenDomProjectModel(module.getProject(), psi.getVirtualFile()) == null) {
            return false;
        }

        PomFile pom = new PomFile((XmlFile) psi);

        return pom.hasPlugin(new MavenId(GROUP_ID, MAVEN_PLUGIN_ID, null)) && pom.hasDependency(new MavenId(GROUP_ID, stdlibArtifactId, null), MavenArtifactScope.COMPILE);
    }

    @Override
    public void configure(@NotNull Project project, Collection<Module> excludeModules) {
        ConfigureDialogWithModulesAndVersion dialog =
                new ConfigureDialogWithModulesAndVersion(project, this, excludeModules);

        dialog.show();
        if (!dialog.isOK()) return;

        NotificationMessageCollector collector = NotificationMessageCollectorKt.createConfigureKotlinNotificationCollector(project);
        for (Module module : MavenModulesRelationshipKt.excludeMavenChildrenModules(project, dialog.getModulesToConfigure())) {
            PsiFile file = findModulePomFile(module);
            if (file != null && canConfigureFile(file)) {
                changePomFile(module, file, dialog.getKotlinVersion(), collector);
                OpenFileAction.openFile(file.getVirtualFile(), project);
            }
            else {
                showErrorMessage(project, "Cannot find pom.xml for module " + module.getName());
            }
        }
        collector.showNotification();
    }

    protected abstract boolean isKotlinModule(@NotNull Module module);

    protected abstract void createExecutions(@NotNull PomFile pomFile, @NotNull MavenDomPlugin kotlinPlugin, @NotNull Module module);

    @NotNull
    protected String getGoal(boolean isTest) {
        return isTest ? TEST_COMPILE_GOAL : COMPILE_GOAL;
    }

    @NotNull
    protected String getExecutionId(boolean isTest) {
        return isTest ? TEST_COMPILE_EXECUTION_ID : COMPILE_EXECUTION_ID;
    }

    protected void changePomFile(
            @NotNull final Module module,
            final @NotNull PsiFile file,
            @NotNull final String version,
            @NotNull NotificationMessageCollector collector
    ) {
        VirtualFile virtualFile = file.getVirtualFile();
        assert virtualFile != null : "Virtual file should exists for psi file " + file.getName();
        MavenDomProjectModel domModel = MavenDomUtil.getMavenDomProjectModel(module.getProject(), virtualFile);
        if (domModel == null) {
            showErrorMessage(module.getProject(), null);
            return;
        }

        new WriteCommandAction(file.getProject()) {
            @Override
            protected void run(@NotNull Result result) {
                PomFile pom = new PomFile((XmlFile) file);
                pom.addProperty(KOTLIN_VERSION_PROPERTY, version);

                pom.addDependency(new MavenId(GROUP_ID, stdlibArtifactId, "${" + KOTLIN_VERSION_PROPERTY + "}"), MavenArtifactScope.COMPILE, null, false, null);
                if (testArtifactId != null) {
                    pom.addDependency(new MavenId(GROUP_ID, testArtifactId, "${" + KOTLIN_VERSION_PROPERTY + "}"), MavenArtifactScope.TEST, null, false, null);
                }
                if (addJunit) {
                    pom.addDependency(new MavenId("junit", "junit", "4.12"), MavenArtifactScope.TEST, null, false, null);
                }

                if (isSnapshot(version)) {
                    pom.addLibraryRepository(ConfigureKotlinInProjectUtilsKt.SNAPSHOT_REPOSITORY, true, false);
                    pom.addPluginRepository(ConfigureKotlinInProjectUtilsKt.SNAPSHOT_REPOSITORY, true, false);
                }
                if (ConfigureKotlinInProjectUtilsKt.isEap(version)) {
                    pom.addLibraryRepository(ConfigureKotlinInProjectUtilsKt.EAP_REPOSITORY, true, false);
                    pom.addPluginRepository(ConfigureKotlinInProjectUtilsKt.EAP_REPOSITORY, true, false);
                }

                MavenDomPlugin plugin = pom.addPlugin(new MavenId(GROUP_ID, MAVEN_PLUGIN_ID, "${" + KOTLIN_VERSION_PROPERTY + "}"));
                createExecutions(pom, plugin, module);

                CodeInsightUtilCore.forcePsiPostprocessAndRestoreElement(file);
            }
        }.execute();

        collector.addMessage(virtualFile.getPath() + " was modified");
    }

    protected void createExecution(
            @NotNull PomFile pomFile,
            @NotNull MavenDomPlugin kotlinPlugin,
            @NotNull Module module,
            boolean isTest
    ) {
        MavenDomPluginExecution execution = pomFile.addExecution(kotlinPlugin, getExecutionId(isTest), getPhase(module, isTest), Collections.singletonList(getGoal(isTest)));

        List<String> sourceDirs = new ArrayList<String>();
        for (ContentEntry contentEntry : ModuleRootManager.getInstance(module).getContentEntries()) {
            SourceFolder[] folders = contentEntry.getSourceFolders();
            for (SourceFolder sourceFolder : folders) {
                if (isRelatedSourceRoot(isTest, sourceFolder)) {
                    VirtualFile sourceFolderFile = sourceFolder.getFile();
                    if (sourceFolderFile != null) {
                        String relativePath = VfsUtilCore.getRelativePath(sourceFolderFile, pomFile.getXmlFile().getVirtualFile().getParent(), '/');
                        sourceDirs.add(relativePath);
                    }
                }
            }
        }

        pomFile.executionSourceDirs(execution, sourceDirs);
    }

    private static boolean isRelatedSourceRoot(boolean isTest, SourceFolder folder) {
        return isTest && folder.getRootType() == JavaSourceRootType.TEST_SOURCE ||
               (!isTest && folder.getRootType() == JavaSourceRootType.SOURCE);
    }

    @NotNull
    private static String getPhase(@NotNull Module module, boolean isTest) {
        if (hasJavaFiles(module)) {
            return isTest ? PROCESS_TEST_SOURCES_PHASE : PROCESS_SOURCES_PHASE;
        }
        return isTest ? TEST_COMPILE_PHASE : COMPILE_PHASE;
    }

    private static boolean hasJavaFiles(@NotNull Module module) {
        return !FileTypeIndex.getFiles(JavaFileType.INSTANCE, GlobalSearchScope.moduleScope(module)).isEmpty();
    }

    @Nullable
    private static PsiFile findModulePomFile(@NotNull Module module) {
        List<VirtualFile> files = MavenProjectsManager.getInstance(module.getProject()).getProjectsFiles();
        for (VirtualFile file : files) {
            Module fileModule = ModuleUtilCore.findModuleForFile(file, module.getProject());
            if (!module.equals(fileModule)) continue;
            PsiFile psiFile = PsiManager.getInstance(module.getProject()).findFile(file);
            if (psiFile == null) continue;
            if (!MavenDomUtil.isProjectFile(psiFile)) continue;
            return psiFile;
        }
        return null;
    }

    private static boolean isSnapshot(@NotNull String version) {
        return version.contains("SNAPSHOT");
    }

    private static boolean canConfigureFile(@NotNull PsiFile file) {
        return WritingAccessProvider.isPotentiallyWritable(file.getVirtualFile(), null);
    }

    private static void showErrorMessage(@NotNull Project project, @Nullable String message) {
        Messages.showErrorDialog(project,
                                 "<html>Couldn't configure kotlin-maven plugin automatically.<br/>" +
                                 (message != null ? message : "") +
                                 "See manual installation instructions <a href=\"http://confluence.jetbrains.com/display/Kotlin/Kotlin+Build+Tools#KotlinBuildTools-Maven\">here</a></html>",
                                 "Configure Kotlin-Maven Plugin");
    }
}
