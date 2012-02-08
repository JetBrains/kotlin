package org.jetbrains.jet.plugin.liveTemplates.macro;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.ClassifierDescriptor;
import org.jetbrains.jet.lang.descriptors.VariableDescriptor;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.TypeUtils;
import org.jetbrains.jet.plugin.JetBundle;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Evgeny Gerashchenko
 * @since 2/7/12
 */
public class JetIterableVariableMacro extends BaseJetVariableMacro {

    private static final List<String> ACCEPTED_FQ_NAMES = Arrays.asList(
            "jet.Array", "java.lang.Iterable", "<java_root>.java.lang.Iterable", "jet.Iterable");

    @Override
    public String getName() {
        return "kotlinIterableVariable";
    }

    @Override
    public String getPresentableName() {
        return JetBundle.message("macro.iterable.variable");
    }

    @Override
    protected boolean isSuitable(@NotNull VariableDescriptor variableDescriptor) {
        // TODO more sophisticated check needed

        JetType outType = variableDescriptor.getOutType();
        List<JetType> types = new ArrayList<JetType>(TypeUtils.getAllSupertypes(outType));
        types.add(outType);
        for (JetType type : types) {
            ClassifierDescriptor declarationDescriptor = type.getConstructor().getDeclarationDescriptor();
            if (ACCEPTED_FQ_NAMES.contains(DescriptorUtils.getFQName(declarationDescriptor))) {
                return true;
            }
        }
        return false;
    }
}
