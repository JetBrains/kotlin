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
import org.jetbrains.jet.cli.jvm.JVMConfigurationKeys;
import org.jetbrains.jet.codegen.forTestCompile.ForTestCompileBuiltins;
import org.jetbrains.jet.codegen.forTestCompile.ForTestCompileRuntime;
import org.jetbrains.jet.codegen.forTestCompile.ForTestPackJdkAnnotations;
import org.jetbrains.jet.config.CompilerConfiguration;
import org.jetbrains.jet.utils.PathUtil;
import org.junit.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.jetbrains.jet.ConfigurationKind.ALL;
import static org.jetbrains.jet.ConfigurationKind.JDK_AND_ANNOTATIONS;

/**
 * @author Stepan Koltsov
 */
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

    public static CompilerConfiguration compilerConfigurationForTests(@NotNull ConfigurationKind configurationKind, boolean mockJdk) {
        List<File> classpath = new ArrayList<File>();
        classpath.add(mockJdk ? JetTestUtils.findMockJdkRtJar() : PathUtil.findRtJar());
        if (configurationKind == ALL) {
            classpath.add(ForTestCompileRuntime.runtimeJarForTests());
        }

        File[] annotationsPath = new File[0];
        if (configurationKind == ALL || configurationKind == JDK_AND_ANNOTATIONS) {
            annotationsPath = new File[]{ForTestPackJdkAnnotations.jdkAnnotationsForTests()};
        }

        CompilerConfiguration configuration = new CompilerConfiguration();
        configuration.putUserData(JVMConfigurationKeys.CLASSPATH_KEY, classpath.toArray(new File[classpath.size()]));
        configuration.putUserData(JVMConfigurationKeys.ANNOTATIONS_PATH_KEY, annotationsPath);
        return configuration;
    }
}
