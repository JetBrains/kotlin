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

package org.jetbrains.jet.plugin.codeInsight;

import com.intellij.openapi.application.Result;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.testFramework.LightProjectDescriptor;
import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.CallableMemberDescriptor;
import org.jetbrains.jet.lang.psi.JetClassOrObject;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.plugin.JetLightProjectDescriptor;
import org.jetbrains.jet.plugin.PluginTestCaseBase;
import org.jetbrains.jet.plugin.project.WholeProjectAnalyzerFacade;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Set;

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

    public void testEmptyClassBodyFunctionMethod() {
        doImplementFileTest();
    }

    public void testFunctionMethod() {
        doImplementFileTest();
    }

    public void testFunctionProperty() {
        doImplementFileTest();
    }

    public void testFunctionWithTypeParameters() {
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

    public void testOverrideExplicitFunction() {
        doOverrideFileTest();
    }

    public void testOverrideExplicitProperty() {
        doOverrideFileTest();
    }

    public void testComplexMultiOverride() {
        doMultiOverrideFileTest();
    }

    public void testOverrideRespectCaretPosition() {
        doMultiOverrideFileTest();
    }

    public void testOverrideJavaMethod() {
        doOverrideDirectoryTest("getAnswer");
    }

    public void testJavaMethodWithPackageVisibility() {
        doOverrideDirectoryTest("getFooBar");
    }

    public void testJavaMethodWithPackageProtectedVisibility() {
        doOverrideDirectoryTest("getFooBar");
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

    private void doOverrideDirectoryTest(@Nullable String memberToImplement) {
        doDirectoryTest(new OverrideMethodsHandler(), memberToImplement);
    }

    public void testSameTypeName() {
        doDirectoryTest(new OverrideMethodsHandler());
    }

    private void doFileTest(OverrideImplementMethodsHandler handler) {
        myFixture.configureByFile(getTestName(true) + ".kt");
        doOverrideImplement(handler, null);
        myFixture.checkResultByFile(getTestName(true) + ".kt.after");
    }

    private void doMultiFileTest(OverrideImplementMethodsHandler handler) {
        myFixture.configureByFile(getTestName(true) + ".kt");
        doMultiOverrideImplement(handler);
        myFixture.checkResultByFile(getTestName(true) + ".kt.after");
    }

    private void doDirectoryTest(OverrideImplementMethodsHandler handler) {
        doDirectoryTest(handler, null);
    }

    private void doDirectoryTest(OverrideImplementMethodsHandler handler, @Nullable String memberToOverride) {
        myFixture.copyDirectoryToProject(getTestName(true), "");
        myFixture.configureFromTempProjectFile("foo/Impl.kt");
        doOverrideImplement(handler, memberToOverride);
        myFixture.checkResultByFile(getTestName(true) + "/foo/Impl.kt.after");
    }

    private void doOverrideImplement(OverrideImplementMethodsHandler handler, @Nullable String memberToOverride) {
        PsiElement elementAtCaret = myFixture.getFile().findElementAt(myFixture.getEditor().getCaretModel().getOffset());
        final JetClassOrObject classOrObject = PsiTreeUtil.getParentOfType(elementAtCaret, JetClassOrObject.class);
        assertNotNull("Caret should be inside class or object", classOrObject);

        final JetFile jetFile = (JetFile) classOrObject.getContainingFile();
        final BindingContext bindingContext = WholeProjectAnalyzerFacade
                .analyzeProjectWithCacheOnAFile(jetFile)
                .getBindingContext();
        Set<CallableMemberDescriptor> descriptors = handler.collectMethodsToGenerate(classOrObject, bindingContext);

        final CallableMemberDescriptor singleToOverride;
        if (memberToOverride == null) {
            assertEquals("Invalid number of available descriptors for override", 1, descriptors.size());
            singleToOverride = descriptors.iterator().next();
        }
        else {
            CallableMemberDescriptor candidateToOverride = null;
            for (CallableMemberDescriptor callable : descriptors) {
                if (callable.getName().getName().equals(memberToOverride)) {
                    if (candidateToOverride != null) {
                        throw new IllegalStateException("more then one descriptor with name " + memberToOverride);
                    }
                    candidateToOverride = callable;
                }
            }
            if (candidateToOverride == null) {
                throw new IllegalStateException("no descriptors to override with name " + memberToOverride + " found");
            }
            singleToOverride = candidateToOverride;
        }

        new WriteCommandAction(myFixture.getProject(), myFixture.getFile()) {
            @Override
            protected void run(Result result) throws Throwable {
                OverrideImplementMethodsHandler.generateMethods(
                        myFixture.getEditor(), classOrObject,
                        OverrideImplementMethodsHandler
                                .membersFromDescriptors(jetFile, Collections.singletonList(singleToOverride), bindingContext));
            }
        }.execute();
    }

    private void doMultiOverrideImplement(OverrideImplementMethodsHandler handler) {
        PsiElement elementAtCaret = myFixture.getFile().findElementAt(myFixture.getEditor().getCaretModel().getOffset());
        final JetClassOrObject classOrObject = PsiTreeUtil.getParentOfType(elementAtCaret, JetClassOrObject.class);
        assertNotNull("Caret should be inside class or object", classOrObject);

        final JetFile jetFile = (JetFile) classOrObject.getContainingFile();
        final BindingContext bindingContext = WholeProjectAnalyzerFacade
                .analyzeProjectWithCacheOnAFile(jetFile)
                .getBindingContext();
        Set<CallableMemberDescriptor> descriptors = handler.collectMethodsToGenerate(classOrObject, bindingContext);

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
                        myFixture.getEditor(), classOrObject,
                        OverrideImplementMethodsHandler.membersFromDescriptors(jetFile, descriptorsList, bindingContext));
            }
        }.execute();
    }
}
