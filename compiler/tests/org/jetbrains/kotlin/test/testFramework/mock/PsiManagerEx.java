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

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.impl.PsiTreeChangeEventImpl;
import com.intellij.psi.impl.file.impl.FileManager;
import org.jetbrains.annotations.NotNull;

public abstract class PsiManagerEx extends PsiManager {
  public abstract boolean isBatchFilesProcessingMode();

  public abstract boolean isAssertOnFileLoading(@NotNull VirtualFile file);

  /**
   * @param runnable to be run before <b>physical</b> PSI change
   */
  public abstract void registerRunnableToRunOnChange(@NotNull Runnable runnable);

  /**
   * @param runnable to be run before <b>physical</b> or <b>non-physical</b> PSI change
   */
  public abstract void registerRunnableToRunOnAnyChange(@NotNull Runnable runnable);

  public abstract void registerRunnableToRunAfterAnyChange(@NotNull Runnable runnable);

  @NotNull
  public abstract FileManager getFileManager();

  public abstract void beforeChildAddition(@NotNull PsiTreeChangeEventImpl event);

  public abstract void beforeChildRemoval(@NotNull PsiTreeChangeEventImpl event);

  public abstract void beforeChildReplacement(@NotNull PsiTreeChangeEventImpl event);

  public abstract void beforeChange(boolean isPhysical);

  public abstract void afterChange(boolean isPhysical);
}
