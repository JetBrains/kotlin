/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.test;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.testFramework.TempFiles;
import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase;
import gnu.trove.THashSet;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.Collection;

import static org.jetbrains.kotlin.test.MuteWithDatabaseKt.isIgnoredInDatabaseWithLog;

public abstract class KotlinLightCodeInsightFixtureTestCaseBase extends LightCodeInsightFixtureTestCase {
    @NotNull
    @Override
    public Project getProject() {
        return super.getProject();
    }

    @NotNull
    @Override
    public Editor getEditor() {
        return super.getEditor();
    }

    @Override
    public PsiFile getFile() {
        return super.getFile();
    }

    protected final Collection<File> myFilesToDelete = new THashSet<>();
    private final TempFiles myTempFiles = new TempFiles(myFilesToDelete);

    @Override
    protected boolean shouldRunTest() {
        return super.shouldRunTest() && !isIgnoredInDatabaseWithLog(this);
    }

    @Override
    protected void tearDown() throws Exception {
        myTempFiles.deleteAll();
        super.tearDown();
    }

    @NotNull
    public VirtualFile createTempFile(
            @NonNls @NotNull String ext,
            @Nullable byte[] bom,
            @NonNls @NotNull String content,
            @NotNull Charset charset
    ) throws IOException {
        File temp = FileUtil.createTempFile("copy", "." + ext);
        setContentOnDisk(temp, bom, content, charset);

        myFilesToDelete.add(temp);
        final VirtualFile file = getVirtualFile(temp);
        assert file != null : temp;
        return file;
    }

    public static void setContentOnDisk(@NotNull File file, @Nullable byte[] bom, @NotNull String content, @NotNull Charset charset)
            throws IOException {
        FileOutputStream stream = new FileOutputStream(file);
        if (bom != null) {
            stream.write(bom);
        }
        try (OutputStreamWriter writer = new OutputStreamWriter(stream, charset)) {
            writer.write(content);
        }
    }

    protected static VirtualFile getVirtualFile(@NotNull File file) {
        return LocalFileSystem.getInstance().refreshAndFindFileByIoFile(file);
    }

}
