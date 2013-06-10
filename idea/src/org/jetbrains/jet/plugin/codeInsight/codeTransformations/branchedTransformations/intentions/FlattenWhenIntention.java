package org.jetbrains.jet.plugin.codeInsight.codeTransformations.branchedTransformations.intentions;

import com.google.common.base.Predicate;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.psi.JetWhenExpression;
import org.jetbrains.jet.plugin.codeInsight.codeTransformations.AbstractCodeTransformationIntention;
import org.jetbrains.jet.plugin.codeInsight.codeTransformations.branchedTransformations.WhenUtils;
import org.jetbrains.jet.plugin.codeInsight.codeTransformations.Transformer;

public class FlattenWhenIntention extends AbstractCodeTransformationIntention<Transformer> {
    private static final Transformer TRANSFORMER = new Transformer() {
        @NotNull
        @Override
        public String getKey() {
            return "flatten.when";
        }

        @Override
        public void transform(@NotNull PsiElement element, @NotNull Editor editor, @NotNull JetFile file) {
            WhenUtils.flattenWhen((JetWhenExpression) element);
        }
    };

    private static final Predicate<PsiElement> IS_APPLICABLE = new Predicate<PsiElement>() {
        @Override
        public boolean apply(@Nullable PsiElement input) {
            return input instanceof JetWhenExpression && WhenUtils.checkFlattenWhen((JetWhenExpression) input);
        }
    };

    public FlattenWhenIntention() {
        super(TRANSFORMER, IS_APPLICABLE);
    }
}
