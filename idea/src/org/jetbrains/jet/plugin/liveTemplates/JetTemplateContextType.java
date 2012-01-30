package org.jetbrains.jet.plugin.liveTemplates;

import com.intellij.codeInsight.template.EverywhereContextType;
import com.intellij.codeInsight.template.TemplateContextType;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiUtilBase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.plugin.JetLanguage;

/**
 * @author Evgeny Gerashchenko
 * @since 1/27/12
 */
public class JetTemplateContextType extends TemplateContextType {
    public JetTemplateContextType() {
        super("KOTLIN", "Kotlin", EverywhereContextType.class);
    }

    @Override
    public boolean isInContext(@NotNull PsiFile file, int offset) {
        return PsiUtilBase.getLanguageAtOffset(file, offset).isKindOf(JetLanguage.INSTANCE);
    }
}
