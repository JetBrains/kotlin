package org.jetbrains.jet.plugin.findUsages;

import com.intellij.psi.ElementDescriptionLocation;
import com.intellij.psi.ElementDescriptionProvider;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiNamedElement;
import com.intellij.usageView.UsageViewLongNameLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.JetElement;

/**
 * @author yole
 */
public class JetElementDescriptionProvider implements ElementDescriptionProvider {
    @Override
    public String getElementDescription(@NotNull PsiElement element, @NotNull ElementDescriptionLocation location) {
        if (location instanceof UsageViewLongNameLocation) {
          if (element instanceof PsiNamedElement && element instanceof JetElement) {
            return ((PsiNamedElement)element).getName();
          }
        }
        return null;
    }
}
