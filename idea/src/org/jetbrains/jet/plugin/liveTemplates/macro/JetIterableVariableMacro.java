package org.jetbrains.jet.plugin.liveTemplates.macro;

import com.intellij.codeInsight.template.Expression;
import com.intellij.codeInsight.template.ExpressionContext;
import com.intellij.codeInsight.template.Macro;
import com.intellij.codeInsight.template.Result;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.plugin.JetBundle;

/**
 * @author Evgeny Gerashchenko
 * @since 2/7/12
 */
public class JetIterableVariableMacro extends Macro {
    @Override
    public String getName() {
        return "kotlinIterableVariable";
    }

    @Override
    public String getPresentableName() {
        return JetBundle.message("macro.iterable.variable");
    }

    @Override
    public Result calculateResult(@NotNull Expression[] params, ExpressionContext context) {
        // TODO
        return null;
    }
}
