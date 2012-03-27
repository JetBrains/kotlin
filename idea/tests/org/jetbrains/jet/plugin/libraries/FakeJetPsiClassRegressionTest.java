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

import com.intellij.psi.PsiReference;
import com.intellij.testFramework.LightProjectDescriptor;
import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.plugin.JetFileType;
import org.jetbrains.jet.plugin.JetLanguage;
import org.jetbrains.jet.plugin.JetWithJdkAndRuntimeLightProjectDescriptor;

/**
 * This is a regression test against KT-1652
 *
 * @author Evgeny Gerashchenko
 * @since 3/27/12
 */
public class FakeJetPsiClassRegressionTest extends LightCodeInsightFixtureTestCase {
    public void testRefToStdlib() {
        String text = "fun foo() { println() }";
        myFixture.configureByText(JetFileType.INSTANCE, text);
        PsiReference ref = myFixture.getFile().findReferenceAt(text.indexOf("println"));
        //noinspection ConstantConditions
        assertSame(JetLanguage.INSTANCE, ref.resolve().getNavigationElement().getLanguage());
    }

    public void testRefToJdk() {
        String text = "val x = java.util.HashMap<String, Int>().get(\"\")";
        myFixture.configureByText(JetFileType.INSTANCE, text);
        PsiReference ref = myFixture.getFile().findReferenceAt(text.indexOf("get"));
        //noinspection ConstantConditions
        ref.resolve().getNavigationElement();
    }

    @NotNull
    @Override
    protected LightProjectDescriptor getProjectDescriptor() {
        return JetWithJdkAndRuntimeLightProjectDescriptor.INSTANCE;
    }
}
