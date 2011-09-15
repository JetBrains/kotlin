package org.jetbrains.jet;

import com.intellij.ide.startup.impl.StartupManagerImpl;
import com.intellij.lang.*;
import com.intellij.lang.impl.PsiBuilderFactoryImpl;
import com.intellij.mock.*;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.impl.FileDocumentManagerImpl;
import com.intellij.openapi.fileEditor.impl.LoadTextUtil;
import com.intellij.openapi.fileTypes.FileTypeFactory;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.options.SchemesManagerFactory;
import com.intellij.openapi.progress.impl.ProgressManagerImpl;
import com.intellij.openapi.startup.StartupManager;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.CharsetToolkit;
import com.intellij.psi.*;
import com.intellij.psi.impl.PsiCachedValuesFactory;
import com.intellij.psi.impl.PsiFileFactoryImpl;
import com.intellij.psi.impl.source.resolve.reference.ReferenceProvidersRegistry;
import com.intellij.psi.impl.source.resolve.reference.ReferenceProvidersRegistryImpl;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.testFramework.LightVirtualFile;
import com.intellij.testFramework.MockSchemesManagerFactory;
import com.intellij.testFramework.PlatformLiteFixture;
import com.intellij.testFramework.TestDataFile;
import com.intellij.util.CachedValuesManagerImpl;
import com.intellij.util.Function;
import com.intellij.util.messages.MessageBus;
import com.intellij.util.messages.MessageBusFactory;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.jet.lang.parsing.JetParserDefinition;
import org.picocontainer.MutablePicoContainer;
import org.picocontainer.PicoContainer;
import org.picocontainer.PicoInitializationException;
import org.picocontainer.PicoIntrospectionException;
import org.picocontainer.defaults.AbstractComponentAdapter;

import java.io.File;
import java.io.IOException;

/**
 * @author abreslav
 */
public class JetLiteFixture extends PlatformLiteFixture {
    protected String myFileExt;
    @NonNls
    protected final String myFullDataPath;
    protected PsiFile myFile;
    private MockPsiManager myPsiManager;
    private PsiFileFactoryImpl myFileFactory;
    protected Language myLanguage;
    protected final ParserDefinition[] myDefinitions;

    public JetLiteFixture(@NonNls String dataPath) {
        myFileExt = "jet";
        myFullDataPath = getTestDataPath() + "/" + dataPath;
        myDefinitions = new ParserDefinition[] {new JetParserDefinition()};
    }

    protected String getTestDataPath() {
        return JetTestCaseBase.getTestDataPathBase();
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        initApplication();
        getApplication().getPicoContainer().registerComponent(new AbstractComponentAdapter("com.intellij.openapi.progress.ProgressManager", Object.class) {
            @Override
            public Object getComponentInstance(PicoContainer container) throws PicoInitializationException, PicoIntrospectionException {
                return new ProgressManagerImpl(getApplication());
            }

            @Override
            public void verify(PicoContainer container) throws PicoIntrospectionException {
            }
        });
        myProject = disposeOnTearDown(new MockProjectEx());
        myPsiManager = new MockPsiManager(myProject);
        myFileFactory = new PsiFileFactoryImpl(myPsiManager);
        final MutablePicoContainer appContainer = getApplication().getPicoContainer();
        registerComponentInstance(appContainer, MessageBus.class, MessageBusFactory.newMessageBus(getApplication()));
        registerComponentInstance(appContainer, SchemesManagerFactory.class, new MockSchemesManagerFactory());
        final MockEditorFactory editorFactory = new MockEditorFactory();
        registerComponentInstance(appContainer, EditorFactory.class, editorFactory);
        registerComponentInstance(appContainer, FileDocumentManager.class, new MockFileDocumentManagerImpl(new Function<CharSequence, Document>() {
            @Override
            public Document fun(CharSequence charSequence) {
                return editorFactory.createDocument(charSequence);
            }
        }, FileDocumentManagerImpl.DOCUMENT_KEY));
        registerComponentInstance(appContainer, PsiDocumentManager.class, new MockPsiDocumentManager());
        myLanguage = myLanguage == null && myDefinitions != null && myDefinitions.length > 0
                     ? myDefinitions[0].getFileNodeType().getLanguage()
                     : myLanguage;
        registerComponentInstance(appContainer, FileTypeManager.class, new MockFileTypeManager(new MockLanguageFileType(myLanguage, myFileExt)));
        registerApplicationService(PsiBuilderFactory.class, new PsiBuilderFactoryImpl());
        registerApplicationService(DefaultASTFactory.class, new DefaultASTFactoryImpl());
        registerApplicationService(ReferenceProvidersRegistry.class, new ReferenceProvidersRegistryImpl());
        myProject.registerService(CachedValuesManager.class, new CachedValuesManagerImpl(myProject, new PsiCachedValuesFactory(myPsiManager)));
        myProject.registerService(PsiManager.class, myPsiManager);
        myProject.registerService(StartupManager.class, new StartupManagerImpl(myProject));
        myProject.registerService(PsiFileFactory.class, new PsiFileFactoryImpl(myPsiManager));

        registerExtensionPoint(FileTypeFactory.FILE_TYPE_FACTORY_EP, FileTypeFactory.class);

        for (ParserDefinition definition : myDefinitions) {
            addExplicitExtension(LanguageParserDefinitions.INSTANCE, definition.getFileNodeType().getLanguage(), definition);
        }
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        myFile = null;
        myProject = null;
        myPsiManager = null;
    }

    protected String loadFile(@NonNls @TestDataFile String name) throws IOException {
        return doLoadFile(myFullDataPath, name);
    }

    private static String doLoadFile(String myFullDataPath, String name) throws IOException {
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
        return myFileFactory.trySetupPsiForFile(virtualFile, myLanguage, true, false);
    }

    protected static void ensureParsed(PsiFile file) {
        file.accept(new PsiElementVisitor() {
            @Override
            public void visitElement(PsiElement element) {
                element.acceptChildren(this);
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

    protected <T> void addExplicitExtension(final LanguageExtension<T> instance, final Language language, final T object) {
        instance.addExplicitExtension(language, object);
        Disposer.register(myProject, new Disposable() {
            @Override
            public void dispose() {
                instance.removeExplicitExtension(language, object);
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
