package org.jetbrains.jet.lang.resolve;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.cfg.JetFlowInformationProvider;
import org.jetbrains.jet.lang.cfg.pseudocode.JetControlFlowDataTraceFactory;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.types.JetStandardClasses;
import org.jetbrains.jet.lang.types.JetType;

import java.util.List;
import java.util.Map;

import static org.jetbrains.jet.lang.types.TypeUtils.NO_EXPECTED_TYPE;

/**
 * @author svtk
 */
public class ControlFlowAnalyzer {
    private TopDownAnalysisContext context;
    private final JetControlFlowDataTraceFactory flowDataTraceFactory;

    public ControlFlowAnalyzer(TopDownAnalysisContext context, JetControlFlowDataTraceFactory flowDataTraceFactory) {
        this.context = context;
        this.flowDataTraceFactory = flowDataTraceFactory;
    }

    public void process() {
        for (JetClass aClass : context.getClasses().keySet()) {
            if (!context.completeAnalysisNeeded(aClass)) continue;
            checkClassOrObject(aClass);
        }
        for (JetObjectDeclaration objectDeclaration : context.getObjects().keySet()) {
            if (!context.completeAnalysisNeeded(objectDeclaration)) continue;
            checkClassOrObject(objectDeclaration);
        }
        for (Map.Entry<JetNamedFunction, FunctionDescriptorImpl> entry : context.getFunctions().entrySet()) {
            JetNamedFunction function = entry.getKey();
            FunctionDescriptorImpl functionDescriptor = entry.getValue();
            if (!context.completeAnalysisNeeded(function)) continue;
            final JetType expectedReturnType = !function.hasBlockBody() && !function.hasDeclaredReturnType()
                                               ? NO_EXPECTED_TYPE
                                               : functionDescriptor.getReturnType();
            checkFunction(function, expectedReturnType);
        }
        for (JetSecondaryConstructor constructor : this.context.getConstructors().keySet()) {
            if (!context.completeAnalysisNeeded(constructor)) continue;
            checkFunction(constructor, JetStandardClasses.getUnitType());
        }
        for (Map.Entry<JetProperty, PropertyDescriptor> entry : context.getProperties().entrySet()) {
            JetProperty property = entry.getKey();
            PropertyDescriptor propertyDescriptor = entry.getValue();
            checkProperty(property, propertyDescriptor);
        }
    }
    
    private void checkClassOrObject(JetClassOrObject klass) {
        JetFlowInformationProvider flowInformationProvider = new JetFlowInformationProvider((JetDeclaration) klass, (JetExpression) klass, flowDataTraceFactory, context.getTrace());
        flowInformationProvider.markUninitializedVariables((JetElement) klass, context.isDeclaredLocally());
    }
    
    private void checkProperty(JetProperty property, PropertyDescriptor propertyDescriptor) {
        for (JetPropertyAccessor accessor : property.getAccessors()) {
            PropertyAccessorDescriptor accessorDescriptor = accessor.isGetter()
                                                            ? propertyDescriptor.getGetter()
                                                            : propertyDescriptor.getSetter();
            assert accessorDescriptor != null;
            checkFunction(accessor, accessorDescriptor.getReturnType());
        }
    }

    private void checkFunction(JetDeclarationWithBody function, final @NotNull JetType expectedReturnType) {
        assert function instanceof JetDeclaration;

        JetExpression bodyExpression = function.getBodyExpression();
        if (bodyExpression == null) return;
        JetFlowInformationProvider flowInformationProvider = new JetFlowInformationProvider((JetDeclaration) function, bodyExpression, flowDataTraceFactory, context.getTrace());

        flowInformationProvider.checkDefiniteReturn(function, expectedReturnType);

        flowInformationProvider.markUninitializedVariables(function.asElement(), context.isDeclaredLocally());

        flowInformationProvider.markUnusedVariables(function.asElement());
    }
}
