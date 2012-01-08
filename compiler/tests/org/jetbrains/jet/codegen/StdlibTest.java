package org.jetbrains.jet.codegen;

import com.intellij.openapi.util.ClassLoaderUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.local.CoreLocalFileSystem;
import com.intellij.testFramework.TestRunnerUtil;
import gnu.trove.THashSet;
import junit.framework.TestCase;
import junit.framework.TestResult;
import junit.framework.TestSuite;
import junit.textui.TestRunner;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.compiler.CompileEnvironment;
import org.jetbrains.jet.compiler.CompileSession;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.psi.JetClass;
import org.jetbrains.jet.lang.psi.JetDeclaration;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.psi.JetPsiUtil;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.parsing.JetParsingTest;

import java.beans.beancontext.BeanContext;
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
            session.addStdLibSources(true);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }

        if (!session.analyze(System.out)) {
            return null;
        }

        return session.generateText();
    }

    @NotNull
    protected ClassFileFactory generateClassesInFile() {
        try {
            CompileSession session = new CompileSession(myEnvironment);
            CompileEnvironment.initializeKotlinRuntime(myEnvironment);
            session.addSources(myFile.getVirtualFile());
            session.addStdLibSources(true);

            if (!session.analyze(System.out)) {
                throw new RuntimeException();
            }

            return session.generate();
        } catch (RuntimeException e) {
            System.out.println(generateToText());
            throw e;
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
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

    public void testKt828 () {
        blackBoxFile("regressions/kt828.kt");
    }

    public void testKt715 () {
        blackBoxFile("regressions/kt715.kt");
    }

    public void testKt864 () {
        blackBoxFile("regressions/kt864.jet");
    }

    public void testKt274 () {
//        blackBoxFile("regressions/kt274.kt");
    }
}
