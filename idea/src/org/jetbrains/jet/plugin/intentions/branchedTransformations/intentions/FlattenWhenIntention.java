package org.jetbrains.jet.plugin.intentions.branchedTransformations.intentions;

import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiElement;
import jet.Function1;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.psi.JetWhenExpression;
import org.jetbrains.jet.plugin.intentions.AbstractCodeTransformationIntention;
import org.jetbrains.jet.plugin.intentions.branchedTransformations.WhenUtils;
import org.jetbrains.jet.plugin.intentions.Transformer;

public class FlattenWhenIntention extends AbstractCodeTransformationIntention {
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

    private static final Function1<PsiElement, Boolean> IS_APPLICABLE = new Function1<PsiElement, Boolean>() {
        @Override
        public Boolean invoke(@Nullable PsiElement input) {
            return input instanceof JetWhenExpression && WhenUtils.checkFlattenWhen((JetWhenExpression) input);
        }
    };

    public FlattenWhenIntention() {
        super(TRANSFORMER, IS_APPLICABLE);
    }
}
