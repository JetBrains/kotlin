/*
 * Copyright 2010-2013 JetBrains s.r.o.
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
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.PlatformTestCase;
import jet.Function1;
import junit.framework.Assert;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public abstract class IDECompilerMessagingTest extends PlatformTestCase {

    protected void performTest(@NotNull Function1<MessageChecker, Void> whatToExpect,
            @NotNull TranslatingCompiler compiler, @NotNull String testDataPath) {
        String pathToTestDir = testDataPath + "/" + getTestName(true);

        VirtualFile testDir = getFile(testDataPath, "");
        VirtualFile sampleFile = getFile(testDataPath, "/src/test.kt");
        VirtualFile outDirectory = getOutDirectory(pathToTestDir, testDir);
        VirtualFile root = getFile(testDataPath, "/src");
        MockCompileContext mockCompileContext = new MockCompileContext(myModule, outDirectory, root);
        MockModuleChunk mockModuleChunk = new MockModuleChunk(myModule);
        setSourceEntryForModule(root);
        assert sampleFile != null;
        compile(compiler, sampleFile, mockCompileContext, mockModuleChunk);
        checkMessages(whatToExpect, mockCompileContext);
    }

    protected VirtualFile getFile(@NotNull String testDataPath, @NotNull String relativePath) {
        String pathToFile = testDataPath + "/" + getTestName(true) + relativePath;
        VirtualFile file = LocalFileSystem.getInstance().findFileByPath(pathToFile);
        Assert.assertNotNull("Can't find path " + pathToFile, file);

        return file;
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
