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

package org.jetbrains.jet.plugin.compilerMessages;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.compiler.CompilerManager;
import com.intellij.openapi.compiler.TranslatingCompiler;
import com.intellij.openapi.roots.ContentEntry;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.roots.ui.configuration.libraryEditor.NewLibraryEditor;
import com.intellij.openapi.roots.ui.configuration.projectRoot.LibrariesContainer;
import com.intellij.openapi.roots.ui.configuration.projectRoot.LibrariesContainerFactory;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.PlatformTestCase;
import jet.Function1;
import junit.framework.Assert;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.plugin.sdk.KotlinSdkDescription;

import java.io.File;
import java.io.IOException;

/**
 * @author Pavel Talanov
 */
public abstract class IDECompilerMessagingTest extends PlatformTestCase {

    protected void performTest(@NotNull Function1<MessageChecker, Void> whatToExpect,
            @NotNull TranslatingCompiler compiler, @NotNull String testDataPath) {
        String pathToTestDir = testDataPath + "/" + getTestName(true);
        VirtualFile testDir = LocalFileSystem.getInstance().findFileByPath(pathToTestDir);
        Assert.assertNotNull(testDir);
        VirtualFile sampleFile = LocalFileSystem.getInstance().findFileByPath(pathToTestDir + "/src/test.kt");
        VirtualFile outDirectory = getOutDirectory(pathToTestDir, testDir);
        String pathToSrc = pathToTestDir + "/src/";
        VirtualFile root = LocalFileSystem.getInstance().findFileByPath(pathToSrc);
        Assert.assertNotNull(root);
        MockCompileContext mockCompileContext = new MockCompileContext(myModule, outDirectory, root);
        MockModuleChunk mockModuleChunk = new MockModuleChunk(myModule);
        setSourceEntryForModule(root);
        setKotlinSdkForModule();
        assert sampleFile != null;
        compile(compiler, sampleFile, mockCompileContext, mockModuleChunk);
        checkMessages(whatToExpect, mockCompileContext);
    }

    private void checkMessages(@NotNull Function1<MessageChecker, Void> whatToExpect, @NotNull MockCompileContext mockCompileContext) {
        MessageChecker checker = new MessageChecker(mockCompileContext);
        checkHeader(checker);
        whatToExpect.invoke(checker);
        checker.finish();
    }

    private static void compile(@NotNull TranslatingCompiler compiler,
            @NotNull VirtualFile sampleFile,
            @NotNull MockCompileContext mockCompileContext,
            @NotNull MockModuleChunk mockModuleChunk) {
        compiler.compile(mockCompileContext, mockModuleChunk, new VirtualFile[] {sampleFile}, new MockOutputSink());
    }

    private void setSourceEntryForModule(final VirtualFile root) {
        ApplicationManager.getApplication().runWriteAction(new Runnable() {
            @Override
            public void run() {
                ModifiableRootModel model = ModuleRootManager.getInstance(myModule).getModifiableModel();
                ContentEntry entry = model.addContentEntry(root);
                entry.addSourceFolder(root, false);
                model.commit();
            }
        });
    }

    private void setKotlinSdkForModule() {
        ApplicationManager.getApplication().runWriteAction(new Runnable() {
            @Override
            public void run() {
                ModifiableRootModel model = ModuleRootManager.getInstance(myModule).getModifiableModel();
                model.addLibraryEntry(createKotlinSdkLibrary());
                model.commit();
            }
        });
    }

    @NotNull
    private Library createKotlinSdkLibrary() {
        NewLibraryEditor editor = new NewLibraryEditor();
        editor.setName("Kotlin SDK");
        KotlinSdkDescription.addSDKRoots(editor, new File("dist/kotlinc"));
        return LibrariesContainerFactory.createContainer(myModule).createLibrary(editor, LibrariesContainer.LibraryLevel.GLOBAL);
    }

    protected abstract void checkHeader(@NotNull MessageChecker checker);

    @NotNull
    private VirtualFile getOutDirectory(@NotNull String pathToTestDir, @NotNull VirtualFile testDir) {
        VirtualFile outDirectory = LocalFileSystem.getInstance().findFileByPath(pathToTestDir + "/out");
        if (outDirectory == null) {
            try {
                outDirectory = LocalFileSystem.getInstance().createChildDirectory(this, testDir, "out");
            }
            catch (IOException e) {
                fail();
            }
        }
        return outDirectory;
    }

    protected <T extends TranslatingCompiler> T getCompiler(Class<T> aClass) {
        T[] compilers = CompilerManager.getInstance(getProject()).getCompilers(aClass);
        assert compilers.length == 1;
        return compilers[0];
    }
}
