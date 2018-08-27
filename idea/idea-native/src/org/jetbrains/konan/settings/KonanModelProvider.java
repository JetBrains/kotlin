/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.konan.settings;

import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.project.Project;
import com.intellij.util.messages.Topic;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.Collection;

@ApiStatus.Experimental
public interface KonanModelProvider {
    Topic<Runnable> RELOAD_TOPIC = new Topic<>("Kotlin/Native Project Model Updater", Runnable.class);

    ExtensionPointName<KonanModelProvider> EP_NAME = ExtensionPointName.create("org.jetbrains.kotlin.native.konanModelProvider");

    @NotNull
    Collection<KonanArtifact> getArtifacts(@NotNull Project project);

    @Nullable
    Path getKonanHome(@NotNull Project project);

    boolean reloadLibraries(@NotNull Project project, @NotNull Collection<Path> libraryPaths);
}
