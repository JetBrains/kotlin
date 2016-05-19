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
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.PathUtil;
import com.intellij.util.text.StringTokenizer;
import org.junit.Assert;

import java.io.File;
import java.io.IOException;

public class VfsTestUtil {
    private VfsTestUtil() {
    }

    public static VirtualFile createFile(VirtualFile root, String relativePath) {
        return createFile(root, relativePath, "");
    }

    public static VirtualFile createFile(VirtualFile root, String relativePath, String text) {
        return createFileOrDir(root, relativePath, text, false);
    }

    private static VirtualFile createFileOrDir(
            VirtualFile root,
            String relativePath,
            String text,
            boolean dir) {
        try {
            AccessToken token = WriteAction.start();
            try {
                VirtualFile parent = root;
                Assert.assertNotNull(parent);
                StringTokenizer parents = new StringTokenizer(PathUtil.getParentPath(relativePath), "/");
                while (parents.hasMoreTokens()) {
                    String name = parents.nextToken();
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

    @SuppressWarnings("UnusedDeclaration")
    public static void overwriteTestData(String filePath, String actual) {
        try {
            FileUtil.writeToFile(new File(filePath), actual);
        }
        catch (IOException e) {
            throw new AssertionError(e);
        }
    }
}