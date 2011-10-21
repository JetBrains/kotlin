package org.jetbrains.jet.plugin.quickfix;

import com.intellij.codeInsight.daemon.quickFix.LightQuickFixTestCase;

/**
 * @author svtk
 */
public class ChangeVariableMutabilityFixTest extends LightQuickFixTestCase {

    public void test() throws Exception {
        doAllTests();
    }

    @Override
    protected String getBasePath() {
        return "/quickfix/changeVariableMutability";
    }

    @Override
    protected String getTestDataPath() {
        return PluginTestCaseBase.getTestDataPathBase();
    }
}
