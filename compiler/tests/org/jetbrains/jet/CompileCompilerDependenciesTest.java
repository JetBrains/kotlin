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

package org.jetbrains.jet;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.codegen.forTestCompile.ForTestCompileBuiltins;
import org.jetbrains.jet.codegen.forTestCompile.ForTestCompileRuntime;
import org.jetbrains.jet.codegen.forTestCompile.ForTestPackJdkAnnotations;
import org.jetbrains.jet.config.CompilerConfiguration;
import org.jetbrains.jet.utils.PathUtil;
import org.junit.Test;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import static org.jetbrains.jet.ConfigurationKind.ALL;
import static org.jetbrains.jet.ConfigurationKind.JDK_AND_ANNOTATIONS;
import static org.jetbrains.jet.cli.jvm.JVMConfigurationKeys.ANNOTATIONS_PATH_KEY;
import static org.jetbrains.jet.cli.jvm.JVMConfigurationKeys.CLASSPATH_KEY;

public class CompileCompilerDependenciesTest {

    @Test
    public void compileBuiltins() {
        ForTestCompileBuiltins.builtinsJarForTests();
    }

    @Test
    public void packJdkAnnotations() {
        ForTestPackJdkAnnotations.jdkAnnotationsForTests();
    }

    @Test
    public void compileRuntime() {
        ForTestCompileRuntime.runtimeJarForTests();
    }

    @NotNull
    public static CompilerConfiguration compilerConfigurationForTests(@NotNull ConfigurationKind configurationKind,
            @NotNull TestJdkKind jdkKind, File... extraClasspath) {
        return compilerConfigurationForTests(configurationKind, jdkKind, Arrays.asList(extraClasspath), Collections.<File>emptyList());
    }

    @NotNull
    public static CompilerConfiguration compilerConfigurationForTests(@NotNull ConfigurationKind configurationKind,
            @NotNull TestJdkKind jdkKind, @NotNull Collection<File> extraClasspath, @NotNull Collection<File> priorityClasspath) {
        CompilerConfiguration configuration = new CompilerConfiguration();
        configuration.addAll(CLASSPATH_KEY, priorityClasspath);
        configuration.add(CLASSPATH_KEY, jdkKind == TestJdkKind.MOCK_JDK ? JetTestUtils.findMockJdkRtJar() : PathUtil.findRtJar());
        if (configurationKind == ALL) {
            configuration.add(CLASSPATH_KEY, ForTestCompileRuntime.runtimeJarForTests());
        }
        configuration.addAll(CLASSPATH_KEY, extraClasspath);

        if (configurationKind == ALL || configurationKind == JDK_AND_ANNOTATIONS) {
            configuration.add(ANNOTATIONS_PATH_KEY, ForTestPackJdkAnnotations.jdkAnnotationsForTests());
        }

        return configuration;
    }
}
