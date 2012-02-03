package org.jetbrains.jet.codegen;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.local.CoreLocalFileSystem;
import gnu.trove.THashSet;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestResult;
import junit.framework.TestSuite;
import org.jetbrains.jet.compiler.CompileSession;
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
        try {
            Class.forName("test.collections.CollectionTest");
            System.out.println("Tests in Kotlin found in classpath. This test suite shall not be run, since testlib tests will run normally.");
            return new TestSuite("Empty.StandardLibrary");
        }
        catch (Throwable e) {
            System.out.println("Tests in Kotlin haven't been found in classpath. This test suite is valid.");
        }
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
            CompileSession session = new CompileSession(myEnvironment);

            myEnvironment.addToClasspath(ForTestCompileStdlib.stdlibJarForTests());

            File junitJar = new File("testlib/lib/junit-4.9.jar");

            if (!junitJar.exists()) {
                throw new AssertionError();
            }

            myEnvironment.addToClasspath(junitJar);

            CoreLocalFileSystem localFileSystem = myEnvironment.getLocalFileSystem();
            VirtualFile path = localFileSystem.findFileByPath(JetParsingTest.getTestDataDir() + "/../../testlib/test");
            session.addSources(path);

            if (!session.analyze(System.out)) {
                throw new RuntimeException();
            }

            ClassFileFactory classFileFactory = session.generate();

            final GeneratedClassLoader loader = new GeneratedClassLoader(
                    classFileFactory,
                    new URLClassLoader(new URL[]{ForTestCompileStdlib.stdlibJarForTests().toURI().toURL()},
                                       TestCase.class.getClassLoader()));

            JetTypeMapper typeMapper = new JetTypeMapper(classFileFactory.state.getStandardLibrary(), session.getMyBindingContext());
            TestSuite suite = new MyTestSuite(loader);
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

    private static class MyTestSuite extends TestSuite {
        private final GeneratedClassLoader loader;

        public MyTestSuite(GeneratedClassLoader loader) {
            super("StandardLibrary");
            this.loader = loader;
        }

        @Override
        public void run(TestResult result) {
            super.run(result);
            loader.dispose();
        }
    }
}
