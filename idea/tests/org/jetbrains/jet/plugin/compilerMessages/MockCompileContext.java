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

package org.jetbrains.jet.plugin.compilerMessages;

import com.google.common.collect.Lists;
import com.intellij.compiler.make.DependencyCache;
import com.intellij.mock.MockProgressIndicator;
import com.intellij.openapi.compiler.CompileScope;
import com.intellij.openapi.compiler.Compiler;
import com.intellij.openapi.compiler.CompilerMessage;
import com.intellij.openapi.compiler.CompilerMessageCategory;
import com.intellij.openapi.compiler.ex.CompileContextEx;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.pom.Navigatable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public class MockCompileContext implements CompileContextEx {

    @NotNull
    private final List<Message> receivedMessages = Lists.newArrayList();
    @NotNull
    private final Module module;
    @NotNull
    private final VirtualFile outputDirectory;
    @NotNull
    private final VirtualFile sourceRoot;

    public MockCompileContext(@NotNull Module module, @NotNull VirtualFile outputDir, @NotNull VirtualFile root) {
        this.module = module;
        this.outputDirectory = outputDir;
        this.sourceRoot = root;
    }

    @NotNull
    public List<Message> getReceivedMessages() {
        return receivedMessages;
    }

    @Override
    public DependencyCache getDependencyCache() {
        throw new UnsupportedOperationException("org.jetbrains.jet.plugin.compilerMessages.MockCompileContext#getDependencyCache");
    }

    @Override
    public VirtualFile getSourceFileByOutputFile(VirtualFile outputFile) {
        throw new UnsupportedOperationException("org.jetbrains.jet.plugin.compilerMessages.MockCompileContext#getSourceFileByOutputFile");
    }

    @Override
    public void addMessage(CompilerMessage message) {
        throw new UnsupportedOperationException("org.jetbrains.jet.plugin.compilerMessages.MockCompileContext#addMessage");
    }

    @NotNull
    @Override
    public Set<VirtualFile> getTestOutputDirectories() {
        throw new UnsupportedOperationException("org.jetbrains.jet.plugin.compilerMessages.MockCompileContext#getTestOutputDirectories");
    }

    @Override
    public boolean isInTestSourceContent(@NotNull VirtualFile fileOrDir) {
        return false;
    }

    @Override
    public boolean isInSourceContent(@NotNull VirtualFile fileOrDir) {
        throw new UnsupportedOperationException("org.jetbrains.jet.plugin.compilerMessages.MockCompileContext#isInSourceContent");
    }

    @Override
    public void addScope(CompileScope additionalScope) {
        throw new UnsupportedOperationException("org.jetbrains.jet.plugin.compilerMessages.MockCompileContext#addScope");
    }

    @Override
    public long getStartCompilationStamp() {
        throw new UnsupportedOperationException("org.jetbrains.jet.plugin.compilerMessages.MockCompileContext#getStartCompilationStamp");
    }

    @Override
    public void recalculateOutputDirs() {
        throw new UnsupportedOperationException("org.jetbrains.jet.plugin.compilerMessages.MockCompileContext#recalculateOutputDirs");
    }

    @Override
    public void markGenerated(Collection<VirtualFile> files) {
        throw new UnsupportedOperationException("org.jetbrains.jet.plugin.compilerMessages.MockCompileContext#markGenerated");
    }

    @Override
    public boolean isGenerated(VirtualFile file) {
        throw new UnsupportedOperationException("org.jetbrains.jet.plugin.compilerMessages.MockCompileContext#isGenerated");
    }

    @Override
    public void assignModule(@NotNull VirtualFile root,
            @NotNull Module module,
            boolean isTestSource,
            @Nullable Compiler compiler) {
        throw new UnsupportedOperationException("org.jetbrains.jet.plugin.compilerMessages.MockCompileContext#assignModule");
    }

    @Override
    public void addMessage(CompilerMessageCategory category, String message, @Nullable String url, int lineNum, int columnNum) {
        receivedMessages.add(new Message(category, message, url, lineNum, columnNum));
    }

    @Override
    public void addMessage(CompilerMessageCategory category,
            String message,
            @Nullable String url,
            int lineNum,
            int columnNum,
            Navigatable navigatable) {
        throw new UnsupportedOperationException("org.jetbrains.jet.plugin.compilerMessages.MockCompileContext#addMessage");
    }

    @Override
    public CompilerMessage[] getMessages(CompilerMessageCategory category) {
        throw new UnsupportedOperationException("org.jetbrains.jet.plugin.compilerMessages.MockCompileContext#getMessages");
    }

    @Override
    public int getMessageCount(CompilerMessageCategory category) {
        throw new UnsupportedOperationException("org.jetbrains.jet.plugin.compilerMessages.MockCompileContext#getMessageCount");
    }

    @NotNull
    @Override
    public ProgressIndicator getProgressIndicator() {
        return new MockProgressIndicator();
    }

    @Override
    public CompileScope getCompileScope() {
        throw new UnsupportedOperationException("org.jetbrains.jet.plugin.compilerMessages.MockCompileContext#getCompileScope");
    }

    @Override
    public CompileScope getProjectCompileScope() {
        throw new UnsupportedOperationException("org.jetbrains.jet.plugin.compilerMessages.MockCompileContext#getProjectCompileScope");
    }

    @Override
    public void requestRebuildNextTime(String message) {
        throw new UnsupportedOperationException("org.jetbrains.jet.plugin.compilerMessages.MockCompileContext#requestRebuildNextTime");
    }

    @Override
    public boolean isRebuildRequested() {
        throw new UnsupportedOperationException("org.jetbrains.jet.plugin.compilerMessages.MockCompileContext.isRebuildRequested");
    }

    @Nullable
    @Override
    public String getRebuildReason() {
        throw new UnsupportedOperationException("org.jetbrains.jet.plugin.compilerMessages.MockCompileContext.getRebuildReason");
    }

    @Override
    public Module getModuleByFile(VirtualFile file) {
        return module;
    }

    @Override
    public VirtualFile[] getSourceRoots(Module module) {
        return new VirtualFile[] {sourceRoot};
    }

    @Override
    public VirtualFile[] getAllOutputDirectories() {
        throw new UnsupportedOperationException("org.jetbrains.jet.plugin.compilerMessages.MockCompileContext#getAllOutputDirectories");
    }

    @Override
    public VirtualFile getModuleOutputDirectory(Module module) {
        return outputDirectory;
    }

    @Override
    public VirtualFile getModuleOutputDirectoryForTests(Module module) {
        return outputDirectory;
    }

    @Override
    public boolean isMake() {
        throw new UnsupportedOperationException("org.jetbrains.jet.plugin.compilerMessages.MockCompileContext#isMake");
    }

    @Override
    public boolean isRebuild() {
        throw new UnsupportedOperationException("org.jetbrains.jet.plugin.compilerMessages.MockCompileContext#isRebuild");
    }

    @Override
    public Project getProject() {
        return module.getProject();
    }

    @Override
    public boolean isAnnotationProcessorsEnabled() {
        throw new UnsupportedOperationException(
                "org.jetbrains.jet.plugin.compilerMessages.MockCompileContext#isAnnotationProcessorsEnabled");
    }

    @Override
    public <T> T getUserData(@NotNull Key<T> key) {
        throw new UnsupportedOperationException("org.jetbrains.jet.plugin.compilerMessages.MockCompileContext#getUserData");
    }

    @Override
    public <T> void putUserData(@NotNull Key<T> key, @Nullable T value) {
        throw new UnsupportedOperationException("org.jetbrains.jet.plugin.compilerMessages.MockCompileContext#putUserData");
    }
}