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

import com.intellij.codeInsight.TargetElementUtilBase;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.refactoring.BaseRefactoringProcessor;
import com.intellij.refactoring.changeSignature.ChangeSignatureProcessor;
import com.intellij.refactoring.changeSignature.ParameterInfoImpl;
import com.intellij.refactoring.changeSignature.ThrownExceptionInfo;
import com.intellij.refactoring.util.CommonRefactoringUtil;
import com.intellij.util.ArrayUtil;
import com.intellij.util.VisibilityUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor;
import org.jetbrains.jet.lang.descriptors.Visibilities;
import org.jetbrains.jet.lang.psi.JetElement;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.dataClassUtils.DataClassUtilsPackage;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;
import org.jetbrains.jet.plugin.KotlinCodeInsightTestCase;
import org.jetbrains.jet.plugin.PluginTestCaseBase;
import org.jetbrains.jet.plugin.caches.resolve.ResolvePackage;
import org.jetbrains.jet.plugin.refactoring.JetRefactoringBundle;

import java.io.File;
import java.util.*;

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
            assertEquals(JetRefactoringBundle.message("cannot.refactor.synthesized.function", DataClassUtilsPackage.createComponentName(1).asString()), e.getMessage());
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
                                                             null, "12", JetPsiFactory(getProject()).createValOrVarNode("val"), null);
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
        changeInfo.getNewParameters()[1].setCurrentTypeText("Float?");
        doTest(changeInfo);
    }

    public void testGenericConstructor() throws Exception {
        JetChangeInfo changeInfo = getChangeInfo();
        changeInfo.setNewVisibility(Visibilities.PUBLIC);
        changeInfo.getNewParameters()[0].setValOrVar(JetValVar.Var);
        changeInfo.getNewParameters()[1].setValOrVar(JetValVar.None);
        changeInfo.getNewParameters()[2].setValOrVar(JetValVar.Val);
        changeInfo.getNewParameters()[0].setName("_x1");
        changeInfo.getNewParameters()[1].setName("_x2");
        changeInfo.getNewParameters()[2].setName("_x3");
        changeInfo.getNewParameters()[1].setCurrentTypeText("Double?");
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
        changeInfo.getNewParameters()[1].setCurrentTypeText("Float?");
        doTest(changeInfo);
    }

    public void testGenericFunctions() throws Exception {
        JetChangeInfo changeInfo = getChangeInfo();
        changeInfo.setNewVisibility(Visibilities.PUBLIC);
        changeInfo.getNewParameters()[0].setName("_x1");
        changeInfo.getNewParameters()[1].setName("_x2");
        changeInfo.getNewParameters()[2].setName("_x3");
        changeInfo.getNewParameters()[1].setCurrentTypeText("Double?");
        doTest(changeInfo);
    }

    public void testExpressionFunction() throws Exception {
        JetChangeInfo changeInfo = getChangeInfo();
        changeInfo.getNewParameters()[0].setName("x1");
        changeInfo.addParameter(new JetParameterInfo(-1, "y1", KotlinBuiltIns.getInstance().getIntType(), null, "", null, null));
        doTest(changeInfo);
    }

    public void testFunctionsAddRemoveArguments() throws Exception {
        JetChangeInfo changeInfo = getChangeInfo();
        changeInfo.setNewVisibility(Visibilities.INTERNAL);
        changeInfo.setNewParameter(2, changeInfo.getNewParameters()[1]);
        changeInfo.setNewParameter(1, changeInfo.getNewParameters()[0]);
        JetParameterInfo newParameter = new JetParameterInfo(-1, "x0", KotlinBuiltIns.getInstance().getNullableAnyType(), null, "null", null, null);
        changeInfo.setNewParameter(0, newParameter);
        doTest(changeInfo);
    }

    public void testFakeOverride() throws Exception {
        JetChangeInfo changeInfo = getChangeInfo();
        JetParameterInfo newParameter = new JetParameterInfo(-1, "i", KotlinBuiltIns.getInstance().getIntType(), null, "", null, null);
        changeInfo.addParameter(newParameter);
        doTest(changeInfo);
    }

    public void testFunctionLiteral() throws Exception {
        JetChangeInfo changeInfo = getChangeInfo();
        changeInfo.getNewParameters()[1].setName("y1");
        changeInfo.addParameter(new JetParameterInfo(-1, "x", KotlinBuiltIns.getInstance().getAnyType(), null, "", null, null));
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
            BindingContext context = ResolvePackage.analyze(method);

            ChangeSignaturePackage
                    .runChangeSignature(getProject(), ChangeSignaturePackage.getOriginalBaseFunctionDescriptor(changeInfo), empty, context, method, "test");
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

    public void testNoDefaultValuesInOverrides() throws Exception {
        JetChangeInfo changeInfo = getChangeInfo();
        JetParameterInfo[] newParameters = changeInfo.getNewParameters();
        changeInfo.setNewParameter(0, newParameters[1]);
        changeInfo.setNewParameter(1, newParameters[0]);
        doTest(changeInfo);
    }

    public void testOverridesInEnumEntries() throws Exception {
        JetChangeInfo changeInfo = getChangeInfo();
        JetParameterInfo newParameter = new JetParameterInfo(-1, "s", KotlinBuiltIns.getInstance().getStringType(), null, "", null, null);
        changeInfo.addParameter(newParameter);
        doTest(changeInfo);
    }

    public void testEnumEntriesWithoutSuperCalls() throws Exception {
        JetChangeInfo changeInfo = getChangeInfo();
        JetParameterInfo newParameter = new JetParameterInfo(-1, "n", KotlinBuiltIns.getInstance().getIntType(), null, "1", null, null);
        changeInfo.addParameter(newParameter);
        doTest(changeInfo);
    }

    public void testParameterChangeInOverrides() throws Exception {
        JetChangeInfo changeInfo = getChangeInfo();
        JetParameterInfo parameterInfo = changeInfo.getNewParameters()[0];
        parameterInfo.setName("n");
        parameterInfo.setCurrentTypeText("Int");
        doTest(changeInfo);
    }

    public void testConstructorJavaUsages() throws Exception {
        JetChangeInfo changeInfo = getChangeInfo();
        JetParameterInfo newParameter = new JetParameterInfo(-1, "s", KotlinBuiltIns.getInstance().getStringType(), null, "\"abc\"", null, null);
        changeInfo.addParameter(newParameter);
        doTest(changeInfo);
    }

    public void testFunctionJavaUsagesAndOverridesAddParam() throws Exception {
        JetChangeInfo changeInfo = getChangeInfo();
        changeInfo.addParameter(new JetParameterInfo(-1, "s", KotlinBuiltIns.getInstance().getStringType(), null, "\"abc\"", null, null));
        changeInfo.addParameter(new JetParameterInfo(-1, "o", KotlinBuiltIns.getInstance().getNullableAnyType(), null, "\"def\"", null, null));
        doTest(changeInfo);
    }

    public void testFunctionJavaUsagesAndOverridesChangeNullability() throws Exception {
        JetChangeInfo changeInfo = getChangeInfo();

        JetParameterInfo[] newParameters = changeInfo.getNewParameters();
        newParameters[1].setCurrentTypeText("String?");
        newParameters[2].setCurrentTypeText("Any");

        changeInfo.setNewReturnTypeText("String?");

        doTest(changeInfo);
    }

    public void testFunctionJavaUsagesAndOverridesChangeTypes() throws Exception {
        JetChangeInfo changeInfo = getChangeInfo();

        JetParameterInfo[] newParameters = changeInfo.getNewParameters();
        newParameters[0].setCurrentTypeText("String?");
        newParameters[1].setCurrentTypeText("Int");
        newParameters[2].setCurrentTypeText("Long?");

        changeInfo.setNewReturnTypeText("Any?");

        doTest(changeInfo);
    }

    public void testGenericsWithOverrides() throws Exception {
        JetChangeInfo changeInfo = getChangeInfo();

        JetParameterInfo[] newParameters = changeInfo.getNewParameters();
        newParameters[0].setCurrentTypeText("List<C>");
        newParameters[1].setCurrentTypeText("A?");
        newParameters[2].setCurrentTypeText("U<B>");

        changeInfo.setNewReturnTypeText("U<C>?");

        doTest(changeInfo);
    }

    public void testJavaMethodKotlinUsages() throws Exception {
        doJavaTest(
                new JavaRefactoringProvider() {
                    @NotNull
                    @Override
                    String getNewName(@NotNull PsiMethod method) {
                        return "bar";
                    }

                    @NotNull
                    @Override
                    ParameterInfoImpl[] getNewParameters(@NotNull PsiMethod method) {
                        return ArrayUtil.remove(super.getNewParameters(method), 1);
                    }
                }
        );
    }

    public void testJavaConstructorKotlinUsages() throws Exception {
        doJavaTest(
                new JavaRefactoringProvider() {
                    @NotNull
                    @Override
                    ParameterInfoImpl[] getNewParameters(@NotNull PsiMethod method) {
                        return ArrayUtil.remove(super.getNewParameters(method), 1);
                    }
                }
        );
    }

    public void testSAMAddToEmptyParamList() throws Exception {
        doJavaTest(
                new JavaRefactoringProvider() {
                    @NotNull
                    @Override
                    ParameterInfoImpl[] getNewParameters(@NotNull PsiMethod method) {
                        PsiType paramType = PsiType.getJavaLangString(getPsiManager(), GlobalSearchScope.allScope(getProject()));
                        return new ParameterInfoImpl[] {new ParameterInfoImpl(-1, "s", paramType)};
                    }
                }
        );
    }

    public void testSAMAddToSingletonParamList() throws Exception {
        doJavaTest(
                new JavaRefactoringProvider() {
                    @NotNull
                    @Override
                    ParameterInfoImpl[] getNewParameters(@NotNull PsiMethod method) {
                        PsiParameter parameter = method.getParameterList().getParameters()[0];
                        ParameterInfoImpl originalParameter = new ParameterInfoImpl(0, parameter.getName(), parameter.getType());
                        ParameterInfoImpl newParameter = new ParameterInfoImpl(-1, "n", PsiType.INT);

                        return new ParameterInfoImpl[] {newParameter, originalParameter};
                    }
                }
        );
    }

    public void testSAMAddToNonEmptyParamList() throws Exception {
        doJavaTest(
                new JavaRefactoringProvider() {
                    @NotNull
                    @Override
                    ParameterInfoImpl[] getNewParameters(@NotNull PsiMethod method) {
                        ParameterInfoImpl[] originalParameters = super.getNewParameters(method);
                        ParameterInfoImpl[] newParameters = Arrays.copyOf(originalParameters, originalParameters.length + 1);

                        PsiType paramType = PsiType.getJavaLangObject(getPsiManager(), GlobalSearchScope.allScope(getProject()));
                        newParameters[originalParameters.length] = new ParameterInfoImpl(-1, "o", paramType);

                        return newParameters;
                    }
                }
        );
    }

    public void testSAMRemoveSingletonParamList() throws Exception {
        doJavaTest(
                new JavaRefactoringProvider() {
                    @NotNull
                    @Override
                    ParameterInfoImpl[] getNewParameters(@NotNull PsiMethod method) {
                        return new ParameterInfoImpl[0];
                    }
                }
        );
    }

    public void testSAMRemoveParam() throws Exception {
        doJavaTest(
                new JavaRefactoringProvider() {
                    @NotNull
                    @Override
                    ParameterInfoImpl[] getNewParameters(@NotNull PsiMethod method) {
                        return ArrayUtil.remove(super.getNewParameters(method), 0);
                    }
                }
        );
    }

    public void testSAMRenameParam() throws Exception {
        doJavaTest(
                new JavaRefactoringProvider() {
                    @NotNull
                    @Override
                    ParameterInfoImpl[] getNewParameters(@NotNull PsiMethod method) {
                        ParameterInfoImpl[] newParameters = super.getNewParameters(method);
                        newParameters[0].setName("p");
                        return newParameters;
                    }
                }
        );
    }

    public void testSAMChangeParamType() throws Exception {
        doJavaTest(
                new JavaRefactoringProvider() {
                    @NotNull
                    @Override
                    ParameterInfoImpl[] getNewParameters(@NotNull PsiMethod method) {
                        ParameterInfoImpl[] newParameters = super.getNewParameters(method);
                        newParameters[0].setType(PsiType.getJavaLangObject(getPsiManager(), GlobalSearchScope.allScope(getProject())));
                        return newParameters;
                    }
                }
        );
    }

    public void testSAMRenameMethod() throws Exception {
        doJavaTest(
                new JavaRefactoringProvider() {
                    @NotNull
                    @Override
                    String getNewName(@NotNull PsiMethod method) {
                        return "bar";
                    }
                }
        );
    }

    public void testSAMChangeMethodReturnType() throws Exception {
        doJavaTest(
                new JavaRefactoringProvider() {
                    @Nullable
                    @Override
                    PsiType getNewReturnType(@NotNull PsiMethod method) {
                        return PsiType.getJavaLangObject(getPsiManager(), GlobalSearchScope.allScope(getProject()));
                    }
                }
        );
    }

    public void testGenericsWithSAMConstructors() throws Exception {
        doJavaTest(
                new JavaRefactoringProvider() {
                    final PsiElementFactory factory = JavaPsiFacade.getInstance(getProject()).getElementFactory();

                    @NotNull
                    @Override
                    ParameterInfoImpl[] getNewParameters(@NotNull PsiMethod method) {
                        ParameterInfoImpl[] newParameters = super.getNewParameters(method);
                        newParameters[0].setType(factory.createTypeFromText("java.util.List<X<B>>", method.getParameterList()));
                        newParameters[1].setType(factory.createTypeFromText("X<java.util.Set<A>>", method.getParameterList()));
                        return newParameters;
                    }

                    @Nullable
                    @Override
                    PsiType getNewReturnType(@NotNull PsiMethod method) {
                        return factory.createTypeFromText("X<java.util.List<A>>", method);
                    }
                }
        );
    }

    public void testFunctionRenameJavaUsages() throws Exception {
        JetChangeInfo changeInfo = getChangeInfo();
        changeInfo.setNewName("bar");
        doTest(changeInfo);
    }

    public void testParameterModifiers() throws Exception {
        JetChangeInfo changeInfo = getChangeInfo();
        changeInfo.addParameter(new JetParameterInfo(-1, "n", KotlinBuiltIns.getInstance().getIntType(), null, "", null, null));
        doTest(changeInfo);
    }

    public void testFqNameShortening() throws Exception {
        JetChangeInfo changeInfo = getChangeInfo();
        JetParameterInfo parameterInfo = new JetParameterInfo(-1, "s", KotlinBuiltIns.getInstance().getAnyType(), null, "", null, null);
        parameterInfo.setCurrentTypeText("kotlin.String");
        changeInfo.addParameter(parameterInfo);
        doTest(changeInfo);
    }

    public void testObjectMember() throws Exception {
        JetChangeInfo changeInfo = getChangeInfo();
        changeInfo.removeParameter(0);
        doTest(changeInfo);
    }

    public void testParameterListAddParam() throws Exception {
        JetChangeInfo changeInfo = getChangeInfo();
        changeInfo.addParameter(new JetParameterInfo(-1, "l", KotlinBuiltIns.getInstance().getLongType(), null, "", null, null));
        doTest(changeInfo);
    }

    public void testParameterListRemoveParam() throws Exception {
        JetChangeInfo changeInfo = getChangeInfo();
        changeInfo.removeParameter(changeInfo.getNewParametersCount() - 1);
        doTest(changeInfo);
    }

    public void testParameterListRemoveAllParams() throws Exception {
        JetChangeInfo changeInfo = getChangeInfo();
        for (int i = changeInfo.getNewParametersCount() - 1; i >= 0; i--) {
            changeInfo.removeParameter(i);
        }
        doTest(changeInfo);
    }

    @NotNull
    @Override
    protected String getTestDataPath() {
        return new File(PluginTestCaseBase.getTestDataPathBase(), "/refactoring/changeSignature").getPath() + File.separator;
    }

    private final List<Editor> editors = new ArrayList<Editor>();

    private static final String[] EXTENSIONS = {".kt", ".java"};

    private void configureFiles() throws Exception {
        editors.clear();

        indexLoop:
        for (int i = 0; ; i++) {
            for (String extension : EXTENSIONS) {
                String extraFileName = getTestName(false) + "Before" + (i > 0 ? "." + i : "") + extension;
                File extraFile = new File(getTestDataPath() + extraFileName);
                if (extraFile.exists()) {
                    configureByFile(extraFileName);
                    editors.add(getEditor());
                    continue indexLoop;
                }
            }
            break;
        }

        setActiveEditor(editors.get(0));
    }

    private JetChangeInfo getChangeInfo() throws Exception {
        configureFiles();

        Editor editor = getEditor();
        PsiFile file = getFile();
        Project project = getProject();

        JetElement element = (JetElement) new JetChangeSignatureHandler().findTargetMember(file, editor);
        assertNotNull("Target element is null", element);

        BindingContext bindingContext = ResolvePackage.analyze(element);
        PsiElement context = file.findElementAt(editor.getCaretModel().getOffset());
        assertNotNull(context);

        FunctionDescriptor functionDescriptor = JetChangeSignatureHandler.findDescriptor(element, project, editor, bindingContext);
        assertNotNull(functionDescriptor);

        JetChangeSignatureDialog dialog = getChangeSignatureDialog(
                project, functionDescriptor, JetChangeSignatureHandler.getConfiguration(), bindingContext, context);
        assertNotNull(dialog);

        dialog.canRun();
        Disposer.register(getTestRootDisposable(), dialog.getDisposable());
        return dialog.evaluateChangeInfo();
    }

    private class JavaRefactoringProvider {
        @NotNull
        String getNewName(@NotNull PsiMethod method) {
            return method.getName();
        }

        @Nullable
        PsiType getNewReturnType(@NotNull PsiMethod method) {
            return method.getReturnType();
        }

        @NotNull
        ParameterInfoImpl[] getNewParameters(@NotNull PsiMethod method) {
            PsiParameter[] parameters = method.getParameterList().getParameters();
            ParameterInfoImpl[] parameterInfos = new ParameterInfoImpl[parameters.length];
            for (int i = 0; i < parameters.length; i++) {
                PsiParameter parameter = parameters[i];
                parameterInfos[i] = new ParameterInfoImpl(i, parameter.getName(), parameter.getType());
            }
            return parameterInfos;
        }

        @NotNull
        final ChangeSignatureProcessor getProcessor(@NotNull PsiMethod method) {
            return new ChangeSignatureProcessor(
                    getProject(),
                    method,
                    false,
                    VisibilityUtil.getVisibilityModifier(method.getModifierList()),
                    getNewName(method),
                    getNewReturnType(method),
                    getNewParameters(method),
                    new ThrownExceptionInfo[0]);
        }
    }

    private void doJavaTest(JavaRefactoringProvider provider) throws Exception {
        configureFiles();

        PsiElement targetElement = TargetElementUtilBase.findTargetElement(getEditor(), TargetElementUtilBase.ELEMENT_NAME_ACCEPTED);
        assertTrue("<caret> is not on method name", targetElement instanceof PsiMethod);

        provider.getProcessor((PsiMethod)targetElement).run();

        compareEditorsWithExpectedData();
    }

    private void doTest(JetChangeInfo changeInfo) throws Exception {
        new JetChangeSignatureProcessor(getProject(), changeInfo, "Change signature").run();
        compareEditorsWithExpectedData();
    }

    private void compareEditorsWithExpectedData() throws Exception {
        for (Editor editor : editors) {
            setActiveEditor(editor);
            checkResultByFile(getFile().getName().replace("Before.", "After."));
        }
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

    @Override
    protected Sdk getTestProjectJdk() {
        return PluginTestCaseBase.jdkFromIdeaHome();
    }
}
