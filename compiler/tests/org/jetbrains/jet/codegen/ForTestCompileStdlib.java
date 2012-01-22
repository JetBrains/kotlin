package org.jetbrains.jet.codegen;

import com.google.common.io.ByteStreams;
import com.google.common.io.Files;
import com.intellij.openapi.util.Pair;
import org.jetbrains.jet.JetTestUtils;
import org.jetbrains.jet.cli.KotlinCompiler;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Stack;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

/**
 * Compile stdlib.jar that can be used in tests
 *
 * @see #stdlibJarForTests()
 *
 * @author Stepan Koltsov
 */
class ForTestCompileStdlib {

    public static final File stdlibJarForTests = new File(
            JetTestUtils.tmpDirForTest(ForTestCompileStdlib.class), "stdlib.jar");

    private static boolean compiled = false;

    static void compileStdlibForTest() {
        if (compiled) {
            return;
        }

        try {
            doCompile();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        compiled = true;
    }

    private static void doCompile() throws Exception {
        System.err.println("compiling stdlib for tests, resulting file: " + stdlibJarForTests);

        File tmp = new File(stdlibJarForTests.getPath() + "~");
        JetTestUtils.mkdirs(tmp.getParentFile());

        FileOutputStream stdlibJar = new FileOutputStream(tmp);
        try {
            JarOutputStream jarOutputStream = new JarOutputStream(stdlibJar);

            copyJavaPartOfStdlib(jarOutputStream);
            compileKotlinPartOfStdlibToJar(jarOutputStream);

            jarOutputStream.close();
            stdlibJar.close();

        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            try {
                stdlibJar.close();
            } catch (Throwable e) { }
        }

        if (!tmp.renameTo(stdlibJarForTests)) {
            throw new RuntimeException();
        }
    }

    private static void copyJavaPartOfStdlib(JarOutputStream os) throws IOException {
        File root = new File("out/production/stdlib");
        if (!new File(root, "jet/JetObject.class").isFile()) {
            throw new RuntimeException();
        }

        copyToJar(root, os);
    }

    private static void copyToJar(File root, JarOutputStream os) throws IOException {
        Stack<Pair<String, File>> toCopy = new Stack<Pair<String, File>>();
        toCopy.add(new Pair<String, File>("", root));
        while (!toCopy.empty()) {
            Pair<String, File> pop = toCopy.pop();
            File file = pop.getSecond();
            if (file.isFile()) {
                os.putNextEntry(new JarEntry(pop.getFirst()));
                Files.copy(file, os);
            } else if (file.isDirectory()) {
                for (File child : file.listFiles()) {
                    String path = pop.getFirst().isEmpty() ? child.getName() : pop.getFirst() + "/" + child.getName();
                    toCopy.add(new Pair<String, File>(path, child));
                }
            } else {
                throw new IllegalStateException();
            }
        }
    }

    private static void compileKotlinPartOfStdlibToJar(JarOutputStream jarOutputStream) throws IOException {
        File file = new File(JetTestUtils.tmpDirForTest(ForTestCompileStdlib.class), "stdlib-kt");
        JetTestUtils.recreateDirectory(file);
        // lame
        KotlinCompiler.main("-excludeStdlib", "-output", file.getPath(), "-src", "./stdlib/ktSrc");
        copyToJar(file, jarOutputStream);
    }

    
    public static File stdlibJarForTests() {
        compileStdlibForTest();
        return stdlibJarForTests;
    }

}
