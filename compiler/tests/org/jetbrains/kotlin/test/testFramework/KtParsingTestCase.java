/*
 * Copyright 2010-2016 JetBrains s.r.o.
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
import com.intellij.mock.*;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.extensions.Extensions;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.impl.LoadTextUtil;
import com.intellij.openapi.fileTypes.FileTypeFactory;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.options.SchemesManagerFactory;
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
import com.intellij.util.CachedValuesManagerImpl;
import com.intellij.util.Function;
import com.intellij.util.messages.MessageBus;
import com.intellij.util.messages.MessageBusFactory;
import junit.framework.TestCase;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.test.testFramework.mock.*;
import org.picocontainer.MutablePicoContainer;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.Set;

@SuppressWarnings("ALL")
public abstract class KtParsingTestCase extends KtPlatformLiteFixture {
    public static final Key<Document> HARD_REF_TO_DOCUMENT_KEY = Key.create("HARD_REF_TO_DOCUMENT_KEY");

    protected String myFilePrefix;
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
        this.myFilePrefix = "";
        this.myDefinitions = definitions;
        this.myFullDataPath = this.getTestDataPath() + "/" + dataPath;
        this.myFileExt = fileExt;
        this.myLowercaseFirstLetter = lowercaseFirstLetter;
    }

    protected void setUp() throws Exception {
        super.setUp();
        this.initApplication();
        //ComponentAdapter component = getApplication().getPicoContainer().getComponentAdapter(ProgressManager.class.getName());
        //if(component == null) {
        //    getApplication().getPicoContainer().registerComponent(new AbstractComponentAdapter(ProgressManager.class.getName(), Object.class) {
        //        public Object getComponentInstance(PicoContainer container) throws PicoInitializationException, PicoIntrospectionException {
        //            return new ProgressManagerImpl();
        //        }
        //
        //        public void verify(PicoContainer container) throws PicoIntrospectionException {
        //        }
        //    });
        //}

        Extensions.registerAreaClass("IDEA_PROJECT", (String)null);
        this.myProject = new MockProjectEx(this.getTestRootDisposable());
        this.myPsiManager = new MockPsiManager(this.myProject);
        this.myFileFactory = new PsiFileFactoryImpl(this.myPsiManager);
        MutablePicoContainer appContainer = getApplication().getPicoContainer();
        registerComponentInstance(appContainer, MessageBus.class, MessageBusFactory.newMessageBus(getApplication()));
        registerComponentInstance(appContainer, SchemesManagerFactory.class, new MockSchemesManagerFactory());
        final MockEditorFactory editorFactory = new MockEditorFactory();
        registerComponentInstance(appContainer, EditorFactory.class, editorFactory);
        registerComponentInstance(appContainer, FileDocumentManager.class, new MockFileDocumentManagerImpl(new Function<CharSequence, Document>() {
            public Document fun(CharSequence charSequence) {
                return editorFactory.createDocument(charSequence);
            }
        }, HARD_REF_TO_DOCUMENT_KEY));
        registerComponentInstance(appContainer, PsiDocumentManager.class, new MockPsiDocumentManager());
        this.registerApplicationService(PsiBuilderFactory.class, new PsiBuilderFactoryImpl());
        this.registerApplicationService(DefaultASTFactory.class, new CoreASTFactory());
        this.registerApplicationService(ReferenceProvidersRegistry.class, new ReferenceProvidersRegistryImpl());

        registerApplicationService(ProgressManager.class, new CoreProgressManager());

        this.myProject.registerService(CachedValuesManager.class, new CachedValuesManagerImpl(this.myProject, new PsiCachedValuesFactory(this.myPsiManager)));
        this.myProject.registerService(PsiManager.class, this.myPsiManager);
        //this.myProject.registerService(StartupManager.class, new StartupManagerImpl(this.myProject));
        this.registerExtensionPoint(FileTypeFactory.FILE_TYPE_FACTORY_EP, FileTypeFactory.class);
        ParserDefinition[] pomModel = this.myDefinitions;
        int var5 = pomModel.length;

        for(int var6 = 0; var6 < var5; ++var6) {
            ParserDefinition definition = pomModel[var6];
            this.addExplicitExtension(LanguageParserDefinitions.INSTANCE, definition.getFileNodeType().getLanguage(), definition);
        }

        if(this.myDefinitions.length > 0) {
            this.configureFromParserDefinition(this.myDefinitions[0], this.myFileExt);
        }

        PomModelImpl var8 = new PomModelImpl(this.myProject);
        this.myProject.registerService(PomModel.class, var8);
        new TreeAspect(var8);
    }

    public void configureFromParserDefinition(ParserDefinition definition, String extension) {
        this.myLanguage = definition.getFileNodeType().getLanguage();
        this.myFileExt = extension;
        this.addExplicitExtension(LanguageParserDefinitions.INSTANCE, this.myLanguage, definition);
        registerComponentInstance(
                getApplication().getPicoContainer(), FileTypeManager.class,
                new KtMockFileTypeManager(new KtMockLanguageFileType(myLanguage, myFileExt)));
    }

    protected <T> void addExplicitExtension(final LanguageExtension<T> instance, final Language language, final T object) {
        instance.addExplicitExtension(language, object);
        Disposer.register(this.myProject, new com.intellij.openapi.Disposable() {
            public void dispose() {
                instance.removeExplicitExtension(language, object);
            }
        });
    }

    protected <T> void registerExtensionPoint(final ExtensionPointName<T> extensionPointName, Class<T> aClass) {
        super.registerExtensionPoint(extensionPointName, aClass);
        Disposer.register(this.myProject, new com.intellij.openapi.Disposable() {
            public void dispose() {
                Extensions.getRootArea().unregisterExtensionPoint(extensionPointName.getName());
            }
        });
    }

    protected <T> void registerApplicationService(final Class<T> aClass, T object) {
        getApplication().registerService(aClass, object);
        Disposer.register(this.myProject, new com.intellij.openapi.Disposable() {
            public void dispose() {
                KtPlatformLiteFixture.getApplication().getPicoContainer().unregisterComponent(aClass.getName());
            }
        });
    }

    public MockProjectEx getProject() {
        return this.myProject;
    }

    public MockPsiManager getPsiManager() {
        return this.myPsiManager;
    }

    protected void tearDown() throws Exception {
        super.tearDown();
        this.myFile = null;
        this.myProject = null;
        this.myPsiManager = null;
    }

    protected String getTestDataPath() {
        return PathManager.getHomePath();
        //return PathManagerEx.getTestDataPath();
    }

    @NotNull
    public final String getTestName() {
        return this.getTestName(this.myLowercaseFirstLetter);
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
        String name = this.getTestName();

        try {
            String e = this.loadFile(name + "." + this.myFileExt);
            this.myFile = this.createPsiFile(name, e);
            ensureParsed(this.myFile);
            assertEquals("light virtual file text mismatch", e, ((LightVirtualFile)this.myFile.getVirtualFile()).getContent().toString());
            assertEquals("virtual file text mismatch", e, LoadTextUtil.loadText(this.myFile.getVirtualFile()));
            assertEquals("doc text mismatch", e, this.myFile.getViewProvider().getDocument().getText());
            assertEquals("psi text mismatch", e, this.myFile.getText());
            ensureCorrectReparse(this.myFile);
            if(checkResult) {
                this.checkResult(name, this.myFile);
            } else {
                toParseTreeText(this.myFile, this.skipSpaces(), this.includeRanges());
            }

        } catch (IOException var4) {
            throw new RuntimeException(var4);
        }
    }

    protected void doTest(String suffix) throws IOException {
        String name = this.getTestName();
        String text = this.loadFile(name + "." + this.myFileExt);
        this.myFile = this.createPsiFile(name, text);
        ensureParsed(this.myFile);
        assertEquals(text, this.myFile.getText());
        this.checkResult(name + suffix, this.myFile);
    }

    protected void doCodeTest(String code) throws IOException {
        String name = this.getTestName();
        this.myFile = this.createPsiFile("a", code);
        ensureParsed(this.myFile);
        assertEquals(code, this.myFile.getText());
        this.checkResult(this.myFilePrefix + name, this.myFile);
    }

    protected PsiFile createPsiFile(String name, String text) {
        return this.createFile(name + "." + this.myFileExt, text);
    }

    protected PsiFile createFile(@NonNls String name, String text) {
        LightVirtualFile virtualFile = new LightVirtualFile(name, this.myLanguage, text);
        virtualFile.setCharset(CharsetToolkit.UTF8_CHARSET);
        return this.createFile(virtualFile);
    }

    protected PsiFile createFile(LightVirtualFile virtualFile) {
        return this.myFileFactory.trySetupPsiForFile(virtualFile, this.myLanguage, true, false);
    }

    protected void checkResult(@NonNls String targetDataName, PsiFile file) throws IOException {
        doCheckResult(this.myFullDataPath, file, this.checkAllPsiRoots(), targetDataName, this.skipSpaces(), this.includeRanges());
    }

    public static void doCheckResult(String testDataDir, PsiFile file, boolean checkAllPsiRoots, String targetDataName, boolean skipSpaces, boolean printRanges) throws IOException {
        FileViewProvider provider = file.getViewProvider();
        Set languages = provider.getLanguages();
        if(checkAllPsiRoots && languages.size() != 1) {
            Iterator var8 = languages.iterator();

            while(var8.hasNext()) {
                Language language = (Language)var8.next();
                PsiFile root = provider.getPsi(language);
                String expectedName = targetDataName + "." + language.getID() + ".txt";
                doCheckResult(testDataDir, expectedName, toParseTreeText(root, skipSpaces, printRanges).trim());
            }

        } else {
            doCheckResult(testDataDir, targetDataName + ".txt", toParseTreeText(file, skipSpaces, printRanges).trim());
        }
    }

    protected void checkResult(String actual) throws IOException {
        String name = this.getTestName();
        doCheckResult(this.myFullDataPath, this.myFilePrefix + name + ".txt", actual);
    }

    protected void checkResult(@NonNls String targetDataName, String actual) throws IOException {
        doCheckResult(this.myFullDataPath, targetDataName, actual);
    }

    public static void doCheckResult(String fullPath, String targetDataName, String actual) throws IOException {
        String expectedFileName = fullPath + File.separatorChar + targetDataName;
        KtUsefulTestCase.assertSameLinesWithFile(expectedFileName, actual);
    }

    protected static String toParseTreeText(PsiElement file, boolean skipSpaces, boolean printRanges) {
        return DebugUtil.psiToString(file, skipSpaces, printRanges);
    }

    protected String loadFile(@NonNls String name) throws IOException {
        return loadFileDefault(this.myFullDataPath, name);
    }

    public static String loadFileDefault(String dir, String name) throws IOException {
        return FileUtil.loadFile(new File(dir, name), "UTF-8", true).trim();
    }

    public static void ensureParsed(PsiFile file) {
        file.accept(new PsiElementVisitor() {
            public void visitElement(PsiElement element) {
                element.acceptChildren(this);
            }
        });
    }

    public static void ensureCorrectReparse(@NotNull PsiFile file) {
        String psiToStringDefault = DebugUtil.psiToString(file, false, false);
        String fileText = file.getText();
        DiffLog diffLog = (new BlockSupportImpl(file.getProject())).reparseRange(file, TextRange.allOf(fileText), fileText, new EmptyProgressIndicator(), fileText);
        diffLog.performActualPsiChange(file);
        TestCase.assertEquals(psiToStringDefault, DebugUtil.psiToString(file, false, false));
    }
}