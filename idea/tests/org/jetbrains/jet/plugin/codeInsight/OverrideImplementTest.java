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

package org.jetbrains.jet.plugin.codeInsight;

import com.intellij.openapi.application.Result;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.testFramework.LightProjectDescriptor;
import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.CallableMemberDescriptor;
import org.jetbrains.jet.lang.psi.JetClassOrObject;
import org.jetbrains.jet.plugin.JetLightProjectDescriptor;
import org.jetbrains.jet.plugin.PluginTestCaseBase;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Set;

/**
 * @author yole
 */
public class OverrideImplementTest extends LightCodeInsightFixtureTestCase {
    @NotNull
    @Override
    protected LightProjectDescriptor getProjectDescriptor() {
        return JetLightProjectDescriptor.INSTANCE;
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        myFixture.setTestDataPath(PluginTestCaseBase.getTestDataPathBase() + "/codeInsight/overrideImplement");
    }

    public void testJavaInterfaceMethod() {
        doDirectoryTest();
    }

    public void testJavaParameters() {
        doDirectoryTest();
    }
    
    public void testGenericMethod() {
        doFileTest();
    }

    public void testProperty() {
        doFileTest();
    }

    public void testTraitGenericOverride() {
        doFileTest();
    }

    public void testTraitNullableFunction() {
        doFileTest();
    }

    public void testGenerateMulti() {
        doMultiFileTest();
    }

    private void doFileTest() {
        myFixture.configureByFile(getTestName(true) + ".kt");
        doImplement();
        myFixture.checkResultByFile(getTestName(true) + ".kt.after");
    }

    private void doMultiFileTest() {
        myFixture.configureByFile(getTestName(true) + ".kt");
        doMultiImplement();
        myFixture.checkResultByFile(getTestName(true) + ".kt.after");
    }

    private void doDirectoryTest() {
        myFixture.copyDirectoryToProject(getTestName(true), "");
        myFixture.configureFromTempProjectFile("foo/Impl.kt");
        doImplement();
        myFixture.checkResultByFile(getTestName(true) + "/foo/Impl.kt.after");
    }

    private void doImplement() {
        final PsiElement elementAtCaret = myFixture.getFile().findElementAt(myFixture.getEditor().getCaretModel().getOffset());
        final JetClassOrObject classOrObject = PsiTreeUtil.getParentOfType(elementAtCaret, JetClassOrObject.class);
        final Set<CallableMemberDescriptor> descriptors = new ImplementMethodsHandler().collectMethodsToGenerate(classOrObject);
        assertEquals(1, descriptors.size());
        new WriteCommandAction(myFixture.getProject(), myFixture.getFile()) {
            @Override
            protected void run(Result result) throws Throwable {
                OverrideImplementMethodsHandler.generateMethods(
                        myFixture.getProject(), myFixture.getEditor(), classOrObject,
                        OverrideImplementMethodsHandler.membersFromDescriptors(descriptors));
            }
        }.execute();
    }

    private void doMultiImplement() {
        final PsiElement elementAtCaret = myFixture.getFile().findElementAt(myFixture.getEditor().getCaretModel().getOffset());
        final JetClassOrObject classOrObject = PsiTreeUtil.getParentOfType(elementAtCaret, JetClassOrObject.class);
        final Set<CallableMemberDescriptor> descriptors = new ImplementMethodsHandler().collectMethodsToGenerate(classOrObject);

        final ArrayList<CallableMemberDescriptor> descriptorsList = new ArrayList<CallableMemberDescriptor>(descriptors);
        Collections.sort(descriptorsList, new Comparator<CallableMemberDescriptor>() {
            @Override
            public int compare(CallableMemberDescriptor desc1, CallableMemberDescriptor desc2) {
                return desc1.getName().compareTo(desc2.getName());
            }
        });

        new WriteCommandAction(myFixture.getProject(), myFixture.getFile()) {
            @Override
            protected void run(Result result) throws Throwable {
                OverrideImplementMethodsHandler.generateMethods(
                        myFixture.getProject(), myFixture.getEditor(), classOrObject,
                        OverrideImplementMethodsHandler.membersFromDescriptors(descriptorsList));
            }
        }.execute();
    }
}
