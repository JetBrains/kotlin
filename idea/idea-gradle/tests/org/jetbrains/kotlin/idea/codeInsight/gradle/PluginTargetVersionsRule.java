/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.idea.codeInsight.gradle;

import org.hamcrest.CustomMatcher;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.gradle.tooling.annotation.PluginTargetVersions;
import org.jetbrains.plugins.gradle.tooling.annotation.TargetVersions;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

// modified copy of org.jetbrains.plugins.gradle.tooling.VersionMatcherRule
public class PluginTargetVersionsRule extends TestWatcher {

    private final String[][] versionRules = {{"1.3.0 <=> 1.4.0", "4.0+"}, {"1.4.0+", "6.0+"}};
    //Specify rules for different version Kotlin Gradle plugin and Gradle

    private class TargetVersionsImpl implements TargetVersions {
        private final String value;

        TargetVersionsImpl(String value) {
            this.value = value;
        }

        @Override
        public String value() {
            return value;
        }

        @Override
        public boolean checkBaseVersions() {
            return true;
        }

        @Override
        public Class<? extends Annotation> annotationType() {
            return null;
        }
    }

    @Nullable
    private CustomMatcher<String> gradleVersionMatcher;

    @Nullable
    private CustomMatcher<String> pluginVersionMatcher;

    private boolean skipForMaster;

    private final List<CustomMatcher<String>[]> versionCombinationsMatcher = new ArrayList<>();

    public boolean matches(String gradleVersion, String pluginVersion, boolean isMaster) {
        if (skipForMaster && isMaster) {
            return false;
        }
        boolean matchGradleVersion = gradleVersionMatcher == null || gradleVersionMatcher.matches(gradleVersion);
        boolean pluginVersionMatches = pluginVersionMatcher == null || pluginVersionMatcher.matches(pluginVersion);
        return matchGradleVersion && pluginVersionMatches &&
               versionCombinationsMatcher.stream().anyMatch(i -> i[0].matches(pluginVersion) && i[1].matches(gradleVersion));
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void starting(Description d) {
        Arrays.stream(versionRules).forEach(i -> versionCombinationsMatcher.add(
                new CustomMatcher[] {
                        VersionMatcherRule.produceMatcher("Plugin", new TargetVersionsImpl(i[0])),
                        VersionMatcherRule.produceMatcher("Gradle", new TargetVersionsImpl(i[1]))
                }));

        PluginTargetVersions pluginTargetVersions = d.getAnnotation(PluginTargetVersions.class);
        if (d.getAnnotation(TargetVersions.class) != null && pluginTargetVersions != null) {
            throw new IllegalArgumentException(String.format("Annotations %s and %s could not be used together. ",
                                                             TargetVersions.class.getName(), PluginTargetVersions.class.getName()));
        }
        if (pluginTargetVersions == null) return;

        gradleVersionMatcher = pluginTargetVersions.gradleVersion().isEmpty() ? null : VersionMatcherRule
                .produceMatcher("Gradle", new TargetVersionsImpl(pluginTargetVersions.gradleVersion()));
        pluginVersionMatcher = pluginTargetVersions.pluginVersion().isEmpty() ? null : VersionMatcherRule
                .produceMatcher("Plugin", new TargetVersionsImpl(pluginTargetVersions.pluginVersion()));
        skipForMaster = pluginTargetVersions.skipForMaster();
    }
}
