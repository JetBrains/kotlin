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

package org.jetbrains.jet.cli.jvm;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.config.CompilerConfiguration;
import org.jetbrains.jet.lang.resolve.java.CompilerSpecialMode;
import org.jetbrains.jet.utils.PathUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.jetbrains.jet.lang.resolve.java.CompilerSpecialMode.*;

/**
 * @author Evgeny Gerashchenko
 * @since 7/4/12
 */
public class CompilerConfigurationUtl {
    // TODO merge with similar K2JVMCompiler method
    public static CompilerConfiguration getDefaultConfiguration(@NotNull CompilerSpecialMode compilerSpecialMode) {
        List<File> classpath = new ArrayList<File>();
        if (includeJdk(compilerSpecialMode)) {
            classpath.add(PathUtil.findRtJar());
        }
        if (includeKotlinRuntime(compilerSpecialMode)) {
            classpath.add(PathUtil.getDefaultRuntimePath());
        }
        File[] annotationsPath = new File[0];
        if (includeJdkAnnotations(compilerSpecialMode)) {
            annotationsPath = new File[]{PathUtil.getJdkAnnotationsPath()};
        }

        CompilerConfiguration configuration = new CompilerConfiguration();
        configuration.putUserData(JVMConfigurationKeys.CLASSPATH_KEY, classpath.toArray(new File[classpath.size()]));
        configuration.putUserData(JVMConfigurationKeys.ANNOTATIONS_PATH_KEY, annotationsPath);
        return configuration;
    }

    private static boolean includeJdkAnnotations(@NotNull CompilerSpecialMode mode) {
        return mode == REGULAR || mode == STDLIB || mode == IDEA;
    }

    public static boolean includeKotlinRuntime(@NotNull CompilerSpecialMode mode) {
        return mode == REGULAR;
    }

    public static boolean includeJdk(@NotNull CompilerSpecialMode mode) {
        return mode != IDEA;
    }
}
