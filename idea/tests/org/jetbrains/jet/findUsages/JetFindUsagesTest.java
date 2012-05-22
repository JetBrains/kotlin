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

package org.jetbrains.jet.findUsages;

import com.intellij.psi.PsiField;
import com.intellij.psi.PsiStatement;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.testFramework.LightProjectDescriptor;
import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase;
import com.intellij.usageView.UsageInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.JetClass;
import org.jetbrains.jet.lang.psi.JetFunction;
import org.jetbrains.jet.plugin.JetLightProjectDescriptor;
import org.jetbrains.jet.plugin.PluginTestCaseBase;

import java.util.Collection;

/**
 * @author yole
 */
public class JetFindUsagesTest extends LightCodeInsightFixtureTestCase {
    @NotNull
    @Override
    protected LightProjectDescriptor getProjectDescriptor() {
        return JetLightProjectDescriptor.INSTANCE;
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        myFixture.setTestDataPath(PluginTestCaseBase.getTestDataPathBase() + "/findUsages");
    }

    public void testFindClassUsages() {
        myFixture.configureByFiles("findClassUsages/Server.kt", "findClassUsages/Client.java");
        JetClass cls = PsiTreeUtil.getParentOfType(myFixture.getElementAtCaret(), JetClass.class, false);
        final Collection<UsageInfo> usages = myFixture.findUsages(cls);
        assertEquals(1, usages.size());
        UsageInfo first = usages.iterator().next();
        final PsiField field = PsiTreeUtil.getParentOfType(first.getElement(), PsiField.class);
        assertEquals("private Server myServer;", field.getText());
    }

    public void testFindMethodUsages() {
        myFixture.configureByFiles("findMethodUsages/Server.kt", "findMethodUsages/Client.java");
        JetFunction function = PsiTreeUtil.getParentOfType(myFixture.getElementAtCaret(), JetFunction.class, false);
        final Collection<UsageInfo> usages = myFixture.findUsages(function);
        assertEquals(1, usages.size());
        UsageInfo first = usages.iterator().next();
        final PsiStatement stmt = PsiTreeUtil.getParentOfType(first.getElement(), PsiStatement.class);
        assertEquals("server.processRequest();", stmt.getText());
    }
}
