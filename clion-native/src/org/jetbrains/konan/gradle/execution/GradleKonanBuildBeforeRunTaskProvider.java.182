/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.konan.gradle.execution;

import com.intellij.execution.BeforeRunTask;
import com.intellij.execution.BeforeRunTaskProvider;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.util.Key;
import com.jetbrains.cidr.CidrBundle;
import org.jetbrains.annotations.NotNull;

public class GradleKonanBuildBeforeRunTaskProvider extends BeforeRunTaskProvider {
  private static final Key<BuildBeforeRunTask> ID = Key.create(BuildBeforeRunTask.class.getName());

  @Override
  public Key<BuildBeforeRunTask> getId() {
    return ID;
  }

  @Override
  public String getName() {
    return CidrBundle.message("build");
  }

  @Override
  public String getDescription(BeforeRunTask task) {
    return CidrBundle.message("build");
  }

  @Override
  public boolean isSingleton() {
    return true;
  }


  @Override
  public BeforeRunTask createTask(@NotNull RunConfiguration runConfiguration) {
    if (!(runConfiguration instanceof GradleKonanAppRunConfiguration)) return null;
    return new BuildBeforeRunTask();
  }

  @Override
  public boolean canExecuteTask(@NotNull RunConfiguration configuration, @NotNull BeforeRunTask task) {
    return configuration instanceof GradleKonanAppRunConfiguration;
  }

  @Override
  public boolean executeTask(DataContext context,
                             @NotNull RunConfiguration configuration,
                             @NotNull ExecutionEnvironment env,
                             @NotNull BeforeRunTask task) {
    if (configuration instanceof GradleKonanAppRunConfiguration) {
      GradleKonanAppRunConfiguration gradleAppRunConfiguration = (GradleKonanAppRunConfiguration)configuration;

      return GradleKonanBuild.INSTANCE.build(configuration.getProject(), env, gradleAppRunConfiguration);
    }
    return false;
  }

  private static class BuildBeforeRunTask extends BeforeRunTask<BuildBeforeRunTask> {
    private BuildBeforeRunTask() {
      super(ID);
      setEnabled(true);
    }
  }
}
