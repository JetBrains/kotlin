/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.decompiler.navigation.files;

import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.ArrayUtil;
import com.intellij.util.LocalTimeCounter;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * In-memory implementation of {@link VirtualFile}.
 */
@SuppressWarnings({"UnnecessaryFinalOnLocalVariableOrParameter", "unused"})
public class BinaryLightVirtualFile extends LightVirtualFileBase {
    private byte[] myContent = ArrayUtil.EMPTY_BYTE_ARRAY;

    public BinaryLightVirtualFile() {
        this("");
    }

    public BinaryLightVirtualFile(@NonNls String name) {
        this(name, ArrayUtil.EMPTY_BYTE_ARRAY);
    }

    public BinaryLightVirtualFile(@NonNls String name, byte[] content) {
        this(name, null, content, LocalTimeCounter.currentTime());
    }

    public BinaryLightVirtualFile(final String name, final FileType fileType, final byte[] content) {
        this(name, fileType, content, LocalTimeCounter.currentTime());
    }

    public BinaryLightVirtualFile(VirtualFile original, final byte[] content, long modificationStamp) {
        this(original.getName(), original.getFileType(), content, modificationStamp);
    }

    public BinaryLightVirtualFile(final String name,
            final FileType fileType,
            final byte[] content,
            final long modificationStamp) {
        super(name, fileType, modificationStamp);
        setContent(content);
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return VfsUtilCore.byteStreamSkippingBOM(myContent, this);
    }

    @Override
    @NotNull
    public OutputStream getOutputStream(Object requestor, final long newModificationStamp, long newTimeStamp) throws IOException {
        return VfsUtilCore.outputStreamAddingBOM(new ByteArrayOutputStream() {
            @Override
            public void close() {
                setModificationStamp(newModificationStamp);

                byte[] content = toByteArray();
                setContent(content);
            }
        }, this);
    }

    @Override
    @NotNull
    public byte[] contentsToByteArray() throws IOException {
        return myContent;
    }

    public void setContent(Object requestor, byte[] content, boolean fireEvent) {
        setContent(content);
        setModificationStamp(LocalTimeCounter.currentTime());
    }

    private void setContent(byte[] content) {
        //StringUtil.assertValidSeparators(content);
        myContent = content;
    }

    public byte[] getContent() {
        return myContent;
    }

    @Override
    public String toString() {
        return "BinaryLightVirtualFile: " + getPresentableUrl();
    }
}