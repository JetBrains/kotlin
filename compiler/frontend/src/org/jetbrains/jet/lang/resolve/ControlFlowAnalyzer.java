/*
 * Copyright 2010-2012 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.jet.lang.resolve;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.cfg.JetFlowInformationProvider;
import org.jetbrains.jet.lang.cfg.pseudocode.JetControlFlowDataTraceFactory;
import org.jetbrains.jet.lang.descriptors.PropertyAccessorDescriptor;
import org.jetbrains.jet.lang.descriptors.PropertyDescriptor;
import org.jetbrains.jet.lang.descriptors.SimpleFunctionDescriptor;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.lang.JetStandardClasses;

import javax.inject.Inject;
import java.util.Map;

import static org.jetbrains.jet.lang.types.TypeUtils.NO_EXPECTED_TYPE;

/**
 * @author svtk
 */
public class ControlFlowAnalyzer {
    private TopDownAnalysisParameters topDownAnalysisParameters;
    private TopDownAnalysisContext context;
    private BindingTrace trace;
    private JetControlFlowDataTraceFactory flowDataTraceFactory;


    @Inject
    public void setTopDownAnalysisParameters(TopDownAnalysisParameters topDownAnalysisParameters) {
        this.topDownAnalysisParameters = topDownAnalysisParameters;
    }

    @Inject
    public void setContext(TopDownAnalysisContext context) {
        this.context = context;
    }

    @Inject
    public void setTrace(BindingTrace trace) {
        this.trace = trace;
    }

    @Inject
    public void setFlowDataTraceFactory(JetControlFlowDataTraceFactory flowDataTraceFactory) {
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
        for (Map.Entry<JetNamedFunction, SimpleFunctionDescriptor> entry : context.getFunctions().entrySet()) {
            JetNamedFunction function = entry.getKey();
            SimpleFunctionDescriptor functionDescriptor = entry.getValue();
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
            if (!context.completeAnalysisNeeded(property)) continue;
            PropertyDescriptor propertyDescriptor = entry.getValue();
            checkProperty(property, propertyDescriptor);
        }
    }
    
    private void checkClassOrObject(JetClassOrObject klass) {
        // A pseudocode of class initialization corresponds to a class
        JetFlowInformationProvider flowInformationProvider = new JetFlowInformationProvider((JetDeclaration) klass, (JetExpression) klass, flowDataTraceFactory, trace);
        flowInformationProvider.markUninitializedVariables((JetElement) klass, topDownAnalysisParameters.isDeclaredLocally());
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
        JetFlowInformationProvider flowInformationProvider = new JetFlowInformationProvider((JetDeclaration) function, bodyExpression, flowDataTraceFactory, trace);

        flowInformationProvider.checkDefiniteReturn(function, expectedReturnType);

        // Property accessor is checked through initialization of a class check (at 'checkClassOrObject')
        boolean isPropertyAccessor = function instanceof JetPropertyAccessor;
        flowInformationProvider.markUninitializedVariables(function.asElement(), topDownAnalysisParameters.isDeclaredLocally() || isPropertyAccessor);

        flowInformationProvider.markUnusedVariables(function.asElement());

        flowInformationProvider.markUnusedLiteralsInBlock(function.asElement());
    }
}
