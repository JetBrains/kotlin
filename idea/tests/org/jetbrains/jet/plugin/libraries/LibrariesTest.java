/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.PlatformTestCase;
import org.jetbrains.jet.cli.KotlinCompiler;
import org.jetbrains.jet.plugin.PluginTestCaseBase;

/**
 * @author Evgeny Gerashchenko
 * @since 3/11/12
 */
public class LibrariesTest extends PlatformTestCase {

    private static final String PACKAGE = "testData.libraries";
    private static final String TEST_DATA_PATH = PluginTestCaseBase.getTestDataPathBase() + "/libraries";
    private VirtualFile myLibraryDir;

    public void testAbstractClass() {
        doTest();
    }

    private void doTest() {
        String testName = getTestName(false);
        VirtualFile packageDir = myLibraryDir.findFileByRelativePath(PACKAGE.replace(".", "/"));
        assertNotNull(packageDir);
        VirtualFile classFile = packageDir.findChild(testName + ".class");
        assertNotNull(classFile);
        Document document = FileDocumentManager.getInstance().getDocument(classFile);
        assert document != null;
        String decompiledText = document.getText();
        assertSameLinesWithFile(TEST_DATA_PATH + "/" + testName + ".kt", decompiledText);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        String libraryDir = FileUtil.getTempDirectory();
        KotlinCompiler.exec("-src", TEST_DATA_PATH + "/library", "-output", libraryDir);
        myLibraryDir = LocalFileSystem.getInstance().findFileByPath(libraryDir);
        assertNotNull(myLibraryDir);

        ApplicationManager.getApplication().runWriteAction(new Runnable() {
            @Override
            public void run() {
                ModifiableRootModel moduleModel = ModuleRootManager.getInstance(myModule).getModifiableModel();

                Library.ModifiableModel libraryModel = moduleModel.getModuleLibraryTable().getModifiableModel().createLibrary("myKotlinLib").getModifiableModel();
                libraryModel.addRoot(myLibraryDir, OrderRootType.CLASSES);
                libraryModel.commit();

                moduleModel.commit();
            }
        });
    }
}
