/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
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
import org.jetbrains.kotlin.test.KotlinTestUtils;
import org.jetbrains.kotlin.test.WithMutedInDatabaseRunTest;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.Collection;

@WithMutedInDatabaseRunTest
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

    @Override
    protected void runTest() throws Throwable {
        //noinspection Convert2MethodRef
        KotlinTestUtils.runTestWithThrowable(this, () -> super.runTest());
    }

    protected boolean isFirPlugin() {
        return false;
    }
}
