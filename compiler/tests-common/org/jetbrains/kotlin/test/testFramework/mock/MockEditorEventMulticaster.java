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
import com.intellij.openapi.editor.event.*;
import org.jetbrains.annotations.NotNull;

public class MockEditorEventMulticaster implements EditorEventMulticaster {
  public MockEditorEventMulticaster() {
  }

  @Override
  public void addDocumentListener(@NotNull DocumentListener listener) {
  }

  @Override
  public void addDocumentListener(@NotNull DocumentListener listener, @NotNull Disposable parentDisposable) {
      }

  @Override
  public void removeDocumentListener(@NotNull DocumentListener listener) {
  }

  @Override
  public void addEditorMouseListener(@NotNull EditorMouseListener listener) {
  }

  @Override
  public void addEditorMouseListener(@NotNull EditorMouseListener listener, @NotNull Disposable parentDisposable) {
  }

  @Override
  public void removeEditorMouseListener(@NotNull EditorMouseListener listener) {
  }

  @Override
  public void addEditorMouseMotionListener(@NotNull EditorMouseMotionListener listener) {
  }

  @Override
  public void addEditorMouseMotionListener(@NotNull EditorMouseMotionListener listener, @NotNull Disposable parentDisposable) {
  }

  @Override
  public void removeEditorMouseMotionListener(@NotNull EditorMouseMotionListener listener) {
  }

  @Override
  public void addCaretListener(@NotNull CaretListener listener) {
  }

  @Override
  public void addCaretListener(@NotNull CaretListener listener, @NotNull Disposable parentDisposable) {
  }

  @Override
  public void removeCaretListener(@NotNull CaretListener listener) {
  }

  @Override
  public void addSelectionListener(@NotNull SelectionListener listener) {
  }

  @Override
  public void addSelectionListener(@NotNull SelectionListener listener, @NotNull Disposable parentDisposable) {
  }

  @Override
  public void removeSelectionListener(@NotNull SelectionListener listener) {
  }

  @Override
  public void addVisibleAreaListener(@NotNull VisibleAreaListener listener) {
  }

  @Override
  public void removeVisibleAreaListener(@NotNull VisibleAreaListener listener) {
  }

}
