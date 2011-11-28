package org.jetbrains.jet.codegen;

import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.CharsetToolkit;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
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
            session.addSources(addStdLib());
        } catch (IOException e) {
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

            session.addSources(myFile.getVirtualFile());
            session.addSources(addStdLib());

            if (!session.analyze(System.out)) {
                return null;
            }

            return session.generate();
        } catch (RuntimeException e) {
            System.out.println(generateToText());
            throw e;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private VirtualFile addStdLib() throws IOException {
        String text = FileUtil.loadFile(new File(JetParsingTest.getTestDataDir() + "/../../stdlib/ktSrc/StandardLibrary.kt"), CharsetToolkit.UTF8).trim();
        text = StringUtil.convertLineSeparators(text);
        PsiFile stdLibFile = createFile("StandardLibrary.kt", text);
        return stdLibFile.getVirtualFile();
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
}
