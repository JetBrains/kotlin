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

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

public class MockPsiDocumentManager extends PsiDocumentManager {
  @Override
  @Nullable
  public PsiFile getPsiFile(@NotNull Document document) {
    throw new UnsupportedOperationException("Method getPsiFile is not yet implemented in " + getClass().getName());
  }

  @Override
  @Nullable
  public PsiFile getCachedPsiFile(@NotNull Document document) {
    throw new UnsupportedOperationException("Method getCachedPsiFile is not yet implemented in " + getClass().getName());
  }

  @Override
  @Nullable
  public Document getDocument(@NotNull PsiFile file) {
    return null;
  }

  @Override
  @Nullable
  public Document getCachedDocument(@NotNull PsiFile file) {
    VirtualFile vFile = file.getViewProvider().getVirtualFile();
    return FileDocumentManager.getInstance().getCachedDocument(vFile);
  }

  @Override
  public void commitAllDocuments() {
  }

  @Override
  public void performForCommittedDocument(@NotNull Document document, @NotNull Runnable action) {
    action.run();
  }

  @Override
  public void commitDocument(@NotNull Document document) {
  }

  @NotNull
  @Override
  public CharSequence getLastCommittedText(@NotNull Document document) {
    return document.getImmutableCharSequence();
  }

  @Override
  public long getLastCommittedStamp(@NotNull Document document) {
    return document.getModificationStamp();
  }

  @Nullable
  @Override
  public Document getLastCommittedDocument(@NotNull PsiFile file) {
    return null;
  }

  @Override
  @NotNull
  public Document[] getUncommittedDocuments() {
    throw new UnsupportedOperationException("Method getUncommittedDocuments is not yet implemented in " + getClass().getName());
  }

  @Override
  public boolean isUncommited(@NotNull Document document) {
    throw new UnsupportedOperationException("Method isUncommited is not yet implemented in " + getClass().getName());
  }

  @Override
  public boolean isCommitted(@NotNull Document document) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean hasUncommitedDocuments() {
    throw new UnsupportedOperationException("Method hasUncommitedDocuments is not yet implemented in " + getClass().getName());
  }

  @Override
  public void commitAndRunReadAction(@NotNull Runnable runnable) {
    throw new UnsupportedOperationException("Method commitAndRunReadAction is not yet implemented in " + getClass().getName());
  }

  @Override
  public <T> T commitAndRunReadAction(@NotNull Computable<T> computation) {
    throw new UnsupportedOperationException("Method commitAndRunReadAction is not yet implemented in " + getClass().getName());
  }

  @Override
  public void addListener(@NotNull Listener listener) {
    throw new UnsupportedOperationException("Method addListener is not yet implemented in " + getClass().getName());
  }

  @Override
  public void removeListener(@NotNull Listener listener) {
    throw new UnsupportedOperationException("Method removeListener is not yet implemented in " + getClass().getName());
  }

  @Override
  public boolean isDocumentBlockedByPsi(@NotNull Document doc) {
    throw new UnsupportedOperationException("Method isDocumentBlockedByPsi is not yet implemented in " + getClass().getName());
  }

  @Override
  public void doPostponedOperationsAndUnblockDocument(@NotNull Document doc) {
    throw new UnsupportedOperationException(
      "Method doPostponedOperationsAndUnblockDocument is not yet implemented in " + getClass().getName());
  }

  @Override
  public boolean performWhenAllCommitted(@NotNull Runnable action) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void performLaterWhenAllCommitted(@NotNull Runnable runnable) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void reparseFiles(@NotNull Collection<VirtualFile> files, boolean includeOpenFiles) {
    throw new UnsupportedOperationException();
  }
}
