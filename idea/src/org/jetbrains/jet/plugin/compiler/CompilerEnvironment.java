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

package org.jetbrains.jet.plugin.compiler;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.cli.common.messages.MessageCollector;
import org.jetbrains.jet.utils.PathUtil;

import java.io.File;

import static org.jetbrains.jet.cli.common.messages.CompilerMessageLocation.NO_LOCATION;
import static org.jetbrains.jet.cli.common.messages.CompilerMessageSeverity.ERROR;

/**
 * @author Pavel Talanov
 */
public final class CompilerEnvironment {

    @NotNull
    public static CompilerEnvironment getEnvironmentFor(boolean tests, File mainOutput, File outputDirectoryForTests) {
        final File outputDir = tests ? outputDirectoryForTests : mainOutput;
        File kotlinHome = PathUtil.getDefaultCompilerPath();
        return new CompilerEnvironment(kotlinHome, outputDir);
    }

    @Nullable
    private final File kotlinHome;
    @Nullable
    private final File output;

    public CompilerEnvironment(@Nullable File home, @Nullable File output) {
        this.kotlinHome = home;
        this.output = output;
    }

    public boolean success() {
        return kotlinHome != null && output != null;
    }

    @NotNull
    public File getKotlinHome() {
        assert kotlinHome != null;
        return kotlinHome;
    }

    @NotNull
    public File getOutput() {
        assert output != null;
        return output;
    }

    public void reportErrorsTo(@NotNull MessageCollector messageCollector) {
        if (output == null) {
            messageCollector.report(ERROR, "[Internal Error] No output directory", NO_LOCATION);
        }
        if (kotlinHome == null) {
            messageCollector.report(ERROR, "Cannot find kotlinc home. Make sure plugin is properly installed", NO_LOCATION);
        }
    }

}
