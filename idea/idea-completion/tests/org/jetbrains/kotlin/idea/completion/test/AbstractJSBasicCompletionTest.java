/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.completion.test;

import com.intellij.codeInsight.completion.CompletionType;
import com.intellij.testFramework.LightProjectDescriptor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.idea.test.KotlinStdJSProjectDescriptor;
import org.jetbrains.kotlin.platform.TargetPlatform;
import org.jetbrains.kotlin.platform.js.JsPlatforms;

public abstract class AbstractJSBasicCompletionTest extends KotlinFixtureCompletionBaseTestCase {
    @NotNull
    @Override
    protected LightProjectDescriptor getProjectDescriptor() {
        return KotlinStdJSProjectDescriptor.INSTANCE;
    }

    @NotNull
    @Override
    public TargetPlatform getPlatform() {
        return JsPlatforms.INSTANCE.getDefaultJsPlatform();
    }

    @NotNull
    @Override
    protected CompletionType defaultCompletionType() {
        return CompletionType.BASIC;
    }
}
