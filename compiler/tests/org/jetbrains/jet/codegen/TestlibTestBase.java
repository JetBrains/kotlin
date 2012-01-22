package org.jetbrains.jet.codegen;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.local.CoreLocalFileSystem;
import gnu.trove.THashSet;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.jetbrains.jet.compiler.CompileEnvironment;
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
import java.net.URLDecoder;
import java.util.Set;

/**
 * @author alex.tkachman
 */
public abstract class TestlibTestBase extends CodegenTestCase {

    /** Binary or source */
    private final boolean binary;

    protected TestlibTestBase(boolean binary) {
        this.binary = binary;
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
            
            if (binary) {
                myEnvironment.addToClasspath(ForTestCompileStdlib.stdlibJarForTests());
            } else {
                CompileEnvironment.initializeKotlinRuntime(myEnvironment);
            }
            
            URLClassLoader classLoader = (URLClassLoader) TestCase.class.getClassLoader();
            CoreLocalFileSystem localFileSystem = myEnvironment.getLocalFileSystem();
            for(URL url: classLoader.getURLs()) {
                if(url.getProtocol().equals("file") && url.getPath().contains("junit")) {
                    File file = new File(URLDecoder.decode(url.getPath()));
                    if(file.exists()) {
                        myEnvironment.addToClasspath(file);
                    }
                }
            }
            VirtualFile path = localFileSystem.findFileByPath(JetParsingTest.getTestDataDir() + "/../../testlib/test");
            session.addSources(path);

            if (!binary) {
                session.addStdLibSources(true);
            }

            if (!session.analyze(System.out)) {
                throw new RuntimeException();
            }

            ClassFileFactory classFileFactory = session.generate();
            GeneratedClassLoader loader;
            if (binary) {
                URLClassLoader parentClassLoader = new URLClassLoader(new URL[]{
                        ForTestCompileStdlib.stdlibJarForTests().toURI().toURL() });
                loader = new GeneratedClassLoader(classFileFactory, parentClassLoader);
            } else {
                loader = new GeneratedClassLoader(classFileFactory);
            }

            JetTypeMapper typeMapper = new JetTypeMapper(classFileFactory.state.getStandardLibrary(), session.getMyBindingContext());
            TestSuite suite = new TestSuite("StandardLibrary");
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
