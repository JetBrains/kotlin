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
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.EditorKind;
import com.intellij.openapi.editor.event.EditorEventMulticaster;
import com.intellij.openapi.editor.event.EditorFactoryListener;
import com.intellij.openapi.editor.impl.DocumentImpl;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.text.CharArrayCharSequence;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MockEditorFactory extends EditorFactory {
  @Override
  public Editor createEditor(@NotNull Document document) {
    return null;
  }

  @Override
  public Editor createViewer(@NotNull Document document) {
    return null;
  }

  @Override
  public Editor createEditor(@NotNull Document document, Project project) {
    return null;
  }

  @Override
  public Editor createEditor(@NotNull Document document, Project project, @NotNull VirtualFile file, boolean isViewer) {
    return null;
  }

  @Override
  public Editor createEditor(@NotNull Document document, Project project, @NotNull FileType fileType, boolean isViewer) {
    return null;
  }

  @Override
  public Editor createViewer(@NotNull Document document, Project project) {
    return null;
  }

  @Override
  public Editor createEditor(
          @NotNull Document document, @Nullable Project project, @NotNull EditorKind kind
  ) {
    return null;
  }

  @Override
  public Editor createEditor(
          @NotNull Document document, Project project, @NotNull VirtualFile file, boolean isViewer, @NotNull EditorKind kind
  ) {
    return null;
  }

  @Override
  public Editor createViewer(
          @NotNull Document document, @Nullable Project project, @NotNull EditorKind kind
  ) {
    return null;
  }

  @Override
  public void releaseEditor(@NotNull Editor editor) {
  }

  @Override
  @NotNull
  public Editor[] getEditors(@NotNull Document document, Project project) {
    return Editor.EMPTY_ARRAY;
  }

  @Override
  @NotNull
  public Editor[] getEditors(@NotNull Document document) {
    return getEditors(document, null);
  }

  @Override
  @NotNull
  public Editor[] getAllEditors() {
    return Editor.EMPTY_ARRAY;
  }

  @Override
  public void addEditorFactoryListener(@NotNull EditorFactoryListener listener) {
  }

  @Override
  public void addEditorFactoryListener(@NotNull EditorFactoryListener listener, @NotNull Disposable parentDisposable) {
  }

  @Override
  public void removeEditorFactoryListener(@NotNull EditorFactoryListener listener) {
  }

  @Override
  @NotNull
  public EditorEventMulticaster getEventMulticaster() {
    return new MockEditorEventMulticaster();
  }

  @Override
  @NotNull
  public Document createDocument(@NotNull CharSequence text) {
    return new DocumentImpl(text);
  }

  @Override
  @NotNull
  public Document createDocument(@NotNull char[] text) {
    return createDocument(new CharArrayCharSequence(text));
  }

  @Override
  public void refreshAllEditors() {
  }

}
