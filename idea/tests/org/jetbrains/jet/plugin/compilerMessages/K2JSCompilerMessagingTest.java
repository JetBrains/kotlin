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

import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.roots.ui.configuration.libraryEditor.NewLibraryEditor;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.util.PathUtil;
import kotlin.Function1;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.plugin.PluginTestCaseBase;
import org.jetbrains.jet.plugin.compiler.K2JsTranslatingCompiler;
import org.jetbrains.jet.testing.ConfigLibraryUtil;

import java.io.File;

import static org.jetbrains.jet.plugin.compilerMessages.Message.*;

public final class K2JSCompilerMessagingTest extends IDECompilerMessagingTest {

    private static final String TEST_DATA_PATH = PluginTestCaseBase.getTestDataPathBase() + "/compilerMessages/k2js";

    public void testHelloWorld() {
        doTest(new Function1<MessageChecker, Void>() {
            @Override
            public Void invoke(MessageChecker checker) {
                //nothing apart from header
                return null;
            }
        });
    }

    public void testSimpleWarning() {
        doTest(new Function1<MessageChecker, Void>() {
            @Override
            public Void invoke(MessageChecker checker) {
                checker.expect(warning().text("Condition 't != null' is always 'true'")
                                       .at("test.kt", 3, 7));
                return null;
            }
        });
    }

    public void testSimpleError() {
        doTest(new Function1<MessageChecker, Void>() {
            @Override
            public Void invoke(MessageChecker checker) {
                checker.expect(
                        error().text("A 'return' expression required in a function with a block body ('{...}')").at("test.kt", 5, 1));
                return null;
            }
        });
    }

    public void testLib() {
        NewLibraryEditor editor = new NewLibraryEditor(null, null);
        editor.setName("testLib");

        String path = PathUtil.getLocalPath(getFile(TEST_DATA_PATH, "/src/lib.zip"));
        assert path != null : "Can't get local path";

        String libUrl = VfsUtil.getUrlForLibraryRoot(new File(path));

        editor.addRoot(libUrl, OrderRootType.CLASSES);
        editor.addRoot(libUrl, OrderRootType.SOURCES);

        try {
            ConfigLibraryUtil.addLibrary(editor, myModule);

            doTest(new Function1<MessageChecker, Void>() {
                @Override
                public Void invoke(MessageChecker checker) {
                    //nothing apart from header
                    return null;
                }
            });
        }
        finally {
            ConfigLibraryUtil.removeLibrary(myModule, "testLib");
        }
    }

    private void doTest(@NotNull Function1<MessageChecker, Void> whatToExpect) {
        performTest(whatToExpect, getCompiler(K2JsTranslatingCompiler.class), TEST_DATA_PATH);
    }

    @Override
    protected void checkHeader(@NotNull MessageChecker checker) {
        checker.expect(info().textStartsWith("Using kotlinHome="));
        checker.expect(info().textStartsWith("Invoking K2JSCompiler"));
        checker.expect(info().textStartsWith("Kotlin Compiler version"));
        checker.expect(stats().textMatchesRegexp("Compiling source files: .*/src/test.kt"));
    }
}