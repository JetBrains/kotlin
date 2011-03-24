package org.jetbrains.jet;

import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.testFramework.fixtures.DefaultLightProjectDescriptor;
import org.jetbrains.jet.resolve.JetResolveTest;

/**
 * @author yole
 */
public class JetLightProjectDescriptor extends DefaultLightProjectDescriptor {
    public static JetLightProjectDescriptor INSTANCE = new JetLightProjectDescriptor();

    @Override
    public Sdk getSdk() {
        return JetResolveTest.jdkFromIdeaHome();
    }
}
