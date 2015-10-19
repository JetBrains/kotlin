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

import com.intellij.testFramework.UsefulTestCase;
import junit.extensions.TestSetup;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys;
import org.jetbrains.kotlin.cli.common.messages.PrintingMessageCollector;
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles;
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment;
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinToJVMBytecodeCompiler;
import org.jetbrains.kotlin.cli.jvm.config.JvmContentRootsKt;
import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime;
import org.jetbrains.kotlin.codegen.state.GenerationState;
import org.jetbrains.kotlin.codegen.state.JetTypeMapper;
import org.jetbrains.kotlin.config.CompilerConfiguration;
import org.jetbrains.kotlin.config.ContentRootsKt;
import org.jetbrains.kotlin.descriptors.ClassDescriptor;
import org.jetbrains.kotlin.psi.KtClass;
import org.jetbrains.kotlin.psi.KtDeclaration;
import org.jetbrains.kotlin.psi.KtFile;
import org.jetbrains.kotlin.resolve.BindingContext;
import org.jetbrains.kotlin.resolve.BindingContextUtils;
import org.jetbrains.kotlin.test.ConfigurationKind;
import org.jetbrains.kotlin.test.JetTestUtils;
import org.jetbrains.kotlin.test.TestJdkKind;
import org.jetbrains.kotlin.types.KtType;

import java.io.File;
import java.lang.reflect.Modifier;

import static org.jetbrains.kotlin.types.TypeUtils.getAllSupertypes;

@SuppressWarnings("JUnitTestCaseWithNoTests")
public class TestlibTest extends UsefulTestCase {
    public static Test suite() {
        return new TestlibTest().buildTestSuite();
    }

    private TestSuite suite;
    private File junitJar;
    private GeneratedClassLoader classLoader;
    private JetTypeMapper typeMapper;
    private GenerationState generationState;
    private KotlinCoreEnvironment myEnvironment;

    private Test buildTestSuite() {
        suite = new TestSuite("stdlib_test");

        return new TestSetup(suite) {
            @Override
            protected void setUp() throws Exception {
                TestlibTest.this.setUp();
            }

            @Override
            protected void tearDown() throws Exception {
                TestlibTest.this.tearDown();
            }
        };
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();

        CompilerConfiguration configuration = JetTestUtils.compilerConfigurationForTests(ConfigurationKind.ALL, TestJdkKind.FULL_JDK);
        JvmContentRootsKt.addJvmClasspathRoot(configuration, JetTestUtils.getAnnotationsJar());

        junitJar = new File("libraries/lib/junit-4.11.jar");
        assertTrue(junitJar.exists());
        JvmContentRootsKt.addJvmClasspathRoot(configuration, junitJar);

        ContentRootsKt.addKotlinSourceRoot(configuration, JetTestUtils.getHomeDirectory() + "/libraries/stdlib/test");
        ContentRootsKt.addKotlinSourceRoot(configuration, JetTestUtils.getHomeDirectory() + "/libraries/kunit/src");
        configuration.put(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY, PrintingMessageCollector.PLAIN_TEXT_TO_SYSTEM_ERR);

        myEnvironment = KotlinCoreEnvironment.createForTests(getTestRootDisposable(), configuration, EnvironmentConfigFiles.JVM_CONFIG_FILES);

        generationState = KotlinToJVMBytecodeCompiler.analyzeAndGenerate(myEnvironment);
        if (generationState == null) {
            throw new RuntimeException("There were compilation errors");
        }

        classLoader = new GeneratedClassLoader(generationState.getFactory(), ForTestCompileRuntime.runtimeAndReflectJarClassLoader()) {
            @Override
            public Class<?> loadClass(@NotNull String name) throws ClassNotFoundException {
                if (name.startsWith("junit.") || name.startsWith("org.junit.")) {
                    //In other way we don't find any test cause will have two different TestCase classes!
                    return TestlibTest.class.getClassLoader().loadClass(name);
                }
                return super.loadClass(name);
            }
        };

        typeMapper = generationState.getTypeMapper();

        for (KtFile jetFile : myEnvironment.getSourceFiles()) {
            for (KtDeclaration declaration : jetFile.getDeclarations()) {
                if (!(declaration instanceof KtClass)) continue;

                ClassDescriptor descriptor = (ClassDescriptor) BindingContextUtils.getNotNull(generationState.getBindingContext(),
                                                                                              BindingContext.DECLARATION_TO_DESCRIPTOR,
                                                                                              declaration);

                for (KtType superType : getAllSupertypes(descriptor.getDefaultType())) {
                    if (!"junit/framework/Test".equals(typeMapper.mapType(superType).getInternalName())) continue;

                    String name = typeMapper.mapClass(descriptor).getInternalName();

                    System.out.println(name);

                    @SuppressWarnings("unchecked")
                    Class<TestCase> aClass = (Class<TestCase>) classLoader.loadClass(name.replace('/', '.'));

                    if (!Modifier.isAbstract(aClass.getModifiers()) && Modifier.isPublic(aClass.getModifiers())) {
                        try {
                            if (Modifier.isPublic(aClass.getConstructor().getModifiers())) {
                                suite.addTestSuite(aClass);
                            }
                        }
                        catch (NoSuchMethodException e) {
                            // Ignore test classes we can't instantiate
                        }
                    }

                    break;
                }
            }
        }
    }

    @Override
    protected void tearDown() throws Exception {
        typeMapper = null;

        classLoader.dispose();
        classLoader = null;

        generationState = null;

        myEnvironment = null;

        junitJar = null;

        super.tearDown();
    }
}
