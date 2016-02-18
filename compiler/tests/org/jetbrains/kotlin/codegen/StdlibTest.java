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

package org.jetbrains.kotlin.codegen;

import com.intellij.testFramework.TestRunnerUtil;
import junit.framework.*;
import junit.textui.TestRunner;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys;
import org.jetbrains.kotlin.cli.common.messages.PrintingMessageCollector;
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles;
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment;
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinToJVMBytecodeCompiler;
import org.jetbrains.kotlin.cli.jvm.config.JvmContentRootsKt;
import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime;
import org.jetbrains.kotlin.codegen.state.GenerationState;
import org.jetbrains.kotlin.codegen.state.GenerationStateEventCallback;
import org.jetbrains.kotlin.config.CompilerConfiguration;
import org.jetbrains.kotlin.config.ContentRootsKt;
import org.jetbrains.kotlin.descriptors.ClassDescriptor;
import org.jetbrains.kotlin.psi.KtClass;
import org.jetbrains.kotlin.psi.KtDeclaration;
import org.jetbrains.kotlin.psi.KtFile;
import org.jetbrains.kotlin.resolve.BindingContext;
import org.jetbrains.kotlin.resolve.BindingContextUtils;
import org.jetbrains.kotlin.resolve.lazy.KotlinTestWithEnvironment;
import org.jetbrains.kotlin.test.ConfigurationKind;
import org.jetbrains.kotlin.test.KotlinTestUtils;
import org.jetbrains.kotlin.test.TestJdkKind;

import java.io.File;
import java.lang.reflect.Modifier;

public class StdlibTest extends KotlinTestWithEnvironment {
    @Override
    protected KotlinCoreEnvironment createEnvironment() {
        CompilerConfiguration configuration = KotlinTestUtils.compilerConfigurationForTests(ConfigurationKind.ALL, TestJdkKind.FULL_JDK);

        File junitJar = new File("libraries/lib/junit-4.11.jar");
        assertTrue(junitJar.exists());
        JvmContentRootsKt.addJvmClasspathRoot(configuration, junitJar);

        ContentRootsKt.addKotlinSourceRoot(configuration, KotlinTestUtils.getHomeDirectory() + "/libraries/stdlib/test");
        configuration.put(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY, PrintingMessageCollector.PLAIN_TEXT_TO_SYSTEM_ERR);

        return KotlinCoreEnvironment.createForTests(getTestRootDisposable(), configuration, EnvironmentConfigFiles.JVM_CONFIG_FILES);
    }

    public void testStdlib() throws ClassNotFoundException {
        GenerationState state = KotlinToJVMBytecodeCompiler.INSTANCE.analyzeAndGenerate(
                getEnvironment(), GenerationStateEventCallback.Companion.getDO_NOTHING());
        if (state == null) {
            fail("There were compilation errors");
        }

        GeneratedClassLoader classLoader = new GeneratedClassLoader(
                state.getFactory(), ForTestCompileRuntime.runtimeAndReflectJarClassLoader()
        ) {
            @Override
            public Class<?> loadClass(@NotNull String name) throws ClassNotFoundException {
                if (name.startsWith("junit.") || name.startsWith("org.junit.")) {
                    return StdlibTest.class.getClassLoader().loadClass(name);
                }
                return super.loadClass(name);
            }
        };

        TestSuite tests = new TestSuite("Standard Library Tests");

        for (KtFile file : getEnvironment().getSourceFiles()) {
            // Skip JS tests
            if (file.getVirtualFile().getPath().contains("/js/")) continue;

            for (KtDeclaration declaration : file.getDeclarations()) {
                if (!(declaration instanceof KtClass)) continue;

                ClassDescriptor descriptor = (ClassDescriptor) BindingContextUtils.getNotNull(
                        state.getBindingContext(), BindingContext.DECLARATION_TO_DESCRIPTOR, declaration
                );

                Test test = createTest(classLoader, state.getTypeMapper().mapClass(descriptor).getClassName());

                if (test != null) {
                    tests.addTest(test);
                }
            }
        }

        TestResult result = new TestRunner(System.err).doRun(tests);
        if (!result.wasSuccessful()) {
            fail("Some stdlib tests failed, see stderr for details");
        }
    }

    @Nullable
    private static Test createTest(@NotNull ClassLoader classLoader, @NotNull String className) {
        try {
            Class<?> aClass = classLoader.loadClass(className);
            if (Modifier.isAbstract(aClass.getModifiers()) ||
                !Modifier.isPublic(aClass.getModifiers()) ||
                !Modifier.isPublic(aClass.getConstructor().getModifiers())) {
                return null;
            }

            return TestCase.class.isAssignableFrom(aClass) ? new TestSuite(aClass) :
                   TestRunnerUtil.isJUnit4TestClass(aClass) ? new JUnit4TestAdapter(aClass) : null;
        }
        catch (NoSuchMethodException e) {
            // Ignore test classes we can't instantiate
            return null;
        }
        catch (ClassNotFoundException e) {
            return null;
        }
    }
}
