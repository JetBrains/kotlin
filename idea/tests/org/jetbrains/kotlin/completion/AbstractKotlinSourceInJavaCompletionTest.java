/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.completion;

import com.intellij.codeInsight.completion.CompletionType;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.testFramework.LightProjectDescriptor;
import com.intellij.util.ArrayUtil;
import com.intellij.util.Function;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.idea.PluginTestCaseBase;
import org.jetbrains.kotlin.idea.project.TargetPlatform;

import java.io.File;

public abstract class AbstractKotlinSourceInJavaCompletionTest extends JetFixtureCompletionBaseTestCase {
    @Override
    public TargetPlatform getPlatform() {
        return TargetPlatform.JVM;
    }

    @Override
    public void doTest(String testPath) throws Exception {
        File mockLibDir = new File(PluginTestCaseBase.getTestDataPathBase() + "/completion/injava/mockLib");
        File[] listFiles = mockLibDir.listFiles();
        assertNotNull(listFiles);
        String[] paths = ArrayUtil.toStringArray(ContainerUtil.map(listFiles, new Function<File, String>() {
            @Override
            public String fun(File file) {
                return FileUtil.toSystemIndependentName(file.getAbsolutePath());
            }
        }));
        myFixture.configureByFiles(paths);
        super.doTest(testPath);
    }

    @NotNull
    @Override
    protected LightProjectDescriptor getProjectDescriptor() {
        return JAVA_LATEST;
    }

    @Override
    protected LookupElement[] complete(int invocationCount) {
        return myFixture.complete(CompletionType.BASIC, invocationCount);
    }
}
