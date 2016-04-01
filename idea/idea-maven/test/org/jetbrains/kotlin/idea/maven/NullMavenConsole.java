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

package org.jetbrains.kotlin.idea.maven;

import com.intellij.execution.process.ProcessHandler;
import org.jetbrains.idea.maven.execution.MavenExecutionOptions;
import org.jetbrains.idea.maven.project.MavenConsole;

public class NullMavenConsole extends MavenConsole {
    public NullMavenConsole() {
        super(MavenExecutionOptions.LoggingLevel.DISABLED, false);
    }

    @Override
    public boolean canPause() {
        return false;
    }

    @Override
    public boolean isOutputPaused() {
        return false;
    }

    @Override
    public void setOutputPaused(boolean outputPaused) {
    }

    @Override
    public void attachToProcess(ProcessHandler processHandler) {
    }

    @Override
    protected void doPrint(String text, MavenConsole.OutputType type) {
    }
}
