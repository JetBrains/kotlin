/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

import com.intellij.testFramework.UsefulTestCase;
import gnu.trove.THashSet;
import junit.extensions.TestSetup;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.jetbrains.jet.ConfigurationKind;
import org.jetbrains.jet.JetTestCaseBuilder;
import org.jetbrains.jet.JetTestUtils;
import org.jetbrains.jet.TestJdkKind;
import org.jetbrains.jet.cli.common.CLIConfigurationKeys;
import org.jetbrains.jet.cli.common.messages.MessageCollectorPlainTextToStream;
import org.jetbrains.jet.cli.jvm.JVMConfigurationKeys;
import org.jetbrains.jet.cli.jvm.compiler.JetCoreEnvironment;
import org.jetbrains.jet.cli.jvm.compiler.KotlinToJVMBytecodeCompiler;
import org.jetbrains.jet.codegen.forTestCompile.ForTestCompileRuntime;
import org.jetbrains.jet.codegen.state.GenerationState;
import org.jetbrains.jet.codegen.state.JetTypeMapper;
import org.jetbrains.jet.codegen.state.JetTypeMapperMode;
import org.jetbrains.jet.config.CommonConfigurationKeys;
import org.jetbrains.jet.config.CompilerConfiguration;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.psi.JetClass;
import org.jetbrains.jet.lang.psi.JetDeclaration;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.types.JetType;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Set;

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
    private JetCoreEnvironment myEnvironment;

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

        CompilerConfiguration configuration = JetTestUtils.compilerConfigurationForTests(ConfigurationKind.ALL,
                                                                                         TestJdkKind.FULL_JDK);
        configuration.add(JVMConfigurationKeys.CLASSPATH_KEY, JetTestUtils.getAnnotationsJar());

        junitJar = new File("libraries/lib/junit-4.9.jar");
        assertTrue(junitJar.exists());
        configuration.add(JVMConfigurationKeys.CLASSPATH_KEY, junitJar);

        configuration.add(CommonConfigurationKeys.SOURCE_ROOTS_KEY, JetTestCaseBuilder.getTestDataPathBase() + "/../../libraries/stdlib/test");
        configuration.add(CommonConfigurationKeys.SOURCE_ROOTS_KEY, JetTestCaseBuilder.getTestDataPathBase() + "/../../libraries/kunit/src");
        configuration.put(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY,
                          new MessageCollectorPlainTextToStream(System.out, MessageCollectorPlainTextToStream.NON_VERBOSE));

        myEnvironment = new JetCoreEnvironment(getTestRootDisposable(), configuration);

        generationState = KotlinToJVMBytecodeCompiler.analyzeAndGenerate(myEnvironment, false);
        if (generationState == null) {
            throw new RuntimeException("There were compilation errors");
        }

        ClassFileFactory classFileFactory = generationState.getFactory();

        classLoader = new GeneratedClassLoader(classFileFactory,
                                               new URLClassLoader(new URL[] {ForTestCompileRuntime.runtimeJarForTests().toURI().toURL(), junitJar.toURI().toURL()},
                                                                  TestCase.class.getClassLoader()));

        typeMapper = generationState.getTypeMapper();

        for (JetFile jetFile : myEnvironment.getSourceFiles()) {
            for (JetDeclaration decl : jetFile.getDeclarations()) {
                if (decl instanceof JetClass) {
                    JetClass jetClass = (JetClass) decl;

                    ClassDescriptor descriptor = (ClassDescriptor) generationState.getBindingContext().get(
                            BindingContext.DECLARATION_TO_DESCRIPTOR, jetClass);

                    assertNotNull("Descriptor for declaration " + jetClass + " shouldn't be null", descriptor);

                    Set<JetType> allSuperTypes = new THashSet<JetType>();
                    DescriptorUtils.addSuperTypes(descriptor.getDefaultType(), allSuperTypes);

                    for (JetType type : allSuperTypes) {
                        String internalName = typeMapper.mapType(type, JetTypeMapperMode.IMPL).getInternalName();
                        if (internalName.equals("junit/framework/Test")) {
                            String name = typeMapper.mapType(descriptor.getDefaultType(), JetTypeMapperMode.IMPL).getInternalName();

                            //noinspection UseOfSystemOutOrSystemErr
                            System.out.println(name);

                            @SuppressWarnings("unchecked")
                            Class<TestCase> aClass = (Class<TestCase>) classLoader.loadClass(name.replace('/', '.'));

                            if ((aClass.getModifiers() & Modifier.ABSTRACT) == 0 && (aClass.getModifiers() & Modifier.PUBLIC) != 0) {
                                try {
                                    Constructor<TestCase> constructor = aClass.getConstructor();
                                    if ((constructor.getModifiers() & Modifier.PUBLIC) != 0) {
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
