/*
 * Copyright 2010-2012 JetBrains s.r.o.
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

package org.jetbrains.jet.plugin.stubindex;

import com.intellij.openapi.fileTypes.StdFileTypes;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.search.DelegatingGlobalSearchScope;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.plugin.JetFileType;

/**
 * @author Nikolay Krasko
 */
public class JetSourceFilterScope extends DelegatingGlobalSearchScope {
    private final ProjectFileIndex myIndex;

    public JetSourceFilterScope(@NotNull final GlobalSearchScope delegate) {
        super(delegate);
        myIndex = ProjectRootManager.getInstance(getProject()).getFileIndex();
    }

    @Override
    public boolean contains(final VirtualFile file) {
        if (!super.contains(file)) {
            return false;
        }

        if (StdFileTypes.CLASS == file.getFileType()) {
            return myIndex.isInLibraryClasses(file);
        }

        return file.getFileType().equals(JetFileType.INSTANCE) && (myIndex.isInSourceContent(file) || myIndex.isInLibrarySource(file));
    }
}
