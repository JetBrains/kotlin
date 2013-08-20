package org.jetbrains.jet.plugin;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.ContentEntry;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.util.ThrowableComputable;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileVisitor;
import com.intellij.openapi.vfs.newvfs.NewVirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.JetTestUtils;
import org.jetbrains.jet.cli.common.ExitCode;
import org.jetbrains.jet.cli.jvm.K2JVMCompiler;
import org.jetbrains.jet.utils.ExceptionUtils;
import org.jetbrains.jet.utils.PathUtil;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;

public class JdkAndMockLibraryProjectDescriptor extends JetLightProjectDescriptor {
    private final String sourcesPath;
    private final boolean withSources;

    public JdkAndMockLibraryProjectDescriptor(String sourcesPath, boolean withSources) {
        this.sourcesPath = sourcesPath;
        this.withSources = withSources;
    }

    @Override
    public void configureModule(Module module, ModifiableRootModel model, ContentEntry contentEntry) {
        try {
            File compiledDir = compileTestLibrary(sourcesPath);

            final VirtualFile baseDir = module.getProject().getBaseDir();
            assertNotNull(baseDir);

            VirtualFile libraryDir = ApplicationManager.getApplication().runWriteAction(new ThrowableComputable<VirtualFile, IOException>() {
                @Override
                public VirtualFile compute() throws IOException {
                    VirtualFile libraryDir = baseDir.createChildDirectory(this, "lib");
                    baseDir.createChildDirectory(this, "src");
                    return libraryDir;
                }
            });

            VirtualFile testDataDir = LocalFileSystem.getInstance().findFileByPath(sourcesPath).getParent();
            assertNotNull(testDataDir);
            VfsUtilCore.visitChildrenRecursively(testDataDir, new VirtualFileVisitor() {
                @Override
                public boolean visitFile(@NotNull VirtualFile file) {
                    file.getChildren();
                    file.refresh(false, true);
                    return true;
                }
            });
            VirtualFile librarySourceDir = LocalFileSystem.getInstance().findFileByPath(sourcesPath);
            assertNotNull(librarySourceDir);

            FileUtil.copyDir(compiledDir, new File(libraryDir.getPath()));

            ((NewVirtualFile)baseDir).markDirtyRecursively();
            baseDir.refresh(false, true);

            Library.ModifiableModel libraryModel = model.getModuleLibraryTable().getModifiableModel().createLibrary("myKotlinLib").getModifiableModel();
            libraryModel.addRoot(libraryDir, OrderRootType.CLASSES);
            if (withSources) {
                libraryModel.addRoot(librarySourceDir, OrderRootType.SOURCES);
            }
            libraryModel.commit();
        }
        catch (IOException e) {
            throw new AssertionError(e);
        }
    }

    private static File compileTestLibrary(String sourcesPath) {
        try {
            File compiledDir = JetTestUtils.tmpDir("dummylib");

            ByteArrayOutputStream outStream = new ByteArrayOutputStream();

            File kotlinCompilerJar = new File(PathUtil.getKotlinPathsForDistDirectory().getLibPath(), "kotlin-compiler.jar");
            URLClassLoader classLoader = new URLClassLoader(new URL[] {kotlinCompilerJar.toURI().toURL()}, Object.class.getClassLoader());

            Class<?> compilerClass = classLoader.loadClass(K2JVMCompiler.class.getName());
            Object compilerObject = compilerClass.newInstance();
            Method execMethod = compilerClass.getMethod("exec", PrintStream.class, String[].class);

            //noinspection IOResourceOpenedButNotSafelyClosed
            Enum<?> invocationResult = (Enum<?>) execMethod
                    .invoke(compilerObject, new PrintStream(outStream),
                            new String[] {"-src", sourcesPath, "-output", compiledDir.getAbsolutePath()});

            assertEquals(new String(outStream.toByteArray()), ExitCode.OK.name(), invocationResult.name());

            return compiledDir;
        }
        catch (Throwable e) {
            throw ExceptionUtils.rethrow(e);
        }
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
