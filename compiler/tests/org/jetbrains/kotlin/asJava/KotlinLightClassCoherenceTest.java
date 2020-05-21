/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.asJava;

import com.google.common.collect.Lists;
import com.intellij.openapi.util.Comparing;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.search.GlobalSearchScope;
import junit.framework.ComparisonFailure;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.asJava.classes.KtLightClass;
import org.jetbrains.kotlin.asJava.classes.KtUltraLightSupport;
import org.jetbrains.kotlin.name.SpecialNames;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;


public class KotlinLightClassCoherenceTest extends KotlinAsJavaTestBase {

    @Override
    protected List<File> getKotlinSourceRoots() {
        return Lists.newArrayList(
                new File("compiler/testData/asJava/lightClassStructure/Declared.kt"),
                new File("compiler/testData/asJava/lightClassStructure/Package.kt"),
                new File("compiler/testData/asJava/lightClassStructure/ClassObject.kt")
        );
    }

    @NotNull
    protected PsiClass doTest() {
        return doTest("test." + getTestName(false));
    }

    @NotNull
    protected PsiClass doTest(String qualifiedName) {
        boolean forceFlag = KtUltraLightSupport.Companion.getForceUsingOldLightClasses();
        KtLightClass psiClass;
        try {
            KtUltraLightSupport.Companion.setForceUsingOldLightClasses(true);
            psiClass = (KtLightClass) finder.findClass(qualifiedName, GlobalSearchScope.allScope(getProject()));
        } finally {
            KtUltraLightSupport.Companion.setForceUsingOldLightClasses(forceFlag);
        }

        assertNotNull("Class not found: " + qualifiedName, psiClass);

        Asserter asserter = new Asserter();

        asserter.assertModifiersCoherent(psiClass);

        asserter.assertPropertyCoherent(psiClass, "isInterface");
        asserter.assertPropertyCoherent(psiClass, "isAnnotationType");
        asserter.assertPropertyCoherent(psiClass, "isEnum");
        asserter.assertPropertyCoherent(psiClass, "hasTypeParameters");
        asserter.assertPropertyCoherent(psiClass, "isDeprecated");

        asserter.reportFailures();

        return psiClass;
    }

    static class Asserter {
        private final List<ComparisonFailure> failures = Lists.newArrayList();

        private void assertEquals(String message, Object expected, Object actual) {
            if (!Comparing.equal(expected, actual)) {
                failures.add(new ComparisonFailure(message, String.valueOf(expected), String.valueOf(actual)));
            }
        }

        public void assertModifiersCoherent(KtLightClass lightClass) {
            PsiClass delegate = lightClass.getClsDelegate();
            for (String modifier : PsiModifier.MODIFIERS) {
                assertEquals("Incoherent modifier: " + modifier,
                             delegate.hasModifierProperty(modifier),
                             lightClass.hasModifierProperty(modifier));
            }
        }

        public void assertPropertyCoherent(KtLightClass lightClass, String methodName) {
            try {
                Method method = PsiClass.class.getMethod(methodName);
                Object lightResult = method.invoke(lightClass);
                Object delegateResult = method.invoke(lightClass.getClsDelegate());
                assertEquals("Result of method " + methodName + "() differs in light class and its delegate", delegateResult, lightResult);
            }
            catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                throw new AssertionError(e);
            }
        }

        public void reportFailures() {
            if (failures.size() == 1) {
                throw failures.get(0);
            }
            if (!failures.isEmpty()) {
                StringBuilder builder = new StringBuilder("\n");
                for (ComparisonFailure failure : failures) {
                    builder.append(failure.getMessage()).append("\n");
                }
                fail(builder.toString());
            }
        }
    }

    public void testFileFacade() throws Exception {
        doTest("test.PackageKt");
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

    public void testNestedPublic() throws Exception {
        doTest("test.Outer.Public");
    }

    public void testNestedProtected() throws Exception {
        doTest("test.Outer.Protected");
    }

    public void testNestedInternal() throws Exception {
        doTest("test.Outer.Internal");
    }

    public void testNestedPrivate() throws Exception {
        doTest("test.Outer.Private");
    }

    public void testInner() throws Exception {
        doTest("test.Outer.Inner");
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

    public void testDeprecatedClass() throws Exception {
        doTest();
    }

    public void testDeprecatedFQN() throws Exception {
        doTest();
    }

    public void testDeprecatedFQNSpaces() throws Exception {
        doTest();
    }

    public void testDeprecatedWithBrackets() throws Exception {
        doTest();
    }

    public void testDeprecatedWithBracketsFQN() throws Exception {
        doTest();
    }

    public void testDeprecatedWithBracketsFQNSpaces() throws Exception {
        doTest();
    }

    public void testClassObject() throws Exception {
        doTest("test.WithClassObject." + SpecialNames.DEFAULT_NAME_FOR_COMPANION_OBJECT.asString());
    }
}
