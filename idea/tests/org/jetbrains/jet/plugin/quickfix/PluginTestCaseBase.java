package org.jetbrains.jet.plugin.quickfix;

import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.projectRoots.impl.JavaSdkImpl;
import org.jetbrains.jet.JetTestCaseBuilder;

/**
 * @author yole
 */
public class PluginTestCaseBase {
    public static String getTestDataPathBase() {
        return JetTestCaseBuilder.getHomeDirectory() + "/idea/testData";
    }

    public static Sdk jdkFromIdeaHome() {
        return new JavaSdkImpl().createJdk("JDK", "compiler/testData/mockJDK-1.7/jre", true);
    }
}
