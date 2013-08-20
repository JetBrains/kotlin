package org.jetbrains.jet.plugin;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.ContentEntry;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.util.io.FileUtilRt;

import java.io.File;

public class JdkAndMockLibraryProjectDescriptor extends JetLightProjectDescriptor {
    private final String sourcesPath;
    private final boolean withSources;

    public JdkAndMockLibraryProjectDescriptor(String sourcesPath, boolean withSources) {
        this.sourcesPath = sourcesPath;
        this.withSources = withSources;
    }

    @Override
    public void configureModule(Module module, ModifiableRootModel model, ContentEntry contentEntry) {
        File libraryJar = MockLibraryUtil.compileLibraryToJar(sourcesPath);
        String jarUrl = "jar://" + FileUtilRt.toSystemIndependentName(libraryJar.getAbsolutePath()) + "!/";

        Library.ModifiableModel libraryModel = model.getModuleLibraryTable().getModifiableModel().createLibrary("myKotlinLib").getModifiableModel();
        libraryModel.addRoot(jarUrl, OrderRootType.CLASSES);
        if (withSources) {
            libraryModel.addRoot(jarUrl + "src/", OrderRootType.SOURCES);
        }
        libraryModel.commit();
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        JdkAndMockLibraryProjectDescriptor that = (JdkAndMockLibraryProjectDescriptor) o;

        if (withSources != that.withSources) return false;
        if (sourcesPath != null ? !sourcesPath.equals(that.sourcesPath) : that.sourcesPath != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = sourcesPath != null ? sourcesPath.hashCode() : 0;
        result = 31 * result + (withSources ? 1 : 0);
        return result;
    }
}
