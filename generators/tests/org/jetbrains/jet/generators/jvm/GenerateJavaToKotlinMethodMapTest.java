package org.jetbrains.jet.generators.jvm;

import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import junit.framework.Assert;
import junit.framework.TestCase;

public class GenerateJavaToKotlinMethodMapTest extends TestCase {
    public void testGenerateJavaToKotlinMethodMap() throws Exception {
        String text = StringUtil.convertLineSeparators(GenerateJavaToKotlinMethodMap.generateText().toString());
        String expected = FileUtil.loadFile(GenerateJavaToKotlinMethodMap.TARGET_FILE, true);
        Assert.assertEquals("To fix this problem you need to run GenerateJavaToKotlinMethodMap", expected, text);
    }
}
