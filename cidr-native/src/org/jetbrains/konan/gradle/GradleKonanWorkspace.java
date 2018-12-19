/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.konan.gradle;

import com.intellij.execution.ExecutionTargetManager;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.externalSystem.service.project.manage.ExternalProjectsManager;
import com.intellij.openapi.progress.BackgroundTaskQueue;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.AtomicClearableLazyValue;
import com.intellij.openapi.util.Trinity;
import com.intellij.util.SmartList;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.MultiMap;
import com.jetbrains.cidr.lang.workspace.OCResolveConfiguration;
import com.jetbrains.cidr.lang.workspace.OCWorkspaceImpl;
import kotlin.Unit;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.konan.gradle.execution.GradleKonanBuildTarget;
import org.jetbrains.konan.gradle.execution.GradleKonanConfiguration;
import org.jetbrains.plugins.gradle.settings.GradleSettings;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;


/**
 * @author Vladislav.Soroka
 */
public class GradleKonanWorkspace {

    public static final String LOADING_GRADLE_KONAN_PROJECT = "Loading Gradle Kotlin/Native Project...";
    private static final Logger LOG = Logger.getInstance(GradleKonanWorkspace.class);
    @NotNull private final AtomicClearableLazyValue<List<GradleKonanBuildTarget>> myTargets;
    private final Project myProject;
    @NotNull private final BackgroundTaskQueue myReloadsQueue;

    public GradleKonanWorkspace(@NotNull Project project) {
        myProject = project;
        myReloadsQueue = new BackgroundTaskQueue(project, LOADING_GRADLE_KONAN_PROJECT);
        myTargets = new AtomicClearableLazyValue<List<GradleKonanBuildTarget>>() {
            @NotNull
            @Override
            protected List<GradleKonanBuildTarget> compute() {
                return loadBuildTargets(project);
            }
        };
        // force reloading of the build targets when external data cache is ready
        ExternalProjectsManager.getInstance(project).runWhenInitialized(() -> update());
    }

    @NotNull
    public static GradleKonanWorkspace getInstance(@NotNull Project project) {
        return ServiceManager.getService(project, GradleKonanWorkspace.class);
    }

    public List<GradleKonanBuildTarget> getModelTargets() {
        return myTargets.getValue();
    }

    @Nullable
    public OCResolveConfiguration getResolveConfigurationFor(@Nullable GradleKonanConfiguration configuration) {
        return configuration == null ? null : OCWorkspaceImpl.getInstanceImpl(myProject).getConfigurationById(configuration.getId());
    }

    public void update() {
        // Skip the update if no Gradle projects are linked with this IDE project.
        if (GradleSettings.getInstance(myProject).getLinkedProjectsSettings().isEmpty()) {
            return;
        }
        myReloadsQueue.run(new Task.Backgroundable(myProject, LOADING_GRADLE_KONAN_PROJECT) {
            @Override
            public void run(@NotNull ProgressIndicator indicator) {
                myTargets.drop();
                myTargets.getValue();
                ApplicationManager.getApplication().invokeLater(() -> ExecutionTargetManager.update(myProject), myProject.getDisposed());
            }
        });
    }

    @NotNull
    private static List<GradleKonanBuildTarget> loadBuildTargets(@NotNull Project project) {
        List<GradleKonanBuildTarget> buildTargets = new SmartList<>();
        KonanProjectDataService.forEachKonanProject(project, (konanModel, moduleData, rootProjectPath) -> {
            MultiMap<Trinity<String, String, String>, GradleKonanConfiguration> configurationsMap = MultiMap.createSmart();
            for (KonanModelArtifact konanArtifact: konanModel.getArtifacts()) {
                String compileTaskName = konanArtifact.getBuildTaskName();
                String id = getConfigurationId(moduleData.getId(), konanArtifact);
                // TODO: We should do something about debug/release for gradle
                GradleKonanConfiguration configuration =
                        new GradleKonanConfiguration(id, konanArtifact.getName(), "Debug",
                                                     konanArtifact.getFile(), konanArtifact.getType(),
                                                     compileTaskName, rootProjectPath, konanArtifact.isTests());
                Trinity<String, String, String> names = Trinity.create(moduleData.getId(), moduleData.getExternalName(), konanArtifact.getName());
                configurationsMap.putValue(names, configuration);
            }

            configurationsMap.entrySet().forEach(entry -> {
                Trinity<String, String, String> names = entry.getKey();
                Collection<GradleKonanConfiguration> value = entry.getValue();
                List<GradleKonanConfiguration> configurations =
                        value instanceof List ? (List<GradleKonanConfiguration>)value : ContainerUtil.newArrayList(value);

                String moduleId = names.first;
                String moduleName = names.second;
                String targetName = names.third;
                String targetId = getBuildTargetId(moduleId, targetName);
                List<GradleKonanConfiguration> nonTestConfigurations = configurations.stream().filter(it -> !it.isTests()).collect(
                        Collectors.toList());
                if (!nonTestConfigurations.isEmpty()) {
                    buildTargets.add(new GradleKonanBuildTarget(targetId, targetName, moduleName, nonTestConfigurations));
                }

                GradleKonanConfiguration testConfiguration = configurations.stream().filter(GradleKonanConfiguration::isTests).findFirst().orElse(null);
                if (testConfiguration != null) {
                    targetName += "Tests";
                    buildTargets.add(new GradleKonanBuildTarget(getBuildTargetId(moduleId, targetName), targetName, moduleName, Collections.singletonList(testConfiguration)));
                }
            });
            return Unit.INSTANCE;
        });

        return buildTargets;
    }

    @NotNull
    private static String getConfigurationId(String moduleId, KonanModelArtifact konanArtifact) {
        return getBuildTargetId(moduleId, konanArtifact.getName()) + ":" + konanArtifact.getBuildTaskName();
    }

    @NotNull
    private static String getBuildTargetId(String moduleId, String targetName) {
        return moduleId + ":" + targetName;
    }
}
