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

package org.jetbrains.jet.plugin.codeInsight.surroundWith;

import com.intellij.codeInsight.generation.surroundWith.SurroundWithHandler;
import com.intellij.lang.surroundWith.Surrounder;
import com.intellij.testFramework.LightCodeInsightTestCase;
import org.jetbrains.annotations.NotNull;

public abstract class AbstractSurroundWithTest extends LightCodeInsightTestCase {

    public void doTestWithIfSurrounder(String path) throws Exception {
        doTest(path, new KotlinIfSurrounder());
    }

    public void doTestWithIfElseSurrounder(String path) throws Exception {
        doTest(path, new KotlinIfElseSurrounder());
    }

    public void doTestWithNotSurrounder(String path) throws Exception {
        doTest(path, new KotlinNotSurrounder());
    }

    private void doTest(String path, Surrounder surrounder) throws Exception{
        configureByFile(path);
        SurroundWithHandler.invoke(getProject(), getEditor(), getFile(), surrounder);
        checkResultByFile(path + ".after");
    }

    @NotNull
    @Override
    protected String getTestDataPath() {
        return "";
    }
}
