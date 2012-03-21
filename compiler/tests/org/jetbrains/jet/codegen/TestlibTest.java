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

import com.intellij.openapi.vfs.local.CoreLocalFileSystem;
import gnu.trove.THashSet;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.jetbrains.jet.compiler.CompileSession;
import org.jetbrains.jet.compiler.MessageRenderer;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.psi.JetClass;
import org.jetbrains.jet.lang.psi.JetDeclaration;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.parsing.JetParsingTest;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Set;

/**
 * @author alex.tkachman
 */
public class TestlibTest extends CodegenTestCase {

    public static Test suite() {
        return new TestlibTest().buildSuite();
    }

    protected TestSuite buildSuite() {
        try {
            setUp();
            return doBuildSuite();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        finally {
            try {
                tearDown();
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    private TestSuite doBuildSuite() {
        try {
            CompileSession session = new CompileSession(myEnvironment, MessageRenderer.PLAIN, System.err, false);

            myEnvironment.addToClasspath(ForTestCompileStdlib.stdlibJarForTests());

            File junitJar = new File("libraries/lib/junit-4.9.jar");

            if (!junitJar.exists()) {
                throw new AssertionError();
            }

            myEnvironment.addToClasspath(junitJar);

            CoreLocalFileSystem localFileSystem = myEnvironment.getLocalFileSystem();
            session.addSources(localFileSystem.findFileByPath(JetParsingTest.getTestDataDir() + "/../../libraries/stdlib/test"));
            session.addSources(localFileSystem.findFileByPath(JetParsingTest.getTestDataDir() + "/../../libraries/kunit/src"));

            if (!session.analyze()) {
                throw new RuntimeException("There were compilation errors");
            }

            ClassFileFactory classFileFactory = session.generate(false);

            final GeneratedClassLoader loader = new GeneratedClassLoader(
                    classFileFactory,
                    new URLClassLoader(new URL[]{ForTestCompileStdlib.stdlibJarForTests().toURI().toURL(), junitJar.toURI().toURL()},
                                       TestCase.class.getClassLoader()));

            ClosureAnnotator closureAnnotator = new ClosureAnnotator(session.getMyBindingContext(), session.getSourceFileNamespaces());
            JetTypeMapper typeMapper = new JetTypeMapper(classFileFactory.state.getStandardLibrary(), session.getMyBindingContext(), closureAnnotator);
            TestSuite suite = new TestSuite("stdlib_test");
            try {
                for(JetFile jetFile : session.getSourceFileNamespaces()) {
                    for(JetDeclaration decl : jetFile.getDeclarations()) {
                        if(decl instanceof JetClass) {
                            JetClass jetClass = (JetClass) decl;

                            ClassDescriptor descriptor = (ClassDescriptor) session.getMyBindingContext().get(BindingContext.DECLARATION_TO_DESCRIPTOR, jetClass);
                            Set<JetType> allSuperTypes = new THashSet<JetType>();
                            CodegenUtil.addSuperTypes(descriptor.getDefaultType(), allSuperTypes);

                            for(JetType type : allSuperTypes) {
                                String internalName = typeMapper.mapType(type).getInternalName();
                                if(internalName.equals("junit/framework/Test")) {
                                    String name = typeMapper.mapType(descriptor.getDefaultType()).getInternalName();
                                    System.out.println(name);
                                    Class<TestCase> aClass = (Class<TestCase>) loader.loadClass(name.replace('/', '.'));
                                    if((aClass.getModifiers() & Modifier.ABSTRACT) == 0
                                     && (aClass.getModifiers() & Modifier.PUBLIC) != 0) {
                                        try {
                                            Constructor<TestCase> constructor = aClass.getConstructor();
                                            if(constructor != null && (constructor.getModifiers() & Modifier.PUBLIC) != 0) {
                                                suite.addTestSuite(aClass);
                                            }
                                        }
                                        catch (NoSuchMethodException e) {
                                        }
                                    }
                                    break;
                                }
                            }
                        }
                    }
                }
            }
            finally {
                typeMapper = null;
            }

            return suite;

        } catch (RuntimeException e) {
            throw e;
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }
    
    @Override
    public void setUp() throws Exception {
        super.setUp();
        createEnvironmentWithFullJdk();
    }
}
