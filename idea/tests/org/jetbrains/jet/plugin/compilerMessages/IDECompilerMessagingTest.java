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

import com.intellij.openapi.compiler.TranslatingCompiler;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.PlatformTestCase;
import jet.Function1;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.plugin.PluginTestCaseBase;
import org.jetbrains.jet.plugin.compiler.JetCompiler;

import java.io.IOException;

import static org.jetbrains.jet.plugin.compilerMessages.Message.error;
import static org.jetbrains.jet.plugin.compilerMessages.Message.warning;

/**
 * @author Pavel Talanov
 */
public abstract class IDECompilerMessagingTest extends PlatformTestCase {

    private static final String TEST_DATA_PATH = PluginTestCaseBase.getTestDataPathBase() + "/compilerMessages";


    protected void performTest(Function1<MessageChecker, Void> whatToExpect, @NotNull TranslatingCompiler compiler) {
        String pathToTestDir = TEST_DATA_PATH + "/" + getTestName(true);
        VirtualFile testDir = LocalFileSystem.getInstance().findFileByPath(pathToTestDir);
        VirtualFile sampleFile = LocalFileSystem.getInstance().findFileByPath(pathToTestDir + "/src/test.kt");
        VirtualFile outDirectory = getOutDirectory(pathToTestDir, testDir);
        VirtualFile root = LocalFileSystem.getInstance().findFileByPath(pathToTestDir + "/src/");
        MockCompileContext mockCompileContext = new MockCompileContext(myModule, outDirectory, root);
        MockModuleChunk mockModuleChunk = new MockModuleChunk(myModule);
        compiler
                .compile(mockCompileContext, mockModuleChunk, new VirtualFile[] {sampleFile}, new MockOutputSink());
        MessageChecker checker = new MessageChecker(mockCompileContext);
        checkHeader(checker);
        whatToExpect.invoke(checker);
        checker.finish();
    }

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

    private static void checkHeader(@NotNull MessageChecker checker) {
        checker.expect(Message.info().textStartsWith("Using kotlinHome="));
        checker.expect(Message.info().textStartsWith("Invoking in-process compiler"));
        checker.expect(Message.info().textStartsWith("Kotlin Compiler version"));
    }
}
