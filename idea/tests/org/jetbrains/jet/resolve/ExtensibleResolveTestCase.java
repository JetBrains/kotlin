package org.jetbrains.jet.resolve;

import com.intellij.codeHighlighting.Pass;
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer;
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzerSettings;
import com.intellij.codeInsight.daemon.impl.DaemonCodeAnalyzerImpl;
import com.intellij.codeInsight.daemon.impl.HighlightInfo;
import com.intellij.injected.editor.EditorWindow;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.vfs.VirtualFileFilter;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.impl.source.tree.injected.InjectedLanguageUtil;
import com.intellij.testFramework.FileTreeAccessFilter;
import com.intellij.testFramework.LightCodeInsightTestCase;
import com.intellij.testFramework.fixtures.impl.CodeInsightTestFixtureImpl;
import com.intellij.util.ArrayUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.JetFile;

import java.util.Collection;
import java.util.List;

/**
 * @author abreslav
 */
public abstract class ExtensibleResolveTestCase extends LightCodeInsightTestCase {
    private final FileTreeAccessFilter myJavaFilesFilter = new FileTreeAccessFilter();
    private ExpectedResolveData expectedResolveData;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        expectedResolveData = getExpectedResolveData();
        ((DaemonCodeAnalyzerImpl) DaemonCodeAnalyzer.getInstance(getProject())).prepareForTest();
        DaemonCodeAnalyzerSettings.getInstance().setImportHintEnabled(false);
    }

    protected abstract ExpectedResolveData getExpectedResolveData();

    @Override
    protected void tearDown() throws Exception {
        ((DaemonCodeAnalyzerImpl) DaemonCodeAnalyzer.getInstance(getProject())).cleanupAfterTest(false); // has to cleanup by hand since light project does not get disposed any time soon
        super.tearDown();
    }

    @Override
    protected void runTest() throws Throwable {
        final Throwable[] throwable = {null};
        CommandProcessor.getInstance().executeCommand(getProject(), new Runnable() {
            @Override
            public void run() {
                try {
                    doRunTest();
                } catch (Throwable t) {
                    throwable[0] = t;
                }
            }
        }, "", null);
        if (throwable[0] != null) {
            throw throwable[0];
        }
    }

    protected void doTest(@NonNls String filePath, boolean checkWarnings, boolean checkInfos) throws Exception {
        configureByFile(filePath);
        doTestConfiguredFile(checkWarnings, checkInfos);
    }

    protected void doTestConfiguredFile(boolean checkWarnings, boolean checkInfos) {
        getJavaFacade().setAssertOnFileLoadingFilter(VirtualFileFilter.NONE);

//        ExpectedHighlightingData expectedData = new ExpectedHighlightingData(getEditor().getDocument(), checkWarnings, checkInfos);
        expectedResolveData.extractData(getEditor().getDocument());

        PsiDocumentManager.getInstance(getProject()).commitAllDocuments();
        getFile().getText(); //to load text
        myJavaFilesFilter.allowTreeAccessForFile(getVFile());
        getJavaFacade().setAssertOnFileLoadingFilter(myJavaFilesFilter); // check repository work

        Collection<HighlightInfo> infos = doHighlighting();

        getJavaFacade().setAssertOnFileLoadingFilter(VirtualFileFilter.NONE);

        expectedResolveData.checkResult((JetFile) getFile());
    }

    @NotNull
    protected List<HighlightInfo> doHighlighting() {
        PsiDocumentManager.getInstance(getProject()).commitAllDocuments();

        int[] toIgnore = doFolding() ? ArrayUtil.EMPTY_INT_ARRAY : new int[]{Pass.UPDATE_FOLDING};
        Editor editor = getEditor();
        PsiFile file = getFile();
        if (editor instanceof EditorWindow) {
            editor = ((EditorWindow) editor).getDelegate();
            file = InjectedLanguageUtil.getTopLevelFile(file);
        }

        return CodeInsightTestFixtureImpl.instantiateAndRun(file, editor, toIgnore, false);
    }

    protected boolean doFolding() {
        return false;
    }
}
