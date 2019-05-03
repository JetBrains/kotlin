/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.gradle.execution;

import com.intellij.build.BuildViewManager;
import com.intellij.compiler.impl.CompilerUtil;
import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.openapi.compiler.ex.CompilerPathsEx;
import com.intellij.openapi.externalSystem.model.DataNode;
import com.intellij.openapi.externalSystem.model.ProjectKeys;
import com.intellij.openapi.externalSystem.model.execution.ExternalSystemTaskExecutionSettings;
import com.intellij.openapi.externalSystem.model.project.ModuleData;
import com.intellij.openapi.externalSystem.service.execution.ProgressExecutionMode;
import com.intellij.openapi.externalSystem.task.TaskCallback;
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootModificationTracker;
import com.intellij.openapi.util.UserDataHolderBase;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.task.*;
import com.intellij.task.impl.JpsProjectTaskRunner;
import com.intellij.util.SmartList;
import com.intellij.util.SystemProperties;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.containers.FactoryMap;
import com.intellij.util.containers.MultiMap;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.config.KotlinFacetSettings;
import org.jetbrains.kotlin.idea.facet.KotlinFacet;
import org.jetbrains.kotlin.platform.IdePlatform;
import org.jetbrains.kotlin.platform.impl.CommonIdePlatformUtil;
import org.jetbrains.kotlin.platform.impl.NativeIdePlatformUtil;
import org.jetbrains.plugins.gradle.execution.build.CachedModuleDataFinder;
import org.jetbrains.plugins.gradle.execution.build.GradleProjectTaskRunner;
import org.jetbrains.plugins.gradle.service.project.GradleBuildSrcProjectsResolver;
import org.jetbrains.plugins.gradle.service.project.GradleProjectResolverUtil;
import org.jetbrains.plugins.gradle.service.task.GradleTaskManager;
import org.jetbrains.plugins.gradle.settings.GradleSettings;
import org.jetbrains.plugins.gradle.settings.GradleSystemRunningSettings;
import org.jetbrains.plugins.gradle.util.GradleConstants;

import java.io.File;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static com.intellij.openapi.externalSystem.service.execution.ExternalSystemRunConfiguration.PROGRESS_LISTENER_KEY;
import static com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil.*;
import static com.intellij.openapi.util.text.StringUtil.*;
import static org.jetbrains.plugins.gradle.execution.GradleRunnerUtil.resolveProjectPath;

/**
 * This is a modified copy of {@link GradleProjectTaskRunner} that allows building Kotlin Common and Kotlin Native modules
 * in IDEA by delegating to Gradle builder ("Delegate IDE build/run actions to gradle"). See #KT-27295, #KT-27296.
 *
 * TODO: Refactor this class to remove duplicated logic when {@link GradleProjectTaskRunner} will allow extending it to
 * collect custom Gradle tasks. See #IDEA-204372, #KT-28880.
 */
class KotlinMPPGradleProjectTaskRunner extends ProjectTaskRunner
{

    @Language("Groovy")
    private static final String FORCE_COMPILE_TASKS_INIT_SCRIPT_TEMPLATE = "projectsEvaluated { \n" +
                                                                           "  rootProject.findProject('%s')?.tasks?.withType(AbstractCompile) {  \n" +
                                                                           "    outputs.upToDateWhen { false } \n" +
                                                                           "  } \n" +
                                                                           "}\n";

    @Override
    public void run(@NotNull Project project,
            @NotNull ProjectTaskContext context,
            @Nullable ProjectTaskNotification callback,
            @NotNull Collection<? extends ProjectTask> tasks) {
        MultiMap<String, String> buildTasksMap = MultiMap.createLinkedSet();
        MultiMap<String, String> cleanTasksMap = MultiMap.createLinkedSet();
        MultiMap<String, String> initScripts = MultiMap.createLinkedSet();

        Map<Class<? extends ProjectTask>, List<ProjectTask>> taskMap = JpsProjectTaskRunner.groupBy(tasks);

        List<Module> modules = addModulesBuildTasks(taskMap.get(ModuleBuildTask.class), buildTasksMap, initScripts);
        // TODO there should be 'gradle' way to build files instead of related modules entirely
        List<Module> modulesOfFiles = addModulesBuildTasks(taskMap.get(ModuleFilesBuildTask.class), buildTasksMap, initScripts);

        // TODO send a message if nothing to build
        Set<String> rootPaths = buildTasksMap.keySet();
        AtomicInteger successCounter = new AtomicInteger();
        AtomicInteger errorCounter = new AtomicInteger();

        TaskCallback taskCallback = callback == null ? null : new TaskCallback() {
            @Override
            public void onSuccess() {
                handle(true);
            }

            @Override
            public void onFailure() {
                handle(false);
            }

            private void handle(boolean success) {
                int successes = success ? successCounter.incrementAndGet() : successCounter.get();
                int errors = success ? errorCounter.get() : errorCounter.incrementAndGet();
                if (successes + errors == rootPaths.size()) {
                    if (!project.isDisposed()) {
                        // refresh on output roots is required in order for the order enumerator to see all roots via VFS
                        final List<Module> affectedModules = ContainerUtil.concat(modules, modulesOfFiles);
                        // have to refresh in case of errors too, because run configuration may be set to ignore errors
                        Collection<String> affectedRoots = ContainerUtil.newHashSet(
                                CompilerPathsEx.getOutputPaths(affectedModules.toArray(Module.EMPTY_ARRAY)));
                        if (!affectedRoots.isEmpty()) {
                            CompilerUtil.refreshOutputRoots(affectedRoots);
                        }
                    }
                    callback.finished(new ProjectTaskResult(false, errors, 0));
                }
            }
        };

        // TODO compiler options should be configurable
        @Language("Groovy")
        String compilerOptionsInitScript = "allprojects {\n" +
                                           "  tasks.withType(JavaCompile) {\n" +
                                           "    options.compilerArgs += [\"-Xlint:deprecation\"]\n" +
                                           "  }" +
                                           "}\n";

        String gradleVmOptions = GradleSettings.getInstance(project).getGradleVmOptions();
        for (String rootProjectPath : rootPaths) {
            Collection<String> buildTasks = buildTasksMap.get(rootProjectPath);
            if (buildTasks.isEmpty()) continue;
            Collection<String> cleanTasks = cleanTasksMap.get(rootProjectPath);

            ExternalSystemTaskExecutionSettings settings = new ExternalSystemTaskExecutionSettings();

            File projectFile = new File(rootProjectPath);
            final String projectName;
            if (projectFile.isFile()) {
                projectName = projectFile.getParentFile().getName();
            }
            else {
                projectName = projectFile.getName();
            }
            String executionName = "Build " + projectName;
            settings.setExecutionName(executionName);
            settings.setExternalProjectPath(rootProjectPath);
            settings.setTaskNames(ContainerUtil.collect(ContainerUtil.concat(cleanTasks, buildTasks).iterator()));
            //settings.setScriptParameters(scriptParameters);
            settings.setVmOptions(gradleVmOptions);
            settings.setExternalSystemIdString(GradleConstants.SYSTEM_ID.getId());

            UserDataHolderBase userData = new UserDataHolderBase();
            userData.putUserData(PROGRESS_LISTENER_KEY, BuildViewManager.class);

            Collection<String> scripts = initScripts.getModifiable(rootProjectPath);
            scripts.add(compilerOptionsInitScript);
            userData.putUserData(GradleTaskManager.INIT_SCRIPT_KEY, join(scripts, SystemProperties.getLineSeparator()));
            userData.putUserData(GradleTaskManager.INIT_SCRIPT_PREFIX_KEY, executionName);

            ExternalSystemUtil.runTask(settings, DefaultRunExecutor.EXECUTOR_ID, project, GradleConstants.SYSTEM_ID,
                                       taskCallback, ProgressExecutionMode.IN_BACKGROUND_ASYNC, false, userData);
        }
    }

    @Override
    public boolean canRun(@NotNull ProjectTask projectTask) {
        if (!GradleSystemRunningSettings.getInstance().isUseGradleAwareMake()) return false;
        if (projectTask instanceof ModuleBuildTask) {
            final ModuleBuildTask moduleBuildTask = (ModuleBuildTask) projectTask;
            final Module module = moduleBuildTask.getModule();

            if (!isExternalSystemAwareModule(GradleConstants.SYSTEM_ID, module)) return false;

            // ---------------------------------------- //
            // TODO BEGIN: Extract custom Kotlin logic. //
            // ---------------------------------------- //
            if (isProjectWithNativeSourceOrCommonProductionSourceModules(module.getProject())) return true;
            // ---------------------------------------- //
            // TODO END: Extract custom Kotlin logic.   //
            // ---------------------------------------- //
        }
        return false;
    }

    private static List<Module> addModulesBuildTasks(@Nullable Collection<? extends ProjectTask> projectTasks,
            @NotNull MultiMap<String, String> buildTasksMap,
            @NotNull MultiMap<String, String> initScripts) {
        if (ContainerUtil.isEmpty(projectTasks)) return Collections.emptyList();

        List<Module> affectedModules = new SmartList<>();
        Map<Module, String> rootPathsMap = FactoryMap.create(module -> notNullize(resolveProjectPath(module)));
        final CachedModuleDataFinder moduleDataFinder = new CachedModuleDataFinder();
        for (ProjectTask projectTask : projectTasks) {
            if (!(projectTask instanceof ModuleBuildTask)) continue;

            ModuleBuildTask moduleBuildTask = (ModuleBuildTask)projectTask;
            Module module = moduleBuildTask.getModule();
            affectedModules.add(module);

            final String rootProjectPath = rootPathsMap.get(module);
            if (isEmpty(rootProjectPath)) continue;

            final String projectId = getExternalProjectId(module);
            if (projectId == null) continue;
            final String externalProjectPath = getExternalProjectPath(module);
            if (externalProjectPath == null || endsWith(externalProjectPath, "buildSrc")) continue;

            final DataNode<? extends ModuleData> moduleDataNode = moduleDataFinder.findMainModuleData(module);
            if (moduleDataNode == null) continue;

            // all buildSrc runtime projects will be built by gradle implicitly
            if (Boolean.parseBoolean(moduleDataNode.getData().getProperty(GradleBuildSrcProjectsResolver.BUILD_SRC_MODULE_PROPERTY))) {
                continue;
            }

            String gradlePath = GradleProjectResolverUtil.getGradlePath(module);
            if (gradlePath == null) continue;
            String taskPrefix = endsWithChar(gradlePath, ':') ? gradlePath : (gradlePath + ':');

            List<String> gradleTasks = ContainerUtil.mapNotNull(
                    findAll(moduleDataNode, ProjectKeys.TASK), node ->
                            node.getData().isInherited() ? null : trimStart(node.getData().getName(), taskPrefix));

            Collection<String> projectInitScripts = initScripts.getModifiable(rootProjectPath);
            Collection<String> buildRootTasks = buildTasksMap.getModifiable(rootProjectPath);
            final String moduleType = getExternalModuleType(module);

            if (!moduleBuildTask.isIncrementalBuild()) {
                projectInitScripts.add(String.format(FORCE_COMPILE_TASKS_INIT_SCRIPT_TEMPLATE, gradlePath));
            }
            String assembleTask = "assemble";
            if (GradleConstants.GRADLE_SOURCE_SET_MODULE_TYPE_KEY.equals(moduleType)) {
                String sourceSetName = GradleProjectResolverUtil.getSourceSetName(module);
                String gradleTask = isEmpty(sourceSetName) || "main".equals(sourceSetName) ? "classes" : sourceSetName + "Classes";
                if (gradleTasks.contains(gradleTask)) {
                    buildRootTasks.add(taskPrefix + gradleTask);
                }
                else if ("main".equals(sourceSetName) || "test".equals(sourceSetName)) {
                    buildRootTasks.add(taskPrefix + assembleTask);
                }
                // ---------------------------------------- //
                // TODO BEGIN: Extract custom Kotlin logic. //
                // ---------------------------------------- //
                else if (isNativeSourceModule(module)) {
                    // Add tasks for Kotlin/Native.
                    buildRootTasks.addAll(addPrefix(findNativeGradleBuildTasks(gradleTasks, sourceSetName), taskPrefix));
                }
                else if (isCommonProductionSourceModule(module)) {
                    // Add tasks for compiling metadata.
                    buildRootTasks.addAll(addPrefix(findMetadataBuildTasks(gradleTasks, sourceSetName), taskPrefix));
                }
                // ---------------------------------------- //
                // TODO END: Extract custom Kotlin logic.   //
                // ---------------------------------------- //
            }
            else {
                if (gradleTasks.contains("classes")) {
                    buildRootTasks.add(taskPrefix + "classes");
                    buildRootTasks.add(taskPrefix + "testClasses");
                }
                else if (gradleTasks.contains(assembleTask)) {
                    buildRootTasks.add(taskPrefix + assembleTask);
                }
            }
        }
        return affectedModules;
    }

    // ---------------------------------------- //
    // TODO BEGIN: Extract custom Kotlin logic. //
    // ---------------------------------------- //
    private static boolean isProjectWithNativeSourceOrCommonProductionSourceModules(Project project) {
        return CachedValuesManager.getManager(project).getCachedValue(
                project,
                () -> new CachedValueProvider.Result<>(
                        Arrays.stream(ModuleManager.getInstance(project).getModules()).anyMatch(
                                module -> isNativeSourceModule(module) || isCommonProductionSourceModule(module)
                        ),
                        ProjectRootModificationTracker.getInstance(project)
                ));
    }

    private static boolean isNativeSourceModule(Module module) {
        final KotlinFacet kotlinFacet = KotlinFacet.Companion.get(module);
        if (kotlinFacet == null) return false;

        final IdePlatform platform = kotlinFacet.getConfiguration().getSettings().getPlatform();
        if (platform == null) return false;

        return NativeIdePlatformUtil.isKotlinNative(platform);
    }

    private static boolean isCommonProductionSourceModule(Module module) {
        final KotlinFacet kotlinFacet = KotlinFacet.Companion.get(module);
        if (kotlinFacet == null) return false;

        final KotlinFacetSettings facetSettings = kotlinFacet.getConfiguration().getSettings();
        if (facetSettings.isTestModule()) return false;

        final IdePlatform platform = facetSettings.getPlatform();
        if (platform == null) return false;

        return CommonIdePlatformUtil.isCommon(platform);
    }

    private static Collection<String> findNativeGradleBuildTasks(Collection<String> gradleTasks, String sourceSetName) {
        // First, attempt to find Kotlin/Native convention Gradle task that unites all outputType-specific build tasks.
        final String conventionGradleTask = sourceSetName + "Binaries";
        if (gradleTasks.contains(conventionGradleTask)) {
            return Collections.singletonList(conventionGradleTask);
        }

        // If convention task not found, then attempt to find all appropriate build tasks for the given source set.
        final Collection<String> linkPrefixes;
        final String targetName;
        if (sourceSetName.endsWith("Main")) {
            targetName = StringUtil.substringBeforeLast(sourceSetName,"Main");
            linkPrefixes = ContainerUtil.newArrayList("link", "linkMain");
        }
        else if (sourceSetName.endsWith("Test")) {
            targetName = StringUtil.substringBeforeLast(sourceSetName,"Test");
            linkPrefixes = Collections.singletonList("linkTest");
        }
        else {
            targetName = sourceSetName;
            linkPrefixes = Collections.singletonList("link");
        }

        return linkPrefixes.stream()
                // get base task name (without disambiguation classifier)
                .map(linkPrefix -> linkPrefix + capitalize(targetName))
                // find all Gradle tasks that start with base task name
                .flatMap(nativeTaskName -> gradleTasks.stream().filter(taskName -> taskName.startsWith(nativeTaskName)))
                .collect(Collectors.toList());
    }

    private static Collection<String> findMetadataBuildTasks(Collection<String> gradleTasks, String sourceSetName) {
        if ("commonMain".equals(sourceSetName)) {
            final String metadataTaskName = "metadataMainClasses";
            if (gradleTasks.contains(metadataTaskName)) {
                return Collections.singletonList(metadataTaskName);
            }
        }

        return Collections.emptyList();
    }

    private static Collection<String> addPrefix(Collection<String> tasks, String taskPrefix) {
        return tasks.stream().map(task -> taskPrefix + task).collect(Collectors.toList());
    }
    // ---------------------------------------- //
    // TODO END: Extract custom Kotlin logic.   //
    // ---------------------------------------- //
}
