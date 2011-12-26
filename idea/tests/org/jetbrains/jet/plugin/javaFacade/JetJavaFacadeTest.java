package org.jetbrains.jet.plugin.javaFacade;

import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.testFramework.LightProjectDescriptor;
import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.plugin.JetLightProjectDescriptor;
import org.jetbrains.jet.plugin.PluginTestCaseBase;

/**
 * @author max
 */
public class JetJavaFacadeTest extends LightCodeInsightFixtureTestCase {
    @NotNull
    @Override
    protected LightProjectDescriptor getProjectDescriptor() {
        return JetLightProjectDescriptor.INSTANCE;
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        myFixture.setTestDataPath(PluginTestCaseBase.getTestDataPathBase() + "/javaFacade");
    }

    public void testInnerClass() throws Exception {
        myFixture.configureByFile(getTestName(true) + ".kt");
        
        JavaPsiFacade facade = myFixture.getJavaFacade();
        PsiClass mirrorClass = facade.findClass("foo.Outer.Inner", GlobalSearchScope.allScope(getProject()));
        
        assertNotNull(mirrorClass);
        PsiMethod[] fun = mirrorClass.findMethodsByName("innerFun", false);
        
        assertEquals(fun[0].getReturnType(), PsiType.VOID);
    }
    
    public void testClassObject() throws Exception {
        myFixture.configureByFile(getTestName(true) + ".kt");
        
        JavaPsiFacade facade = myFixture.getJavaFacade();
        PsiClass theClass = facade.findClass("foo.TheClass", GlobalSearchScope.allScope(getProject()));

        assertNotNull(theClass);

        PsiField classobj = theClass.findFieldByName("$classobj", false);
        assertTrue(classobj != null && classobj.hasModifierProperty(PsiModifier.STATIC));

        PsiType type = classobj.getType();
        assertTrue(type instanceof PsiClassType);
        
        assertEquals("foo.TheClass.ClassObject$", type.getCanonicalText());

        PsiClass classObjectClass = ((PsiClassType) type).resolve();
        assertTrue(classObjectClass != null && classObjectClass.hasModifierProperty(PsiModifier.STATIC));
        PsiMethod[] methods = classObjectClass.findMethodsByName("getOut", false);
        
        assertEquals("java.io.PrintStream", methods[0].getReturnType().getCanonicalText());
    }
}
