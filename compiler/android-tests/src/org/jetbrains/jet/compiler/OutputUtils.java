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

package org.jetbrains.jet.compiler;

import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.openapi.util.SystemInfo;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.compiler.run.RunUtils;
import org.jetbrains.jet.compiler.run.result.RunResult;

import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class OutputUtils {

    private final static Pattern EMULATOR_PROCESS_PATTERN = Pattern.compile("\\w*[\\s]+([0-9]*) .* java .* emulator .*");

    public static boolean isBuildFailed(String output) {
        return output.contains("BUILD FAILED") || output.contains("Build failed");
    }

    public static void checkResult(RunResult result) {
        if (!result.getStatus()) {
            throw new RuntimeException(result.getOutput());
        }
    }

    @Nullable
    public static String getPidFromPsCommand(String output) {
        if (!output.isEmpty()) {
            Matcher matcher = EMULATOR_PROCESS_PATTERN.matcher(output);
            if (matcher.find()) {
                return matcher.group(1);
            }
        }
        return null;
    }

    private OutputUtils() {
    }
}
