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

package org.jetbrains.jet.cli.common.util;

import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.utils.PathUtil;

import java.io.File;

/**
 * @author Maxim.Manuylov
 *         Date: 20.05.12
 */
public class CompilerPathUtil {
    private CompilerPathUtil() {}

    @Nullable
    public static File getSDKHome() {
        File compilerJar = new File(PathUtil.getJarPathForClass(CompilerPathUtil.class));
        if (!compilerJar.exists()) return null;

        if (compilerJar.getName().equals(PathUtil.KOTLIN_COMPILER_JAR)) {
            File lib = compilerJar.getParentFile();
            File answer = lib.getParentFile();
            return answer.exists() ? answer : null;
        }

        File current = new File("").getAbsoluteFile(); // CWD

        do {
            File atDevHome = new File(current, "dist/kotlinc");
            if (atDevHome.exists()) return atDevHome;
            current = current.getParentFile();
        } while (current != null);

        return null;
    }

    @Nullable
    public static File getRuntimePath() {
        return PathUtil.getRuntimePath(getSDKHome());
    }

    @Nullable
    public static File getJdkAnnotationsPath() {
        return PathUtil.getJdkAnnotationsPath(getSDKHome());
    }
}
