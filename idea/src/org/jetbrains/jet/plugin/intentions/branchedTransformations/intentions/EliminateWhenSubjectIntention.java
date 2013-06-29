package org.jetbrains.jet.plugin.intentions.branchedTransformations.intentions;

import com.google.common.base.Predicate;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.psi.JetWhenExpression;
import org.jetbrains.jet.plugin.intentions.AbstractCodeTransformationIntention;
import org.jetbrains.jet.plugin.intentions.branchedTransformations.WhenUtils;
import org.jetbrains.jet.plugin.intentions.Transformer;

public class EliminateWhenSubjectIntention extends AbstractCodeTransformationIntention<Transformer> {
    private static final Transformer TRANSFORMER = new Transformer() {
        @NotNull
        @Override
        public String getKey() {
            return "eliminate.when.subject";
        }

        @Override
        public void transform(@NotNull PsiElement element, @NotNull Editor editor, @NotNull JetFile file) {
            WhenUtils.eliminateWhenSubject((JetWhenExpression) element);
        }
    };

    private static final Predicate<PsiElement> IS_APPLICABLE = new Predicate<PsiElement>() {
        @Override
        public boolean apply(@Nullable PsiElement input) {
            return input instanceof JetWhenExpression && WhenUtils.checkEliminateWhenSubject((JetWhenExpression) input);
        }
    };

    public EliminateWhenSubjectIntention() {
        super(TRANSFORMER, IS_APPLICABLE);
    }
}
