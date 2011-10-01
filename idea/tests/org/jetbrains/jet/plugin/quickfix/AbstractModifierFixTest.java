package org.jetbrains.jet.plugin.quickfix;

import com.intellij.codeInsight.daemon.quickFix.LightQuickFixTestCase;
import org.jetbrains.jet.JetTestCaseBase;

/**
 * @author svtk
 */
public class AbstractModifierFixTest extends LightQuickFixTestCase {

    public void test() throws Exception {
        doAllTests();
    }

    @Override
    protected String getBasePath() {
        return "/quickfix/abstract";
    }

    @Override
    protected String getTestDataPath() {
        return JetTestCaseBase.getTestDataPathBase();
    }
}
