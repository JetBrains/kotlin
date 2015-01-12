/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.k2jsrun;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.RunProfile;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.GenericProgramRunner;
import com.intellij.execution.ui.RunContentDescriptor;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static org.jetbrains.kotlin.idea.k2jsrun.K2JSRunnerUtils.copyJSFileFromOutputToDestination;
import static org.jetbrains.kotlin.idea.k2jsrun.K2JSRunnerUtils.openBrowser;

public final class K2JSBrowserProgramRunner extends GenericProgramRunner {
    @Nullable
    @Override
    protected RunContentDescriptor doExecute(
            Project project,
            RunProfileState state,
            RunContentDescriptor contentToReuse,
            ExecutionEnvironment env
    ) throws ExecutionException {
        if (project == null) {
            return null;
        }
        try {
            copyJSFileFromOutputToDestination(project, K2JSRunnerUtils.getSettings(env));
            openBrowser(K2JSRunnerUtils.getSettings(env));
        }
        catch (Throwable e) {
            throw new ExecutionException(e);
        }
        return null;
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
