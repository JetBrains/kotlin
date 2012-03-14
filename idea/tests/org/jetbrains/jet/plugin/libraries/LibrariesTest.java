/*
 * Copyright 2000-2012 JetBrains s.r.o.
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

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.impl.compiled.ClsElementImpl;
import com.intellij.psi.impl.compiled.ClsFileImpl;
import com.intellij.testFramework.PlatformTestCase;
import org.jetbrains.jet.cli.KotlinCompiler;
import org.jetbrains.jet.lang.psi.JetDeclaration;
import org.jetbrains.jet.plugin.PluginTestCaseBase;

import java.util.Map;

/**
 * @author Evgeny Gerashchenko
 * @since 3/11/12
 */
public class LibrariesTest extends PlatformTestCase {

    private static final String PACKAGE = "testData.libraries";
    private static final String TEST_DATA_PATH = PluginTestCaseBase.getTestDataPathBase() + "/libraries";
    private VirtualFile myLibraryDir;
    private VirtualFile myClassFile;

    public void testAbstractClass() {
        doTest();
    }

    public void testClassWithAbstractAndOpenMembers() {
        doTest();
    }

    public void testColor() {
        doTest();
    }

    public void testnamespace() {
        doTest();
    }

    public void testSimpleClass() {
        doTest();
    }

    public void testSimpleTrait() {
        doTest();
    }

    public void testSimpleTraitImpl() {
        doTest();
    }

    public void testWithInnerAndObject() {
        doTest();
    }

    public void testWithTraitClassObject() {
        doTest();
    }

    private void doTest() {
        myClassFile = getClassFile();

        Map<ClsElementImpl, JetDeclaration> map = getDecompiledData(myClassFile).getClsElementsToJetElements();
        checkNavigationElements(map);
        String decompiledTextWithMarks = getDecompiledTextWithMarks(map);

        assertSameLinesWithFile(TEST_DATA_PATH + "/" + getTestName(false) + ".kt", decompiledTextWithMarks);
    }

    private String getDecompiledTextWithMarks(Map<ClsElementImpl, JetDeclaration> map) {
        String decompiledText = getDecompiledText();

        int[] openings = new int[decompiledText.length() + 1];
        int[] closings = new int[decompiledText.length() + 1];
        for (JetDeclaration jetDeclaration : map.values()) {
            TextRange textRange = jetDeclaration.getTextRange();
            openings[textRange.getStartOffset()]++;
            closings[textRange.getEndOffset()]++;
        }

        StringBuilder result = new StringBuilder();
        for (int i = 0; i <= decompiledText.length(); i++) {
            result.append(StringUtil.repeat("]", closings[i]));
            result.append(StringUtil.repeat("[", openings[i]));
            if (i < decompiledText.length()) {
                result.append(decompiledText.charAt(i));
            }
        }
        return result.toString();
    }

    private String getDecompiledText() {
        Document document = FileDocumentManager.getInstance().getDocument(myClassFile);
        assertNotNull(document);
        return document.getText();
    }

    private JetDecompiledData getDecompiledData(VirtualFile classFile) {
        PsiFile classPsiFile = getPsiManager().findFile(classFile);
        assertInstanceOf(classPsiFile, ClsFileImpl.class);
        ClsFileImpl clsFile = (ClsFileImpl) classPsiFile;
        return JetDecompiledData.getDecompiledData(clsFile);
    }

    private void checkNavigationElements(Map<ClsElementImpl, JetDeclaration> map) {
        PsiFile classPsiFile = getPsiManager().findFile(myClassFile);
        for (Map.Entry<ClsElementImpl, JetDeclaration> clsToJet : map.entrySet()) {
            assertSame(classPsiFile, clsToJet.getKey().getContainingFile());
            assertSame(clsToJet.getValue(), clsToJet.getKey().getNavigationElement());
        }
    }

    private VirtualFile getClassFile() {
        VirtualFile packageDir = myLibraryDir.findFileByRelativePath(PACKAGE.replace(".", "/"));
        assertNotNull(packageDir);
        VirtualFile classFile = packageDir.findChild(getTestName(false) + ".class");
        assertNotNull(classFile);
        return classFile;
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        String libraryDir = FileUtil.getTempDirectory();
        int compilerExec = new KotlinCompiler().exec("-src", TEST_DATA_PATH + "/library", "-output", libraryDir);
        assertEquals(0, compilerExec);
        myLibraryDir = LocalFileSystem.getInstance().findFileByPath(libraryDir);
        assertNotNull(myLibraryDir);

        ApplicationManager.getApplication().runWriteAction(new Runnable() {
            @Override
            public void run() {
                ModifiableRootModel moduleModel = ModuleRootManager.getInstance(myModule).getModifiableModel();

                Library.ModifiableModel libraryModel = moduleModel.getModuleLibraryTable().getModifiableModel().createLibrary("myKotlinLib").getModifiableModel();
                libraryModel.addRoot(myLibraryDir, OrderRootType.CLASSES);
                libraryModel.commit();

                moduleModel.commit();
            }
        });
    }
}
