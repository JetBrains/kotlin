package org.jetbrains.jet;

import com.intellij.openapi.fileEditor.impl.LoadTextUtil;
import com.intellij.openapi.project.Project;
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
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.plugin.JetLanguage;

import java.io.File;
import java.io.IOException;

/**
 * @author abreslav
 */
public abstract class JetLiteFixture extends UsefulTestCase {
    @NonNls
    protected final String myFullDataPath;
    protected JetFile myFile;
    private JetCoreEnvironment myEnvironment;

    public JetLiteFixture(@NonNls String dataPath) {
        myFullDataPath = getTestDataPath() + "/" + dataPath;
    }

    public JetLiteFixture() {
        myFullDataPath = getTestDataPath();
    }

    protected String getTestDataPath() {
        return JetTestCaseBase.getTestDataPathBase();
    }

    public Project getProject() {
        return myEnvironment.getProject();
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        myEnvironment = new JetCoreEnvironment(getTestRootDisposable());
        final File rtJar = new File(JetTestCaseBase.getHomeDirectory(), "compiler/testData/mockJDK-1.7/jre/lib/rt.jar");
        myEnvironment.addToClasspath(rtJar);
        myEnvironment.addToClasspath(new File(JetTestCaseBase.getHomeDirectory(), "compiler/testData/mockJDK-1.7/jre/lib/annotations.jar"));
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

    protected JetFile createPsiFile(String name, String text) {
        return (JetFile) createFile(name + ".jet", text);
    }

    protected JetFile loadPsiFile(String name) {
        try {
            return createPsiFile(name, loadFile(name));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected PsiFile createFile(@NonNls String name, String text) {
        LightVirtualFile virtualFile = new LightVirtualFile(name, JetLanguage.INSTANCE, text);
        virtualFile.setCharset(CharsetToolkit.UTF8_CHARSET);
        return ((PsiFileFactoryImpl) PsiFileFactory.getInstance(myEnvironment.getProject())).trySetupPsiForFile(virtualFile, JetLanguage.INSTANCE, true, false);
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
        String text = loadFile(name + ".jet");
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
