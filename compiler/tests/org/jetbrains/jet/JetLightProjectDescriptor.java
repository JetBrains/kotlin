package org.jetbrains.jet;

import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.testFramework.fixtures.DefaultLightProjectDescriptor;

/**
 * @author yole
 */
public class JetLightProjectDescriptor extends DefaultLightProjectDescriptor {
    public static JetLightProjectDescriptor INSTANCE = new JetLightProjectDescriptor();

    @Override
    public Sdk getSdk() {
        return JetTestCaseBase.jdkFromIdeaHome();
    }
}
