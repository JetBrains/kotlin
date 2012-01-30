package org.jetbrains.jet.plugin.liveTemplates;

import com.intellij.codeInsight.template.EverywhereContextType;
import com.intellij.codeInsight.template.TemplateContextType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.psi.util.PsiUtilBase;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.plugin.JetLanguage;

/**
 * @author Evgeny Gerashchenko
 * @since 1/27/12
 */
public abstract class JetTemplateContextType extends TemplateContextType {
    protected JetTemplateContextType(@NotNull @NonNls String id, @NotNull String presentableName, @Nullable Class<? extends TemplateContextType> baseContextType) {
        super(id, presentableName, baseContextType);
    }

    @Override
    public boolean isInContext(@NotNull PsiFile file, int offset) {
        if (PsiUtilBase.getLanguageAtOffset(file, offset).isKindOf(JetLanguage.INSTANCE)) {
            PsiElement element = file.findElementAt(offset);
            if (element instanceof PsiWhiteSpace) {

            }
            return element != null && isInContext(element);
        }

        return false;
    }

    protected abstract boolean isInContext(@NotNull PsiElement element);

    public static class Generic extends JetTemplateContextType {
        public Generic() {
            super("KOTLIN", "Kotlin", EverywhereContextType.class);
        }

        @Override
        protected boolean isInContext(@NotNull PsiElement element) {
            return true;
        }
    }

    public static class Namespace extends JetTemplateContextType {
        public Namespace() {
            super("KOTLIN_NAMESPACE", "Namespace", Generic.class);
        }

        @Override
        protected boolean isInContext(@NotNull PsiElement element) {
            PsiElement e = element;
            while (e != null) {
                if (e instanceof JetProperty || e instanceof JetNamedFunction
                    || e instanceof JetClassOrObject) {
                    return false;
                }
                e = e.getParent();
            }
            return true;
        }
    }

    public static class Expression extends JetTemplateContextType {
        public Expression() {
            super("KOTLIN_EXPRESSION", "Expression", Generic.class);
        }

        @Override
        protected boolean isInContext(@NotNull PsiElement element) {
            PsiElement parent = element.getParent();
            if (parent instanceof JetSimpleNameExpression) {
                parent = parent.getParent();
            }
            return parent instanceof JetBlockExpression;
        }
    }
}
