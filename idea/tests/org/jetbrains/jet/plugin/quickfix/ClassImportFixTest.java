package org.jetbrains.jet.plugin.quickfix;

import com.intellij.codeInsight.daemon.quickFix.LightQuickFixTestCase;
import com.intellij.openapi.projectRoots.Sdk;
import org.jetbrains.jet.JetTestCaseBase;

/**
 * @author svtk
 */
public class ClassImportFixTest extends LightQuickFixTestCase {

    public void test() throws Exception {
        doAllTests();
    }

    @Override
    protected String getBasePath() {
        return "/quickfix/classImport";
    }

    @Override
    protected String getTestDataPath() {
        return JetTestCaseBase.getTestDataPathBase();
    }

    @Override
    protected Sdk getProjectJDK() {
        return JetTestCaseBase.jdkFromIdeaHome();
    }

}

