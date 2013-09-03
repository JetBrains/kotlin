package org.jetbrains.jet.plugin.configuration;

import com.intellij.openapi.module.Module;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.plugin.JetPluginUtil;
import org.jetbrains.plugins.groovy.lang.psi.GroovyFile;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.blocks.GrClosableBlock;

public class KotlinAndroidGradleModuleConfigurator extends KotlinWithGradleConfigurator {
    public static final String NAME = "android-gradle";

    private static final String APPLY_KOTLIN_ANDROID = "apply plugin: 'kotlin-android'";

    @NotNull
    @Override
    public String getName() {
        return NAME;
    }

    @NotNull
    @Override
    public String getPresentableText() {
        return "As Android project with Gradle";
    }

    @Override
    public boolean isApplicable(@NotNull Module module) {
        return JetPluginUtil.isAndroidGradleModule(module);
    }

    @Override
    protected String getApplyPluginDirective() {
        return APPLY_KOTLIN_ANDROID;
    }

    @Override
    protected void addSourceSetsBlock(@NotNull GroovyFile file) {
        GrClosableBlock androidBlock = getAndroidBlock(file);
        addLastExpressionInBlockIfNeeded(SOURCE_SET, getSourceSetsBlock(androidBlock));
    }

    @NotNull
    private static GrClosableBlock getAndroidBlock(@NotNull GroovyFile file) {
        return getBlockOrCreate(file, "android");
    }

    KotlinAndroidGradleModuleConfigurator() {
    }
}
