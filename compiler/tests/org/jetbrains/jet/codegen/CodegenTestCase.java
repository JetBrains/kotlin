package org.jetbrains.jet.codegen;

import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.JetLiteFixture;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.psi.JetNamespace;
import org.jetbrains.jet.lang.resolve.AnalyzingUtils;
import org.jetbrains.jet.parsing.JetParsingTest;
import org.junit.Assert;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author yole
 */
public abstract class CodegenTestCase extends JetLiteFixture {
    private MyClassLoader myClassLoader;

    protected static void assertThrows(Method foo, Class<? extends Throwable> exceptionClass, Object instance, Object... args) throws IllegalAccessException {
        boolean caught = false;
        try {
            foo.invoke(instance, args);
        }
        catch(InvocationTargetException ex) {
            caught = exceptionClass.isInstance(ex.getTargetException());
        }
        assertTrue(caught);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        myClassLoader = new MyClassLoader(NamespaceGenTest.class.getClassLoader());
    }

    @Override
    protected void tearDown() throws Exception {
        myFile = null;
        myClassLoader = null;
        super.tearDown();
    }

    protected void loadText(final String text) {
        myFile = (JetFile) createFile("a.jet", text);
    }

    @Override
    protected String loadFile(final String name) {
        try {
            final String content = doLoadFile(JetParsingTest.getTestDataDir() + "/codegen/", name);
            myFile = (JetFile) createFile(name, content);
            return content;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected void loadFile() {
        loadFile(getPrefix() + "/" + getTestName(true) + ".jet");
    }

    protected String getPrefix() {
        throw new UnsupportedOperationException();
    }

    protected void blackBoxFile(String filename) {
        loadFile(filename);
        String actual;
        try {
            actual = blackBox();
        } catch (NoClassDefFoundError e) {
            System.out.println(generateToText());
            throw e;
        } catch (Throwable e) {
            System.out.println(generateToText());
            throw new RuntimeException(e);
        }
        if (!"OK".equals(actual)) {
            System.out.println(generateToText());
        }
        assertEquals("OK", actual);
    }

    protected String blackBox() throws Exception {
        ClassFileFactory codegens = generateClassesInFile();
        GeneratedClassLoader loader = new GeneratedClassLoader(codegens);

        final JetNamespace namespace = myFile.getRootNamespace();
        String fqName = NamespaceCodegen.getJVMClassName(CodegenUtil.getFQName(namespace)).replace("/", ".");
        Class<?> namespaceClass = loader.loadClass(fqName);
        Method method = namespaceClass.getMethod("box");
        return (String) method.invoke(null);
    }

    protected String generateToText() {
        GenerationState state = new GenerationState(getProject(), ClassBuilderFactory.TEXT);
        AnalyzingUtils.checkForSyntacticErrors(myFile);
        state.compile(myFile);

        return state.createText();
    }

    protected Class generateNamespaceClass() {
        ClassFileFactory state = generateClassesInFile();
        return loadRootNamespaceClass(state);
    }

    protected Class loadRootNamespaceClass(ClassFileFactory state) {
        final JetNamespace namespace = myFile.getRootNamespace();
        String fqName = NamespaceCodegen.getJVMClassName(CodegenUtil.getFQName(namespace)).replace("/", ".");
        Map<String, Class> classMap = loadAllClasses(state);
        return classMap.get(fqName);
    }

    protected Class loadClass(String fqName, ClassFileFactory state) {
        List<String> files = state.files();
        for (String file : files) {
            if (file.equals(fqName.replace('.', '/') + ".class")) {
                final byte[] data = state.asBytes(file);
                return myClassLoader.doDefineClass(fqName, data);
            }
        }

        fail("No classfile was generated for: " + fqName);
        return null;
    }

    protected Map<String, Class> loadAllClasses(ClassFileFactory state) {
        Map<String, Class> result = new HashMap<String, Class>();
        for (String fileName : state.files()) {
            String className = StringUtil.trimEnd(fileName, ".class").replace('/', '.');
            byte[] data = state.asBytes(fileName);
            Class aClass = myClassLoader.doDefineClass(className, data);
            result.put(className, aClass);
        }
        return result;
    }

    protected ClassFileFactory generateClassesInFile() {
        try {
            GenerationState state = new GenerationState(getProject(), ClassBuilderFactory.BINARIES);
            AnalyzingUtils.checkForSyntacticErrors(myFile);
            state.compile(myFile);

            return state.getFactory();
        } catch (RuntimeException e) {
            System.out.println(generateToText());
            throw e;
        }
    }

    protected Method generateFunction() {
        Class aClass = generateNamespaceClass();
        try {
            Method r = null;
            for (Method method : aClass.getMethods()) {
                if (method.getDeclaringClass().equals(Object.class)) {
                    continue;
                }

                if (r != null) {
                    throw new AssertionError("more then one public method in class " + aClass);
                }

                r = method;
            }
            if (r == null)
                throw new AssertionError();
            return r;
        } catch (Error e) {
            System.out.println(generateToText());
            throw e;
        }
    }

    protected Method generateFunction(String name) {
        Class aClass = generateNamespaceClass();
        final Method method = findMethodByName(aClass, name);
        if (method == null) {
            throw new IllegalArgumentException("couldn't find method " + name);
        }
        return method;
    }

    @Nullable
    protected static Method findMethodByName(Class aClass, String name) {
        for (Method method : aClass.getDeclaredMethods()) {
            if (method.getName().equals(name)) {
                return method;
            }
        }
        return null;
    }

    protected static void assertIsCurrentTime(long returnValue) {
        long currentTime = System.currentTimeMillis();
        assertTrue(Math.abs(returnValue - currentTime) <= 1L);
    }

    protected Class loadImplementationClass(ClassFileFactory codegens, final String name) {
        return loadClass(name, codegens);
    }

    private static class MyClassLoader extends ClassLoader {
      public MyClassLoader(ClassLoader parent) {
        super(parent);
      }

      public Class doDefineClass(String name, byte[] data) {
        return defineClass(name, data, 0, data.length);
      }

      @Override
      public Class<?> loadClass(String name) throws ClassNotFoundException {
        return super.loadClass(name);
      }
    }
}
