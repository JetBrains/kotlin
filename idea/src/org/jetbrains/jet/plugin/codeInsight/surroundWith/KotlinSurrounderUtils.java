package org.jetbrains.jet.plugin.codeInsight.surroundWith;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.psi.JetBlockExpression;

public class KotlinSurrounderUtils {

    public static void addStatementsInBlock(
            @NotNull JetBlockExpression block,
            @NotNull PsiElement[] statements
    ) {
        PsiElement lBrace = block.getFirstChild();
        block.addRangeAfter(statements[0], statements[statements.length - 1], lBrace);
    }
}
