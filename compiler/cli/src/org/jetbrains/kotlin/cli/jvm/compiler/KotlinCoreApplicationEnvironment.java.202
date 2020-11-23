/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.cli.jvm.compiler;

import com.intellij.DynamicBundle;
import com.intellij.codeInsight.ContainerProvider;
import com.intellij.codeInsight.runner.JavaMainMethodProvider;
import com.intellij.core.JavaCoreApplicationEnvironment;
import com.intellij.lang.MetaLanguage;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.extensions.Extensions;
import com.intellij.openapi.vfs.VirtualFileSystem;
import com.intellij.psi.FileContextProvider;
import com.intellij.psi.augment.PsiAugmentProvider;
import com.intellij.psi.compiled.ClassFileDecompilers;
import com.intellij.psi.meta.MetaDataContributor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.cli.jvm.modules.CoreJrtFileSystem;

public class KotlinCoreApplicationEnvironment extends JavaCoreApplicationEnvironment {
    public static KotlinCoreApplicationEnvironment create(@NotNull Disposable parentDisposable, boolean unitTestMode) {
        KotlinCoreApplicationEnvironment environment = new KotlinCoreApplicationEnvironment(parentDisposable, unitTestMode);
        registerExtensionPoints();
        return environment;
    }

    private KotlinCoreApplicationEnvironment(@NotNull Disposable parentDisposable, boolean unitTestMode) {
        super(parentDisposable, unitTestMode);
    }

    private static void registerExtensionPoints() {
        registerApplicationExtensionPoint(DynamicBundle.LanguageBundleEP.EP_NAME, DynamicBundle.LanguageBundleEP.class);
        registerApplicationExtensionPoint(FileContextProvider.EP_NAME, FileContextProvider.class);

        registerApplicationExtensionPoint(MetaDataContributor.EP_NAME, MetaDataContributor.class);
        registerApplicationExtensionPoint(PsiAugmentProvider.EP_NAME, PsiAugmentProvider.class);
        registerApplicationExtensionPoint(JavaMainMethodProvider.EP_NAME, JavaMainMethodProvider.class);

        registerApplicationExtensionPoint(ContainerProvider.EP_NAME, ContainerProvider.class);
        registerApplicationExtensionPoint(ClassFileDecompilers.getInstance().EP_NAME, ClassFileDecompilers.Decompiler.class);

        registerApplicationExtensionPoint(MetaLanguage.EP_NAME, MetaLanguage.class);

        IdeaExtensionPoints.INSTANCE.registerVersionSpecificAppExtensionPoints(Extensions.getRootArea());
    }

    @Nullable
    @Override
    protected VirtualFileSystem createJrtFileSystem() {
        return new CoreJrtFileSystem();
    }
}