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

package org.jetbrains.jet.codegen;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.CompileCompilerDependenciesTest;
import org.jetbrains.jet.ConfigurationKind;
import org.jetbrains.jet.JetTestUtils;
import org.jetbrains.jet.TestJdkKind;
import org.jetbrains.jet.cli.jvm.JVMConfigurationKeys;
import org.jetbrains.jet.cli.jvm.compiler.JetCoreEnvironment;
import org.jetbrains.jet.config.CompilerConfiguration;

import java.io.File;

/**
 * @author udalov
 */
public class GenerateNotNullAssertionsTest extends CodegenTestCase {
    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    private void setUpEnvironment(boolean generateAssertions, File... extraClassPath) {
        CompilerConfiguration configuration = CompileCompilerDependenciesTest.compilerConfigurationForTests(
                ConfigurationKind.JDK_ONLY, TestJdkKind.MOCK_JDK, extraClassPath);

        configuration.put(JVMConfigurationKeys.GENERATE_NOT_NULL_ASSERTIONS, generateAssertions);

        myEnvironment = new JetCoreEnvironment(getTestRootDisposable(), configuration);
    }

    private void doTestGenerateAssertions(boolean generateAssertions, String ktFile) throws Exception {
        File javaClassesTempDirectory = compileJava("notNullAssertions/A.java");

        setUpEnvironment(generateAssertions, javaClassesTempDirectory);

        blackBoxMultiFile("OK", false, "notNullAssertions/AssertionChecker.kt", ktFile);
    }

    public void testGenerateAssertions() throws Exception {
        doTestGenerateAssertions(true, "notNullAssertions/doGenerateAssertions.kt");
    }

    public void testDoNotGenerateAssertions() throws Exception {
        doTestGenerateAssertions(false, "notNullAssertions/doNotGenerateAssertions.kt");
    }
}
