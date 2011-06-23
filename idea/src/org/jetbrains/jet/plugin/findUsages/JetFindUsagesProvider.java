package org.jetbrains.jet.plugin.findUsages;

import com.intellij.lang.cacheBuilder.WordsScanner;
import com.intellij.lang.findUsages.FindUsagesProvider;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiNamedElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.*;

/**
 * @author yole
 */
public class JetFindUsagesProvider implements FindUsagesProvider {
    @Override
    public boolean canFindUsagesFor(@NotNull PsiElement psiElement) {
        return psiElement instanceof JetNamedDeclaration;
    }

    @Override
    public WordsScanner getWordsScanner() {
        return new JetWordsScanner();
    }


    @Override
    public String getHelpId(@NotNull PsiElement psiElement) {
        return null;
    }

    @NotNull
    @Override
    public String getType(@NotNull PsiElement psiElement) {
        if (psiElement instanceof JetNamedFunction) {
            return "function";
        }
        if (psiElement instanceof JetClass) {
            return "class";
        }
        if (psiElement instanceof JetParameter) {
            return "parameter";
        }
        if (psiElement instanceof JetProperty) {
            return "property";
        }
        return "";
    }

    @NotNull
    @Override
    public String getDescriptiveName(@NotNull PsiElement psiElement) {
        if (psiElement instanceof PsiNamedElement) {
          final String name = ((PsiNamedElement)psiElement).getName();
          return name == null ? "<unnamed>" : name;
        }
        return "";
    }

    @NotNull
    @Override
    public String getNodeText(@NotNull PsiElement psiElement, boolean useFullName) {
        return getDescriptiveName(psiElement);
    }
}
