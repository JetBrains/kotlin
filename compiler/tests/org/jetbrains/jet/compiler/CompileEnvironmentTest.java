package org.jetbrains.jet.compiler;

import jet.modules.IModuleBuilder;
import jet.modules.IModuleSetBuilder;
import junit.framework.TestCase;
import org.jetbrains.jet.cli.KotlinCompiler;
import org.jetbrains.jet.codegen.ClassFileFactory;
import org.jetbrains.jet.parsing.JetParsingTest;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

/**
 * @author yole
 * @author alex.tkachman
 */
public class CompileEnvironmentTest extends TestCase {
    private CompileEnvironment environment;

    protected void setUp() throws Exception {
        super.setUp();
        environment = new CompileEnvironment();
    }

    @Override
    protected void tearDown() throws Exception {
        environment.dispose();
        super.tearDown();
    }

    public void testSmoke() throws IOException {
        final File activeRtJar = CompileEnvironment.findRtJar(true);
        environment.setJavaRuntime(activeRtJar);
        environment.initializeKotlinRuntime();
        final String testDataDir = JetParsingTest.getTestDataDir() + "/compiler/smoke/";
        final IModuleSetBuilder setBuilder = environment.loadModuleScript(testDataDir + "Smoke.kts");
        assertEquals(1, setBuilder.getModules().size());
        final IModuleBuilder moduleBuilder = setBuilder.getModules().get(0);
        final ClassFileFactory factory = environment.compileModule(moduleBuilder, testDataDir);
        assertNotNull(factory);
        assertNotNull(factory.asBytes("Smoke/namespace.class"));

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        CompileEnvironment.writeToJar(factory, baos, null, false);
        JarInputStream is = new JarInputStream(new ByteArrayInputStream(baos.toByteArray()));
        final List<String> entries = listEntries(is);
        assertTrue(entries.contains("Smoke/namespace.class"));
    }

    public void testSmokeWithCompilerJar() throws IOException {
        File tempFile = File.createTempFile("compilerTest", "compilerTest");
        try {
            KotlinCompiler.main(Arrays.asList("-module", JetParsingTest.getTestDataDir() + "/compiler/smoke/Smoke.kts", "-jar", tempFile.getAbsolutePath()).toArray(new String[0]));
            FileInputStream fileInputStream = new FileInputStream(tempFile);
            try {
                JarInputStream is = new JarInputStream(fileInputStream);
                try {
                    final List<String> entries = listEntries(is);
                    assertTrue(entries.contains("Smoke/namespace.class"));
                    assertEquals(1, entries.size());
                }
                finally {
                    is.close();
                }
            }
            finally {
                fileInputStream.close();
            }
        }
        finally {
            tempFile.delete();
        }
    }
    
    private static void delete(File file) {
        if(file.isDirectory()) {
            for (File child : file.listFiles()) {
                delete(child);
            }
        }

        file.delete();
    }
    
    public void testSmokeWithCompilerOutput() throws IOException {
        File tempFile = File.createTempFile("compilerTest", "compilerTest");
        tempFile.delete();
        tempFile = new File(tempFile.getAbsolutePath());
        tempFile.mkdir();
        try {
            KotlinCompiler.main(Arrays.asList("-src", JetParsingTest.getTestDataDir() + "/compiler/smoke/Smoke.kt", "-output", tempFile.getAbsolutePath()).toArray(new String[0]));
            assertEquals(1, tempFile.listFiles().length);
            assertEquals(1, tempFile.listFiles()[0].listFiles().length);
        }
        finally {
            delete(tempFile);
        }
    }

    private List<String> listEntries(JarInputStream is) throws IOException {
        List<String> entries = new ArrayList<String>();
        while (true) {
            final JarEntry jarEntry = is.getNextJarEntry();
            if (jarEntry == null) {
                break;
            }
            entries.add(jarEntry.getName());
        }
        return entries;
    }
}
