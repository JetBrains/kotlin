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

package org.jetbrains.kotlin.test.testFramework.mock;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.impl.PsiManagerEx;
import com.intellij.psi.impl.PsiModificationTrackerImpl;
import com.intellij.psi.impl.PsiTreeChangeEventImpl;
import com.intellij.psi.impl.file.impl.FileManager;
import com.intellij.psi.util.PsiModificationTracker;
import gnu.trove.THashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class MockPsiManager extends PsiManagerEx {
  private final Project myProject;
  private final Map<VirtualFile,PsiDirectory> myDirectories = new THashMap<VirtualFile, PsiDirectory>();
  private MockFileManager myMockFileManager;
  private PsiModificationTrackerImpl myPsiModificationTracker;

  public MockPsiManager(@NotNull Project project) {
    myProject = project;
  }

  @SuppressWarnings("unused")
  public void addPsiDirectory(VirtualFile file, PsiDirectory psiDirectory) {
    myDirectories.put(file, psiDirectory);
  }

  @Override
  @NotNull
  public Project getProject() {
    return myProject;
  }

  @Override
  public PsiFile findFile(@NotNull VirtualFile file) {
    return null;
  }

  @Override
  @Nullable
  public
  FileViewProvider findViewProvider(@NotNull VirtualFile file) {
    return null;
  }

  @Override
  public PsiDirectory findDirectory(@NotNull VirtualFile file) {
    return myDirectories.get(file);
  }

  @Override
  public boolean areElementsEquivalent(PsiElement element1, PsiElement element2) {
    return Comparing.equal(element1, element2);
  }

  @Override
  public void reloadFromDisk(@NotNull PsiFile file) {
  }

  @Override
  public void addPsiTreeChangeListener(@NotNull PsiTreeChangeListener listener) {
  }

  @Override
  public void addPsiTreeChangeListener(@NotNull PsiTreeChangeListener listener, @NotNull Disposable parentDisposable) {
  }

  @Override
  public void removePsiTreeChangeListener(@NotNull PsiTreeChangeListener listener) {
  }

  @Override
  @NotNull
  public PsiModificationTracker getModificationTracker() {
    if (myPsiModificationTracker == null) {
      myPsiModificationTracker = new PsiModificationTrackerImpl(myProject);
    }
    return myPsiModificationTracker;
  }

  @Override
  public void startBatchFilesProcessingMode() {
  }

  @Override
  public void finishBatchFilesProcessingMode() {
  }

  @Override
  public <T> T getUserData(@NotNull Key<T> key) {
    return null;
  }

  @Override
  public <T> void putUserData(@NotNull Key<T> key, T value) {
  }

  @Override
  public boolean isDisposed() {
    return false;
  }

  @Override
  public void dropResolveCaches() {
    getFileManager().cleanupForNextTest();
  }

  @Override
  public boolean isInProject(@NotNull PsiElement element) {
    return false;
  }

  @Override
  public boolean isBatchFilesProcessingMode() {
    return false;
  }

  @Override
  public boolean isAssertOnFileLoading(@NotNull VirtualFile file) {
    return false;
  }

  @Override
  public void beforeChange(boolean isPhysical) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void afterChange(boolean isPhysical) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void registerRunnableToRunOnChange(@NotNull Runnable runnable) {
  }

  @Override
  public void registerRunnableToRunOnAnyChange(@NotNull Runnable runnable) {
  }

  @Override
  public void registerRunnableToRunAfterAnyChange(@NotNull Runnable runnable) {
    throw new UnsupportedOperationException("Method registerRunnableToRunAfterAnyChange is not yet implemented in " + getClass().getName());
  }

  @Override
  @NotNull
  public FileManager getFileManager() {
    if (myMockFileManager == null) {
      myMockFileManager = new MockFileManager(this);
    }
    return myMockFileManager;
  }

  @Override
  public void beforeChildRemoval(@NotNull PsiTreeChangeEventImpl event) {
  }

  @Override
  public void beforeChildReplacement(@NotNull PsiTreeChangeEventImpl event) {
  }

  @Override
  public void beforeChildAddition(@NotNull PsiTreeChangeEventImpl event) {
  }
}
