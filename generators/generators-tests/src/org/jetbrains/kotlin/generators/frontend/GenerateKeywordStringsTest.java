/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.frontend;

import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.kotlin.test.testFramework.KtUsefulTestCase;

import java.io.IOException;

public class GenerateKeywordStringsTest extends KtUsefulTestCase {
    public void testGeneratedDataIsUpToDate() throws IOException {
        String text = GenerateKeywordStrings.generate();
        assertEquals("Contents differ. Regenerate " + GenerateKeywordStrings.class.getName(),
                     StringUtil.convertLineSeparators(text),
                     FileUtil.loadFile(GenerateKeywordStrings.DEST_FILE, true));
    }
}
