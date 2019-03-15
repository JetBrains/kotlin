/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.inspections.gradle;

import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.externalSystem.model.DataNode;
import com.intellij.openapi.externalSystem.model.project.ModuleData;
import org.gradle.tooling.model.idea.IdeaProject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;

public interface KotlinGradleModelFacade {
    ExtensionPointName<KotlinGradleModelFacade> EP_NAME = ExtensionPointName.create("org.jetbrains.kotlin.gradleModelFacade");

    @Deprecated
    @Nullable
    default String getResolvedKotlinStdlibVersionByModuleData(@NotNull DataNode<?> moduleData, @NotNull List<String> libraryIds) {
        return null;
    }

    @Nullable
    default String getResolvedVersionByModuleData(
            @NotNull DataNode<?> moduleData,
            @NotNull String groupId,
            @NotNull List<String> libraryIds
    ) {
        //noinspection deprecation
        return getResolvedKotlinStdlibVersionByModuleData(moduleData, libraryIds);
    }

    @NotNull
    Collection<DataNode<ModuleData>> getDependencyModules(@NotNull DataNode<ModuleData> ideModule, @NotNull IdeaProject gradleIdeaProject);
}
