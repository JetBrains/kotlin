package org.jetbrains.jet.plugin;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleType;
import com.intellij.openapi.module.StdModuleTypes;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.roots.ContentEntry;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.testFramework.LightProjectDescriptor;

/**
 * @author yole
 */
public class JetLightProjectDescriptor implements LightProjectDescriptor {
    private JetLightProjectDescriptor() {
    }
    
    public static final JetLightProjectDescriptor INSTANCE = new JetLightProjectDescriptor();
    
    @Override
    public ModuleType getModuleType() {
        return StdModuleTypes.JAVA;
    }

    @Override
    public Sdk getSdk() {
        return PluginTestCaseBase.jdkFromIdeaHome();
    }

    @Override
    public void configureModule(Module module, ModifiableRootModel model, ContentEntry contentEntry) {
    }
}
