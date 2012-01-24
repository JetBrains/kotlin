package org.jetbrains.jet.codegen;

import com.google.common.io.Files;
import com.intellij.openapi.util.Pair;
import org.jetbrains.jet.JetTestUtils;
import org.jetbrains.jet.cli.KotlinCompiler;
import org.junit.Assert;

import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

/**
 * Compile stdlib.jar that can be used in tests
 *
 * @see #stdlibJarForTests()
 *
 * @author Stepan Koltsov
 */
public class ForTestCompileStdlib {

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
            // TODO: do not try to compile again if failed
            throw new RuntimeException(e);
        }

        compiled = true;
    }

    private static void doCompile() throws Exception {
        System.err.println("compiling stdlib for tests, resulting file: " + stdlibJarForTests);

        File tmpFile = new File(stdlibJarForTests.getPath() + "~");
        JetTestUtils.mkdirs(tmpFile.getParentFile());
        
        File tmpDir = new File(JetTestUtils.tmpDirForTest(ForTestCompileStdlib.class), "classes");
        JetTestUtils.recreateDirectory(tmpDir);
        compileKotlinPartOfStdlib(tmpDir);
        compileJavaPartOfStdlib(tmpDir);

        FileOutputStream stdlibJar = new FileOutputStream(tmpFile);
        try {
            JarOutputStream jarOutputStream = new JarOutputStream(stdlibJar);

            copyToJar(tmpDir, jarOutputStream);

            jarOutputStream.close();
            stdlibJar.close();

        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            try {
                stdlibJar.close();
            } catch (Throwable e) { }
        }

        if (!tmpFile.renameTo(stdlibJarForTests)) {
            throw new RuntimeException();
        }
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

    private static void compileKotlinPartOfStdlib(File destdir) throws IOException {
        // lame
        KotlinCompiler.main("-output", destdir.getPath(), "-src", "./stdlib/ktSrc");
    }
    
    private static List<File> javaFilesInDir(File dir) {
        List<File> r = new ArrayList<File>();
        Stack<File> stack = new Stack<File>();
        stack.push(dir);
        while (!stack.empty()) {
            File file = stack.pop();
            if (file.isDirectory()) {
                stack.addAll(Arrays.asList(file.listFiles()));
            } else if (file.getName().endsWith(".java")) {
                r.add(file);
            }
        }
        return r;
    }
    
    private static void compileJavaPartOfStdlib(File destdir) throws IOException {
        JavaCompiler javaCompiler = ToolProvider.getSystemJavaCompiler();

        StandardJavaFileManager fileManager = javaCompiler.getStandardFileManager(null, Locale.ENGLISH, Charset.forName("utf-8"));
        try {
            Iterable<? extends JavaFileObject> javaFileObjectsFromFiles = fileManager.getJavaFileObjectsFromFiles(javaFilesInDir(new File("stdlib/src")));
            List<String> options = Arrays.asList(
                    "-d", destdir.getPath()
            );
            JavaCompiler.CompilationTask task = javaCompiler.getTask(null, fileManager, null, options, null, javaFileObjectsFromFiles);

            Assert.assertTrue(task.call());
        } finally {
            fileManager.close();
        }
    }

    
    public static File stdlibJarForTests() {
        compileStdlibForTest();
        return stdlibJarForTests;
    }

}
