package org.jetbrains.jet.compiler;

import com.intellij.openapi.util.io.FileUtil;
import junit.framework.TestCase;
import org.jetbrains.jet.cli.KotlinCompiler;
import org.jetbrains.jet.codegen.ForTestCompileStdlib;
import org.jetbrains.jet.parsing.JetParsingTest;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
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

    public void testSmokeWithCompilerJar() throws IOException {
        File tempDir = FileUtil.createTempDirectory("compilerTest", "compilerTest");

        try {
            File stdlib = new File(tempDir, "stdlib.jar");
            FileUtil.copy(ForTestCompileStdlib.stdlibJarForTests(), stdlib);
            File resultJar = new File(tempDir, "result.jar");
            KotlinCompiler.main("-module", JetParsingTest.getTestDataDir() + "/compiler/smoke/Smoke.kts",
                                "-jar", resultJar.getAbsolutePath(),
                                "-stdlib", stdlib.getAbsolutePath());
            FileInputStream fileInputStream = new FileInputStream(resultJar);
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
            FileUtil.delete(tempDir);
        }
    }

    public void testSmokeWithCompilerOutput() throws IOException {
        File tempDir = FileUtil.createTempDirectory("compilerTest", "compilerTest");
        try {
            File out = new File(tempDir, "out");
            File stdlib = new File(tempDir, "stdlib.jar");
            FileUtil.copy(ForTestCompileStdlib.stdlibJarForTests(), stdlib);
            KotlinCompiler.main("-src", JetParsingTest.getTestDataDir() + "/compiler/smoke/Smoke.kt",
                                "-output", out.getAbsolutePath(),
                                "-stdlib", stdlib.getAbsolutePath());

            assertEquals(1, out.listFiles().length);
            assertEquals(1, out.listFiles()[0].listFiles().length);
        } finally {
            FileUtil.delete(tempDir);
        }
    }

    private static List<String> listEntries(JarInputStream is) throws IOException {
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
