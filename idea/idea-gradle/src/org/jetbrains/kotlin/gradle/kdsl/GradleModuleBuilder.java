/*
 * Copyright 2010-2017 JetBrains s.r.o.
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
package org.jetbrains.kotlin.gradle.kdsl;

import com.intellij.ide.fileTemplates.FileTemplate;
import com.intellij.ide.fileTemplates.FileTemplateManager;
import com.intellij.ide.highlighter.ModuleFileType;
import com.intellij.ide.projectWizard.ProjectSettingsStep;
import com.intellij.ide.util.EditorHelper;
import com.intellij.ide.util.projectWizard.JavaModuleBuilder;
import com.intellij.ide.util.projectWizard.ModuleWizardStep;
import com.intellij.ide.util.projectWizard.SettingsStep;
import com.intellij.ide.util.projectWizard.WizardContext;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.components.StorageScheme;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.externalSystem.ExternalSystemModulePropertyManager;
import com.intellij.openapi.externalSystem.importing.ImportSpec;
import com.intellij.openapi.externalSystem.importing.ImportSpecBuilder;
import com.intellij.openapi.externalSystem.model.ExternalSystemDataKeys;
import com.intellij.openapi.externalSystem.model.project.ProjectData;
import com.intellij.openapi.externalSystem.model.project.ProjectId;
import com.intellij.openapi.externalSystem.service.execution.ProgressExecutionMode;
import com.intellij.openapi.externalSystem.service.project.manage.ExternalProjectsManagerImpl;
import com.intellij.openapi.externalSystem.service.project.wizard.AbstractExternalModuleBuilder;
import com.intellij.openapi.externalSystem.service.project.wizard.ExternalModuleSettingsStep;
import com.intellij.openapi.externalSystem.settings.AbstractExternalSystemSettings;
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil;
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.impl.LoadTextUtil;
import com.intellij.openapi.module.*;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ex.ProjectManagerEx;
import com.intellij.openapi.projectRoots.JavaSdkType;
import com.intellij.openapi.projectRoots.SdkTypeId;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.ui.configuration.ModulesProvider;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.io.FileUtilRt;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.codeStyle.CodeStyleSettingsManager;
import com.intellij.util.containers.ContainerUtil;
import org.gradle.util.GradleVersion;
import org.jdom.JDOMException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.gradle.kdsl.frameworkSupport.BuildScriptDataBuilder;
import org.jetbrains.kotlin.gradle.kdsl.frameworkSupport.KotlinBuildScriptDataBuilder;
import org.jetbrains.plugins.gradle.service.settings.GradleProjectSettingsControl;
import org.jetbrains.plugins.gradle.settings.DistributionType;
import org.jetbrains.plugins.gradle.settings.GradleProjectSettings;
import org.jetbrains.plugins.gradle.util.GradleConstants;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.util.Map;

import static org.jetbrains.kotlin.gradle.kdsl.GradleKotlinDSLConstants.DEFAULT_SCRIPT_NAME;

public class GradleModuleBuilder extends AbstractExternalModuleBuilder<GradleProjectSettings> {

  private static final Logger LOG = Logger.getInstance(GradleModuleBuilder.class);

  private static final String TEMPLATE_GRADLE_SETTINGS = "Gradle Kotlin DSL Settings.gradle";
  private static final String TEMPLATE_GRADLE_SETTINGS_MERGE = "Gradle Settings merge.gradle";
  private static final String TEMPLATE_GRADLE_BUILD_WITH_WRAPPER = "Gradle Build Script with wrapper.gradle";
  private static final String DEFAULT_TEMPLATE_GRADLE_BUILD = "Gradle Kotlin DSL Build Script.gradle";

  private static final String TEMPLATE_ATTRIBUTE_PROJECT_NAME = "PROJECT_NAME";
  private static final String TEMPLATE_ATTRIBUTE_MODULE_PATH = "MODULE_PATH";
  private static final String TEMPLATE_ATTRIBUTE_MODULE_FLAT_DIR = "MODULE_FLAT_DIR";
  private static final String TEMPLATE_ATTRIBUTE_MODULE_NAME = "MODULE_NAME";
  private static final String TEMPLATE_ATTRIBUTE_MODULE_GROUP = "MODULE_GROUP";
  private static final String TEMPLATE_ATTRIBUTE_MODULE_VERSION = "MODULE_VERSION";
  private static final String TEMPLATE_ATTRIBUTE_GRADLE_VERSION = "GRADLE_VERSION";
  private static final String TEMPLATE_ATTRIBUTE_BUILD_FILE_NAME = "BUILD_FILE_NAME";
  private static final Key<KotlinBuildScriptDataBuilder> BUILD_SCRIPT_DATA =
    Key.create("gradle.module.kotlinBuildScriptData");

  private WizardContext myWizardContext;

  @Nullable
  private ProjectData myParentProject;
  private boolean myInheritGroupId;
  private boolean myInheritVersion;
  private ProjectId myProjectId;
  private String rootProjectPath;

  public GradleModuleBuilder() {
    super(GradleConstants.SYSTEM_ID, new GradleProjectSettings());
  }

  @Override
  public String getPresentableName() {
    return "Gradle (Kotlin DSL)";
  }

  @NotNull
  @Override
  public Module createModule(@NotNull ModifiableModuleModel moduleModel)
    throws InvalidDataException, IOException, ModuleWithNameAlreadyExists, JDOMException, ConfigurationException {
    LOG.assertTrue(getName() != null);
    final String originModuleFilePath = getModuleFilePath();
    LOG.assertTrue(originModuleFilePath != null);

    String moduleName;
    if (myProjectId == null) {
      moduleName = getName();
    }
    else {
      moduleName = getExternalProjectSettings().isUseQualifiedModuleNames() && StringUtil.isNotEmpty(myProjectId.getGroupId())
                   ? (myProjectId.getGroupId() + '.' + myProjectId.getArtifactId())
                   : myProjectId.getArtifactId();
    }
    Project contextProject = myWizardContext.getProject();
    String projectFileDirectory = null;
    if (myWizardContext.isCreatingNewProject() || contextProject == null || contextProject.getBasePath() == null) {
      projectFileDirectory = myWizardContext.getProjectFileDirectory();
    }
    else if (myWizardContext.getProjectStorageFormat() == StorageScheme.DEFAULT) {
      String moduleFileDirectory = getModuleFileDirectory();
      if (moduleFileDirectory != null) {
        projectFileDirectory = moduleFileDirectory;
      }
    }
    if (projectFileDirectory == null) {
      projectFileDirectory = contextProject.getBasePath();
    }
    if (myWizardContext.getProjectStorageFormat() == StorageScheme.DIRECTORY_BASED) {
      projectFileDirectory += "/.idea/modules";
    }
    String moduleFilePath = projectFileDirectory + "/" + moduleName + ModuleFileType.DOT_DEFAULT_EXTENSION;
    deleteModuleFile(moduleFilePath);
    final ModuleType moduleType = getModuleType();
    final Module module = moduleModel.newModule(moduleFilePath, moduleType.getId());
    setupModule(module);
    return module;
  }

  @Override
  public void setupRootModel(final ModifiableRootModel modifiableRootModel) throws ConfigurationException {
    String contentEntryPath = getContentEntryPath();
    if (StringUtil.isEmpty(contentEntryPath)) {
      return;
    }
    File contentRootDir = new File(contentEntryPath);
    FileUtilRt.createDirectory(contentRootDir);
    LocalFileSystem fileSystem = LocalFileSystem.getInstance();
    VirtualFile modelContentRootDir = fileSystem.refreshAndFindFileByIoFile(contentRootDir);
    if (modelContentRootDir == null) {
      return;
    }

    modifiableRootModel.addContentEntry(modelContentRootDir);
    // todo this should be moved to generic ModuleBuilder
    if (myJdk != null) {
      modifiableRootModel.setSdk(myJdk);
    }
    else {
      modifiableRootModel.inheritSdk();
    }

    final Project project = modifiableRootModel.getProject();
    if (myParentProject != null) {
      rootProjectPath = myParentProject.getLinkedExternalProjectPath();
    }
    else {
      rootProjectPath =
        FileUtil.toCanonicalPath(myWizardContext.isCreatingNewProject() ? project.getBasePath() : modelContentRootDir.getPath());
    }
    assert rootProjectPath != null;

    final VirtualFile gradleBuildFile = setupGradleBuildFile(modelContentRootDir);
    setupGradleSettingsFile(
      rootProjectPath, modelContentRootDir, modifiableRootModel.getProject().getName(),
      myProjectId == null ? modifiableRootModel.getModule().getName() : myProjectId.getArtifactId(),
      myWizardContext.isCreatingNewProject() || myParentProject == null
    );

    if (gradleBuildFile != null) {
      modifiableRootModel.getModule().putUserData(
        BUILD_SCRIPT_DATA, new KotlinBuildScriptDataBuilder(gradleBuildFile));
    }
  }

  @Override
  protected void setupModule(Module module) throws ConfigurationException {
    super.setupModule(module);
    assert rootProjectPath != null;

    VirtualFile buildScriptFile = null;
    final BuildScriptDataBuilder buildScriptDataBuilder = getBuildScriptData(module);
    try {
      if (buildScriptDataBuilder != null) {
        buildScriptFile = buildScriptDataBuilder.getBuildScriptFile();
        String lineSeparator = lineSeparator(buildScriptFile);
        String configurationPart = StringUtil.convertLineSeparators(buildScriptDataBuilder.buildConfigurationPart(), lineSeparator);
        String existingText = StringUtil.trimTrailing(VfsUtilCore.loadText(buildScriptFile));
        String content = (!configurationPart.isEmpty() ? configurationPart + lineSeparator : "") +
                         (!existingText.isEmpty() ? existingText + lineSeparator : "") +
                         lineSeparator +
                         StringUtil.convertLineSeparators(buildScriptDataBuilder.buildMainPart(), lineSeparator);
        VfsUtil.saveText(buildScriptFile, content);
      }
    }
    catch (IOException e) {
      LOG.warn("Unexpected exception on applying frameworks templates", e);
    }

    // it will be set later in any case, but save is called immediately after project creation, so, to ensure that it will be properly saved as external system module
    ExternalSystemModulePropertyManager.getInstance(module).setExternalId(GradleConstants.SYSTEM_ID);

    final Project project = module.getProject();
    if (myWizardContext.isCreatingNewProject()) {
      getExternalProjectSettings().setExternalProjectPath(rootProjectPath);
      AbstractExternalSystemSettings settings = ExternalSystemApiUtil.getSettings(project, GradleConstants.SYSTEM_ID);
      project.putUserData(ExternalSystemDataKeys.NEWLY_CREATED_PROJECT, Boolean.TRUE);
      //noinspection unchecked
      settings.linkProject(getExternalProjectSettings());
    }
    else {
      FileDocumentManager.getInstance().saveAllDocuments();
      final GradleProjectSettings gradleProjectSettings = getExternalProjectSettings();
      final VirtualFile finalBuildScriptFile = buildScriptFile;
      Runnable runnable = () -> {
        if (myParentProject == null) {
          gradleProjectSettings.setExternalProjectPath(rootProjectPath);
          AbstractExternalSystemSettings settings = ExternalSystemApiUtil.getSettings(project, GradleConstants.SYSTEM_ID);
          //noinspection unchecked
          settings.linkProject(gradleProjectSettings);
        }

        ImportSpec importSpec = new ImportSpecBuilder(project, GradleConstants.SYSTEM_ID)
          .use(ProgressExecutionMode.IN_BACKGROUND_ASYNC)
          .createDirectoriesForEmptyContentRoots()
          .useDefaultCallback()
          .build();
        ExternalSystemUtil.refreshProject(rootProjectPath, importSpec);

        final PsiFile psiFile;
        if (finalBuildScriptFile != null) {
          psiFile = PsiManager.getInstance(project).findFile(finalBuildScriptFile);
          if (psiFile != null) {
            EditorHelper.openInEditor(psiFile);
          }
        }
      };

      // execute when current dialog is closed
      ExternalSystemUtil.invokeLater(project, ModalityState.NON_MODAL, runnable);
    }
  }

  @Override
  public ModuleWizardStep[] createWizardSteps(@NotNull WizardContext wizardContext, @NotNull ModulesProvider modulesProvider) {
    myWizardContext = wizardContext;
    return new ModuleWizardStep[]{
      new GradleModuleWizardStep(this, wizardContext),
      new ExternalModuleSettingsStep<>(
        wizardContext, this, new GradleProjectSettingsControl(getExternalProjectSettings()))
    };
  }

  @Nullable
  @Override
  public ModuleWizardStep getCustomOptionsStep(WizardContext context, Disposable parentDisposable) {
    final GradleFrameworksWizardStep step = new GradleFrameworksWizardStep(context, this);
    Disposer.register(parentDisposable, step);
    return step;
  }

  @Override
  public boolean isSuitableSdkType(SdkTypeId sdk) {
    return sdk instanceof JavaSdkType;
  }

  @Override
  public String getParentGroup() {
    return JavaModuleType.BUILD_TOOLS_GROUP;
  }

  @Override
  public int getWeight() {
    return JavaModuleBuilder.BUILD_SYSTEM_WEIGHT;
  }

  @Override
  public ModuleType getModuleType() {
    return StdModuleTypes.JAVA;
  }

  @Nullable
  private VirtualFile setupGradleBuildFile(@NotNull VirtualFile modelContentRootDir)
    throws ConfigurationException {
    final VirtualFile file = getOrCreateExternalProjectConfigFile(modelContentRootDir.getPath(), GradleKotlinDSLConstants.DEFAULT_SCRIPT_NAME);

    if (file != null) {
      final String templateName = getExternalProjectSettings().getDistributionType() == DistributionType.WRAPPED
                                  ? TEMPLATE_GRADLE_BUILD_WITH_WRAPPER
                                  : DEFAULT_TEMPLATE_GRADLE_BUILD;
      Map<String, String> attributes = ContainerUtil.newHashMap();
      if (myProjectId != null) {
        attributes.put(TEMPLATE_ATTRIBUTE_MODULE_VERSION, myProjectId.getVersion());
        attributes.put(TEMPLATE_ATTRIBUTE_MODULE_GROUP, myProjectId.getGroupId());
        attributes.put(TEMPLATE_ATTRIBUTE_GRADLE_VERSION, GradleVersion.current().getVersion());
      }
      saveFile(file, templateName, attributes);
    }
    return file;
  }

  @Nullable
  public static VirtualFile setupGradleSettingsFile(@NotNull String rootProjectPath,
                                                    @NotNull VirtualFile modelContentRootDir,
                                                    String projectName,
                                                    String moduleName,
                                                    boolean renderNewFile)
    throws ConfigurationException {
    final VirtualFile file = getOrCreateExternalProjectConfigFile(rootProjectPath, GradleConstants.SETTINGS_FILE_NAME);
    if (file == null) return null;

    if (renderNewFile) {
      final String moduleDirName = VfsUtilCore.getRelativePath(modelContentRootDir, file.getParent(), '/');

      Map<String, String> attributes = ContainerUtil.newHashMap();
      attributes.put(TEMPLATE_ATTRIBUTE_PROJECT_NAME, projectName);
      attributes.put(TEMPLATE_ATTRIBUTE_MODULE_PATH, moduleDirName);
      attributes.put(TEMPLATE_ATTRIBUTE_MODULE_NAME, moduleName);
      attributes.put(TEMPLATE_ATTRIBUTE_BUILD_FILE_NAME, DEFAULT_SCRIPT_NAME); // TODO: gradle > 4.0 doesn't need this
      saveFile(file, TEMPLATE_GRADLE_SETTINGS, attributes);
    }
    else {
      char separatorChar = file.getParent() == null || !VfsUtilCore.isAncestor(file.getParent(), modelContentRootDir, true) ? '/' : ':';
      String modulePath = VfsUtil.getPath(file, modelContentRootDir, separatorChar);

      Map<String, String> attributes = ContainerUtil.newHashMap();
      attributes.put(TEMPLATE_ATTRIBUTE_MODULE_NAME, moduleName);
      // check for flat structure
      final String flatStructureModulePath =
        modulePath != null && StringUtil.startsWith(modulePath, "../") ? StringUtil.trimStart(modulePath, "../") : null;
      if (StringUtil.equals(flatStructureModulePath, modelContentRootDir.getName())) {
        attributes.put(TEMPLATE_ATTRIBUTE_MODULE_FLAT_DIR, "true");
        attributes.put(TEMPLATE_ATTRIBUTE_MODULE_PATH, flatStructureModulePath);
      }
      else {
        attributes.put(TEMPLATE_ATTRIBUTE_MODULE_PATH, modulePath);
      }

      appendToFile(file, TEMPLATE_GRADLE_SETTINGS_MERGE, attributes);
    }
    return file;
  }

  private static void saveFile(@NotNull VirtualFile file, @NotNull String templateName, @Nullable Map templateAttributes)
    throws ConfigurationException {
    FileTemplateManager manager = FileTemplateManager.getDefaultInstance();
    FileTemplate template = manager.getInternalTemplate(templateName);
    try {
      appendToFile(file, templateAttributes != null ? template.getText(templateAttributes) : template.getText());
    }
    catch (IOException e) {
      LOG.warn(String.format("Unexpected exception on applying template %s config", GradleConstants.SYSTEM_ID.getReadableName()), e);
      throw new ConfigurationException(
        e.getMessage(), String.format("Can't apply %s template config text", GradleConstants.SYSTEM_ID.getReadableName())
      );
    }
  }

  private static void appendToFile(@NotNull VirtualFile file, @NotNull String templateName, @Nullable Map templateAttributes)
    throws ConfigurationException {
    FileTemplateManager manager = FileTemplateManager.getDefaultInstance();
    FileTemplate template = manager.getInternalTemplate(templateName);
    try {
      appendToFile(file, templateAttributes != null ? template.getText(templateAttributes) : template.getText());
    }
    catch (IOException e) {
      LOG.warn(String.format("Unexpected exception on appending template %s config", GradleConstants.SYSTEM_ID.getReadableName()), e);
      throw new ConfigurationException(
        e.getMessage(), String.format("Can't append %s template config text", GradleConstants.SYSTEM_ID.getReadableName())
      );
    }
  }


  @Nullable
  private static VirtualFile getOrCreateExternalProjectConfigFile(@NotNull String parent, @NotNull String fileName) {
    File file = new File(parent, fileName);
    FileUtilRt.createIfNotExists(file);
    return LocalFileSystem.getInstance().refreshAndFindFileByIoFile(file);
  }

  public void setParentProject(@Nullable ProjectData parentProject) {
    myParentProject = parentProject;
  }

  public boolean isInheritGroupId() {
    return myInheritGroupId;
  }

  public void setInheritGroupId(boolean inheritGroupId) {
    myInheritGroupId = inheritGroupId;
  }

  public boolean isInheritVersion() {
    return myInheritVersion;
  }

  public void setInheritVersion(boolean inheritVersion) {
    myInheritVersion = inheritVersion;
  }

  public ProjectId getProjectId() {
    return myProjectId;
  }

  public void setProjectId(@NotNull ProjectId projectId) {
    myProjectId = projectId;
  }

  @Nullable
  @Override
  public ModuleWizardStep modifySettingsStep(@NotNull SettingsStep settingsStep) {
    if (settingsStep instanceof ProjectSettingsStep) {
      final ProjectSettingsStep projectSettingsStep = (ProjectSettingsStep)settingsStep;
      if (myProjectId != null) {
        final JTextField moduleNameField = settingsStep.getModuleNameField();
        if (moduleNameField != null) {
          moduleNameField.setText(myProjectId.getArtifactId());
        }
        projectSettingsStep.setModuleName(myProjectId.getArtifactId());
      }
      projectSettingsStep.bindModuleSettings();
    }
    return super.modifySettingsStep(settingsStep);
  }

  public static void appendToFile(@NotNull VirtualFile file, @NotNull String text) throws IOException {
    String lineSeparator = lineSeparator(file);
    final String existingText = StringUtil.trimTrailing(VfsUtilCore.loadText(file));
    String content = (StringUtil.isNotEmpty(existingText) ? existingText + lineSeparator : "") +
                     StringUtil.convertLineSeparators(text, lineSeparator);
    VfsUtil.saveText(file, content);
  }

  @NotNull
  private static String lineSeparator(@NotNull VirtualFile file) {
    String lineSeparator = LoadTextUtil.detectLineSeparator(file, true);
    if (lineSeparator == null) {
      lineSeparator = CodeStyleSettingsManager.getSettings(ProjectManagerEx.getInstanceEx().getDefaultProject()).getLineSeparator();
    }
    return lineSeparator;
  }

  @Nullable
  public static BuildScriptDataBuilder getBuildScriptData(@Nullable Module module) {
    return module == null ? null : module.getUserData(BUILD_SCRIPT_DATA);
  }

  @Nullable
  @Override
  public Project createProject(String name, String path) {
    Project project = super.createProject(name, path);
    if (project != null) {
      GradleProjectSettings settings = getExternalProjectSettings();
      ExternalProjectsManagerImpl.getInstance(project).setStoreExternally(settings.isStoreProjectFilesExternally());
    }
    return project;
  }
}