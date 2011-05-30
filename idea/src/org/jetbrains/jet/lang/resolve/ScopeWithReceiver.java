package org.jetbrains.jet.lang.resolve;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.JetTypeChecker;

/**
 * @author abreslav
 */
public class ScopeWithReceiver extends JetScopeImpl {

    private final JetType receiverType;
    private final JetScope outerScope;
    private final JetTypeChecker typeChecker;

    public ScopeWithReceiver(@NotNull JetScope outerScope, @NotNull JetType receiverType, @NotNull JetTypeChecker typeChecker) {
        this.outerScope = outerScope;
        this.receiverType = receiverType;
        this.typeChecker = typeChecker;
    }

    @NotNull
    @Override
    public FunctionGroup getFunctionGroup(@NotNull String name) {
        JetScope memberScope = receiverType.getMemberScope();
        assert memberScope != null : receiverType;
        FunctionGroup functionGroup = memberScope.getFunctionGroup(name);
        if (functionGroup.isEmpty()) {
            return outerScope.getFunctionGroup(name);
//            return FunctionDescriptorUtil.filteredFunctionGroup(outerScope.getFunctionGroup(name),
//                    new Function<FunctionDescriptor, Boolean>() {
//                        @Override
//                        public Boolean apply(@Nullable FunctionDescriptor functionDescriptor) {
//                            if (functionDescriptor == null) return false;
//                            JetType functionReceiverType = functionDescriptor.getReceiverType();
//                            if (functionReceiverType == null) {
//                                return false;
//                            }
//                            // TODO : in case of inferred type arguments, substitute the receiver type first
//                            return typeChecker.isSubtypeOf(receiverType, functionReceiverType);
//                        }
//                    });
        }
        return functionGroup; // TODO
    }

    @Override
    public ClassifierDescriptor getClassifier(@NotNull String name) {
        return super.getClassifier(name); // TODO
    }

    @Override
    public VariableDescriptor getVariable(@NotNull String name) {
        VariableDescriptor variable = receiverType.getMemberScope().getVariable(name);
        if (variable != null) {
            return variable;
        }
        variable = outerScope.getVariable(name);
        if (variable instanceof PropertyDescriptor) {
            PropertyDescriptor propertyDescriptor = (PropertyDescriptor) variable;
            JetType receiverType = propertyDescriptor.getReceiverType();
            // TODO : in case of type arguments, substitute the receiver type first
            if (receiverType != null
                    && typeChecker.isSubtypeOf(receiverType, receiverType)) {
                return variable;
            }
        }
        return null;
    }

    @Override
    public NamespaceDescriptor getNamespace(@NotNull String name) {
        return receiverType.getMemberScope().getNamespace(name);
    }

    @NotNull
    @Override
    public JetType getThisType() {
        return receiverType;
    }

    @NotNull
    @Override
    public DeclarationDescriptor getContainingDeclaration() {
        return outerScope.getContainingDeclaration();
    }
}
