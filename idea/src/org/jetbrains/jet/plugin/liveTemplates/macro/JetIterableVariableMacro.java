package org.jetbrains.jet.plugin.liveTemplates.macro;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.VariableDescriptor;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.types.expressions.ExpressionTypingUtils;
import org.jetbrains.jet.plugin.JetBundle;

/**
 * @author Evgeny Gerashchenko
 * @since 2/7/12
 */
public class JetIterableVariableMacro extends BaseJetVariableMacro {
    @Override
    public String getName() {
        return "kotlinIterableVariable";
    }

    @Override
    public String getPresentableName() {
        return JetBundle.message("macro.iterable.variable");
    }

    @Override
    protected boolean isSuitable(@NotNull VariableDescriptor variableDescriptor, @NotNull JetScope scope, @NotNull Project project) {
        return ExpressionTypingUtils.isVariableIterable(project, variableDescriptor, scope);
    }
}
