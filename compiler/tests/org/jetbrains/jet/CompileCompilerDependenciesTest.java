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
import org.jetbrains.jet.codegen.forTestCompile.ForTestPackJdkAnnotations;
import org.jetbrains.jet.codegen.forTestCompile.ForTestCompileRuntime;
import org.jetbrains.jet.config.CompilerConfiguration;
import org.jetbrains.jet.lang.resolve.java.CompilerDependencies;
import org.jetbrains.jet.lang.resolve.java.CompilerSpecialMode;
import org.jetbrains.jet.utils.PathUtil;
import org.junit.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

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

    /**
     * @see CompilerDependencies#compilerDependenciesForProduction(org.jetbrains.jet.lang.resolve.java.CompilerSpecialMode)
     */
    @NotNull
    public static CompilerDependencies compilerDependenciesForTests(@NotNull CompilerSpecialMode compilerSpecialMode, boolean mockJdk) {
        return new CompilerDependencies(
                compilerSpecialMode,
                compilerSpecialMode.includeJdk() ? (mockJdk ? JetTestUtils.findMockJdkRtJar() : PathUtil.findRtJar()) : null,
                compilerSpecialMode.includeJdkAnnotations() ? ForTestPackJdkAnnotations.jdkAnnotationsForTests() : null,
                compilerSpecialMode.includeKotlinRuntime() ? ForTestCompileRuntime.runtimeJarForTests() : null);
    }

    public static CompilerConfiguration compilerConfigurationForTests(@NotNull CompilerSpecialMode compilerSpecialMode, boolean mockJdk) {
        List<File> classpath = new ArrayList<File>();
        if (compilerSpecialMode.includeJdk()) {
            classpath.add(mockJdk ? JetTestUtils.findMockJdkRtJar() : PathUtil.findRtJar());
        }
        if (compilerSpecialMode.includeKotlinRuntime()) {
            classpath.add(ForTestCompileRuntime.runtimeJarForTests());
        }
        File[] annotationsPath = new File[0];
        if (compilerSpecialMode.includeJdkAnnotations()) {
            annotationsPath = new File[]{ForTestPackJdkAnnotations.jdkAnnotationsForTests()};
        }

        CompilerConfiguration configuration = new CompilerConfiguration();
        configuration.putUserData(JVMConfigurationKeys.CLASSPATH_KEY, classpath.toArray(new File[classpath.size()]));
        configuration.putUserData(JVMConfigurationKeys.ANNOTATIONS_PATH_KEY, annotationsPath);
        return configuration;
    }
}
