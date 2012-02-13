package org.jetbrains.jet.plugin;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleType;
import com.intellij.openapi.module.StdModuleTypes;
import com.intellij.openapi.projectRoots.JavaSdk;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.roots.ContentEntry;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.vfs.JarFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.LightProjectDescriptor;
import org.apache.commons.lang.SystemUtils;
import org.jetbrains.jet.codegen.ForTestCompileStdlib;

/**
* @author Evgeny Gerashchenko
* @since 2/13/12
*/
public class JetWithJdkAndRuntimeLightProjectDescriptor implements LightProjectDescriptor {
    private JetWithJdkAndRuntimeLightProjectDescriptor() {
    }

    public static final JetWithJdkAndRuntimeLightProjectDescriptor INSTANCE = new JetWithJdkAndRuntimeLightProjectDescriptor();

    @Override
    public ModuleType getModuleType() {
        return StdModuleTypes.JAVA;
    }

    @Override
    public Sdk getSdk() {
        return JavaSdk.getInstance().createJdk("JDK", SystemUtils.getJavaHome().getAbsolutePath());
    }

    @Override
    public void configureModule(Module module, ModifiableRootModel model, ContentEntry contentEntry) {
        Library.ModifiableModel modifiableModel = model.getModuleLibraryTable().createLibrary("ktl").getModifiableModel();
        VirtualFile cd = JarFileSystem.getInstance().findFileByPath(ForTestCompileStdlib.stdlibJarForTests() + "!/");
        assert cd != null;
        modifiableModel.addRoot(cd, OrderRootType.CLASSES);
        modifiableModel.commit();
    }
}
