package org.jetbrains.jet;

import com.intellij.core.CoreEnvironment;
import com.intellij.lang.Language;
import com.intellij.openapi.fileEditor.impl.LoadTextUtil;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.CharsetToolkit;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileFactory;
import com.intellij.psi.impl.PsiFileFactoryImpl;
import com.intellij.testFramework.LightVirtualFile;
import com.intellij.testFramework.TestDataFile;
import com.intellij.testFramework.UsefulTestCase;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.jet.lang.parsing.JetParserDefinition;
import org.jetbrains.jet.plugin.JetFileType;
import org.jetbrains.jet.plugin.JetLanguage;

import java.io.File;
import java.io.IOException;

/**
 * @author abreslav
 */
public abstract class JetLiteFixture extends UsefulTestCase {
    protected String myFileExt;
    @NonNls
    protected final String myFullDataPath;
    protected PsiFile myFile;
    protected Language myLanguage;
    private CoreEnvironment myEnvironment;

    public JetLiteFixture(@NonNls String dataPath) {
        myFileExt = "jet";
        myFullDataPath = getTestDataPath() + "/" + dataPath;
    }

    protected String getTestDataPath() {
        return JetTestCaseBase.getTestDataPathBase();
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        myEnvironment = new CoreEnvironment(getTestRootDisposable());
        myEnvironment.registerFileType(JetFileType.INSTANCE, ".jet");
        myLanguage = JetLanguage.INSTANCE;
        myEnvironment.registerParserDefinition(new JetParserDefinition());
    }

    @Override
    protected void tearDown() throws Exception {
        myFile = null;
        myEnvironment = null;
        super.tearDown();
    }

    protected String loadFile(@NonNls @TestDataFile String name) throws IOException {
        return doLoadFile(myFullDataPath, name);
    }

    protected static String doLoadFile(String myFullDataPath, String name) throws IOException {
        String fullName = myFullDataPath + File.separatorChar + name;
        String text = FileUtil.loadFile(new File(fullName), CharsetToolkit.UTF8).trim();
        text = StringUtil.convertLineSeparators(text);
        return text;
    }

    protected PsiFile createPsiFile(String name, String text) {
        return createFile(name + "." + myFileExt, text);
    }

    protected PsiFile createFile(@NonNls String name, String text) {
        LightVirtualFile virtualFile = new LightVirtualFile(name, myLanguage, text);
        virtualFile.setCharset(CharsetToolkit.UTF8_CHARSET);
        PsiFileFactoryImpl psiFileFactory = (PsiFileFactoryImpl) PsiFileFactory.getInstance(myEnvironment.getProject());
        return psiFileFactory.trySetupPsiForFile(virtualFile, myLanguage, true, false);
    }

    protected static void ensureParsed(PsiFile file) {
        file.accept(new PsiElementVisitor() {
            @Override
            public void visitElement(PsiElement element) {
                element.acceptChildren(this);
            }
        });
    }

    protected void prepareForTest(String name) throws IOException {
        String text = loadFile(name + "." + myFileExt);
        createAndCheckPsiFile(name, text);
    }

    protected void createAndCheckPsiFile(String name, String text) {
        myFile = createPsiFile(name, text);
        ensureParsed(myFile);
        assertEquals("light virtual file text mismatch", text, ((LightVirtualFile) myFile.getVirtualFile()).getContent().toString());
        assertEquals("virtual file text mismatch", text, LoadTextUtil.loadText(myFile.getVirtualFile()));
        assertEquals("doc text mismatch", text, myFile.getViewProvider().getDocument().getText());
        assertEquals("psi text mismatch", text, myFile.getText());
    }
}
