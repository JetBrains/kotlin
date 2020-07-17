/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.completion.test;

import com.intellij.codeInsight.CodeInsightSettings;
import com.intellij.codeInsight.completion.CompletionTestCase;
import com.intellij.openapi.vfs.newvfs.impl.VfsRootAccess;
import com.intellij.util.ArrayUtil;
import org.jetbrains.kotlin.test.KotlinTestUtils;
import org.jetbrains.kotlin.test.WithMutedInDatabaseRunTest;

@WithMutedInDatabaseRunTest
abstract public class KotlinCompletionTestCase extends CompletionTestCase {
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        VfsRootAccess.allowRootAccess(KotlinTestUtils.getHomeDirectory());
        CodeInsightSettings.getInstance().EXCLUDED_PACKAGES = new String[]{"excludedPackage", "somePackage.ExcludedClass"};
    }

    @Override
    protected void tearDown() throws Exception {
        CodeInsightSettings.getInstance().EXCLUDED_PACKAGES = ArrayUtil.EMPTY_STRING_ARRAY;
        VfsRootAccess.disallowRootAccess(KotlinTestUtils.getHomeDirectory());
        super.tearDown();
    }

    @Override
    protected void runTest() throws Throwable {
        //noinspection Convert2MethodRef
        KotlinTestUtils.runTestWithThrowable(this, () -> super.runTest());
    }
}
