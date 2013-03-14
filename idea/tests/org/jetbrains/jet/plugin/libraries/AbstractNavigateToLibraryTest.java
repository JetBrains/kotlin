/*
 * Copyright 2010-2013 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.jet.plugin.libraries;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileVisitor;
import com.intellij.openapi.vfs.newvfs.NewVirtualFile;
import com.intellij.testFramework.PlatformTestCase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.JetTestUtils;
import org.jetbrains.jet.cli.common.ExitCode;
import org.jetbrains.jet.cli.jvm.K2JVMCompiler;
import org.jetbrains.jet.plugin.PluginTestCaseBase;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;

public abstract class AbstractNavigateToLibraryTest extends PlatformTestCase {
    protected static final String PACKAGE = "testData.libraries";
    protected static final String TEST_DATA_PATH = PluginTestCaseBase.getTestDataPathBase() + "/libraries";
    protected static final String SOURCES_PATH = TEST_DATA_PATH + "/library";
    protected static final String SRC_DIR_NAME = "src";
    private static File tempDirWithCompiled;
    protected VirtualFile libraryDir;
    protected VirtualFile librarySourceDir;

    protected abstract boolean isWithSources();

    @NotNull
    private static File getTempDirWithCompiled() throws Exception {
        if (tempDirWithCompiled == null) {
            tempDirWithCompiled = JetTestUtils.tmpDir("dummylib");

            ByteArrayOutputStream outStream = new ByteArrayOutputStream();

            ExitCode compilerExec = new K2JVMCompiler().exec(
                    new PrintStream(outStream), "-src", SOURCES_PATH, "-output", tempDirWithCompiled.getAbsolutePath());
            assertEquals(new String(outStream.toByteArray()), ExitCode.OK, compilerExec);
        }

        return tempDirWithCompiled;
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        final VirtualFile baseDir = getProject().getBaseDir();
        assertNotNull(baseDir);
        ApplicationManager.getApplication().runWriteAction(new Runnable() {
            @Override
            public void run() {
                try {
                    libraryDir = baseDir.createChildDirectory(this, "lib");
                    baseDir.createChildDirectory(this, SRC_DIR_NAME);
                }
                catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });

        VirtualFile testDataDir = LocalFileSystem.getInstance().findFileByPath(TEST_DATA_PATH);
        assertNotNull(testDataDir);
        VfsUtilCore.visitChildrenRecursively(testDataDir, new VirtualFileVisitor() {
            @Override
            public boolean visitFile(@NotNull VirtualFile file) {
                file.getChildren();
                file.refresh(false, true);
                return true;
            }
        });
        librarySourceDir = LocalFileSystem.getInstance().findFileByPath(SOURCES_PATH);
        assertNotNull(librarySourceDir);

        FileUtil.copyDir(getTempDirWithCompiled(), new File(libraryDir.getPath()));

        ((NewVirtualFile)baseDir).markDirtyRecursively();
        baseDir.refresh(false, true);

        ApplicationManager.getApplication().runWriteAction(new Runnable() {
            @Override
            public void run() {
                ModifiableRootModel moduleModel = ModuleRootManager.getInstance(myModule).getModifiableModel();

                Library.ModifiableModel libraryModel = moduleModel.getModuleLibraryTable().getModifiableModel().createLibrary("myKotlinLib").getModifiableModel();
                libraryModel.addRoot(libraryDir, OrderRootType.CLASSES);
                if (isWithSources()) {
                    libraryModel.addRoot(librarySourceDir, OrderRootType.SOURCES);
                }
                libraryModel.commit();

                VirtualFile srcDir = baseDir.findChild(SRC_DIR_NAME);
                assertNotNull(srcDir);
                moduleModel.addContentEntry(srcDir).addSourceFolder(srcDir, false);

                moduleModel.commit();
            }
        });
    }

    @NotNull
    protected VirtualFile copyFileToSrcDir(@NotNull String path) {
        VirtualFile originalFile = LocalFileSystem.getInstance().findFileByPath(path);
        assertNotNull(originalFile);

        VirtualFile srcDir = getProject().getBaseDir().findChild(SRC_DIR_NAME);
        assertNotNull(srcDir);
        try {
            VfsUtilCore.copyFile(null, originalFile, srcDir);
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }

        ((NewVirtualFile)srcDir).markDirtyRecursively();
        srcDir.refresh(false, true);

        VirtualFile result = srcDir.findChild(originalFile.getName());
        assertNotNull(result);
        return result;
    }
}
