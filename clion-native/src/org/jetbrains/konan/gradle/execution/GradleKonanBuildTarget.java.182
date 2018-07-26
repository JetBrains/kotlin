/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.konan.gradle.execution;

import com.intellij.icons.AllIcons;
import com.intellij.util.containers.ContainerUtil;
import com.jetbrains.cidr.execution.CidrBuildTarget;
import icons.CLionIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * @author Vladislav.Soroka
 */
public class GradleKonanBuildTarget implements Serializable, CidrBuildTarget<GradleKonanConfiguration> {
  public static final Icon EXECUTABLE_ICON = CLionIcons.CMakeTarget_Executable;
  public static final Icon LIBRARY_ICON = AllIcons.Modules.Library;
  public static final Icon TEST_ICON = AllIcons.Modules.Library;

  @NotNull private final String myId;
  @NotNull private final String myName;
  @NotNull private final String myProjectName;
  @NotNull private final List<GradleKonanConfiguration> myConfigurations;

  public GradleKonanBuildTarget(@NotNull String id,
                                @NotNull String name,
                                @NotNull String projectName,
                                @NotNull List<GradleKonanConfiguration> configurations) {
    myId = id;
    myName = name;
    myProjectName = projectName;
    myConfigurations = Collections.unmodifiableList(configurations);
  }

  @NotNull
  public String getId() {
    return myId;
  }

  @NotNull
  @Override
  public String getName() {
    return myName;
  }

  @NotNull
  @Override
  public String getProjectName() {
    return myProjectName;
  }

  @Nullable
  @Override
  public Icon getIcon() {
    GradleKonanConfiguration target = ContainerUtil.getFirstItem(getBuildConfigurations());
    if (target == null || target.getTargetType() == null) return null;
    switch (target.getTargetType()) {
      case PROGRAM:
        return EXECUTABLE_ICON;
      case LIBRARY:
      case STATIC:
      case DYNAMIC:
      case FRAMEWORK:
        return LIBRARY_ICON;
      default:
        return null;
    }
  }

  @Override
  public boolean isExecutable() {
    return ContainerUtil.exists(getBuildConfigurations(), configuration -> configuration.isExecutable());
  }

  @NotNull
  public List<GradleKonanConfiguration> getBuildConfigurations() {
    return myConfigurations;
  }

  @Override
  public String toString() {
    return myName + " [" + myConfigurations.size() + " configs]";
  }
}
