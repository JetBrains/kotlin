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

package org.jetbrains.jet.plugin.filters;

import com.intellij.execution.filters.Filter;
import com.intellij.execution.filters.OpenFileHyperlinkInfo;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.refactoring.MultiFileTestCase;
import com.intellij.testFramework.PsiTestUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.resolve.java.PackageClassUtils;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.plugin.PluginTestCaseBase;
import org.jetbrains.jet.utils.ExceptionUtils;

import java.io.File;

public class JetExceptionFilterTest extends MultiFileTestCase {
    private final JetExceptionFilterFactory jetExceptionFilterFactory = new JetExceptionFilterFactory();

    private VirtualFile rootDir;

    @Override
    @NotNull
    protected String getTestDataPath() {
        return PluginTestCaseBase.getTestDataPathBase();
    }

    @Override
    @NotNull
    protected String getTestRoot() {
        return "/filters/exceptionFilter/";
    }

    private void configure() {
        try {
            String path = getTestDataPath() + getTestRoot() + getTestName(true);

            rootDir = PsiTestUtil.createTestProjectStructure(myProject, myModule, path, myFilesToDelete, false);
            prepareProject(rootDir);
            PsiDocumentManager.getInstance(myProject).commitAllDocuments();
        }
        catch (Exception e) {
            ExceptionUtils.rethrow(e);
        }
    }

    @NotNull
    private String createStackTraceElementLine(@NotNull String fileName, @NotNull String className, int lineNumber) {
        // Method name doesn't matter
        String methodName = "foo";

        // File's last name appears in stack trace
        String fileLastName = new File(fileName).getName();

        StackTraceElement element = new StackTraceElement(className, methodName, fileLastName, lineNumber);
        return "\tat " + element + "\n";
    }

    private void doTest(@NotNull String fileName, @NotNull String className, int lineNumber) {
        if (rootDir == null) {
            configure();
        }
        assert rootDir != null;

        Filter filter = jetExceptionFilterFactory.create(GlobalSearchScope.allScope(myProject));

        String line = createStackTraceElementLine(fileName, className, lineNumber);
        Filter.Result result = filter.applyFilter(line, 0);

        assertNotNull(result);
        assertInstanceOf(result.hyperlinkInfo, OpenFileHyperlinkInfo.class);
        OpenFileHyperlinkInfo info = (OpenFileHyperlinkInfo) result.hyperlinkInfo;
        OpenFileDescriptor descriptor = info.getDescriptor();
        assertNotNull(descriptor);

        VirtualFile expectedFile = VfsUtil.findRelativeFile(fileName, rootDir);
        assertNotNull(expectedFile);
        assertEquals(expectedFile, descriptor.getFile());

        Document document = FileDocumentManager.getInstance().getDocument(expectedFile);
        assertNotNull(document);
        int expectedOffset = document.getLineStartOffset(lineNumber - 1);
        assertEquals(expectedOffset, descriptor.getOffset());
    }

    public void testSimple() {
        doTest("simple.kt", PackageClassUtils.getPackageClassName(FqName.ROOT), 2);
    }

    public void testKt2489() {
        doTest("a.kt", PackageClassUtils.getPackageClassName(FqName.ROOT) + "$a$f$1", 3);
        doTest("main.kt", PackageClassUtils.getPackageClassName(FqName.ROOT) + "$main$f$1", 3);
    }

    public void testMultiSameName() {
        // The order and the exact names do matter here
        doTest("1/foo.kt", "multiSameName." + PackageClassUtils.getPackageClassName(new FqName("multiSameName")) + "$foo$f$1", 4);
        doTest("2/foo.kt", "multiSameName." + PackageClassUtils.getPackageClassName(new FqName("multiSameName")) + "$foo$f$2", 4);
    }
}
