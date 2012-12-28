/*
 * Copyright 2010-2012 JetBrains s.r.o.
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

package org.jetbrains.jet.plugin.k2jsrun;

import com.intellij.ide.browsers.BrowsersConfiguration;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

public final class K2JSConfigurationSettings {

    @NotNull
    private String pageToOpenFilePath = "";

    @NotNull
    private String generatedFilePath;

    private boolean shouldOpenInBrowserAfterTranslation = false;

    @NotNull
    private BrowsersConfiguration.BrowserFamily browserFamily = BrowsersConfiguration.getInstance().getActiveBrowsers().get(0);

    public K2JSConfigurationSettings(@NotNull Project project) {
        String basePath = project.getBasePath();
        generatedFilePath = basePath != null ? basePath : "";
    }

    public K2JSConfigurationSettings() {
        generatedFilePath = "";
    }

    @NotNull
    public BrowsersConfiguration.BrowserFamily getBrowserFamily() {
        return browserFamily;
    }

    public void setBrowserFamily(@NotNull BrowsersConfiguration.BrowserFamily browserFamily) {
        this.browserFamily = browserFamily;
    }

    @NotNull
    public String getPageToOpenFilePath() {
        return pageToOpenFilePath;
    }

    public void setPageToOpenFilePath(@NotNull String pageToOpenFilePath) {
        this.pageToOpenFilePath = pageToOpenFilePath;
    }

    @NotNull
    public String getGeneratedFilePath() {
        return generatedFilePath;
    }

    public void setGeneratedFilePath(@NotNull String generatedFilePath) {
        this.generatedFilePath = generatedFilePath;
    }

    public boolean isShouldOpenInBrowserAfterTranslation() {
        return shouldOpenInBrowserAfterTranslation;
    }

    public void setShouldOpenInBrowserAfterTranslation(boolean shouldOpenInBrowserAfterTranslation) {
        this.shouldOpenInBrowserAfterTranslation = shouldOpenInBrowserAfterTranslation;
    }
}
