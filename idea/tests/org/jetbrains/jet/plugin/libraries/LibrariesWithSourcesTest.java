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
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiReference;
import jet.Tuple2;
import org.jetbrains.jet.plugin.references.JetPsiReference;

import java.util.*;

/**
 * @author Evgeny Gerashchenko
 * @since 3/23/12
 */
public class LibrariesWithSourcesTest extends AbstractLibrariesTest {

    private VirtualFile myLibrarySourceFile;

    public void test() {
        myLibrarySourceFile = LocalFileSystem.getInstance().findFileByPath(TEST_DATA_PATH + "/library/library.kt");
        assertNotNull(myLibrarySourceFile);

        String annotatedLibrary = getAnnotatedLibrarySourceText();
        assertSameLinesWithFile(TEST_DATA_PATH + "/usercode/library.marked.kt", annotatedLibrary);
    }

    private Collection<JetPsiReference> collectInterestingReferences() {
        VirtualFile userFile = LocalFileSystem.getInstance().findFileByPath(TEST_DATA_PATH + "/usercode/usercode.kt");
        assert userFile != null;

        PsiFile psiFile = getPsiManager().findFile(userFile);
        assert psiFile != null;
        Map<PsiElement, JetPsiReference> referenceContainersToReferences = new LinkedHashMap<PsiElement, JetPsiReference>();
        for (int offset = 0; offset < psiFile.getTextLength(); offset++) {
            PsiReference ref = psiFile.findReferenceAt(offset);
            if (ref instanceof JetPsiReference && !referenceContainersToReferences.containsKey(ref.getElement())) {
                PsiElement target = ref.resolve();
                if (target == null) {
                    continue;
                }
                PsiFile targetNavPsiFile = target.getNavigationElement().getContainingFile();
                if (targetNavPsiFile == null) {
                    continue;
                }
                if (myLibrarySourceFile.equals(targetNavPsiFile.getVirtualFile())) {
                    referenceContainersToReferences.put(ref.getElement(), (JetPsiReference)ref);
                }
            }
        }
        return referenceContainersToReferences.values();
    }

    private String getAnnotatedLibrarySourceText() {
        List<Tuple2<Integer, Integer>> numbersAndOffsets = new ArrayList<Tuple2<Integer, Integer>>();
        for (JetPsiReference ref : collectInterestingReferences()) {
            PsiElement target = ref.resolve();
            assertNotNull(target);
            PsiElement navigationElement = target.getNavigationElement();
            numbersAndOffsets.add(new Tuple2<Integer, Integer>(numbersAndOffsets.size() + 1, navigationElement.getTextOffset()));
        }

        Collections.sort(numbersAndOffsets, Collections.reverseOrder(new Comparator<Tuple2<Integer, Integer>>() {
            @Override
            public int compare(Tuple2<Integer, Integer> t1, Tuple2<Integer, Integer> t2) {
                int offsets = t1._2.compareTo(t2._2);
                return offsets == 0 ? t1._1.compareTo(t2._1) : offsets;
            }
        }));

        Document document = FileDocumentManager.getInstance().getDocument(myLibrarySourceFile);
        assertNotNull(document);
        StringBuilder result = new StringBuilder(document.getText());
        for (Tuple2<Integer, Integer> numberOffset : numbersAndOffsets) {
            result.insert(numberOffset._2, String.format("<%d>", numberOffset._1));
        }

        return result.toString();
    }

    @Override
    protected boolean isWithSources() {
        return true;
    }
}
