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
import com.intellij.psi.xml.XmlTag;
import com.intellij.util.xml.DomElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.idea.maven.dom.MavenDomUtil;
import org.jetbrains.idea.maven.dom.model.*;
import org.jetbrains.idea.maven.project.MavenProjectsManager;
import org.jetbrains.jps.model.java.JavaSourceRootType;
import org.jetbrains.kotlin.cli.common.KotlinVersion;
import org.jetbrains.kotlin.idea.KotlinPluginUtil;
import org.jetbrains.kotlin.idea.framework.ui.ConfigureDialogWithModulesAndVersion;

import java.util.List;

public abstract class KotlinMavenConfigurator implements KotlinProjectConfigurator {
    private static final String[] KOTLIN_VERSIONS = {KotlinVersion.VERSION};

    public static final String NAME = "maven";

    private static final String GROUP_ID = "org.jetbrains.kotlin";
    private static final String MAVEN_PLUGIN_ID = "kotlin-maven-plugin";
    private static final String KOTLIN_VERSION_PROPERTY = "kotlin.version";
    private static final String SNAPSHOT_REPOSITORY_ID = "sonatype.oss.snapshots";

    private static final String PROCESS_TEST_SOURCES_PHASE = "process-test-sources";
    private static final String PROCESS_SOURCES_PHASE = "process-sources";
    private static final String TEST_COMPILE_PHASE = "test-compile";
    private static final String COMPILE_PHASE = "compile";
    private static final String TEST_COMPILE_GOAL = "test-compile";
    private static final String COMPILE_GOAL = "compile";
    private static final String TEST_COMPILE_EXECUTION_ID = "test-compile";
    private static final String COMPILE_EXECUTION_ID = "compile";

    private final String libraryId;
    private final String name;
    private final String presentableText;

    protected KotlinMavenConfigurator(@NotNull String libraryId, @NotNull String name, @NotNull String presentableText) {
        this.libraryId = libraryId;
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

        MavenDomProjectModel domProjectModel = getMavenDomProjectModel(module);
        if (domProjectModel == null) return false;

        return hasKotlinMavenPlugin(domProjectModel) && hasDependencyOnLibrary(domProjectModel);
    }

    @Override
    public void configure(@NotNull Project project) {
        List<Module> nonConfiguredModules = ConfigureKotlinInProjectUtils.getNonConfiguredModules(project, this);

        ConfigureDialogWithModulesAndVersion dialog =
                new ConfigureDialogWithModulesAndVersion(project, nonConfiguredModules, KOTLIN_VERSIONS);

        dialog.show();
        if (!dialog.isOK()) return;

        for (Module module : dialog.getModulesToConfigure()) {
            PsiFile file = findModulePomFile(module);
            if (file != null && canConfigureFile(file)) {
                changePomFile(module, file, dialog.getKotlinVersion());
                OpenFileAction.openFile(file.getVirtualFile(), project);
            }
            else {
                showErrorMessage(project, "Cannot find pom.xml for module " + module.getName());
            }
        }
    }

    protected abstract boolean isKotlinModule(@NotNull Module module);

    protected abstract void createExecutions(VirtualFile virtualFile, MavenDomPlugin kotlinPlugin, Module module);

    @NotNull
    protected String getGoal(boolean isTest) {
        return isTest ? TEST_COMPILE_GOAL : COMPILE_GOAL;
    }

    @NotNull
    protected String getExecutionId(boolean isTest) {
        return isTest ? TEST_COMPILE_EXECUTION_ID : COMPILE_EXECUTION_ID;
    }

    protected void changePomFile(@NotNull final Module module, final @NotNull PsiFile file, @NotNull final String version) {
        final VirtualFile virtualFile = file.getVirtualFile();
        assert virtualFile != null : "Virtual file should exists for psi file " + file.getName();
        final MavenDomProjectModel domModel = MavenDomUtil.getMavenDomProjectModel(module.getProject(), virtualFile);
        if (domModel == null) {
            showErrorMessage(module.getProject(), null);
            return;
        }
        new WriteCommandAction(file.getProject()) {
            @Override
            protected void run(@NotNull Result result) {
                addKotlinVersionPropertyIfNeeded(domModel, version);

                if (isSnapshot(version)) {
                    addPluginRepositoryIfNeeded(domModel);
                    addLibraryRepositoryIfNeeded(domModel);
                }

                addPluginIfNeeded(domModel, module, virtualFile);
                addLibraryDependencyIfNeeded(domModel);

                CodeInsightUtilCore.forcePsiPostprocessAndRestoreElement(file);
            }
        }.execute();

        ConfigureKotlinInProjectUtils.showInfoNotification(module.getProject(), virtualFile.getPath() + " was modified");
    }

    protected void createExecution(
            @NotNull VirtualFile virtualFile,
            @NotNull MavenDomPlugin kotlinPlugin,
            @NotNull Module module,
            boolean isTest
    ) {
        MavenDomPluginExecution execution = kotlinPlugin.getExecutions().addExecution();
        String tagValue = getExecutionId(isTest);
        execution.getId().setStringValue(tagValue);
        execution.getPhase().setStringValue(getPhase(module, isTest));
        createTagIfNeeded(execution.getGoals(), "goal", getGoal(isTest));

        XmlTag sourcesTag = createTagIfNeeded(execution.getConfiguration(), "sourceDirs", "");

        for (ContentEntry contentEntry : ModuleRootManager.getInstance(module).getContentEntries()) {
            SourceFolder[] folders = contentEntry.getSourceFolders();
            for (SourceFolder sourceFolder : folders) {
                if (isRelatedSourceRoot(isTest, sourceFolder)) {
                    VirtualFile sourceFolderFile = sourceFolder.getFile();
                    if (sourceFolderFile != null) {
                        String relativePath = VfsUtilCore.getRelativePath(sourceFolderFile, virtualFile.getParent(), '/');
                        XmlTag newTag = sourcesTag.createChildTag("source", sourcesTag.getNamespace(), relativePath, false);
                        sourcesTag.addSubTag(newTag, true);
                    }
                }
            }
        }
    }

    private static boolean isRelatedSourceRoot(boolean isTest, SourceFolder folder) {
        return isTest && folder.getRootType() == JavaSourceRootType.TEST_SOURCE ||
               (!isTest && folder.getRootType() == JavaSourceRootType.SOURCE);
    }

    @Nullable
    private static MavenDomProjectModel getMavenDomProjectModel(@NotNull Module module) {
        PsiFile pomFile = findModulePomFile(module);
        if (pomFile == null) return null;

        VirtualFile virtualFile = pomFile.getVirtualFile();
        assert virtualFile != null : "Virtual file should exists for psi file " + pomFile.getName();

        MavenDomProjectModel domModel = MavenDomUtil.getMavenDomProjectModel(pomFile.getProject(), virtualFile);
        assert domModel != null : "maven dom model should not be null";
        return domModel;
    }

    private static boolean checkCoordinates(
            @NotNull MavenDomShortArtifactCoordinates mavenDomElement,
            @NotNull String groupId,
            @NotNull String artifactId
    ) {
        return groupId.equals(mavenDomElement.getGroupId().getRawText()) && artifactId.equals(mavenDomElement.getArtifactId().getRawText());
    }

    private static boolean hasKotlinMavenPlugin(@NotNull MavenDomProjectModel domModel) {
        for(MavenDomPlugin mavenDomPlugin : domModel.getBuild().getPlugins().getPlugins()) {
            if (checkCoordinates(mavenDomPlugin, GROUP_ID, MAVEN_PLUGIN_ID)) return true;
        }

        return false;
    }

    private boolean hasDependencyOnLibrary(@NotNull MavenDomProjectModel domModel) {
        for(MavenDomDependency mavenDomDependency : domModel.getDependencies().getDependencies()) {
            if (checkCoordinates(mavenDomDependency, GROUP_ID, libraryId)) return true;
        }

        return false;
    }

    private static void addKotlinVersionPropertyIfNeeded(MavenDomProjectModel domModel, String version) {
        createTagIfNeeded(domModel.getProperties(), KOTLIN_VERSION_PROPERTY, version);
    }

    private static void addLibraryRepositoryIfNeeded(MavenDomProjectModel domModel) {
        MavenDomRepositories repositories = domModel.getRepositories();
        if (!isRepositoryConfigured(repositories.getRepositories())) {
            MavenDomRepository newPluginRepository = repositories.addRepository();
            configureRepository(newPluginRepository);
        }
    }

    private static void addPluginRepositoryIfNeeded(MavenDomProjectModel domModel) {
        MavenDomPluginRepositories pluginRepositories = domModel.getPluginRepositories();
        if (!isRepositoryConfigured(pluginRepositories.getPluginRepositories())) {
            MavenDomRepository newPluginRepository = pluginRepositories.addPluginRepository();
            configureRepository(newPluginRepository);
        }
    }

    private void addLibraryDependencyIfNeeded(MavenDomProjectModel domModel) {
        for (MavenDomDependency dependency : domModel.getDependencies().getDependencies()) {
            if (libraryId.equals(dependency.getArtifactId().getStringValue())) {
                return;
            }
        }

        MavenDomDependency dependency = MavenDomUtil.createDomDependency(domModel, null);
        dependency.getGroupId().setStringValue("org.jetbrains.kotlin");
        dependency.getArtifactId().setStringValue(libraryId);
        dependency.getVersion().setStringValue("${" + KOTLIN_VERSION_PROPERTY + "}");
    }

    private void addPluginIfNeeded(MavenDomProjectModel domModel, Module module, VirtualFile virtualFile) {
        MavenDomPlugins plugins = domModel.getBuild().getPlugins();
        for (MavenDomPlugin plugin : plugins.getPlugins()) {
            if (MAVEN_PLUGIN_ID.equals(plugin.getArtifactId().getStringValue())) {
                return;
            }
        }
        MavenDomPlugin kotlinPlugin = plugins.addPlugin();
        kotlinPlugin.getArtifactId().setStringValue("kotlin-maven-plugin");
        kotlinPlugin.getGroupId().setStringValue("org.jetbrains.kotlin");
        kotlinPlugin.getVersion().setStringValue("${" + KOTLIN_VERSION_PROPERTY + "}");
        createExecutions(virtualFile, kotlinPlugin, module);
    }

    private static boolean isRepositoryConfigured(List<MavenDomRepository> pluginRepositories) {
        for (MavenDomRepository repository : pluginRepositories) {
            if (SNAPSHOT_REPOSITORY_ID.equals(repository.getId().getStringValue())) {
                return true;
            }
        }
        return false;
    }

    private static void configureRepository(@NotNull MavenDomRepository repository) {
        repository.getId().setStringValue(SNAPSHOT_REPOSITORY_ID);
        repository.getName().setStringValue("Sonatype OSS Snapshot Repository");
        repository.getUrl().setStringValue("http://oss.sonatype.org/content/repositories/snapshots");
        createTagIfNeeded(repository.getReleases(), "enabled", "false");
        createTagIfNeeded(repository.getSnapshots(), "enabled", "true");
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

    @NotNull
    private static XmlTag createTagIfNeeded(@NotNull DomElement parent, @NotNull String tagName, @NotNull String value) {
        XmlTag parentTag = parent.ensureTagExists();
        XmlTag tagWithGivenName = parentTag.findFirstSubTag(tagName);
        if (tagWithGivenName != null) {
            return tagWithGivenName;
        }
        XmlTag newTag = parentTag.createChildTag(tagName, parentTag.getNamespace(), value, false);
        return parentTag.addSubTag(newTag, true);
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
