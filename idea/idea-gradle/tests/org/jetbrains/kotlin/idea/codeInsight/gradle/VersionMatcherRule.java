/*
 * Copyright 2010-2016 JetBrains s.r.o.
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
package org.jetbrains.kotlin.idea.codeInsight.gradle;

import org.gradle.util.GradleVersion;
import org.hamcrest.CoreMatchers;
import org.hamcrest.CustomMatcher;
import org.hamcrest.Matcher;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.gradle.tooling.annotation.TargetVersions;
import org.jetbrains.plugins.gradle.tooling.util.VersionMatcher;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

// copy of org.jetbrains.plugins.gradle.tooling.VersionMatcherRule
public class VersionMatcherRule extends TestWatcher {

  @Nullable
  private CustomMatcher myMatcher;

  @NotNull
  public Matcher getMatcher() {
    return myMatcher != null ? myMatcher : CoreMatchers.anything();
  }

  @Override
  protected void starting(Description d) {
    final TargetVersions targetVersions = d.getAnnotation(TargetVersions.class);
    if (targetVersions == null) return;

    myMatcher = new CustomMatcher<String>("Gradle version '" + targetVersions.value() + "'") {
      @Override
      public boolean matches(Object item) {
        return item instanceof String && new VersionMatcher(GradleVersion.version(item.toString())).isVersionMatch(targetVersions);
      }
    };
  }
}
