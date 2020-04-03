/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.cli.jvm.compiler;

import com.intellij.codeInsight.ContainerProvider;
import com.intellij.codeInsight.runner.JavaMainMethodProvider;
import com.intellij.core.CoreApplicationEnvironment;
import com.intellij.core.JavaCoreApplicationEnvironment;
import com.intellij.lang.MetaLanguage;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.extensions.Extensions;
import com.intellij.openapi.extensions.ExtensionsArea;
import com.intellij.openapi.fileTypes.FileTypeExtensionPoint;
import com.intellij.openapi.vfs.VirtualFileSystem;
import com.intellij.psi.FileContextProvider;
import com.intellij.psi.augment.PsiAugmentProvider;
import com.intellij.psi.augment.TypeAnnotationModifier;
import com.intellij.psi.compiled.ClassFileDecompilers;
import com.intellij.psi.meta.MetaDataContributor;
import com.intellij.psi.stubs.BinaryFileStubBuilders;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.cli.jvm.modules.CoreJrtFileSystem;

// Mostly a placeholder for the functionality added to the bunch 193
public class KotlinCoreApplicationEnvironment extends JavaCoreApplicationEnvironment {

  public static KotlinCoreApplicationEnvironment create(@NotNull Disposable parentDisposable, boolean unitTestMode) {
    Extensions.cleanRootArea(parentDisposable);
    registerExtensionPoints();
    return new KotlinCoreApplicationEnvironment(parentDisposable, unitTestMode);
  }

  private KotlinCoreApplicationEnvironment(@NotNull Disposable parentDisposable, boolean unitTestMode) {
    super(parentDisposable, unitTestMode);
  }

  private static void registerExtensionPoints() {
    ExtensionsArea area = Extensions.getRootArea();

    CoreApplicationEnvironment.registerExtensionPoint(area, BinaryFileStubBuilders.EP_NAME, FileTypeExtensionPoint.class);
    CoreApplicationEnvironment.registerExtensionPoint(area, FileContextProvider.EP_NAME, FileContextProvider.class);

    CoreApplicationEnvironment.registerExtensionPoint(area, MetaDataContributor.EP_NAME, MetaDataContributor.class);
    CoreApplicationEnvironment.registerExtensionPoint(area, PsiAugmentProvider.EP_NAME, PsiAugmentProvider.class);
    CoreApplicationEnvironment.registerExtensionPoint(area, JavaMainMethodProvider.EP_NAME, JavaMainMethodProvider.class);

    CoreApplicationEnvironment.registerExtensionPoint(area, ContainerProvider.EP_NAME, ContainerProvider.class);
    CoreApplicationEnvironment.registerExtensionPoint(area, ClassFileDecompilers.EP_NAME, ClassFileDecompilers.Decompiler.class);

    CoreApplicationEnvironment.registerExtensionPoint(area, TypeAnnotationModifier.EP_NAME, TypeAnnotationModifier.class);
    CoreApplicationEnvironment.registerExtensionPoint(area, MetaLanguage.EP_NAME, MetaLanguage.class);

    IdeaExtensionPoints.INSTANCE.registerVersionSpecificAppExtensionPoints(area);
  }

  @Nullable
  @Override
  protected VirtualFileSystem createJrtFileSystem() {
    return new CoreJrtFileSystem();
  }
}