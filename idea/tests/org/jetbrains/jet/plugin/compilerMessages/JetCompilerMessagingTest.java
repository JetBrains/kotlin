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

import kotlin.Function1;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.plugin.PluginTestCaseBase;
import org.jetbrains.jet.plugin.compiler.K2JvmTranslatingCompiler;

import static org.jetbrains.jet.plugin.compilerMessages.Message.error;
import static org.jetbrains.jet.plugin.compilerMessages.Message.warning;

public final class JetCompilerMessagingTest extends IDECompilerMessagingTest {

    private static final String TEST_DATA_PATH = PluginTestCaseBase.getTestDataPathBase() + "/compilerMessages/k2jvm";

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
                checker.expect(warning().text("Unnecessary non-null assertion (!!) on a non-null receiver of type kotlin.String")
                                       .at("test.kt", 4, 4));
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

    public void testVisibilityError() {
        doTest(new Function1<MessageChecker, Void>() {
            @Override
            public Void invoke(MessageChecker checker) {
                checker
                        .expect(error().text("'f' hides member of supertype 'TestTrait' and needs 'override' modifier").at("test.kt", 6, 5))
                        .expect(error().text("Cannot weaken access privilege 'public' for 'f' in 'TestTrait'").at("test.kt", 6, 9))
                ;
                return null;
            }
        });
    }

    private void doTest(@NotNull Function1<MessageChecker, Void> whatToExpect) {
        performTest(whatToExpect, getCompiler(K2JvmTranslatingCompiler.class), TEST_DATA_PATH);
    }

    @Override
    protected void checkHeader(@NotNull MessageChecker checker) {
        checker.expect(Message.info().textStartsWith("Using kotlinHome="));
        checker.expect(Message.info().textStartsWith("Invoking K2JVMCompiler"));
        checker.expect(Message.info().textStartsWith("Kotlin Compiler version"));
        checker.expect(Message.stats().textStartsWith("Using Kotlin home directory"));
        checker.expect(Message.stats().text("Configuring the compilation environment"));
    }
}