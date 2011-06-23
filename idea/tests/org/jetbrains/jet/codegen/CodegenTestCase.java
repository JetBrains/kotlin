package org.jetbrains.jet.codegen;

import com.intellij.openapi.util.text.StringUtil;
import com.intellij.testFramework.LightProjectDescriptor;
import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.JetLightProjectDescriptor;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.psi.JetNamespace;
import org.jetbrains.jet.lang.resolve.AnalyzingUtils;
import org.jetbrains.jet.parsing.JetParsingTest;
import org.jetbrains.jet.plugin.JetFileType;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author yole
 */
public abstract class CodegenTestCase extends LightCodeInsightFixtureTestCase {
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
        myClassLoader = null;
        super.tearDown();
    }

    protected void loadText(final String text) {
        myFixture.configureByText(JetFileType.INSTANCE, text);
    }

    protected void loadFile(final String name) {
        myFixture.configureByFile(JetParsingTest.getTestDataDir() + "/codegen/" + name);
    }

    protected void loadFile() {
        loadFile(getPrefix() + "/" + getTestName(true) + ".jet");
    }

    protected String getPrefix() {
        throw new UnsupportedOperationException();
    }

    protected void blackBoxFile(String filename) throws Exception {
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
        assertEquals("OK", actual);
    }

    protected String blackBox() throws Exception {
        ClassFileFactory codegens = generateClassesInFile();
        CodegensClassLoader loader = new CodegensClassLoader(codegens);

        JetFile jetFile = (JetFile) myFixture.getFile();
        final JetNamespace namespace = jetFile.getRootNamespace();
        String fqName = NamespaceCodegen.getJVMClassName(namespace.getFQName()).replace("/", ".");
        Class<?> namespaceClass = loader.loadClass(fqName);
        Method method = namespaceClass.getMethod("box");
        return (String) method.invoke(null);
    }

    protected String generateToText() {
        GenerationState state = new GenerationState(getProject(), true);
        JetFile jetFile = (JetFile) myFixture.getFile();
        AnalyzingUtils.checkForSyntacticErrors(jetFile);
        state.compile(jetFile);

        StringBuilder answer = new StringBuilder();

        final ClassFileFactory factory = state.getFactory();
        List<String> files = factory.files();
        for (String file : files) {
            answer.append("@").append(file).append('\n');
            answer.append(factory.asText(file));
        }

        return answer.toString();
    }

    protected Class generateNamespaceClass() {
        ClassFileFactory state = generateClassesInFile();
        return loadRootNamespaceClass(state);
    }

    protected Class loadRootNamespaceClass(ClassFileFactory state) {
        JetFile jetFile = (JetFile) myFixture.getFile();
        final JetNamespace namespace = jetFile.getRootNamespace();
        String fqName = NamespaceCodegen.getJVMClassName(namespace.getFQName()).replace("/", ".");
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
            GenerationState state = new GenerationState(getProject(), false);
            JetFile jetFile = (JetFile) myFixture.getFile();
            AnalyzingUtils.checkForSyntacticErrors(jetFile);
            state.compile(jetFile);

            return state.getFactory();
        } catch (RuntimeException e) {
            System.out.println(generateToText());
            throw e;
        }
    }

    protected Method generateFunction() {
        Class aClass = generateNamespaceClass();
        try {
            return aClass.getMethods()[0];
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
        loadClass(name, codegens);
        return loadClass(name + "$$Impl", codegens);
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

    private static class CodegensClassLoader extends ClassLoader {
        private final ClassFileFactory state;

        public CodegensClassLoader(ClassFileFactory state) {
            super(CodegenTestCase.class.getClassLoader());
            this.state = state;
        }

        @Override
        protected Class<?> findClass(String name) throws ClassNotFoundException {
            String file = name.replace('.', '/') + ".class";
            if (state.files().contains(file)) {
                byte[] bytes = state.asBytes(file);
                return defineClass(name, bytes, 0, bytes.length);
            }
            return super.findClass(name);
        }
    }

    @NotNull
    @Override
    protected LightProjectDescriptor getProjectDescriptor() {
        return JetLightProjectDescriptor.INSTANCE;
    }
}
