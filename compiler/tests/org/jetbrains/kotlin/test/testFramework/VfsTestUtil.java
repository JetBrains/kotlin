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

package org.jetbrains.kotlin.test.testFramework;

import com.intellij.openapi.application.AccessToken;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.PathUtil;
import com.intellij.util.text.StringTokenizer;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;

import java.io.File;
import java.io.IOException;

public class VfsTestUtil {
    public static final Key<String> TEST_DATA_FILE_PATH = Key.create("TEST_DATA_FILE_PATH");

    private VfsTestUtil() {
    }

    public static VirtualFile createFile(final VirtualFile root, final String relativePath) {
        return createFile(root, relativePath, "");
    }

    public static VirtualFile createFile(final VirtualFile root, final String relativePath, final String text) {
        return createFileOrDir(root, relativePath, text, false);
    }

    public static VirtualFile createDir(final VirtualFile root, final String relativePath) {
        return createFileOrDir(root, relativePath, "", true);
    }

    private static VirtualFile createFileOrDir(final VirtualFile root,
            final String relativePath,
            final String text,
            final boolean dir) {
        try {
            AccessToken token = WriteAction.start();
            try {
                VirtualFile parent = root;
                Assert.assertNotNull(parent);
                StringTokenizer parents = new StringTokenizer(PathUtil.getParentPath(relativePath), "/");
                while (parents.hasMoreTokens()) {
                    final String name = parents.nextToken();
                    VirtualFile child = parent.findChild(name);
                    if (child == null || !child.isValid()) {
                        child = parent.createChildDirectory(VfsTestUtil.class, name);
                    }
                    parent = child;
                }

                VirtualFile file;
                parent.getChildren();//need this to ensure that fileCreated event is fired
                if (dir) {
                    file = parent.createChildDirectory(VfsTestUtil.class, PathUtil.getFileName(relativePath));
                }
                else {
                    file = parent.findFileByRelativePath(relativePath);
                    if (file == null) {
                        file = parent.createChildData(VfsTestUtil.class, PathUtil.getFileName(relativePath));
                    }
                    VfsUtil.saveText(file, text);
                }
                return file;
            }
            finally {
                token.finish();
            }
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void deleteFile(@NotNull VirtualFile file) {
        UtilKt.deleteFile(file);
    }

    public static void clearContent(@NotNull final VirtualFile file) {
        ApplicationManager.getApplication().runWriteAction(new Runnable() {
            @Override
            public void run() {
                try {
                    VfsUtil.saveText(file, "");
                }
                catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    @SuppressWarnings("UnusedDeclaration")
    public static void overwriteTestData(String filePath, String actual) {
        try {
            FileUtil.writeToFile(new File(filePath), actual);
        }
        catch (IOException e) {
            throw new AssertionError(e);
        }
    }

    @NotNull
    public static VirtualFile findFileByCaseSensitivePath(@NotNull String absolutePath) {
        String vfsPath = FileUtil.toSystemIndependentName(absolutePath);
        VirtualFile vFile = LocalFileSystem.getInstance().findFileByPath(vfsPath);
        Assert.assertNotNull("file " + absolutePath + " not found", vFile);
        String realVfsPath = vFile.getPath();
        if (!SystemInfo.isFileSystemCaseSensitive && !vfsPath.equals(realVfsPath) &&
            vfsPath.equalsIgnoreCase(realVfsPath)) {
            Assert.fail("Please correct case-sensitivity of path to prevent test failure on case-sensitive file systems:\n" +
                        "     path " + vfsPath + "\n" +
                        "real path " + realVfsPath);
        }
        return vFile;
    }

    public static void assertFilePathEndsWithCaseSensitivePath(@NotNull VirtualFile file, @NotNull String suffixPath) {
        String vfsSuffixPath = FileUtil.toSystemIndependentName(suffixPath);
        String vfsPath = file.getPath();
        if (!SystemInfo.isFileSystemCaseSensitive && !vfsPath.endsWith(vfsSuffixPath) &&
            StringUtil.endsWithIgnoreCase(vfsPath, vfsSuffixPath)) {
            String realSuffixPath = vfsPath.substring(vfsPath.length() - vfsSuffixPath.length());
            Assert.fail("Please correct case-sensitivity of path to prevent test failure on case-sensitive file systems:\n" +
                        "     path " + suffixPath + "\n" +
                        "real path " + realSuffixPath);
        }
    }
}