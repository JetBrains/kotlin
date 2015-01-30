/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.resolve;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.cfg.JetFlowInformationProvider;
import org.jetbrains.kotlin.descriptors.PropertyAccessorDescriptor;
import org.jetbrains.kotlin.descriptors.PropertyDescriptor;
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.types.JetType;

import javax.inject.Inject;
import java.util.Map;

import static org.jetbrains.kotlin.types.TypeUtils.NO_EXPECTED_TYPE;

public class ControlFlowAnalyzer {
    private BindingTrace trace;

    @Inject
    public void setTrace(BindingTrace trace) {
        this.trace = trace;
    }

    public void process(@NotNull BodiesResolveContext c) {
        for (JetFile file : c.getFiles()) {
            checkDeclarationContainer(c, file);
        }
        for (JetClassOrObject aClass : c.getDeclaredClasses().keySet()) {
            checkDeclarationContainer(c, aClass);
        }
        for (Map.Entry<JetNamedFunction, SimpleFunctionDescriptor> entry : c.getFunctions().entrySet()) {
            JetNamedFunction function = entry.getKey();
            SimpleFunctionDescriptor functionDescriptor = entry.getValue();
            JetType expectedReturnType = !function.hasBlockBody() && !function.hasDeclaredReturnType()
                                               ? NO_EXPECTED_TYPE
                                               : functionDescriptor.getReturnType();
            checkFunction(c, function, expectedReturnType);
        }
        for (Map.Entry<JetProperty, PropertyDescriptor> entry : c.getProperties().entrySet()) {
            JetProperty property = entry.getKey();
            PropertyDescriptor propertyDescriptor = entry.getValue();
            checkProperty(c, property, propertyDescriptor);
        }
    }

    private void checkDeclarationContainer(@NotNull BodiesResolveContext c, JetDeclarationContainer declarationContainer) {
        // A pseudocode of class/object initialization corresponds to a class/object
        // or initialization of properties corresponds to a package declared in a file
        JetFlowInformationProvider flowInformationProvider = new JetFlowInformationProvider((JetElement) declarationContainer, trace);
        if (c.getTopDownAnalysisParameters().isDeclaredLocally()) {
            flowInformationProvider.checkForLocalClassOrObjectMode();
            return;
        }
        flowInformationProvider.checkDeclaration();
    }

    private void checkProperty(@NotNull BodiesResolveContext c, JetProperty property, PropertyDescriptor propertyDescriptor) {
        for (JetPropertyAccessor accessor : property.getAccessors()) {
            PropertyAccessorDescriptor accessorDescriptor = accessor.isGetter()
                                                            ? propertyDescriptor.getGetter()
                                                            : propertyDescriptor.getSetter();
            assert accessorDescriptor != null : "no property accessor descriptor " + accessor.getText();
            JetType returnType = accessorDescriptor.getReturnType();
            checkFunction(c, accessor, returnType);
        }
    }

    private void checkFunction(@NotNull BodiesResolveContext c, @NotNull JetDeclarationWithBody function, @Nullable JetType expectedReturnType) {
        if (!function.hasBody()) return;
        JetFlowInformationProvider flowInformationProvider = new JetFlowInformationProvider(function, trace);
        if (c.getTopDownAnalysisParameters().isDeclaredLocally()) {
            flowInformationProvider.checkForLocalClassOrObjectMode();
            return;
        }
        flowInformationProvider.checkDeclaration();
        flowInformationProvider.checkFunction(expectedReturnType);
    }
}
