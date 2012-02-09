package org.jetbrains.jet.plugin.liveTemplates.macro;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.VariableDescriptor;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.plugin.JetBundle;

/**
 * @author Evgeny Gerashchenko
 * @since 1/30/12
 */
public class JetAnyVariableMacro extends BaseJetVariableMacro {
    @Override
    public String getName() {
        return "kotlinVariable";
    }

    @Override
    public String getPresentableName() {
        return JetBundle.message("macro.variable.of.type");
    }

    @Override
    protected boolean isSuitable(@NotNull VariableDescriptor variableDescriptor, @NotNull JetScope scope, @NotNull Project project) {
        return true;
    }
}
                          