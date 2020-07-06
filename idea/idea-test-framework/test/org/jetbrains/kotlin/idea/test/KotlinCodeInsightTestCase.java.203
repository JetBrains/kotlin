/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.test;

import com.intellij.codeInsight.CodeInsightTestCase;
import com.intellij.openapi.vfs.newvfs.impl.VfsRootAccess;
import com.intellij.util.ThrowableRunnable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.test.KotlinTestUtils;
import org.jetbrains.kotlin.test.WithMutedInDatabaseRunTest;

/**
 * Please use KotlinLightCodeInsightFixtureTestCase as the base class for all new tests.
 */
@WithMutedInDatabaseRunTest
@Deprecated
public abstract class KotlinCodeInsightTestCase extends CodeInsightTestCase {
    @Override
    protected void setUp() throws Exception {
        VfsRootAccess.allowRootAccess(KotlinTestUtils.getHomeDirectory());
        super.setUp();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        VfsRootAccess.disallowRootAccess(KotlinTestUtils.getHomeDirectory());
    }

    @Override
    protected void runTestRunnable(@NotNull ThrowableRunnable<Throwable> testRunnable) throws Throwable {
        KotlinTestUtils.runTestWithThrowable(this, () -> super.runTestRunnable(testRunnable));
    }
}
