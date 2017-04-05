/*
 * Copyright 2000-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.test.testFramework;

import com.intellij.core.CoreASTFactory;
import com.intellij.lang.*;
import com.intellij.lang.impl.PsiBuilderFactoryImpl;
import com.intellij.mock.MockFileDocumentManagerImpl;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.extensions.Extensions;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.impl.LoadTextUtil;
import com.intellij.openapi.fileTypes.FileTypeFactory;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.progress.EmptyProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.impl.CoreProgressManager;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.CharsetToolkit;
import com.intellij.pom.PomModel;
import com.intellij.pom.core.impl.PomModelImpl;
import com.intellij.pom.tree.TreeAspect;
import com.intellij.psi.*;
import com.intellij.psi.impl.DebugUtil;
import com.intellij.psi.impl.PsiCachedValuesFactory;
import com.intellij.psi.impl.PsiFileFactoryImpl;
import com.intellij.psi.impl.source.resolve.reference.ReferenceProvidersRegistry;
import com.intellij.psi.impl.source.resolve.reference.ReferenceProvidersRegistryImpl;
import com.intellij.psi.impl.source.text.BlockSupportImpl;
import com.intellij.psi.impl.source.text.DiffLog;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.testFramework.LightVirtualFile;
import com.intellij.testFramework.TestDataFile;
import com.intellij.util.CachedValuesManagerImpl;
import com.intellij.util.Function;
import com.intellij.util.messages.MessageBus;
import com.intellij.util.messages.MessageBusFactory;
import junit.framework.TestCase;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.test.testFramework.mock.*;
import org.picocontainer.ComponentAdapter;
import org.picocontainer.MutablePicoContainer;

import java.io.File;
import java.io.IOException;
import java.util.Set;

@SuppressWarnings("ALL")
public abstract class KtParsingTestCase extends KtPlatformLiteFixture {
    public static final Key<Document> HARD_REF_TO_DOCUMENT_KEY = Key.create("HARD_REF_TO_DOCUMENT_KEY");
    protected String myFilePrefix = "";
    protected String myFileExt;
    protected final String myFullDataPath;
    protected PsiFile myFile;
    private MockPsiManager myPsiManager;
    private PsiFileFactoryImpl myFileFactory;
    protected Language myLanguage;
    private final ParserDefinition[] myDefinitions;
    private final boolean myLowercaseFirstLetter;

    protected KtParsingTestCase(@NonNls @NotNull String dataPath, @NotNull String fileExt, @NotNull ParserDefinition... definitions) {
        this(dataPath, fileExt, false, definitions);
    }

    protected KtParsingTestCase(@NonNls @NotNull String dataPath, @NotNull String fileExt, boolean lowercaseFirstLetter, @NotNull ParserDefinition... definitions) {
        myDefinitions = definitions;
        myFullDataPath = getTestDataPath() + "/" + dataPath;
        myFileExt = fileExt;
        myLowercaseFirstLetter = lowercaseFirstLetter;
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        initApplication();
        ComponentAdapter component = getApplication().getPicoContainer().getComponentAdapter(ProgressManager.class.getName());

        Extensions.registerAreaClass("IDEA_PROJECT", null);
        myProject = new MockProjectEx(getTestRootDisposable());
        myPsiManager = new MockPsiManager(myProject);
        myFileFactory = new PsiFileFactoryImpl(myPsiManager);
        MutablePicoContainer appContainer = getApplication().getPicoContainer();
        registerComponentInstance(appContainer, MessageBus.class, MessageBusFactory.newMessageBus(getApplication()));
        final MockEditorFactory editorFactory = new MockEditorFactory();
        registerComponentInstance(appContainer, EditorFactory.class, editorFactory);
        registerComponentInstance(appContainer, FileDocumentManager.class, new MockFileDocumentManagerImpl(new Function<CharSequence, Document>() {
            @Override
            public Document fun(CharSequence charSequence) {
                return editorFactory.createDocument(charSequence);
            }
        }, HARD_REF_TO_DOCUMENT_KEY));
        registerComponentInstance(appContainer, PsiDocumentManager.class, new MockPsiDocumentManager());
        registerApplicationService(PsiBuilderFactory.class, new PsiBuilderFactoryImpl());
        registerApplicationService(DefaultASTFactory.class, new CoreASTFactory());
        registerApplicationService(ReferenceProvidersRegistry.class, new ReferenceProvidersRegistryImpl());

        registerApplicationService(ProgressManager.class, new CoreProgressManager());

        myProject.registerService(CachedValuesManager.class, new CachedValuesManagerImpl(myProject, new PsiCachedValuesFactory(myPsiManager)));
        myProject.registerService(PsiManager.class, myPsiManager);

        this.registerExtensionPoint(FileTypeFactory.FILE_TYPE_FACTORY_EP, FileTypeFactory.class);

        for (ParserDefinition definition : myDefinitions) {
            addExplicitExtension(LanguageParserDefinitions.INSTANCE, definition.getFileNodeType().getLanguage(), definition);
        }
        if (myDefinitions.length > 0) {
            configureFromParserDefinition(myDefinitions[0], myFileExt);
        }

        // That's for reparse routines
        final PomModelImpl pomModel = new PomModelImpl(myProject);
        myProject.registerService(PomModel.class, pomModel);
        new TreeAspect(pomModel);
    }

    public void configureFromParserDefinition(ParserDefinition definition, String extension) {
        myLanguage = definition.getFileNodeType().getLanguage();
        myFileExt = extension;
        addExplicitExtension(LanguageParserDefinitions.INSTANCE, this.myLanguage, definition);
        registerComponentInstance(
                getApplication().getPicoContainer(), FileTypeManager.class,
                new KtMockFileTypeManager(new KtMockLanguageFileType(myLanguage, myFileExt)));
    }

    protected <T> void addExplicitExtension(final LanguageExtension<T> instance, final Language language, final T object) {
        instance.addExplicitExtension(language, object);
        Disposer.register(myProject, new Disposable() {
            @Override
            public void dispose() {
                instance.removeExplicitExtension(language, object);
            }
        });
    }

    @Override
    protected <T> void registerExtensionPoint(final ExtensionPointName<T> extensionPointName, Class<T> aClass) {
        super.registerExtensionPoint(extensionPointName, aClass);
        Disposer.register(myProject, new Disposable() {
            @Override
            public void dispose() {
                Extensions.getRootArea().unregisterExtensionPoint(extensionPointName.getName());
            }
        });
    }

    protected <T> void registerApplicationService(final Class<T> aClass, T object) {
        getApplication().registerService(aClass, object);
        Disposer.register(myProject, new Disposable() {
            @Override
            public void dispose() {
                getApplication().getPicoContainer().unregisterComponent(aClass.getName());
            }
        });
    }

    public MockProjectEx getProject() {
        return myProject;
    }

    public MockPsiManager getPsiManager() {
        return myPsiManager;
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        myFile = null;
        myProject = null;
        myPsiManager = null;
    }

    protected String getTestDataPath() {
        return PathManager.getHomePath();
    }

    @NotNull
    public final String getTestName() {
        return getTestName(myLowercaseFirstLetter);
    }

    protected boolean includeRanges() {
        return false;
    }

    protected boolean skipSpaces() {
        return false;
    }

    protected boolean checkAllPsiRoots() {
        return true;
    }

    protected void doTest(boolean checkResult) {
        String name = getTestName();
        try {
            String text = loadFile(name + "." + myFileExt);
            myFile = createPsiFile(name, text);
            ensureParsed(myFile);
            assertEquals("light virtual file text mismatch", text, ((LightVirtualFile)myFile.getVirtualFile()).getContent().toString());
            assertEquals("virtual file text mismatch", text, LoadTextUtil.loadText(myFile.getVirtualFile()));
            assertEquals("doc text mismatch", text, myFile.getViewProvider().getDocument().getText());
            assertEquals("psi text mismatch", text, myFile.getText());
            ensureCorrectReparse(myFile);
            if (checkResult){
                checkResult(name, myFile);
            }
            else{
                toParseTreeText(myFile, skipSpaces(), includeRanges());
            }
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected void doTest(String suffix) throws IOException {
        String name = getTestName();
        String text = loadFile(name + "." + myFileExt);
        myFile = createPsiFile(name, text);
        ensureParsed(myFile);
        assertEquals(text, myFile.getText());
        checkResult(name + suffix, myFile);
    }

    protected void doCodeTest(String code) throws IOException {
        String name = getTestName();
        myFile = createPsiFile("a", code);
        ensureParsed(myFile);
        assertEquals(code, myFile.getText());
        checkResult(myFilePrefix + name, myFile);
    }

    protected PsiFile createPsiFile(String name, String text) {
        return createFile(name + "." + myFileExt, text);
    }

    protected PsiFile createFile(@NonNls String name, String text) {
        LightVirtualFile virtualFile = new LightVirtualFile(name, myLanguage, text);
        virtualFile.setCharset(CharsetToolkit.UTF8_CHARSET);
        return createFile(virtualFile);
    }

    protected PsiFile createFile(LightVirtualFile virtualFile) {
        return myFileFactory.trySetupPsiForFile(virtualFile, myLanguage, true, false);
    }

    protected void checkResult(@NonNls @TestDataFile String targetDataName, final PsiFile file) throws IOException {
        doCheckResult(myFullDataPath, file, checkAllPsiRoots(), targetDataName, skipSpaces(), includeRanges());
    }

    public static void doCheckResult(String testDataDir,
            PsiFile file,
            boolean checkAllPsiRoots,
            String targetDataName,
            boolean skipSpaces,
            boolean printRanges) throws IOException {
        FileViewProvider provider = file.getViewProvider();
        Set<Language> languages = provider.getLanguages();

        if (!checkAllPsiRoots || languages.size() == 1) {
            doCheckResult(testDataDir, targetDataName + ".txt", toParseTreeText(file, skipSpaces, printRanges).trim());
            return;
        }

        for (Language language : languages) {
            PsiFile root = provider.getPsi(language);
            String expectedName = targetDataName + "." + language.getID() + ".txt";
            doCheckResult(testDataDir, expectedName, toParseTreeText(root, skipSpaces, printRanges).trim());
        }
    }

    protected void checkResult(String actual) throws IOException {
        String name = getTestName();
        doCheckResult(myFullDataPath, myFilePrefix + name + ".txt", actual);
    }

    protected void checkResult(@TestDataFile @NonNls String targetDataName, String actual) throws IOException {
        doCheckResult(myFullDataPath, targetDataName, actual);
    }

    public static void doCheckResult(String fullPath, String targetDataName, String actual) throws IOException {
        String expectedFileName = fullPath + File.separatorChar + targetDataName;
        KtUsefulTestCase.assertSameLinesWithFile(expectedFileName, actual);
    }

    protected static String toParseTreeText(PsiElement file,  boolean skipSpaces, boolean printRanges) {
        return DebugUtil.psiToString(file, skipSpaces, printRanges);
    }

    protected String loadFile(@NonNls @TestDataFile String name) throws IOException {
        return loadFileDefault(myFullDataPath, name);
    }

    public static String loadFileDefault(String dir, String name) throws IOException {
        return FileUtil.loadFile(new File(dir, name), CharsetToolkit.UTF8, true).trim();
    }

    public static void ensureParsed(PsiFile file) {
        file.accept(new PsiElementVisitor() {
            @Override
            public void visitElement(PsiElement element) {
                element.acceptChildren(this);
            }
        });
    }

    public static void ensureCorrectReparse(@NotNull PsiFile file) {
        String psiToStringDefault = DebugUtil.psiToString(file, false, false);
        String fileText = file.getText();
        DiffLog diffLog = (new BlockSupportImpl(file.getProject())).reparseRange(
                file, file.getNode(), TextRange.allOf(fileText), fileText, new EmptyProgressIndicator(), fileText);
        diffLog.performActualPsiChange(file);

        TestCase.assertEquals(psiToStringDefault, DebugUtil.psiToString(file, false, false));
    }
}