/*
 * Copyright 2010-2012 JetBrains s.r.o.
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
