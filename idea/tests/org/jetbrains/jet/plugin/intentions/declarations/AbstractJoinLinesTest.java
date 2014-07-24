/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.plugin.intentions.declarations;

import com.intellij.codeInsight.editorActions.JoinLinesHandler;
import com.intellij.openapi.projectRoots.JavaSdk;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.testFramework.LightCodeInsightTestCase;
import org.apache.commons.lang.SystemUtils;
import org.jetbrains.annotations.NotNull;

public abstract class AbstractJoinLinesTest extends LightCodeInsightTestCase {
    public void doTest(@NotNull String path) throws Exception {
        configureByFile(path);
        new JoinLinesHandler(null).execute(getEditor(), getCurrentEditorDataContext());
        checkResultByFile(path + ".after");
    }

    @NotNull
    @Override
    protected String getTestDataPath() {
        return "";
    }

    protected static Sdk getFullJavaJDK() {
        return JavaSdk.getInstance().createJdk("JDK", SystemUtils.getJavaHome().getAbsolutePath());
    }
}
