package org.jetbrains.jet.codegen;

import junit.framework.TestCase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.compiler.CompileEnvironment;
import org.jetbrains.jet.compiler.CompileSession;

import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

/**
 * @author alex.tkachman
 */
public class StdlibTest extends CodegenTestCase {
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        createEnvironmentWithFullJdk();
        myEnvironment.addToClasspath(ForTestCompileStdlib.stdlibJarForTests());
        CompileEnvironment.ensureRuntime(myEnvironment);
    }

    @Override
    protected GeneratedClassLoader createClassLoader(ClassFileFactory codegens) throws MalformedURLException {
        return new GeneratedClassLoader(
                codegens,
                new URLClassLoader(new URL[]{ForTestCompileStdlib.stdlibJarForTests().toURI().toURL()},
                                   getClass().getClassLoader()));
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
        blackBoxFile("regressions/kt274.kt");
    }

    //from ClassGenTest
    public void testKt344 () throws Exception {
        loadFile("regressions/kt344.jet");
//        System.out.println(generateToText());
        blackBox();
    }

    //from ExtensionFunctionsTest
    public void testGeneric() throws Exception {
        blackBoxFile("extensionFunctions/generic.jet");
    }

    //from NamespaceGenTest
    public void testPredicateOperator() throws Exception {
        loadText("fun foo(s: String) = s?startsWith(\"J\")");
        final Method main = generateFunction();
        try {
            assertEquals("JetBrains", main.invoke(null, "JetBrains"));
            assertNull(main.invoke(null, "IntelliJ"));
        } catch (Throwable t) {
//            System.out.println(generateToText());
            t.printStackTrace();
            throw t instanceof Exception ? (Exception)t : new RuntimeException(t);
        }
    }

    public void testForInString() throws Exception {
        loadText("fun foo() : Int {        var sum = 0\n" +
                 "        for(c in \"239\")\n" +
                 "            sum += (c.int - '0'.int)\n" +
                 "        return sum" +
                 "}" );
        final Method main = generateFunction();
        try {
            assertEquals(14, main.invoke(null));
        } catch (Throwable t) {
            System.out.println(generateToText());
            t.printStackTrace();
            throw t instanceof Exception ? (Exception)t : new RuntimeException(t);
        }
    }
}
