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

package org.jetbrains.kotlin.idea.framework;

import com.intellij.facet.impl.ui.libraries.LibraryCompositionSettings;
import com.intellij.facet.impl.ui.libraries.LibraryOptionsPanel;
import com.intellij.framework.library.FrameworkLibraryVersionFilter;
import com.intellij.ide.util.projectWizard.ModuleBuilder;
import com.intellij.ide.util.projectWizard.ModuleWizardStep;
import com.intellij.ide.util.projectWizard.SettingsStep;
import com.intellij.openapi.module.JavaModuleType;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.roots.ui.configuration.libraries.CustomLibraryDescription;
import com.intellij.openapi.roots.ui.configuration.projectRoot.LibrariesContainer;
import com.intellij.openapi.roots.ui.configuration.projectRoot.LibrariesContainerFactory;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.io.FileUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.idea.project.TargetPlatform;

import javax.swing.*;
import java.util.ArrayList;

public class KotlinModuleSettingStep extends ModuleWizardStep {
    private final TargetPlatform targetPlatform;

    @Nullable
    private final ModuleWizardStep myJavaStep;

    private final CustomLibraryDescription customLibraryDescription;
    private final LibrariesContainer librariesContainer;

    private LibraryOptionsPanel libraryOptionsPanel;
    private LibraryCompositionSettings libraryCompositionSettings;

    private final String basePath;

    public KotlinModuleSettingStep(TargetPlatform targetPlatform, ModuleBuilder moduleBuilder, @NotNull SettingsStep settingsStep) {
        this.targetPlatform = targetPlatform;

        myJavaStep = JavaModuleType.getModuleType().modifyProjectTypeStep(settingsStep, moduleBuilder);

        basePath = moduleBuilder.getContentEntryPath();
        librariesContainer = LibrariesContainerFactory.createContainer(settingsStep.getContext().getProject());

        customLibraryDescription = getCustomLibraryDescription(settingsStep.getContext().getProject());

        moduleBuilder.addModuleConfigurationUpdater(createModuleConfigurationUpdater());

        settingsStep.addSettingsField(getLibraryLabelText(), getLibraryPanel().getSimplePanel());
    }

    protected ModuleBuilder.ModuleConfigurationUpdater createModuleConfigurationUpdater() {
        return new ModuleBuilder.ModuleConfigurationUpdater() {
            @Override
            public void update(@NotNull Module module, @NotNull ModifiableRootModel rootModel) {
                if (libraryCompositionSettings != null) {
                    libraryCompositionSettings.addLibraries(rootModel, new ArrayList<Library>(), librariesContainer);

                    if (customLibraryDescription instanceof CustomLibraryDescriptorWithDeferredConfig) {
                        ((CustomLibraryDescriptorWithDeferredConfig) customLibraryDescription).finishLibConfiguration(module, rootModel);
                    }
                }
            }
        };
    }

    @Override
    public void disposeUIResources() {
        if (libraryOptionsPanel != null) {
            Disposer.dispose(libraryOptionsPanel);
        }
    }

    @Override
    public JComponent getComponent() {
        return getLibraryPanel().getMainPanel();
    }

    @NotNull
    protected String getLibraryLabelText() {
        if (targetPlatform == TargetPlatform.JVM) return "\u001BKotlin runtime:";
        if (targetPlatform == TargetPlatform.JS) return "\u001BKotlin JS library:";
        throw new IllegalStateException("Only JS and JVM target are supported");
    }

    @NotNull
    protected CustomLibraryDescription getCustomLibraryDescription(@Nullable Project project) {
        if (targetPlatform == TargetPlatform.JVM) return new JavaRuntimeLibraryDescription(project);
        if (targetPlatform == TargetPlatform.JS) return new JSLibraryStdDescription(project);
        throw new IllegalStateException("Only JS and JVM target are supported");
    }

    @Override
    public void updateDataModel() {
        libraryCompositionSettings = getLibraryPanel().apply();
        if (myJavaStep != null) {
            myJavaStep.updateDataModel();
        }
    }

    @Override
    public boolean validate() throws ConfigurationException {
        return super.validate() && (myJavaStep == null || myJavaStep.validate());
    }

    protected LibraryOptionsPanel getLibraryPanel() {
        if (libraryOptionsPanel == null) {
            String baseDirPath = basePath != null ? FileUtil.toSystemIndependentName(basePath) : "";

            libraryOptionsPanel = new LibraryOptionsPanel(
                    customLibraryDescription,
                    baseDirPath,
                    FrameworkLibraryVersionFilter.ALL,
                    librariesContainer,
                    false);
        }

        return libraryOptionsPanel;
    }
}
