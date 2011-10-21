package org.jetbrains.jet.plugin.quickfix;

import com.intellij.codeInsight.daemon.quickFix.LightQuickFixTestCase;
import com.intellij.openapi.projectRoots.Sdk;
import org.jetbrains.jet.JetTestCaseBase;

/**
 * @author svtk
 */
public class TypeAdditionFixTests extends LightQuickFixTestCase {

    public void test() throws Exception {
        doAllTests();
    }

    @Override
    protected String getBasePath() {
        return "/quickfix/typeAddition";
    }

    @Override
    protected String getTestDataPath() {
        return PluginTestCaseBase.getTestDataPathBase();
    }

    @Override
    protected Sdk getProjectJDK() {
        return JetTestCaseBase.jdkFromIdeaHome();
    }
}

