package org.jetbrains.jet.types;

import org.jetbrains.jet.JetLiteFixture;
import org.jetbrains.jet.JetTestCaseBuilder;
import org.jetbrains.jet.JetTestUtils;
import org.jetbrains.jet.lang.JetSemanticServices;
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor;
import org.jetbrains.jet.lang.descriptors.ModuleDescriptor;
import org.jetbrains.jet.lang.psi.JetNamedFunction;
import org.jetbrains.jet.lang.psi.JetPsiFactory;
import org.jetbrains.jet.lang.resolve.DescriptorResolver;
import org.jetbrains.jet.lang.resolve.OverloadUtil;
import org.jetbrains.jet.lang.types.JetStandardLibrary;

/**
 * @author Stepan Koltsov
 */
public class JetOverloadTest extends JetLiteFixture {

    private ModuleDescriptor root = new ModuleDescriptor("test_root");
    private JetStandardLibrary library;
    private JetSemanticServices semanticServices;
    private DescriptorResolver descriptorResolver;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        library          = JetStandardLibrary.getJetStandardLibrary(getProject());
        semanticServices = JetSemanticServices.createSemanticServices(library);
        descriptorResolver = semanticServices.getClassDescriptorResolver(JetTestUtils.DUMMY_TRACE);
    }

    @Override
    protected String getTestDataPath() {
        return JetTestCaseBuilder.getTestDataPathBase();
    }

    public void testBasic() throws Exception {

        assertNotOverloadable(
                "fun a() : Int",
                "fun a() : Int");

        assertNotOverloadable(
                "fun a() : Int",
                "fun a() : Any");

        assertNotOverloadable(
                "fun a<T1>() : T1",
                "fun a<T>() : T");

        assertNotOverloadable(
                "fun a<T1>(a : T1) : T1",
                "fun a<T>(a : T) : T");

        assertNotOverloadable(
                "fun a<T1, X : T1>(a : T1) : T1",
                "fun a<T, Y : T>(a : T) : T");

        assertNotOverloadable(
                "fun a<T1, X : T1>(a : T1) : T1",
                "fun a<T, Y : T>(a : T) : Y");

        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////


        assertNotOverloadable(
                "fun a() : Int",
                "fun a() : Any");

        assertOverloadable(
                "fun a(a : Int) : Int",
                "fun a() : Int");

        assertOverloadable(
                "fun a() : Int",
                "fun a(a : Int) : Int");

        assertOverloadable(
                "fun a(a : Int?) : Int",
                "fun a(a : Int) : Int");

        // XXX: different from overridable
        /*
        assertNotOverloadable(
                "fun a<T>(a : Int) : Int",
                "fun a(a : Int) : Int");
        */

        assertOverloadable(
                "fun a<T1, X : T1>(a : T1) : T1",
                "fun a<T, Y>(a : T) : T");

        assertOverloadable(
                "fun a<T1, X : T1>(a : T1) : T1",
                "fun a<T, Y : T>(a : Y) : T");

        assertNotOverloadable(
                "fun a<T1, X : T1>(a : T1) : X",
                "fun a<T, Y : T>(a : T) : T");

        assertNotOverloadable(
                "fun a<T1, X : Array<out T1>>(a : Array<in T1>) : T1",
                "fun a<T, Y : Array<out T>>(a : Array<in T>) : T");

        assertOverloadable(
                "fun a<T1, X : Array<T1>>(a : Array<in T1>) : T1",
                "fun a<T, Y : Array<out T>>(a : Array<in T>) : T");

        assertOverloadable(
                "fun a<T1, X : Array<out T1>>(a : Array<in T1>) : T1",
                "fun a<T, Y : Array<in T>>(a : Array<in T>) : T");

        assertOverloadable(
                "fun a<T1, X : Array<out T1>>(a : Array<in T1>) : T1",
                "fun a<T, Y : Array<*>>(a : Array<in T>) : T");

        assertOverloadable(
                "fun a<T1, X : Array<out T1>>(a : Array<in T1>) : T1",
                "fun a<T, Y : Array<out T>>(a : Array<out T>) : T");

        assertOverloadable(
                "fun a<T1, X : Array<out T1>>(a : Array<*>) : T1",
                "fun a<T, Y : Array<out T>>(a : Array<in T>) : T");

        assertOverloadable(
                "fun ff() : Int",
                "fun Int.ff() : Int"
        );
    }

    private void assertNotOverloadable(String funA, String funB) {
        assertOverloadabilityRelation(funA, funB, true);
    }

    private void assertOverloadable(String funA, String funB) {
        assertOverloadabilityRelation(funA, funB, false);
    }

    private void assertOverloadabilityRelation(String funA, String funB, boolean expectedIsError) {
        FunctionDescriptor a = makeFunction(funA);
        FunctionDescriptor b = makeFunction(funB);
        {
            OverloadUtil.OverloadCompatibilityInfo overloadableWith = OverloadUtil.isOverloadble(a, b);
            assertEquals(overloadableWith.getMessage(), expectedIsError, !overloadableWith.isSuccess());
        }
        {
            OverloadUtil.OverloadCompatibilityInfo overloadableWith = OverloadUtil.isOverloadble(b, a);
            assertEquals(overloadableWith.getMessage(), expectedIsError, !overloadableWith.isSuccess());
        }
    }

    private FunctionDescriptor makeFunction(String funDecl) {
        JetNamedFunction function = JetPsiFactory.createFunction(getProject(), funDecl);
        return descriptorResolver.resolveFunctionDescriptor(root, library.getLibraryScope(), function);
    }

}
