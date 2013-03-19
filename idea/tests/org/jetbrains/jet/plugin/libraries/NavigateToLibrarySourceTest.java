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

package org.jetbrains.jet.plugin.libraries;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiReference;
import com.intellij.util.containers.MultiMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.JetTestUtils;
import org.jetbrains.jet.plugin.references.JetPsiReference;

import java.util.*;

/**
 * Attaching library with sources, and trying to navigate to its entities from source code.
 */
public class NavigateToLibrarySourceTest extends AbstractNavigateToLibraryTest {
    private VirtualFile userFile;

    public void testEnum() {
        doTest();
    }

    public void testProperty() {
        doTest();
    }

    public void testGlobalProperty() {
        doTest();
    }

    public void testExtensionProperty() {
        doTest();
    }

    public void testGlobalFunction() {
        doTest();
    }

    public void testClassObject() {
        doTest();
    }

    public void testExtensionFunction() {
        doTest();
    }

    public void testSameNameInDifferentSources() {
        doTest();
    }

    public void testConstructor() {
        doTest();
    }

    public void testNamedObject() {
        doTest();
    }

    public void testTypeWithSameShortName() {
        doTest();
    }

    public void testOverloadedFunWithTypeParam() {
        doTest();
    }

    private void doTest() {
        userFile = copyFileToSrcDir(TEST_DATA_PATH + "/usercode/" + getTestName(false) + ".kt");

        checkAnnotatedLibraryCode(false);
        checkAnnotatedLibraryCode(true);
    }

    @Override
    protected void tearDown() throws Exception {
        JetSourceNavigationHelper.setForceResolve(false);
        super.tearDown();
    }

    private void checkAnnotatedLibraryCode(boolean forceResolve) {
        JetSourceNavigationHelper.setForceResolve(forceResolve);
        String actualCode = getActualAnnotatedLibraryCode();
        String expectedCode = getExpectedAnnotatedLibraryCode();
        assertSameLines(expectedCode, actualCode);
    }

    private Collection<JetPsiReference> collectInterestingReferences() {
        PsiFile psiFile = getPsiManager().findFile(userFile);
        assert psiFile != null;
        Map<PsiElement, JetPsiReference> referenceContainersToReferences = new LinkedHashMap<PsiElement, JetPsiReference>();
        for (int offset = 0; offset < psiFile.getTextLength(); offset++) {
            PsiReference ref = psiFile.findReferenceAt(offset);
            if (ref instanceof JetPsiReference && !referenceContainersToReferences.containsKey(ref.getElement())) {
                PsiElement target = ref.resolve();
                if (target == null) continue;
                PsiFile targetNavPsiFile = target.getNavigationElement().getContainingFile();
                if (targetNavPsiFile == null) continue;
                VirtualFile targetNavFile = targetNavPsiFile.getVirtualFile();
                if (targetNavFile == null) continue;
                if (VfsUtilCore.isAncestor(librarySourceDir, targetNavFile, true)) {
                    referenceContainersToReferences.put(ref.getElement(), (JetPsiReference)ref);
                }
            }
        }
        return referenceContainersToReferences.values();
    }

    @NotNull
    private String getRelativePath(@NotNull PsiFile librarySourceFile) {
        VirtualFile virtualFile = librarySourceFile.getVirtualFile();
        if (virtualFile == null) {
            return "";
        }
        String relativePath = VfsUtilCore.getRelativePath(virtualFile, librarySourceDir, '/');
        return relativePath == null ? "" : relativePath;
    }

    private String getActualAnnotatedLibraryCode() {
        MultiMap<PsiFile, Pair<Integer, Integer>> filesToNumbersAndOffsets = new MultiMap<PsiFile, Pair<Integer, Integer>>();
        int refNumber = 1;
        for (JetPsiReference ref : collectInterestingReferences()) {
            PsiElement target = ref.resolve();
            assertNotNull(target);
            PsiElement navigationElement = target.getNavigationElement();
            Pair<Integer, Integer> numberAndOffset = new Pair<Integer, Integer>(refNumber++, navigationElement.getTextOffset());
            filesToNumbersAndOffsets.putValue(navigationElement.getContainingFile(), numberAndOffset);
        }

        if (filesToNumbersAndOffsets.isEmpty()) {
            return "<no references>";
        }

        List<PsiFile> files = new ArrayList<PsiFile>(filesToNumbersAndOffsets.keySet());
        Collections.sort(files, new Comparator<PsiFile>() {
            @Override
            public int compare(PsiFile o1, PsiFile o2) {
                return getRelativePath(o1).compareTo(getRelativePath(o2));
            }
        });

        StringBuilder result = new StringBuilder();
        for (PsiFile file : files) {
            List<Pair<Integer, Integer>> numbersAndOffsets = new ArrayList<Pair<Integer, Integer>>(filesToNumbersAndOffsets.get(file));

            Collections.sort(numbersAndOffsets, Collections.reverseOrder(new Comparator<Pair<Integer, Integer>>() {
                @Override
                public int compare(Pair<Integer, Integer> t1, Pair<Integer, Integer> t2) {
                    int offsets = t1.second.compareTo(t2.second);
                    return offsets == 0 ? t1.first.compareTo(t2.first) : offsets;
                }
            }));

            Document document = PsiDocumentManager.getInstance(getProject()).getDocument(file);
            assertNotNull(document);
            StringBuilder resultForFile = new StringBuilder(document.getText());
            for (Pair<Integer, Integer> numberOffset : numbersAndOffsets) {
                resultForFile.insert(numberOffset.second, String.format("<%d>", numberOffset.first));
            }

            int minLine = Integer.MAX_VALUE;
            int maxLine = Integer.MIN_VALUE;
            for (Pair<Integer, Integer> numberOffset : numbersAndOffsets) {
                int lineNumber = document.getLineNumber(numberOffset.second);
                minLine = Math.min(minLine, lineNumber);
                maxLine = Math.max(maxLine, lineNumber);
            }

            Document annotated = EditorFactory.getInstance().createDocument(resultForFile);
            String filePart = annotated.getText().substring(annotated.getLineStartOffset(minLine),
                                                             annotated.getLineEndOffset(maxLine));
            result.append(" ").append(getRelativePath(file)).append("\n");
            result.append(filePart).append("\n");
        }
        return result.toString();
    }

    private String getExpectedAnnotatedLibraryCode() {
        Document document = FileDocumentManager.getInstance().getDocument(userFile);
        assertNotNull(document);
        return JetTestUtils.getLastCommentedLines(document);
    }

    @Override
    protected boolean isWithSources() {
        return true;
    }
}
