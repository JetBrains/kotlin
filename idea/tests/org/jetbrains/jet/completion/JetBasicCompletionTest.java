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

package org.jetbrains.jet.completion;

import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.roots.ui.configuration.libraryEditor.NewLibraryEditor;
import com.intellij.openapi.vfs.VfsUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.plugin.PluginTestCaseBase;
import org.jetbrains.jet.testing.ConfigLibraryUtil;

import java.io.File;

public class JetBasicCompletionTest extends JetCompletionTestBase {
    public void testAutoCastAfterIf() {
        doTest();
    }

    public void testAutoCastAfterIfMethod() {
        doTest();
    }

    public void testAutoCastForThis() {
        doTest();
    }

    public void testAutoCastInWhen() {
        doTest();
    }

    public void testBasicAny() {
        doTest();
    }

    public void testBasicInt() {
        doTest();
    }

    public void testBeforeDotInCall() {
        doTest();
    }

    public void testCallLocalLambda() {
        doTest();
    }

    public void testDoNotCompleteForErrorReceivers() {
        doTest();
    }

    public void testExtendClassName() {
        doTest();
    }

    public void testExtendQualifiedClassName() {
        doTest();
    }

    public void testExtensionFromStandardLibrary() {
        doTest();
    }

    public void testExtensionFunReceiver() {
        doTest();
    }

    public void testExtensionWithAdditionalTypeParameters() {
        doTest();
    }

    public void testExtensionWithGenericParamInReceiver() {
        doTest();
    }

    public void testExtensionWithGenericReceiver() {
        doTest();
    }

    public void testExtensionWithInternalGenericParameters() {
        doTest();
    }

    public void testExtensionWithManyTypeParamsInReceiver() {
        doTest();
    }

    public void testExtensionForProperty() {
        doTest();
    }

    public void testFromImports() {
        doTest();
    }

    public void testFunctionCompletionFormatting() {
        doTest();
    }

    public void testInCallExpression() {
        doTest();
    }

    public void testInClassInitializer() {
        doTest();
    }

    public void testInClassPropertyAccessor() {
        doTest();
    }

    public void testInEmptyImport() {
        doTest();
    }

    public void testInFileWithMultiDeclaration() {
        doTest();
    }

    public void testInFileWithTypedef() {
        doTest();
    }

    public void testInFunInClassInitializer() {
        doTest();
    }

    public void testInGlobalPropertyInitializer() {
        doTest();
    }

    public void testInImport() {
        doTest();
    }

    public void testInInitializerInPropertyAccessor() {
        doTest();
    }

    public void testInLocalObjectDeclaration() {
        doTest();
    }

    public void testInLongDotQualifiedExpression() {
        doTest();
    }

    public void testInMiddleOfNamespace() {
        doTest();
    }

    public void testInMiddleOfPackage() {
        doTest();
    }

    public void testInObjectInDelegationSpecifier() {
        doTest();
    }

    public void testInPackage() {
        doTest();
    }

    public void testInPackageBegin() {
        doTest();
    }

    public void testInTypeAnnotation() {
        doTest();
    }

    public void testJavaClassNames() {
        doTest();
    }

    public void testJavaPackage() {
        doTest();
    }

    public void testLocalMultideclarationValues() {
        doTest();
    }

    public void testNamedObject() {
        doTest();
    }

    public void testNoClassNameDuplication() {
        doTest();
    }

    public void testNoClassNameDuplicationForRuntimeClass() {
        doTest();
    }

    public void testNoEmptyNamespace() {
        doTest();
    }

    public void testNoImportedJavaClassDuplication() {
        doTest();
    }

    public void testNoObjectInTypePosition() {
        doTest();
    }

    public void testNoTopLevelCompletionInQualifiedUserTypes() {
        doTest();
    }

    public void testOnlyScopedClassesWithoutExplicit() {
        doTest();
    }

    public void testOverloadFunctions() {
        doTest();
    }

    public void testStandardJetArrayFirst() {
        doTest();
    }

    public void testStandardJetDoubleFirst() {
        doTest();
    }

    public void testSubpackageInFun() {
        doTest();
    }

    public void testTopLevelFromStandardLibrary() {
        doTest();
    }

    public void testTopLevelFromStandardLibraryWithoutParam() {
        doTest();
    }

    public void testVariableClassName() {
        doTest();
    }

    public void testVisibilityClassMembersFromExternal() {
        doTest();
    }

    public void testVisibilityClassMembersFromExternalForce() {
        doTest();
    }

    public void testVisibilityInSubclass() {
        doTest();
    }

    public void testVisibilityInSubclassForce() {
        doTest();
    }

    public void testCompletionInSetter() {
        doTest();
    }

    public void testClassRedeclaration1() {
        doTest();
    }

    public void testClassRedeclaration2() {
        doTest();
    }

    public void testObjectRedeclaration1() {
        doTest();
    }

    public void testObjectRedeclaration2() {
        doTest();
    }

    public void testTopLevelNonImportedFun() {
        doTestWithJar();
    }

    public void testTopLevelNonImportedExtFun() {
        doTestWithJar();
    }

    public void doTestWithJar() {
        NewLibraryEditor editor = new NewLibraryEditor(null, null);
        editor.setName("doTestWithJarLib");
        editor.addRoot(VfsUtil.getUrlForLibraryRoot(new File(getTestDataPath() + "/" + getTestName(false) + ".jar")), OrderRootType.CLASSES);

        try {
            ConfigLibraryUtil.configureLibrary(getModule(), getFullJavaJDK(), editor);
            doTest();
        }
        finally {
            ConfigLibraryUtil.unConfigureLibrary(getModule(), getFullJavaJDK(), editor.getName());
        }
    }

    @NotNull
    @Override
    protected String getTestDataPath() {
        return new File(PluginTestCaseBase.getTestDataPathBase(), "/completion/basic").getPath() +
               File.separator;
    }
}
