package org.jetbrains.jet.plugin.parameterInfo;

import com.intellij.lang.parameterInfo.UpdateParameterInfoContext;
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
public class MockUpdateParameterInfoContext implements UpdateParameterInfoContext {
    private int myCurrentParameter = -1;
    private PsiFile myFile;
    private JavaCodeInsightTestFixture myFixture;

    MockUpdateParameterInfoContext(PsiFile file, JavaCodeInsightTestFixture fixture) {
        myFile = file;
        myFixture = fixture;
    }
    

    @Override
    public void removeHint() {
    }

    @Override
    public void setParameterOwner(PsiElement o) {
    }

    @Override
    public PsiElement getParameterOwner() {
        return null;
    }

    @Override
    public void setHighlightedParameter(Object parameter) {
    }

    @Override
    public void setCurrentParameter(int index) {
        myCurrentParameter = index;
    }
    
    public int getCurrentParameter() {
        return myCurrentParameter;
    }

    @Override
    public boolean isUIComponentEnabled(int index) {
        return false;
    }

    @Override
    public void setUIComponentEnabled(int index, boolean b) {
    }

    @Override
    public int getParameterListStart() {
        return 0;
    }

    @Override
    public Object[] getObjectsToView() {
        return ArrayUtil.EMPTY_OBJECT_ARRAY;
    }

    @Override
    public Project getProject() {
        return null;
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
