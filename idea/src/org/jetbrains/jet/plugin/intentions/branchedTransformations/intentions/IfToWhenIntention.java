package org.jetbrains.jet.plugin.intentions.branchedTransformations.intentions;

import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiElement;
import jet.Function1;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.psi.JetIfExpression;
import org.jetbrains.jet.plugin.intentions.AbstractCodeTransformationIntention;
import org.jetbrains.jet.plugin.intentions.branchedTransformations.IfWhenUtils;
import org.jetbrains.jet.plugin.intentions.Transformer;

public class IfToWhenIntention extends AbstractCodeTransformationIntention {
    private static final Transformer TRANSFORMER = new Transformer() {
        @NotNull
        @Override
        public String getKey() {
            return "if.to.when";
        }

        @Override
        public void transform(@NotNull PsiElement element, @NotNull Editor editor, @NotNull JetFile file) {
            IfWhenUtils.transformIfToWhen((JetIfExpression) element);
        }
    };

    private static final Function1<PsiElement, Boolean> IS_APPLICABLE = new Function1<PsiElement, Boolean>() {
        @Override
        public Boolean invoke(@Nullable PsiElement input) {
            return (input instanceof JetIfExpression) && IfWhenUtils.checkIfToWhen((JetIfExpression) input);
        }
    };

    public IfToWhenIntention() {
        super(TRANSFORMER, IS_APPLICABLE);
    }
}
