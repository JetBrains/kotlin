/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.checkers;

import com.intellij.testFramework.LightProjectDescriptor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.idea.test.KotlinLightCodeInsightFixtureTestCase;
import org.jetbrains.kotlin.idea.test.KotlinStdJSProjectDescriptor;

public abstract class AbstractJsCheckerTest extends KotlinLightCodeInsightFixtureTestCase {
    @NotNull
    @Override
    protected LightProjectDescriptor getProjectDescriptor() {
        return KotlinStdJSProjectDescriptor.INSTANCE;
    }

    public void doTest(String filePath) {
        myFixture.configureByFile(filePath);
        myFixture.checkHighlighting(true, false, false);
    }
}
