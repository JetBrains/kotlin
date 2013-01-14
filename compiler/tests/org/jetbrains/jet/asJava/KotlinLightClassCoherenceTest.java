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

package org.jetbrains.jet.asJava;

import com.google.common.collect.Lists;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;


public class KotlinLightClassCoherenceTest extends KotlinAsJavaTestBase {

    @Override
    protected List<File> getKotlinSourceRoots() {
        return Lists.newArrayList(
                new File("compiler/testData/asJava/lightClasses/Declared.kt"),
                new File("compiler/testData/asJava/lightClasses/Package.kt")
        );
    }

    @NotNull
    protected PsiClass doTest() {
        return doTest("test." + getTestName(false));
    }

    @NotNull
    protected PsiClass doTest(String qualifiedName) {
        KotlinLightClass psiClass = (KotlinLightClass) finder.findClass(qualifiedName, GlobalSearchScope.allScope(getProject()));
        assertNotNull(psiClass);

        assertModifiersCoherent(psiClass);

        assertPropertyCoherent(psiClass, "isInterface");
        assertPropertyCoherent(psiClass, "isAnnotationType");
        assertPropertyCoherent(psiClass, "isEnum");
        assertPropertyCoherent(psiClass, "hasTypeParameters");
        assertPropertyCoherent(psiClass, "isDeprecated");

        return psiClass;
    }

    private static void assertModifiersCoherent(KotlinLightClass lightClass) {
        PsiClass delegate = lightClass.getDelegate();
        for (String modifier : PsiModifier.MODIFIERS) {
            assertEquals("Incoherent modifier: " + modifier,
                         delegate.hasModifierProperty(modifier),
                         lightClass.hasModifierProperty(modifier));
        }
    }

    private static void assertPropertyCoherent(KotlinLightClass lightClass, String methodName) {
        Class<?> reflect = PsiClass.class;
        try {
            Method method = reflect.getMethod(methodName);
            Object lightResult = method.invoke(lightClass);
            Object delegateResult = method.invoke(lightClass.getDelegate());
            assertEquals("Result of method " + methodName + "() differs in light class and its delegate", delegateResult, lightResult);
        }
        catch (NoSuchMethodException e) {
            throw new AssertionError(e);
        }
        catch (InvocationTargetException e) {
            throw new AssertionError(e);
        }
        catch (IllegalAccessException e) {
            throw new AssertionError(e);
        }
    }

    public void testPackage() throws Exception {
        doTest("test.namespace");
    }

    public void testNoModifiers() throws Exception {
        doTest();
    }

    public void testPublic() throws Exception {
        doTest();
    }

    public void testPrivate() throws Exception {
        doTest();
    }

    public void testInternal() throws Exception {
        doTest();
    }

    public void testInnerPublic() throws Exception {
        doTest("test.Outer.Public");
    }

    public void testInnerProtected() throws Exception {
        doTest("test.Outer.Protected");
    }

    public void testInnerInternal() throws Exception {
        doTest("test.Outer.Internal");
    }

    public void testInnerPrivate() throws Exception {
        doTest("test.Outer.Private");
    }

    public void testAbstract() throws Exception {
        doTest();
    }

    public void testOpen() throws Exception {
        doTest();
    }

    public void testFinal() throws Exception {
        doTest();
    }

    public void testAnnotation() throws Exception {
        doTest();
    }

    public void testEnum() throws Exception {
        doTest();
    }

    public void testTrait() throws Exception {
        doTest();
    }

    public void testDeprectaed() throws Exception {
        doTest();
    }

    public void testDeprecatedWithBrackets() throws Exception {
        doTest();
    }
}
