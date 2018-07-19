package org.jetbrains.konan;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.impl.PsiFileFactoryImpl;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.stubs.PsiFileStub;
import com.intellij.testFramework.LightVirtualFile;
import kotlin.Pair;
import kotlin.jvm.functions.Function1;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.analyzer.ModuleContent;
import org.jetbrains.kotlin.analyzer.ModuleInfo;
import org.jetbrains.kotlin.context.ModuleContext;
import org.jetbrains.kotlin.idea.KotlinFileType;
import org.jetbrains.kotlin.idea.KotlinLanguage;
import org.jetbrains.kotlin.idea.caches.project.LibraryInfo;
import org.jetbrains.kotlin.idea.caches.project.ModuleProductionSourceInfo;
import org.jetbrains.kotlin.idea.decompiler.KotlinDecompiledFileViewProvider;
import org.jetbrains.kotlin.idea.decompiler.textBuilder.DecompiledText;
import org.jetbrains.kotlin.idea.decompiler.textBuilder.LoggingErrorReporter;
import org.jetbrains.kotlin.idea.references.KtReference;
import org.jetbrains.kotlin.psi.KtBlockExpression;
import org.jetbrains.kotlin.psi.KtExpressionCodeFragment;
import org.jetbrains.kotlin.psi.KtFile;
import org.jetbrains.kotlin.psi.KtPsiFactory;
import org.jetbrains.kotlin.psi.psiUtil.PsiUtilsKt;
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes;
import org.jetbrains.kotlin.resolve.lazy.declarations.DeclarationProviderFactory;
import org.jetbrains.kotlin.resolve.lazy.declarations.DeclarationProviderFactoryService;

import java.util.Collection;

public class KotlinWorkaroundUtil {
  @NotNull
  public static PsiFileStub createFileStub(@NotNull Project project, @NotNull String text) {
    final LightVirtualFile virtualFile = new LightVirtualFile("dummy.kt", KotlinFileType.INSTANCE, text);
    virtualFile.setLanguage(KotlinLanguage.INSTANCE);
    SingleRootFileViewProvider.doNotCheckFileSizeLimit(virtualFile);

    PsiFileFactoryImpl psiFileFactory = (PsiFileFactoryImpl)PsiFileFactory.getInstance(project);
    PsiFile file = psiFileFactory.trySetupPsiForFile(virtualFile, KotlinLanguage.INSTANCE, false, false);
    assert file != null;
    return (PsiFileStub)KtStubElementTypes.FILE.getBuilder().buildStubTree(file);
  }

  @NotNull
  public static KtExpressionCodeFragment createCodeFragment(@NotNull Project project,
                                                            @NotNull String referenceText,
                                                            @NotNull KtBlockExpression blockExpression) {
    return new KtPsiFactory(project, false).createExpressionCodeFragment(referenceText, blockExpression);
  }

  @Nullable
  public static PsiElement resolveMainReference(@NotNull KtReference mainReference) {
    return mainReference.resolve();
  }

  @Nullable
  public static KtBlockExpression getNonStrictParentOfType(@Nullable PsiElement contextElement) {
    if (contextElement == null) return null;
    return PsiUtilsKt.getNonStrictParentOfType(contextElement, KtBlockExpression.class);
  }

  @NotNull
  public static LoggingErrorReporter createLoggingErrorReporter(@NotNull Logger log) {
    return new LoggingErrorReporter(log);
  }

  @NotNull
  public static FileViewProvider createDecompiledFileViewProvider(@NotNull PsiManager manager,
                                                                  @NotNull VirtualFile file,
                                                                  boolean physical,
                                                                  @NotNull Function1<VirtualFile, DecompiledText> buildDecompiledText) {
    return new KotlinDecompiledFileViewProvider(manager, file, physical,
                                                provider -> new KonanDecompiledFile(provider, buildDecompiledText));
  }

  @NotNull
  public static Project getProject(@NotNull ModuleContext moduleContext) {
    return moduleContext.getProject();
  }

  @NotNull
  public static <M extends ModuleInfo> Pair<Collection<KtFile>, GlobalSearchScope> destructModuleContent(@NotNull ModuleContent<M> moduleContent) {
    return new Pair<>(moduleContent.component2(), moduleContent.component3());
  }

  @NotNull
  public static <M extends ModuleInfo> DeclarationProviderFactory createDeclarationProviderFactory(@NotNull Project project,
                                                                                                   @NotNull ModuleContext moduleContext,
                                                                                                   @NotNull Collection<KtFile> syntheticFiles,
                                                                                                   @NotNull M moduleInfo,
                                                                                                   @Nullable GlobalSearchScope globalSearchScope) {
    return DeclarationProviderFactoryService.createDeclarationProviderFactory(
      project,
      moduleContext.getStorageManager(),
      syntheticFiles,
      globalSearchScope,
      moduleInfo);
  }

  @NotNull
  public static LibraryInfo createLibraryInfo(@NotNull Project project, @NotNull Library library) {
    return new LibraryInfo(project, library);
  }

  @NotNull
  public static Module getModule(@NotNull ModuleProductionSourceInfo sourceInfo) {
    return sourceInfo.getModule();
  }
}
