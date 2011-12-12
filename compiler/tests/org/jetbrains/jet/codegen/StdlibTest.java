package org.jetbrains.jet.codegen;

import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.CharsetToolkit;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import org.jetbrains.jet.compiler.CompileEnvironment;
import org.jetbrains.jet.compiler.CompileSession;
import org.jetbrains.jet.lang.psi.JetNamespace;
import org.jetbrains.jet.lang.resolve.AnalyzingUtils;
import org.jetbrains.jet.parsing.JetParsingTest;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * @author alex.tkachman
 */
public class StdlibTest extends CodegenTestCase {
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        createEnvironmentWithFullJdk();
    }
    
    protected String generateToText() {
        CompileSession session = new CompileSession(myEnvironment);

        session.addSources(myFile.getVirtualFile());
        try {
            session.addStdLibSources();
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }

        if (!session.analyze(System.out)) {
            return null;
        }

        return session.generateText();
    }

    protected ClassFileFactory generateClassesInFile() {
        try {
            CompileSession session = new CompileSession(myEnvironment);
            CompileEnvironment.initializeKotlinRuntime(myEnvironment);
            session.addSources(myFile.getVirtualFile());
            session.addStdLibSources();

            if (!session.analyze(System.out)) {
                return null;
            }

            return session.generate();
        } catch (RuntimeException e) {
            System.out.println(generateToText());
            throw e;
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public void testInputStreamIterator () {
        blackBoxFile("inputStreamIterator.jet");
//        System.out.println(generateToText());
    }

    public void testKt533 () {
        blackBoxFile("regressions/kt533.kt");
    }

    public void testKt529 () {
        blackBoxFile("regressions/kt529.kt");
    }

    public void testKt528 () {
        blackBoxFile("regressions/kt528.kt");
    }

    public void testKt789 () {
//        blackBoxFile("regressions/kt789.jet");
    }

    public void testCollectionSize () throws Exception {
        loadText("import std.util.*; fun box() = if(java.util.Arrays.asList(0, 1, 2)?.size == 3) \"OK\" else \"fail\"");
//        System.out.println(generateToText());
        blackBox();
    }

    public void testCollectionEmpty () throws Exception {
        loadText("import std.util.*; fun box() = if(java.util.Arrays.asList(0, 1, 2)?.empty ?: false) \"OK\" else \"fail\"");
//        System.out.println(generateToText());
        blackBox();
    }
}
