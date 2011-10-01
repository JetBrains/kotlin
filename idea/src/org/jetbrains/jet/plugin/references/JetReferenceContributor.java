package org.jetbrains.jet.plugin.references;

import com.intellij.psi.*;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.JetArrayAccessExpression;
import org.jetbrains.jet.lang.psi.JetSimpleNameExpression;
import org.jetbrains.jet.lang.psi.JetThisReferenceExpression;

import static com.intellij.patterns.PlatformPatterns.psiElement;

/**
 * @author yole
 */
public class JetReferenceContributor extends PsiReferenceContributor {
    @Override
    public void registerReferenceProviders(PsiReferenceRegistrar registrar) {
        registrar.registerReferenceProvider(psiElement(JetSimpleNameExpression.class),
                                            new PsiReferenceProvider() {
                                                @NotNull
                                                @Override
                                                public PsiReference[] getReferencesByElement(@NotNull PsiElement element, @NotNull ProcessingContext processingContext) {
                                                    return new PsiReference[] { new JetSimpleNameReference((JetSimpleNameExpression) element) };
                                                }
                                            });

        registrar.registerReferenceProvider(psiElement(JetThisReferenceExpression.class),
                                            new PsiReferenceProvider() {
                                                @NotNull
                                                @Override
                                                public PsiReference[] getReferencesByElement(@NotNull PsiElement element, @NotNull ProcessingContext processingContext) {
                                                    return new PsiReference[] { new JetThisReference((JetThisReferenceExpression) element) };
                                                }
                                            });

        registrar.registerReferenceProvider(psiElement(JetArrayAccessExpression.class),
                                            new PsiReferenceProvider() {
                                                @NotNull
                                                @Override
                                                public PsiReference[] getReferencesByElement(@NotNull PsiElement element, @NotNull ProcessingContext processingContext) {
                                                    return JetArrayAccessReference.create((JetArrayAccessExpression) element);
                                                }
                                            });
    }
}
