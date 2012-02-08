package org.jetbrains.jet.plugin.liveTemplates.macro;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.VariableDescriptor;
import org.jetbrains.jet.plugin.JetBundle;

/**
 * @author Evgeny Gerashchenko
 * @since 1/30/12
 */
public class JetVariableOfTypeMacro extends BaseJetVariableMacro {
    @Override
    public String getName() {
        return "kotlinVariableOfType";
    }

    @Override
    public String getPresentableName() {
        return JetBundle.message("macro.variable.of.type");
    }

    @Override
    protected boolean isSuitable(@NotNull VariableDescriptor variableDescriptor) {
        return true;
    }
}
                          