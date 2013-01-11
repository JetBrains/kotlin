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

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.impl.compiled.ClsElementImpl;
import com.intellij.psi.impl.compiled.ClsFileImpl;
import org.jetbrains.jet.lang.psi.JetDeclaration;

import java.util.Map;

public class NavigateToDecompiledLibraryTest extends AbstractNavigateToLibraryTest {
    private VirtualFile classFile;

    public void testAbstractClass() {
        doTest();
    }

    public void testClassWithAbstractAndOpenMembers() {
        doTest();
    }

    public void testColor() {
        doTest();
    }

    public void testLibrariesPackage() {
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

    public void testClassWithConstructor() {
        doTest();
    }

    public void testNamedObject() {
        doTest();
    }

    private void doTest() {
        classFile = getClassFile();

        Map<ClsElementImpl, JetDeclaration> map = getDecompiledData(classFile).getClsElementsToJetElements();
        checkNavigationElements(map);
        String decompiledTextWithMarks = getDecompiledTextWithMarks(map);

        assertSameLinesWithFile(TEST_DATA_PATH + "/decompiled/" + getTestName(false) + ".kt", decompiledTextWithMarks);
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
        Document document = FileDocumentManager.getInstance().getDocument(classFile);
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
        PsiFile classPsiFile = getPsiManager().findFile(classFile);
        for (Map.Entry<ClsElementImpl, JetDeclaration> clsToJet : map.entrySet()) {
            assertSame(classPsiFile, clsToJet.getKey().getContainingFile());
            assertSame(clsToJet.getValue(), clsToJet.getKey().getNavigationElement());
        }
    }

    private VirtualFile getClassFile() {
        VirtualFile packageDir = libraryDir.findFileByRelativePath(PACKAGE.replace(".", "/"));
        assertNotNull(packageDir);
        VirtualFile classFile = packageDir.findChild(getTestName(false) + ".class");
        assertNotNull(classFile);
        return classFile;
    }

    @Override
    protected boolean isWithSources() {
        return false;
    }
}