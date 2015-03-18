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
import com.intellij.openapi.vfs.DeprecatedVirtualFileSystem;
import com.intellij.openapi.vfs.NonPhysicalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileSystem;
import com.intellij.util.LocalTimeCounter;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

/**
 * In-memory implementation of {@link VirtualFile}.
 */
@SuppressWarnings({"UnnecessaryFinalOnLocalVariableOrParameter", "unused"})
public abstract class LightVirtualFileBase extends VirtualFile {
    private FileType myFileType;
    private String myName = "";
    private long myModStamp = LocalTimeCounter.currentTime();
    private boolean myIsWritable = true;
    private boolean myValid = true;
    private VirtualFile myOriginalFile;

    public LightVirtualFileBase(final String name, final FileType fileType, final long modificationStamp) {
        myName = name;
        myFileType = fileType;
        myModStamp = modificationStamp;
    }

    public void setFileType(final FileType fileType) {
        myFileType = fileType;
    }

    public VirtualFile getOriginalFile() {
        return myOriginalFile;
    }

    public void setOriginalFile(VirtualFile originalFile) {
        myOriginalFile = originalFile;
    }

    private static class MyVirtualFileSystem extends DeprecatedVirtualFileSystem implements NonPhysicalFileSystem {
        @NonNls private static final String PROTOCOL = "mock";

        private MyVirtualFileSystem() {
            startEventPropagation();
        }

        @Override
        @NotNull
        public String getProtocol() {
            return PROTOCOL;
        }

        @Override
        @Nullable
        public VirtualFile findFileByPath(@NotNull String path) {
            return null;
        }

        @Override
        public void refresh(boolean asynchronous) {
        }

        @Override
        @Nullable
        public VirtualFile refreshAndFindFileByPath(@NotNull String path) {
            return null;
        }

        @Override
        public void deleteFile(Object requestor, @NotNull VirtualFile vFile) throws IOException {
        }

        @Override
        public void moveFile(Object requestor, @NotNull VirtualFile vFile, @NotNull VirtualFile newParent) throws IOException {
        }

        @NotNull
        @Override
        public VirtualFile copyFile(Object requestor,
                @NotNull VirtualFile vFile,
                @NotNull VirtualFile newParent,
                @NotNull final String copyName) throws IOException {
            throw new IOException("Cannot copy files");
        }

        @Override
        public void renameFile(Object requestor, @NotNull VirtualFile vFile, @NotNull String newName) throws IOException {
        }

        @NotNull
        @Override
        public VirtualFile createChildFile(Object requestor, @NotNull VirtualFile vDir, @NotNull String fileName) throws IOException {
            throw new IOException("Cannot create files");
        }

        @Override
        @NotNull
        public VirtualFile createChildDirectory(Object requestor, @NotNull VirtualFile vDir, @NotNull String dirName) throws IOException {
            throw new IOException("Cannot create directories");
        }
    }

    private static final MyVirtualFileSystem ourFileSystem = new MyVirtualFileSystem();

    @Override
    @NotNull
    public VirtualFileSystem getFileSystem() {
        return ourFileSystem;
    }

    @Nullable
    public FileType getAssignedFileType() {
        return myFileType;
    }

    @NotNull
    @Override
    public String getPath() {
        return "/" + getName();
    }

    @Override
    @NotNull
    public String getName() {
        return myName;
    }

    @Override
    public boolean isWritable() {
        return myIsWritable;
    }

    @Override
    public boolean isDirectory() {
        return false;
    }

    @Override
    public boolean isValid() {
        return myValid;
    }

    public void setValid(boolean valid) {
        myValid = valid;
    }

    @Override
    public VirtualFile getParent() {
        return null;
    }

    @Override
    public VirtualFile[] getChildren() {
        return EMPTY_ARRAY;
    }

    @Override
    public long getModificationStamp() {
        return myModStamp;
    }

    protected void setModificationStamp(long stamp) {
        myModStamp = stamp;
    }

    @Override
    public long getTimeStamp() {
        return 0; // todo[max] : Add UnsupportedOperationException at better times.
    }

    @Override
    public long getLength() {
        try {
            return contentsToByteArray().length;
        }
        catch (IOException e) {
            //noinspection CallToPrintStackTrace
            e.printStackTrace();
            assert false;
            return 0;
        }
    }

    @Override
    public void refresh(boolean asynchronous, boolean recursive, Runnable postRunnable) {
    }

    @Override
    public void setWritable(boolean b) {
        myIsWritable = b;
    }

    @Override
    public void rename(Object requestor, @NotNull String newName) throws IOException {
        myName = newName;
    }
}
