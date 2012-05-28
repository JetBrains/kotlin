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
    private BindingTrace trace;

    @Inject
    public void setTopDownAnalysisParameters(TopDownAnalysisParameters topDownAnalysisParameters) {
        this.topDownAnalysisParameters = topDownAnalysisParameters;
    }

    @Inject
    public void setTrace(BindingTrace trace) {
        this.trace = trace;
    }

    public void process(@NotNull BodiesResolveContext bodiesResolveContext) {
        for (JetClass aClass : bodiesResolveContext.getClasses().keySet()) {
            if (!bodiesResolveContext.completeAnalysisNeeded(aClass)) continue;
            checkClassOrObject(aClass);
        }
        for (JetObjectDeclaration objectDeclaration : bodiesResolveContext.getObjects().keySet()) {
            if (!bodiesResolveContext.completeAnalysisNeeded(objectDeclaration)) continue;
            checkClassOrObject(objectDeclaration);
        }
        for (Map.Entry<JetNamedFunction, SimpleFunctionDescriptor> entry : bodiesResolveContext.getFunctions().entrySet()) {
            JetNamedFunction function = entry.getKey();
            SimpleFunctionDescriptor functionDescriptor = entry.getValue();
            if (!bodiesResolveContext.completeAnalysisNeeded(function)) continue;
            final JetType expectedReturnType = !function.hasBlockBody() && !function.hasDeclaredReturnType()
                                               ? NO_EXPECTED_TYPE
                                               : functionDescriptor.getReturnType();
            checkFunction(function, expectedReturnType);
        }
        for (JetSecondaryConstructor constructor : bodiesResolveContext.getConstructors().keySet()) {
            if (!bodiesResolveContext.completeAnalysisNeeded(constructor)) continue;
            checkFunction(constructor, JetStandardClasses.getUnitType());
        }
        for (Map.Entry<JetProperty, PropertyDescriptor> entry : bodiesResolveContext.getProperties().entrySet()) {
            JetProperty property = entry.getKey();
            if (!bodiesResolveContext.completeAnalysisNeeded(property)) continue;
            PropertyDescriptor propertyDescriptor = entry.getValue();
            checkProperty(property, propertyDescriptor);
        }
    }

    private void checkClassOrObject(JetClassOrObject klass) {
        // A pseudocode of class initialization corresponds to a class
        JetFlowInformationProvider flowInformationProvider = new JetFlowInformationProvider((JetDeclaration) klass, trace);
        flowInformationProvider.markUninitializedVariables(topDownAnalysisParameters.isDeclaredLocally());
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
        JetFlowInformationProvider flowInformationProvider = new JetFlowInformationProvider((JetDeclaration) function, trace);

        flowInformationProvider.checkDefiniteReturn(expectedReturnType);

        // Property accessor is checked through initialization of a class check (at 'checkClassOrObject')
        boolean isPropertyAccessor = function instanceof JetPropertyAccessor;
        flowInformationProvider.markUninitializedVariables(topDownAnalysisParameters.isDeclaredLocally() || isPropertyAccessor);

        flowInformationProvider.markUnusedVariables();

        flowInformationProvider.markUnusedLiteralsInBlock();
    }
}
