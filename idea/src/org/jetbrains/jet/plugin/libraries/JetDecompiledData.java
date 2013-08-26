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

package org.jetbrains.jet.plugin.libraries;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.psi.JetDeclaration;
import org.jetbrains.jet.lang.psi.JetFile;

import java.util.Map;

@SuppressWarnings("FieldAccessedSynchronizedAndUnsynchronized")
public class JetDecompiledData {

    private static final Key<JetDecompiledData> USER_DATA_KEY = new Key<JetDecompiledData>("USER_DATA_KEY");
    private static final Object LOCK = new String("decompiled data lock");

    @NotNull
    private final JetFile file;
    @NotNull
    private final Map<String, TextRange> renderedDescriptorsToRanges;

    JetDecompiledData(@NotNull JetFile file, @NotNull Map<String,TextRange> renderedDescriptorsToRanges) {
        this.file = file;
        this.renderedDescriptorsToRanges = renderedDescriptorsToRanges;
    }

    @NotNull
    public String getFileText() {
        return file.getText();
    }

    @NotNull
    public static JetDecompiledData getDecompiledData(@NotNull VirtualFile virtualFile, @NotNull Project project) {
        synchronized (LOCK) {
            JetDecompiledData cachedData = virtualFile.getUserData(USER_DATA_KEY);
            //TODO: use cached value that keeps modification tracking for this virtual file
            if (cachedData == null || cachedData.getProject() != project) {
                virtualFile.putUserData(USER_DATA_KEY, DecompiledDataFactory.createDecompiledData(virtualFile, project));
            }
            JetDecompiledData decompiledData = virtualFile.getUserData(USER_DATA_KEY);
            assert decompiledData != null;
            return decompiledData;
        }
    }

    @TestOnly
    @NotNull
    public Map<String, TextRange> getRenderedDescriptorsToRanges() {
        return renderedDescriptorsToRanges;
    }

    @Nullable
    public JetDeclaration getDeclarationForDescriptor(@NotNull DeclarationDescriptor descriptor) {
        String key = descriptorToKey(descriptor);
        TextRange range = renderedDescriptorsToRanges.get(key);
        if (range == null) {
            return null;
        }
        return PsiTreeUtil.findElementOfClassAtRange(file, range.getStartOffset(), range.getEndOffset(), JetDeclaration.class);
    }

    //TODO: should use more accurate way to identify descriptors
    @NotNull
    static String descriptorToKey(@NotNull DeclarationDescriptor descriptor) {
        return DecompiledDataFactory.DESCRIPTOR_RENDERER.render(descriptor);
    }

    @NotNull
    public JetFile getFile() {
        return file;
    }

    @NotNull
    public Project getProject() {
        return file.getProject();
    }
}
