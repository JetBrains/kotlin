package org.jetbrains.jet.plugin.quickfix;

import org.jetbrains.jet.JetTestCaseBase;

/**
 * @author yole
 */
public class PluginTestCaseBase {
    public static String getTestDataPathBase() {
        return JetTestCaseBase.getHomeDirectory() + "/idea/testData";
    }
}
