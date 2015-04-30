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

package org.jetbrains.kotlin.idea.completion.test;

import com.intellij.codeInsight.completion.CompletionType;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.testFramework.LightProjectDescriptor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.idea.js.KotlinJavaScriptLibraryManager;
import org.jetbrains.kotlin.idea.project.TargetPlatform;
import org.jetbrains.kotlin.idea.test.KotlinStdJSProjectDescriptor;

public abstract class AbstractJSBasicCompletionTest extends JetFixtureCompletionBaseTestCase {
    @NotNull
    @Override
    protected LightProjectDescriptor getProjectDescriptor() {
        return KotlinStdJSProjectDescriptor.Companion.getInstance();
    }

    @Override
    public TargetPlatform getPlatform() {
        return TargetPlatform.JS;
    }

    @Override
    protected void setUpFixture(@NotNull String testPath) {
        super.setUpFixture(testPath);
        KotlinJavaScriptLibraryManager.getInstance(getProject()).syncUpdateProjectLibrary();
    }

    @Override
    protected LookupElement[] complete(int invocationCount) {
        return myFixture.complete(CompletionType.BASIC, invocationCount);
    }
}
