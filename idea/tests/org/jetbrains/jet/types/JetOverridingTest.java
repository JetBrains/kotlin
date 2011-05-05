package org.jetbrains.jet.types;

import com.intellij.codeInsight.daemon.LightDaemonAnalyzerTestCase;
import com.intellij.openapi.application.PathManager;
import org.jetbrains.jet.lang.ErrorHandler;
import org.jetbrains.jet.lang.JetSemanticServices;
import org.jetbrains.jet.lang.psi.JetChangeUtil;
import org.jetbrains.jet.lang.psi.JetFunction;
import org.jetbrains.jet.lang.resolve.ClassDescriptorResolver;
import org.jetbrains.jet.lang.types.*;
import org.jetbrains.jet.parsing.JetParsingTest;

import java.io.File;

/**
 * @author abreslav
 */
public class JetOverridingTest extends LightDaemonAnalyzerTestCase {

    private ModuleDescriptor root = new ModuleDescriptor("test_root");
    private JetStandardLibrary library;
    private JetSemanticServices semanticServices;
    private ClassDescriptorResolver classDescriptorResolver;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        library          = JetStandardLibrary.getJetStandardLibrary(getProject());
        semanticServices = JetSemanticServices.createSemanticServices(library, ErrorHandler.DO_NOTHING);
        classDescriptorResolver = semanticServices.getClassDescriptorResolver(BindingTrace.DUMMY);
    }

    @Override
    protected String getTestDataPath() {
        return getHomeDirectory() + "/idea/testData";
    }

    private static String getHomeDirectory() {
       return new File(PathManager.getResourceRoot(JetParsingTest.class, "/org/jetbrains/jet/parsing/JetParsingTest.class")).getParentFile().getParentFile().getParent();
    }

    public void testBasic() throws Exception {
        assertOverridable(
                "fun a() : Int",
                "fun a() : Int");

        assertOverridable(
                "fun a<T1>() : T1",
                "fun a<T>() : T");

        assertOverridable(
                "fun a<T1>(a : T1) : T1",
                "fun a<T>(a : T) : T");

        assertOverridable(
                "fun a<T1, X : T1>(a : T1) : T1",
                "fun a<T, Y : T>(a : T) : T");

        assertOverridable(
                "fun a<T1, X : T1>(a : T1) : T1",
                "fun a<T, Y : T>(a : T) : Y");

        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////


        assertNotOverridable(
                "fun ab() : Int",
                "fun a() : Int");

        assertNotOverridable(
                "fun a() : Int",
                "fun a() : Any");

        assertNotOverridable(
                "fun a(a : Int) : Int",
                "fun a() : Int");

        assertNotOverridable(
                "fun a() : Int",
                "fun a(a : Int) : Int");

        assertNotOverridable(
                "fun a(a : Int?) : Int",
                "fun a(a : Int) : Int");

        assertNotOverridable(
                "fun a<T>(a : Int) : Int",
                "fun a(a : Int) : Int");

        assertNotOverridable(
                "fun a<T1, X : T1>(a : T1) : T1",
                "fun a<T, Y>(a : T) : T");

        assertNotOverridable(
                "fun a<T1, X : T1>(a : T1) : T1",
                "fun a<T, Y : T>(a : Y) : T");

        assertNotOverridable(
                "fun a<T1, X : T1>(a : T1) : X",
                "fun a<T, Y : T>(a : T) : T");

        assertOverridable(
                "fun a<T1, X : Array<out T1>>(a : Array<in T1>) : T1",
                "fun a<T, Y : Array<out T>>(a : Array<in T>) : T");

        assertNotOverridable(
                "fun a<T1, X : Array<T1>>(a : Array<in T1>) : T1",
                "fun a<T, Y : Array<out T>>(a : Array<in T>) : T");

        assertNotOverridable(
                "fun a<T1, X : Array<out T1>>(a : Array<in T1>) : T1",
                "fun a<T, Y : Array<in T>>(a : Array<in T>) : T");

        assertNotOverridable(
                "fun a<T1, X : Array<out T1>>(a : Array<in T1>) : T1",
                "fun a<T, Y : Array<*>>(a : Array<in T>) : T");

        assertNotOverridable(
                "fun a<T1, X : Array<out T1>>(a : Array<in T1>) : T1",
                "fun a<T, Y : Array<out T>>(a : Array<out T>) : T");

        assertNotOverridable(
                "fun a<T1, X : Array<out T1>>(a : Array<*>) : T1",
                "fun a<T, Y : Array<out T>>(a : Array<in T>) : T");

    }

    private void assertOverridable(String superFun, String subFun) {
        assertOverridabilityRelation(superFun, subFun, false);
    }

    private void assertNotOverridable(String superFun, String subFun) {
        assertOverridabilityRelation(superFun, subFun, true);
    }

    private void assertOverridabilityRelation(String superFun, String subFun, boolean expectedIsError) {
        FunctionDescriptor a = makeFunction(superFun);
        FunctionDescriptor b = makeFunction(subFun);
        FunctionDescriptorUtil.OverrideCompatibilityInfo overridableWith = FunctionDescriptorUtil.isOverridableWith(semanticServices.getTypeChecker(), a, b);
        assertEquals(overridableWith.getMessage(), expectedIsError, overridableWith.isError());
    }

    private FunctionDescriptor makeFunction(String funDecl) {
        JetFunction function = JetChangeUtil.createFunction(getProject(), funDecl);
        return classDescriptorResolver.resolveFunctionDescriptor(root, library.getLibraryScope(), function);
    }
}
