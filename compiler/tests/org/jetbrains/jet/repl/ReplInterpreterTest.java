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

package org.jetbrains.jet.repl;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.CompileCompilerDependenciesTest;
import org.jetbrains.jet.cli.jvm.repl.ReplInterpreter;
import org.jetbrains.jet.lang.resolve.java.CompilerDependencies;
import org.jetbrains.jet.lang.resolve.java.CompilerSpecialMode;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;

/**
 * @author Stepan Koltsov
 */
public class ReplInterpreterTest {

    static {
        System.setProperty("java.awt.headless", "true");
    }

    private final Disposable disposable = new Disposable() {
        @Override
        public void dispose() {
        }
    };

    @After
    public void tearDown() {
        Disposer.dispose(disposable);
    }

    private void testFile(@NotNull String relativePath) {
        CompilerDependencies compilerDependencies = CompileCompilerDependenciesTest.compilerDependenciesForTests(CompilerSpecialMode.JDK_HEADERS, false);
        ReplInterpreter repl = new ReplInterpreter(disposable, compilerDependencies);

        ReplSessionTestFile file = ReplSessionTestFile.load(new File("compiler/testData/repl/" + relativePath));
        for (Pair<String, String> t : file.getLines()) {
            String code = t.first;
            String expected = t.second;

            Object actual = repl.eval(code);
            String actualString = actual != null ? actual.toString() : "null";

            Assert.assertEquals("after evaluation of: " + code, actualString, expected);
        }
    }

    @Test
    public void constants() {
        testFile("constants.repl");
    }

    @Test
    public void simple() {
        testFile("simple.repl");
    }

}
