package org.jetbrains.jet.plugin.parameterInfo;

import com.intellij.lang.parameterInfo.CreateParameterInfoContext;
import com.intellij.lang.parameterInfo.ParameterInfoHandler;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.testFramework.fixtures.JavaCodeInsightTestFixture;
import com.intellij.util.ArrayUtil;
import org.jetbrains.annotations.NotNull;

/**
 * User: Alexander Podkhalyuzin
 * Date: 24.01.12
 */
public class MockCreateParameterInfoContext implements CreateParameterInfoContext {
    private Object[] myItemsToShow = ArrayUtil.EMPTY_OBJECT_ARRAY;
    private PsiElement myHighlightedElement = null;
    private JavaCodeInsightTestFixture myFixture;
    private PsiFile myFile;

    MockCreateParameterInfoContext(PsiFile file, JavaCodeInsightTestFixture fixture) {
        myFile = file;
        myFixture = fixture;
    }

    @Override
    public Object[] getItemsToShow() {
        return myItemsToShow;
    }

    @Override
    public void setItemsToShow(Object[] items) {
        myItemsToShow = items;
    }

    @Override
    public void showHint(PsiElement element, int offset, ParameterInfoHandler handler) {
    }

    @Override
    public int getParameterListStart() {
        return 0;
    }

    @Override
    public PsiElement getHighlightedElement() {
        return myHighlightedElement;
    }

    @Override
    public void setHighlightedElement(PsiElement elements) {
        myHighlightedElement = elements;
    }

    @Override
    public Project getProject() {
        return myFixture.getProject();
    }

    @Override
    public PsiFile getFile() {
        return myFile;
    }

    @Override
    public int getOffset() {
        return myFixture.getCaretOffset();
    }

    @NotNull
    @Override
    public Editor getEditor() {
        return myFixture.getEditor();
    }
}
