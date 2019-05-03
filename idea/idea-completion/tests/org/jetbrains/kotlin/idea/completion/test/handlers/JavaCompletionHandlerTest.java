/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.completion.test.handlers;

import org.jetbrains.kotlin.idea.completion.test.CompletionTestUtilKt;
import org.jetbrains.kotlin.idea.completion.test.KotlinCompletionTestCase;
import org.jetbrains.kotlin.test.JUnit3WithIdeaConfigurationRunner;
import org.junit.runner.RunWith;

import java.io.File;
@RunWith(JUnit3WithIdeaConfigurationRunner.class)
public class JavaCompletionHandlerTest extends KotlinCompletionTestCase {
    public void testClassAutoImport() {
        doTest();
    }

    public void doTest() {
        String fileName = getTestName(false);
        try {
            configureByFiles(null, fileName + ".java", fileName + ".kt");
            complete(2);
            checkResultByFile(fileName + ".after.java");
        } catch (@SuppressWarnings("CaughtExceptionImmediatelyRethrown") AssertionError assertionError) {
            throw assertionError;
        } catch (Exception e) {
            throw new AssertionError(e);
        }
    }

    @Override
    protected String getTestDataPath() {
        return new File(CompletionTestUtilKt.getCOMPLETION_TEST_DATA_BASE_PATH(), "/handlers/injava").getPath() + File.separator;
    }
}
