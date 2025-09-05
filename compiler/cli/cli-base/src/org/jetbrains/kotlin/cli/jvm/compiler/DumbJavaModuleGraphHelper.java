/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.jvm.compiler;

import com.intellij.psi.JavaModuleGraphHelper;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiJavaModule;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Set;

/**
 * An implementation of {@link JavaModuleGraphHelper} which does not support the Java platform module system:
 * no modules are defined, and everything is accessible.
 */
public final class DumbJavaModuleGraphHelper extends JavaModuleGraphHelper {
  @Override
  public @Nullable PsiJavaModule findDescriptorByElement(@Nullable PsiElement element) {
    return null;
  }

  @Override
  public @NotNull Set<PsiJavaModule> getAllTransitiveDependencies(@NotNull PsiJavaModule psiJavaModule) {
    return Collections.emptySet();
  }
}
