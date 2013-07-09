package org.jetbrains.jet.plugin.intentions.branchedTransformations.intentions;

import com.google.common.base.Predicate;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.psi.JetIfExpression;
import org.jetbrains.jet.plugin.intentions.AbstractCodeTransformationIntention;
import org.jetbrains.jet.plugin.intentions.branchedTransformations.IfWhenUtils;
import org.jetbrains.jet.plugin.intentions.Transformer;

public class IfToWhenIntention extends AbstractCodeTransformationIntention<Transformer> {
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

    private static final Predicate<PsiElement> IS_APPLICABLE = new Predicate<PsiElement>() {
        @Override
        public boolean apply(@Nullable PsiElement input) {
            return (input instanceof JetIfExpression) && IfWhenUtils.checkIfToWhen((JetIfExpression) input);
        }
    };

    public IfToWhenIntention() {
        super(TRANSFORMER, IS_APPLICABLE);
    }
}
