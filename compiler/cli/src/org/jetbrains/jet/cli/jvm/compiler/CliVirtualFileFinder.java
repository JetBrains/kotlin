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

package org.jetbrains.jet.cli.jvm.compiler;

import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.resolve.kotlin.*;
import org.jetbrains.jet.lang.resolve.kotlin.header.KotlinClassHeader;
import org.jetbrains.jet.lang.resolve.name.FqName;

public class CliVirtualFileFinder extends VirtualFileKotlinClassFinder implements VirtualFileFinder {

    @NotNull
    private final ClassPath classPath;

    public CliVirtualFileFinder(@NotNull ClassPath path) {
        classPath = path;
    }

    @Nullable
    @Override
    public VirtualFile findVirtualFile(@NotNull FqName className) {
        for (VirtualFile root : classPath) {
            VirtualFile fileInRoot = findFileInRoot(className.asString(), root);
            if (fileInRoot != null) {
                return fileInRoot;
            }
        }
        return null;
    }

    //NOTE: copied with some changes from CoreJavaFileManager
    @Nullable
    private VirtualFile findFileInRoot(@NotNull String qName, @NotNull VirtualFile root) {
        String pathRest = qName;
        VirtualFile cur = root;

        while (true) {
            int dot = pathRest.indexOf('.');
            if (dot < 0) break;

            String pathComponent = pathRest.substring(0, dot);
            VirtualFile child = cur.findChild(pathComponent);

            if (child == null) break;
            pathRest = pathRest.substring(dot + 1);
            cur = child;
        }

        String className = pathRest.replace('.', '$');
        VirtualFile vFile = cur.findChild(className + ".class");
        if (vFile != null) {
            if (!vFile.isValid()) {
                //TODO: log
                return null;
            }
            //NOTE: currently we use VirtualFileFinder to find Kotlin binaries only
            if (KotlinClassHeader.read(createKotlinClass(vFile)) != null) {
                return vFile;
            }
        }
        return null;
    }
}
