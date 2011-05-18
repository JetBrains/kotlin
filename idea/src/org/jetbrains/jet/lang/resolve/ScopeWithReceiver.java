package org.jetbrains.jet.lang.resolve;

import com.google.common.base.Function;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.JetTypeChecker;

/**
 * @author abreslav
 */
public class ScopeWithReceiver extends JetScopeImpl {

    private final JetScope receiverTypeScope;
    private final JetScope outerScope;
    private final JetTypeChecker typeChecker;

    public ScopeWithReceiver(JetScope outerScope, JetType receiverType, JetTypeChecker typeChecker) {
        this.outerScope = outerScope;
        this.receiverTypeScope = receiverType.getMemberScope();
        this.typeChecker = typeChecker;
    }

    @NotNull
    @Override
    public FunctionGroup getFunctionGroup(@NotNull String name) {
        FunctionGroup functionGroup = receiverTypeScope.getFunctionGroup(name);
        if (functionGroup.isEmpty()) {
            return FunctionDescriptorUtil.filteredFunctionGroup(outerScope.getFunctionGroup(name),
                    new Function<FunctionDescriptor, Boolean>() {
                        @Override
                        public Boolean apply(@Nullable FunctionDescriptor functionDescriptor) {
                            if (functionDescriptor == null) return false;
                            JetType functionReceiverType = functionDescriptor.getReceiverType();
                            if (functionReceiverType == null) {
                                return false;
                            }
                            return typeChecker.isSubtypeOf(receiverTypeScope.getThisType(), functionReceiverType);
                        }
                    });
        }
        return functionGroup; // TODO
    }

    @Override
    public ClassifierDescriptor getClassifier(@NotNull String name) {
        return super.getClassifier(name); // TODO
    }

    @Override
    public VariableDescriptor getVariable(@NotNull String name) {
        return receiverTypeScope.getVariable(name);
        // TODO : extension properties
    }

    @Override
    public NamespaceDescriptor getNamespace(@NotNull String name) {
        return receiverTypeScope.getNamespace(name);
    }

    @NotNull
    @Override
    public JetType getThisType() {
        return receiverTypeScope.getThisType();
    }

    @NotNull
    @Override
    public DeclarationDescriptor getContainingDeclaration() {
        return outerScope.getContainingDeclaration();
    }
}
