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
 * @author slukjanov aka Frostman
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

    public void testFunctionMethod() {
        doImplementFileTest();
    }

    public void testFunctionProperty() {
        doImplementFileTest();
    }

    public void testJavaInterfaceMethod() {
        doImplementDirectoryTest();
    }

    public void testJavaParameters() {
        doImplementDirectoryTest();
    }
    
    public void testGenericMethod() {
        doImplementFileTest();
    }

    public void testProperty() {
        doImplementFileTest();
    }

    public void testTraitGenericImplement() {
        doImplementFileTest();
    }

    public void testRespectCaretPosition() {
        doMultiImplementFileTest();
    }

    public void testGenerateMulti() {
        doMultiImplementFileTest();
    }

    public void testTraitNullableFunction() {
        doImplementFileTest();
    }

    public void testOverrideUnitFunction() {
        doOverrideFileTest();
    }

    public void testOverrideNonUnitFunction() {
        doOverrideFileTest();
    }

    public void testOverrideFunctionProperty() {
        doOverrideFileTest();
    }

    public void testOverrideGenericFunction() {
        doOverrideFileTest();
    }

    public void testMultiOverride() {
        doMultiOverrideFileTest();
    }

    public void testComplexMultiOverride() {
        doMultiOverrideFileTest();
    }

    public void testOverrideRespectCaretPosition() {
        doMultiOverrideFileTest();
    }

    public void testOverrideJavaMethod() {
        doOverrideDirectoryTest();
    }

    public void testInheritVisibilities() {
        doMultiOverrideFileTest();
    }

    private void doImplementFileTest() {
        doFileTest(new ImplementMethodsHandler());
    }

    private void doOverrideFileTest() {
        doFileTest(new OverrideMethodsHandler());
    }

    private void doMultiImplementFileTest() {
        doMultiFileTest(new ImplementMethodsHandler());
    }

    private void doMultiOverrideFileTest() {
        doMultiFileTest(new OverrideMethodsHandler());
    }

    private void doImplementDirectoryTest() {
        doDirectoryTest(new ImplementMethodsHandler());
    }

    private void doOverrideDirectoryTest() {
        doDirectoryTest(new OverrideMethodsHandler());
    }

    private void doFileTest(OverrideImplementMethodsHandler handler) {
        myFixture.configureByFile(getTestName(true) + ".kt");
        doOverrideImplement(handler);
        myFixture.checkResultByFile(getTestName(true) + ".kt.after");
    }

    private void doMultiFileTest(OverrideImplementMethodsHandler handler) {
        myFixture.configureByFile(getTestName(true) + ".kt");
        doMultiOverrideImplement(handler);
        myFixture.checkResultByFile(getTestName(true) + ".kt.after");
    }

    private void doDirectoryTest(OverrideImplementMethodsHandler handler) {
        myFixture.copyDirectoryToProject(getTestName(true), "");
        myFixture.configureFromTempProjectFile("foo/Impl.kt");
        doOverrideImplement(handler);
        myFixture.checkResultByFile(getTestName(true) + "/foo/Impl.kt.after");
    }

    private void doOverrideImplement(OverrideImplementMethodsHandler handler) {
        final PsiElement elementAtCaret = myFixture.getFile().findElementAt(myFixture.getEditor().getCaretModel().getOffset());
        final JetClassOrObject classOrObject = PsiTreeUtil.getParentOfType(elementAtCaret, JetClassOrObject.class);
        assertNotNull("Caret should be inside class or object", classOrObject);
        final Set<CallableMemberDescriptor> descriptors = handler.collectMethodsToGenerate(classOrObject);
        assertEquals("Invalid number of available descriptors for override", 1, descriptors.size());
        new WriteCommandAction(myFixture.getProject(), myFixture.getFile()) {
            @Override
            protected void run(Result result) throws Throwable {
                OverrideImplementMethodsHandler.generateMethods(
                        myFixture.getProject(), myFixture.getEditor(), classOrObject,
                        OverrideImplementMethodsHandler.membersFromDescriptors(descriptors));
            }
        }.execute();
    }

    private void doMultiOverrideImplement(OverrideImplementMethodsHandler handler) {
        final PsiElement elementAtCaret = myFixture.getFile().findElementAt(myFixture.getEditor().getCaretModel().getOffset());
        final JetClassOrObject classOrObject = PsiTreeUtil.getParentOfType(elementAtCaret, JetClassOrObject.class);
        assertNotNull("Caret should be inside class or object", classOrObject);
        final Set<CallableMemberDescriptor> descriptors = handler.collectMethodsToGenerate(classOrObject);

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
