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

package org.jetbrains.jet.plugin.refactoring.changeSignature;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.refactoring.BaseRefactoringProcessor;
import com.intellij.refactoring.util.CommonRefactoringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor;
import org.jetbrains.jet.lang.descriptors.Visibilities;
import org.jetbrains.jet.lang.psi.JetElement;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;
import org.jetbrains.jet.plugin.KotlinCodeInsightTestCase;
import org.jetbrains.jet.plugin.PluginTestCaseBase;
import org.jetbrains.jet.plugin.project.AnalyzerFacadeWithCache;
import org.jetbrains.jet.plugin.refactoring.JetRefactoringBundle;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.jetbrains.jet.lang.psi.PsiPackage.JetPsiFactory;
import static org.jetbrains.jet.plugin.refactoring.changeSignature.ChangeSignaturePackage.getChangeSignatureDialog;

public class JetChangeSignatureTest extends KotlinCodeInsightTestCase {
    public void testBadSelection() throws Exception {
        configureByFile(getTestName(false) + "Before.kt");
        Editor editor = getEditor();
        PsiFile file = getFile();
        assertNull(new JetChangeSignatureHandler().findTargetMember(file, editor));
    }

    public void testRenameFunction() throws Exception {
        JetChangeInfo changeInfo = getChangeInfo();
        changeInfo.setNewName("after");
        doTest(changeInfo);
    }

    public void testChangeReturnType() throws Exception {
        JetChangeInfo changeInfo = getChangeInfo();
        changeInfo.setNewReturnTypeText("Float");
        doTest(changeInfo);
    }

    public void testAddReturnType() throws Exception {
        JetChangeInfo changeInfo = getChangeInfo();
        changeInfo.setNewReturnTypeText("Float");
        doTest(changeInfo);
    }

    public void testRemoveReturnType() throws Exception {
        JetChangeInfo changeInfo = getChangeInfo();
        changeInfo.setNewReturnTypeText("Unit");
        doTest(changeInfo);
    }

    public void testChangeConstructorVisibility() throws Exception {
        JetChangeInfo changeInfo = getChangeInfo();
        changeInfo.setNewVisibility(Visibilities.PROTECTED);
        doTest(changeInfo);
    }

    public void testSynthesized() throws Exception {
        try {
            getChangeInfo();
        }
        catch (CommonRefactoringUtil.RefactoringErrorHintException e) {
            assertEquals(JetRefactoringBundle.message("cannot.refactor.synthesized.function", "component1"), e.getMessage());
            return;
        }
        fail();
    }

    public void testPreferContainedInClass() throws Exception {
        JetChangeInfo changeInfo = getChangeInfo();
        assertEquals("param", changeInfo.getNewParameters()[0].getName());
    }

    public void testAddConstructorVisibility() throws Exception {
        JetChangeInfo changeInfo = getChangeInfo();
        changeInfo.setNewVisibility(Visibilities.PROTECTED);
        JetParameterInfo newParameter = new JetParameterInfo(-1, "x", KotlinBuiltIns.getInstance().getAnyType(),
                                                             null, JetPsiFactory(getProject()).createValOrVarNode("val"));
        newParameter.setDefaultValueText("12");
        changeInfo.addParameter(newParameter);
        doTest(changeInfo);
    }

    public void testConstructor() throws Exception {
        JetChangeInfo changeInfo = getChangeInfo();
        changeInfo.setNewVisibility(Visibilities.PUBLIC);
        changeInfo.getNewParameters()[0].setValOrVar(JetValVar.Var);
        changeInfo.getNewParameters()[1].setValOrVar(JetValVar.None);
        changeInfo.getNewParameters()[2].setValOrVar(JetValVar.Val);
        changeInfo.getNewParameters()[0].setName("_x1");
        changeInfo.getNewParameters()[1].setName("_x2");
        changeInfo.getNewParameters()[2].setName("_x3");
        changeInfo.getNewParameters()[1].setTypeText("Float?");
        doTest(changeInfo);
    }

    public void testConstructorSwapArguments() throws Exception {
        JetChangeInfo changeInfo = getChangeInfo();
        changeInfo.getNewParameters()[0].setName("_x1");
        changeInfo.getNewParameters()[1].setName("_x2");
        JetParameterInfo param = changeInfo.getNewParameters()[0];
        changeInfo.setNewParameter(0, changeInfo.getNewParameters()[2]);
        changeInfo.setNewParameter(2, param);
        doTest(changeInfo);
    }

    public void testFunctions() throws Exception {
        JetChangeInfo changeInfo = getChangeInfo();
        changeInfo.setNewVisibility(Visibilities.PUBLIC);
        changeInfo.getNewParameters()[0].setName("_x1");
        changeInfo.getNewParameters()[1].setName("_x2");
        changeInfo.getNewParameters()[2].setName("_x3");
        changeInfo.getNewParameters()[1].setTypeText("Float?");
        doTest(changeInfo);
    }

    public void testExpressionFunction() throws Exception {
        JetChangeInfo changeInfo = getChangeInfo();
        changeInfo.getNewParameters()[0].setName("x1");
        changeInfo.addParameter(new JetParameterInfo("y1", KotlinBuiltIns.getInstance().getIntType()));
        doTest(changeInfo);
    }

    public void testFunctionsAddRemoveArguments() throws Exception {
        JetChangeInfo changeInfo = getChangeInfo();
        changeInfo.setNewVisibility(Visibilities.INTERNAL);
        changeInfo.setNewParameter(2, changeInfo.getNewParameters()[1]);
        changeInfo.setNewParameter(1, changeInfo.getNewParameters()[0]);
        JetParameterInfo newParameter = new JetParameterInfo("x0", KotlinBuiltIns.getInstance().getNullableAnyType());
        newParameter.setDefaultValueText("null");
        changeInfo.setNewParameter(0, newParameter);
        doTest(changeInfo);
    }

    public void testFakeOverride() throws Exception {
        JetChangeInfo changeInfo = getChangeInfo();
        JetParameterInfo newParameter = new JetParameterInfo("i", KotlinBuiltIns.getInstance().getIntType());
        changeInfo.addParameter(newParameter);
        doTest(changeInfo);
    }

    public void testFunctionLiteral() throws Exception {
        JetChangeInfo changeInfo = getChangeInfo();
        changeInfo.getNewParameters()[1].setName("y1");
        changeInfo.addParameter(new JetParameterInfo("x", KotlinBuiltIns.getInstance().getAnyType()));
        changeInfo.setNewReturnTypeText("Int");
        doTest(changeInfo);
    }

    public void testVarargs() throws Exception {
        try {
            getChangeInfo();
        }
        catch (CommonRefactoringUtil.RefactoringErrorHintException e) {
            assertEquals("Can't refactor the function with variable arguments", e.getMessage());
            return;
        }

        fail("Exception expected");
    }

    public void testUnmodifiableFromLibrary() throws Exception {
        doTestUnmodifiableCheck();
    }


    public void testUnmodifiableFromBuiltins() throws Exception {
        doTestUnmodifiableCheck();
    }

    private void doTestUnmodifiableCheck() throws Exception {
        try {
            JetChangeInfo changeInfo = getChangeInfo();
            JetElement method = (JetElement) changeInfo.getMethod();
            JetChangeSignatureConfiguration empty = new JetChangeSignatureConfiguration() {
                @Override
                public void configure(
                        JetChangeSignatureData data, BindingContext bindingContext
                ) {
                }

                @Override
                public boolean performSilently(Collection<? extends PsiElement> elements) {
                    return true;
                }
            };
            BindingContext context = AnalyzerFacadeWithCache.getContextForElement(method);

            ChangeSignaturePackage
                    .runChangeSignature(getProject(), changeInfo.getOldDescriptor(), empty, context, method, "test");
        }
        catch (RuntimeException e) {
            assertTrue(e.getMessage().startsWith("Refactoring cannot be"));
            return;
        }
        fail("Exception expected");
    }

    public void testInnerFunctionsConflict() throws Exception {
        JetChangeInfo changeInfo = getChangeInfo();
        changeInfo.setNewName("inner2");
        changeInfo.getNewParameters()[0].setName("y");
        doTestConflict(changeInfo);
    }

    public void testMemberFunctionsConflict() throws Exception {
        JetChangeInfo changeInfo = getChangeInfo();
        changeInfo.setNewName("inner2");
        changeInfo.getNewParameters()[0].setName("y");
        doTestConflict(changeInfo);
    }

    public void testTopLevelFunctionsConflict() throws Exception {
        JetChangeInfo changeInfo = getChangeInfo();
        changeInfo.setNewName("fun2");
        doTestConflict(changeInfo);
    }

    public void testConstructorsConflict() throws Exception {
        JetChangeInfo changeInfo = getChangeInfo();
        changeInfo.getNewParameters()[0].setName("_x");
        changeInfo.getNewParameters()[1].setName("_y");
        changeInfo.getNewParameters()[2].setName("_z");
        doTestConflict(changeInfo);
    }

    @NotNull
    @Override
    protected String getTestDataPath() {
        return new File(PluginTestCaseBase.getTestDataPathBase(), "/refactoring/changeSignature").getPath() + File.separator;
    }

    private JetChangeInfo getChangeInfo() throws Exception {
        configureByFile(getTestName(false) + "Before.kt");
        Editor editor = getEditor();
        PsiFile file = getFile();
        JetElement element = (JetElement) new JetChangeSignatureHandler().findTargetMember(file, editor);
        assertNotNull("Target element is null", element);
        Project project = getProject();
        BindingContext bindingContext =
                AnalyzerFacadeWithCache.getContextForElement(element);
        PsiElement context = file.findElementAt(editor.getCaretModel().getOffset());
        assertNotNull(context);
        FunctionDescriptor functionDescriptor = JetChangeSignatureHandler.findDescriptor(element, project, editor, bindingContext);
        assertNotNull(functionDescriptor);
        JetChangeSignatureDialog dialog = getChangeSignatureDialog(project, functionDescriptor,
                                                                   JetChangeSignatureHandler.getConfiguration(), bindingContext, context);
        assertNotNull(dialog);
        dialog.canRun();
        Disposer.register(getTestRootDisposable(), dialog.getDisposable());
        return dialog.evaluateChangeInfo();
    }

    private void doTest(JetChangeInfo changeInfo) throws Exception {
        new JetChangeSignatureProcessor(getProject(), changeInfo, "Change signature").run();
        checkResultByFile(getTestName(false) + "After.kt");
    }

    private void doTestConflict(JetChangeInfo changeInfo) throws Exception {
        try {
            new JetChangeSignatureProcessor(getProject(), changeInfo, "Change signature").run();
        }
        catch (BaseRefactoringProcessor.ConflictsInTestsException e) {
            List<String> messages = new ArrayList<String>(e.getMessages());
            Collections.sort(messages);
            File conflictsFile = new File(getTestDataPath() + getTestName(false) + "Messages.txt");
            assertSameLinesWithFile(conflictsFile.getAbsolutePath(), StringUtil.join(messages, "\n"));
            return;
        }

        fail("No conflicts found");
    }
}
