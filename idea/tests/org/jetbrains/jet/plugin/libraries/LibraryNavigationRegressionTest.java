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

package org.jetbrains.jet.plugin.libraries;

import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.testFramework.LightProjectDescriptor;
import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.plugin.JetFileType;
import org.jetbrains.jet.plugin.JetLanguage;
import org.jetbrains.jet.plugin.JetWithJdkAndRuntimeLightProjectDescriptor;

/**
 * @author Evgeny Gerashchenko
 * @since 3/27/12
 */
public class LibraryNavigationRegressionTest extends LightCodeInsightFixtureTestCase {
    /**
     * Regression test against KT-1652
     */
    public void testRefToStdlib() {
        PsiElement navigationElement = configureAndResolve("fun foo() { <caret>println() }");
        assertSame(JetLanguage.INSTANCE, navigationElement.getLanguage());
    }

    /**
     * Regression test against KT-1652
     */
    public void testRefToJdk() {
        configureAndResolve("val x = java.util.HashMap<String, Int>().<caret>get(\"\")");
    }

    /**
     * Regression test against KT-1815
     */
    public void testRefToClassesWithAltSignatureAnnotations() {
        PsiElement navigationElement = configureAndResolve("fun foo(e : java.util.Map.Entry<String, String>) { e.<caret>getKey(); }");
        PsiClass expectedClass =
                JavaPsiFacade.getInstance(getProject()).findClass("java.util.Map.Entry", GlobalSearchScope.allScope(getProject()));
        assertSame(expectedClass, navigationElement.getParent());
    }


    public void testRefToFunctionWithVararg() {
        PsiElement navigationElement = configureAndResolve("val x = <caret>arrayList(\"\", \"\")");
        assertSame(JetLanguage.INSTANCE, navigationElement.getLanguage());
    }

    @NotNull
    @Override
    protected LightProjectDescriptor getProjectDescriptor() {
        return JetWithJdkAndRuntimeLightProjectDescriptor.INSTANCE;
    }

    private PsiElement configureAndResolve(String text) {
        myFixture.configureByText(JetFileType.INSTANCE, text);
        PsiReference ref = myFixture.getFile().findReferenceAt(myFixture.getCaretOffset());
        //noinspection ConstantConditions
        return ref.resolve().getNavigationElement();
    }
}
