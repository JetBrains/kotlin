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

package org.jetbrains.jet.plugin.configuration;

import com.intellij.codeInsight.CodeInsightUtilBase;
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
import org.jetbrains.jet.plugin.JetPluginUtil;
import org.jetbrains.jet.plugin.framework.KotlinFrameworkDetector;
import org.jetbrains.jet.plugin.framework.ui.ConfigureDialogWithModulesAndVersion;

import java.util.List;

public class KotlinMavenConfigurator implements KotlinProjectConfigurator {
    private static final String[] KOTLIN_VERSIONS = {"0.6.594", "0.1-SNAPSHOT"};

    public static final String NAME = "maven";

    private static final String STD_LIB_ID = "kotlin-stdlib";
    private static final String MAVEN_PLUGIN_ID = "kotlin-maven-plugin";
    private static final String KOTLIN_VERSION_PROPERTY = "kotlin.version";
    private static final String SNAPSHOT_REPOSITORY_ID = "sonatype.oss.snapshots";

    @Override
    public boolean isApplicable(@NotNull Module module) {
        return JetPluginUtil.isMavenModule(module);
    }

    @Override
    public boolean isConfigured(@NotNull Module module) {
        if (KotlinFrameworkDetector.isJavaKotlinModule(module)) {
            return true;
        }

        PsiFile pomFile = findModulePomFile(module);
        if (pomFile == null) return false;
        String text = pomFile.getText();
        return text.contains("<artifactId>" + MAVEN_PLUGIN_ID + "</artifactId>") &&
               text.contains("<artifactId>" + STD_LIB_ID + "</artifactId>");
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

    protected static void changePomFile(@NotNull final Module module, final @NotNull PsiFile file, @NotNull final String version) {
        final VirtualFile virtualFile = file.getVirtualFile();
        assert virtualFile != null : "Virtual file should exists for psi file " + file.getName();
        final MavenDomProjectModel domModel = MavenDomUtil.getMavenDomProjectModel(module.getProject(), virtualFile);
        if (domModel == null) {
            showErrorMessage(module.getProject(), null);
            return;
        }
        new WriteCommandAction(file.getProject()) {
            @Override
            protected void run(Result result) {
                addKotlinVersionPropertyIfNeeded(domModel, version);

                if (isSnapshot(version)) {
                    addPluginRepositoryIfNeeded(domModel);
                    addLibraryRepositoryIfNeeded(domModel);
                }

                addPluginIfNeeded(domModel, module, virtualFile);
                addLibraryDependencyIfNeeded(domModel);

                CodeInsightUtilBase.forcePsiPostprocessAndRestoreElement(file);
            }
        }.execute();

        ConfigureKotlinInProjectUtils.showInfoNotification(virtualFile.getPath() + " was modified");
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

    private static void addLibraryDependencyIfNeeded(MavenDomProjectModel domModel) {
        for (MavenDomDependency dependency : domModel.getDependencies().getDependencies()) {
            if (STD_LIB_ID.equals(dependency.getArtifactId().getStringValue())) {
                return;
            }
        }

        MavenDomDependency dependency = MavenDomUtil.createDomDependency(domModel, null);
        dependency.getGroupId().setStringValue("org.jetbrains.kotlin");
        dependency.getArtifactId().setStringValue(STD_LIB_ID);
        dependency.getVersion().setStringValue("${" + KOTLIN_VERSION_PROPERTY + "}");
    }

    private static void addPluginIfNeeded(MavenDomProjectModel domModel, Module module, VirtualFile virtualFile) {
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
        createExecution(virtualFile, kotlinPlugin, module, false);
        createExecution(virtualFile, kotlinPlugin, module, true);
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

    private static void createExecution(
            @NotNull VirtualFile virtualFile,
            @NotNull MavenDomPlugin kotlinPlugin,
            @NotNull Module module,
            boolean isTest
    ) {
        MavenDomPluginExecution execution = kotlinPlugin.getExecutions().addExecution();
        String tagValue = isTest ? "test-compile" : "compile";
        execution.getId().setStringValue(tagValue);
        if (hasJavaFiles(module)) {
            execution.getPhase().setStringValue(isTest ? "process-test-sources" : "process-sources");
        }
        else {
            execution.getPhase().setStringValue(tagValue);
        }
        createTagIfNeeded(execution.getGoals(), "goal", tagValue);

        XmlTag sourcesTag = createTagIfNeeded(execution.getConfiguration(), "sourceDirs", "");

        for (ContentEntry contentEntry : ModuleRootManager.getInstance(module).getContentEntries()) {
            SourceFolder[] folders = contentEntry.getSourceFolders();
            for (SourceFolder sourceFolder : folders) {
                if (isTest && sourceFolder.isTestSource() || !isTest && !sourceFolder.isTestSource()) {
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

    @NotNull
    @Override
    public String getPresentableText() {
        return "Maven";
    }

    @NotNull
    @Override
    public String getName() {
        return NAME;
    }

    private static boolean canConfigureFile(@NotNull PsiFile file) {
        return WritingAccessProvider.isPotentiallyWritable(file.getVirtualFile(), null);
    }

    protected static void showErrorMessage(@NotNull Project project, @Nullable String message) {
        Messages.showErrorDialog(project,
                                 "<html>Couldn't configure kotlin-maven plugin automatically.<br/>" +
                                 (message != null ? message : "") +
                                 "See manual installation instructions <a href=\"http://confluence.jetbrains.com/display/Kotlin/Kotlin+Build+Tools#KotlinBuildTools-Maven\">here</a></html>",
                                 "Configure Kotlin-Maven Plugin");
    }
}
