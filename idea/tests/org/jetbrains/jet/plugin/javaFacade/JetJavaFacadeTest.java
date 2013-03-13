/*
 * Copyright 2010-2013 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.jet.plugin.javaFacade;

import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.testFramework.LightProjectDescriptor;
import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.asJava.KotlinLightClass;
import org.jetbrains.jet.asJava.LightClassUtil;
import org.jetbrains.jet.lang.psi.JetClass;
import org.jetbrains.jet.lang.psi.JetNamedFunction;
import org.jetbrains.jet.lang.resolve.java.JvmAbi;
import org.jetbrains.jet.plugin.JetLightProjectDescriptor;
import org.jetbrains.jet.plugin.PluginTestCaseBase;

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

    public void testDoNotWrapFunFromLocalClass() {
        doTestWrapMethod(false);
    }

    public void testDoNotWrapFunInAnonymousObject() {
        doTestWrapMethod(false);
    }

    public void testWrapFunInClassObject() {
        doTestWrapMethod(true);
    }

    public void testWrapTopLevelFun() {
        doTestWrapMethod(true);
    }

    public void testWrapFunWithDefaultParam() {
        doTestWrapMethod(true);
    }

    public void testWrapFunWithImplInTrait() {
        doTestWrapMethod(true);
    }

    public void testWrapFunWithoutImplInTrait() {
        doTestWrapMethod(true);
    }

    public void testWrapFunInObject() {
        doTestWrapMethod(true);
    }

    public void testWrapFunInObjectInObject() {
        doTestWrapMethod(true);
    }

    public void testKt2764() {
        doTestWrapClass();
    }

    public void testEa37034() {
        doTestWrapClass();
    }

    public void testEa38770() {
        myFixture.configureByFile(getTestName(true) + ".kt");

        PsiReference reference = myFixture.getFile().findReferenceAt(myFixture.getCaretOffset());
        assertNotNull(reference);
        PsiElement element = reference.resolve();
        assertNotNull(element);
        assertInstanceOf(element, JetNamedFunction.class);
        JetNamedFunction toString = (JetNamedFunction) element;

        assertNull("There should be no wrapper for built-in function", LightClassUtil.getLightClassMethod(toString));
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

        PsiField classobjField = theClass.findFieldByName("$classobj", false);
        assertNull(classobjField);

        PsiClass classObjectClass = theClass.findInnerClassByName("object", false);
        assertNotNull(classObjectClass);
        assertEquals("foo.TheClass.object", classObjectClass.getQualifiedName());
        assertTrue(classObjectClass.hasModifierProperty(PsiModifier.STATIC));

        PsiField instance = theClass.findFieldByName(JvmAbi.CLASS_OBJECT_FIELD, false);
        assertNotNull(instance);
        assertEquals("foo.TheClass.object", instance.getType().getCanonicalText());
        assertTrue(instance.hasModifierProperty(PsiModifier.PUBLIC));
        assertTrue(instance.hasModifierProperty(PsiModifier.STATIC));
        assertTrue(instance.hasModifierProperty(PsiModifier.FINAL));

        PsiMethod[] methods = classObjectClass.findMethodsByName("getOut", false);
        
        assertEquals("java.io.PrintStream", methods[0].getReturnType().getCanonicalText());
    }

    public void testLightClassIsNotCreatedForBuiltins() throws Exception {
        myFixture.configureByFile(getTestName(true) + ".kt");

        PsiReference reference = myFixture.getFile().findReferenceAt(myFixture.getCaretOffset());
        assert reference != null;
        PsiElement element = reference.resolve();
        assertInstanceOf(element, JetClass.class);
        JetClass aClass = (JetClass) element;

        KotlinLightClass createdByWrapDelegate = LightClassUtil.createLightClass(aClass);
        assertNull(createdByWrapDelegate);
    }

    private void doTestWrapMethod(boolean shouldBeWrapped) {
        myFixture.configureByFile(getTestName(true) + ".kt");

        int offset = myFixture.getEditor().getCaretModel().getOffset();
        PsiElement elementAt = myFixture.getFile().findElementAt(offset);

        assertNotNull("Caret should be set for tested file", elementAt);

        JetNamedFunction jetFunction = PsiTreeUtil.getParentOfType(elementAt, JetNamedFunction.class);
        assertNotNull("Caret should be placed to function definition", jetFunction);

        // Should not fail!
        PsiMethod psiMethod = LightClassUtil.getLightClassMethod(jetFunction);

        if (shouldBeWrapped) {
            assertNotNull(String.format("Failed to wrap jetFunction '%s' to method", jetFunction.getText()), psiMethod);
            assertInstanceOf(psiMethod, PsiCompiledElement.class);
            assertEquals("Invalid original element for generated method", ((PsiCompiledElement) psiMethod).getMirror(), jetFunction);
        }
        else {
            assertNull("There should be no wrapper for given method", psiMethod);
        }
    }

    private void doTestWrapClass() {
        myFixture.configureByFile(getTestName(true) + ".kt");

        int offset = myFixture.getEditor().getCaretModel().getOffset();
        PsiElement elementAt = myFixture.getFile().findElementAt(offset);

        assertNotNull("Caret should be set for tested file", elementAt);

        JetClass jetClass = PsiTreeUtil.getParentOfType(elementAt, JetClass.class);
        assertNotNull("Caret should be placed to class definition", jetClass);

        // Should not fail!
        KotlinLightClass lightClass = LightClassUtil.createLightClass(jetClass);

        assertNotNull(String.format("Failed to wrap jetClass '%s' to class", jetClass.getText()), lightClass);

        // This invokes codegen with ClassBuilderMode = SIGNATURES
        // No exception/error should happen here
        lightClass.getDelegate();
    }
}
