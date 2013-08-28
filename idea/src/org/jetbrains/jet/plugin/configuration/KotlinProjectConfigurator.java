package org.jetbrains.jet.plugin.configuration;

import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

public interface KotlinProjectConfigurator {
    ExtensionPointName<KotlinProjectConfigurator> EP_NAME = ExtensionPointName.create("org.jetbrains.kotlin.projectConfigurator");

    boolean isConfigured(@NotNull Module module);

    boolean isApplicable(@NotNull Module module);

    void configure(Project project);

    @NotNull String getPresentableText();

    @NotNull String getName();
}
