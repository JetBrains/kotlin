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

import com.intellij.execution.ExecutionException;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.RunProfile;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.GenericProgramRunner;
import com.intellij.execution.ui.RunContentDescriptor;
import com.intellij.ide.browsers.BrowsersConfiguration;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFileManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.plugin.actions.TranslateToJsAction;

/**
 * @author Pavel Talanov
 */
public final class K2JSBrowserProgramRunner extends GenericProgramRunner {
    @Override
    protected RunContentDescriptor doExecute(Project project,
                                             Executor executor,
                                             RunProfileState state,
                                             RunContentDescriptor contentToReuse,
                                             ExecutionEnvironment env) throws ExecutionException {
        if (project == null) {
            return null;
        }
        try {
            K2JSConfigurationSettings configurationSettings = getSettings(state);
            try {
                TranslateToJsAction.doPerform(project);
            }
            catch (Throwable e) {
                TranslateToJsAction.notifyFailure(e);
                return null;
            }
            openBrowser(configurationSettings);
        }
        catch (Exception e) {
            throw new ExecutionException(e);
        }
        return null;
    }

    private static void openBrowser(@NotNull K2JSConfigurationSettings configurationSettings) {
        String filePath = configurationSettings.getFilePath();
        String url = VirtualFileManager.constructUrl(LocalFileSystem.PROTOCOL, filePath);
        BrowsersConfiguration.launchBrowser(BrowsersConfiguration.BrowserFamily.FIREFOX, url);
    }

    @NotNull
    private static K2JSConfigurationSettings getSettings(@NotNull RunProfileState state) {
        RunProfile profile = state.getRunnerSettings().getRunProfile();
        assert profile instanceof K2JSRunConfiguration;
        return ((K2JSRunConfiguration)profile).settings();
    }

    @NotNull
    @Override
    public String getRunnerId() {
        return "K2JSBrowserRunner";
    }

    @Override
    public boolean canRun(@NotNull String executorId, @NotNull RunProfile profile) {
        return (profile instanceof K2JSRunConfiguration);
    }
}
