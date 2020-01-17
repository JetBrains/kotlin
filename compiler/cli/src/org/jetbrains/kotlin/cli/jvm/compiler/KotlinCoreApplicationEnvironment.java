/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package org.jetbrains.kotlin.cli.jvm.compiler;

import com.intellij.codeInsight.ContainerProvider;
import com.intellij.codeInsight.JavaContainerProvider;
import com.intellij.codeInsight.folding.JavaCodeFoldingSettings;
import com.intellij.codeInsight.folding.impl.JavaCodeFoldingSettingsBase;
import com.intellij.codeInsight.folding.impl.JavaFoldingBuilderBase;
import com.intellij.codeInsight.runner.JavaMainMethodProvider;
import com.intellij.core.CoreApplicationEnvironment;
import com.intellij.core.CoreJavaDirectoryService;
import com.intellij.core.CorePsiPackageImplementationHelper;
import com.intellij.ide.highlighter.ArchiveFileType;
import com.intellij.ide.highlighter.JavaClassFileType;
import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.lang.LanguageASTFactory;
import com.intellij.lang.MetaLanguage;
import com.intellij.lang.folding.LanguageFolding;
import com.intellij.lang.java.JavaLanguage;
import com.intellij.lang.java.JavaParserDefinition;
import com.intellij.navigation.ItemPresentationProviders;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.extensions.Extensions;
import com.intellij.openapi.extensions.ExtensionsArea;
import com.intellij.openapi.fileTypes.FileTypeExtensionPoint;
import com.intellij.openapi.fileTypes.PlainTextFileType;
import com.intellij.openapi.fileTypes.PlainTextLanguage;
import com.intellij.openapi.fileTypes.PlainTextParserDefinition;
import com.intellij.openapi.projectRoots.JavaVersionService;
import com.intellij.openapi.util.KeyWithDefaultValue;
import com.intellij.openapi.vfs.VirtualFileSystem;
import com.intellij.psi.*;
import com.intellij.psi.augment.PsiAugmentProvider;
import com.intellij.psi.augment.TypeAnnotationModifier;
import com.intellij.psi.compiled.ClassFileDecompilers;
import com.intellij.psi.impl.EmptySubstitutorImpl;
import com.intellij.psi.impl.LanguageConstantExpressionEvaluator;
import com.intellij.psi.impl.PsiExpressionEvaluator;
import com.intellij.psi.impl.compiled.ClassFileStubBuilder;
import com.intellij.psi.impl.file.PsiPackageImplementationHelper;
import com.intellij.psi.impl.search.MethodSuperSearcher;
import com.intellij.psi.impl.source.tree.JavaASTFactory;
import com.intellij.psi.impl.source.tree.PlainTextASTFactory;
import com.intellij.psi.meta.MetaDataContributor;
import com.intellij.psi.presentation.java.*;
import com.intellij.psi.search.searches.SuperMethodsSearch;
import com.intellij.psi.stubs.BinaryFileStubBuilders;
import com.intellij.util.QueryExecutor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.cli.jvm.modules.CoreJrtFileSystem;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

/**
 * adapted from com.intellij.core.JavaCoreApplicationEnvironment
 * TODO: initiate removal original from com.intellij.core since it seems that there are no usages left
 */
public class KotlinCoreApplicationEnvironment extends CoreApplicationEnvironment {

  public static KotlinCoreApplicationEnvironment create(@NotNull Disposable parentDisposable, boolean unitTestMode) {
    return new KotlinCoreApplicationEnvironment(parentDisposable, unitTestMode);
  }

  private KotlinCoreApplicationEnvironment(@NotNull Disposable parentDisposable, boolean unitTestMode) {
    super(parentDisposable, unitTestMode);

    registerExtensionPoints();

    registerExtensions();
  }

  private void registerExtensionPoints() {
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

  private void registerExtensions() {
    registerFileType(JavaClassFileType.INSTANCE, "class");
    registerFileType(JavaFileType.INSTANCE, "java");
    registerFileType(ArchiveFileType.INSTANCE, "jar;zip");
    registerFileType(PlainTextFileType.INSTANCE, "txt;sh;bat;cmd;policy;log;cgi;MF;jad;jam;htaccess");

    addExplicitExtension(LanguageASTFactory.INSTANCE, PlainTextLanguage.INSTANCE, new PlainTextASTFactory());
    registerParserDefinition(new PlainTextParserDefinition());

    addExplicitExtension(FileTypeFileViewProviders.INSTANCE, JavaClassFileType.INSTANCE, new ClassFileViewProviderFactory());
    addExplicitExtension(BinaryFileStubBuilders.INSTANCE, JavaClassFileType.INSTANCE, new ClassFileStubBuilder());

    addExplicitExtension(LanguageASTFactory.INSTANCE, JavaLanguage.INSTANCE, new JavaASTFactory());
    registerParserDefinition(new JavaParserDefinition());
    addExplicitExtension(LanguageConstantExpressionEvaluator.INSTANCE, JavaLanguage.INSTANCE, new PsiExpressionEvaluator());

    addExtension(ContainerProvider.EP_NAME, new JavaContainerProvider());

    myApplication.registerService(PsiPackageImplementationHelper.class, new CorePsiPackageImplementationHelper());

    EmptySubstitutorImpl emptySubstitutor = new EmptySubstitutorImpl();
    myApplication.registerService(EmptySubstitutor.class, emptySubstitutor);

    // Patch null values obtained because of cyclic dependency during initialization
    updateInterfaceField(PsiSubstitutor.class, "EMPTY", emptySubstitutor);
    updateInterfaceField(PsiSubstitutor.class, "UNKNOWN", PsiSubstitutor.EMPTY);

    myApplication.registerService(JavaDirectoryService.class, createJavaDirectoryService());
    myApplication.registerService(JavaVersionService.class, new JavaVersionService());

    addExplicitExtension(ItemPresentationProviders.INSTANCE, PsiPackage.class, new PackagePresentationProvider());
    addExplicitExtension(ItemPresentationProviders.INSTANCE, PsiClass.class, new ClassPresentationProvider());
    addExplicitExtension(ItemPresentationProviders.INSTANCE, PsiMethod.class, new MethodPresentationProvider());
    addExplicitExtension(ItemPresentationProviders.INSTANCE, PsiField.class, new FieldPresentationProvider());
    addExplicitExtension(ItemPresentationProviders.INSTANCE, PsiLocalVariable.class, new VariablePresentationProvider());
    addExplicitExtension(ItemPresentationProviders.INSTANCE, PsiParameter.class, new VariablePresentationProvider());

    registerApplicationService(JavaCodeFoldingSettings.class, new JavaCodeFoldingSettingsBase());
    addExplicitExtension(LanguageFolding.INSTANCE, JavaLanguage.INSTANCE, new JavaFoldingBuilderBase() {
      @Override
      protected boolean shouldShowExplicitLambdaType(@NotNull PsiAnonymousClass anonymousClass, @NotNull PsiNewExpression expression) {
        return false;
      }

      @Override
      protected boolean isBelowRightMargin(@NotNull PsiFile file, int lineLength) {
        return false;
      }
    });

    registerApplicationExtensionPoint(SuperMethodsSearch.EP_NAME, QueryExecutor.class);
    addExtension(SuperMethodsSearch.EP_NAME, new MethodSuperSearcher());
  }

  // overridden in upsource
  protected CoreJavaDirectoryService createJavaDirectoryService() {
    return new CoreJavaDirectoryService();
  }

  @Nullable
  @Override
  protected VirtualFileSystem createJrtFileSystem() {
    return new CoreJrtFileSystem();
  }

  private static void updateInterfaceField(Class<?> klass, String name, Object value) {
    try {
      Field field = klass.getDeclaredField(name);

      boolean wasAccessible = field.isAccessible();

      try {
        if (!wasAccessible) {
          field.setAccessible(true);
        }

        Field modifiersField = Field.class.getDeclaredField("modifiers");

        int modifiers = field.getModifiers();

        try {
          modifiersField.setAccessible(true);
          modifiersField.setInt(field, modifiers & ~Modifier.FINAL);

          field.set(null, value);
        }
        finally {
          modifiersField.setInt(field, modifiers);
          modifiersField.setAccessible(false);
        }
      }
      finally {
        if (!wasAccessible) {
          field.setAccessible(false);
        }
      }
    }
    catch (NoSuchFieldException | IllegalAccessException e) {
      throw new IllegalStateException(e);
    }
  }
}