/*
 * Copyright 2010-2013 JetBrains s.r.o.
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
import org.jetbrains.jet.lang.descriptors.CallableMemberDescriptor;
import org.jetbrains.jet.lang.descriptors.ScriptDescriptor;
import org.jetbrains.jet.lang.descriptors.SimpleFunctionDescriptor;
import org.jetbrains.jet.lang.descriptors.impl.PropertyDescriptorImpl;
import org.jetbrains.jet.lang.descriptors.impl.ScriptDescriptorImpl;
import org.jetbrains.jet.lang.psi.JetDeclaration;
import org.jetbrains.jet.lang.psi.JetNamedFunction;
import org.jetbrains.jet.lang.psi.JetProperty;
import org.jetbrains.jet.lang.psi.JetScript;
import org.jetbrains.jet.lang.resolve.calls.autocasts.DataFlowInfo;
import org.jetbrains.jet.lang.resolve.lazy.ForceResolveUtil;
import org.jetbrains.jet.lang.resolve.lazy.data.DataPackage;
import org.jetbrains.jet.lang.resolve.scopes.WritableScope;
import org.jetbrains.jet.lang.types.ErrorUtils;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.expressions.CoercionStrategy;
import org.jetbrains.jet.lang.types.expressions.ExpressionTypingContext;
import org.jetbrains.jet.lang.types.expressions.ExpressionTypingServices;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.jetbrains.jet.lang.types.TypeUtils.NO_EXPECTED_TYPE;


// SCRIPT: resolve symbols in scripts
public class ScriptBodyResolver {

    @NotNull
    private ExpressionTypingServices expressionTypingServices;

    @Inject
    public void setExpressionTypingServices(@NotNull ExpressionTypingServices expressionTypingServices) {
        this.expressionTypingServices = expressionTypingServices;
    }



    public void resolveScriptBodies(@NotNull BodiesResolveContext c, @NotNull BindingTrace trace) {
        for (Map.Entry<JetScript, ScriptDescriptor> e : c.getScripts().entrySet()) {
            JetScript declaration = e.getKey();
            ScriptDescriptor descriptor = e.getValue();

            if (c.getTopDownAnalysisParameters().isLazyTopDownAnalysis()) {
                ForceResolveUtil.forceResolveAllContents(descriptor);
                continue;
            }

            ScriptDescriptorImpl descriptorImpl = (ScriptDescriptorImpl) descriptor;

            // TODO: lock in resolveScriptDeclarations
            descriptorImpl.getScopeForBodyResolution().changeLockLevel(WritableScope.LockLevel.READING);

            JetType returnType = resolveScriptReturnType(declaration, descriptor, trace);

            List<PropertyDescriptorImpl> properties = new ArrayList<PropertyDescriptorImpl>();
            List<SimpleFunctionDescriptor> functions = new ArrayList<SimpleFunctionDescriptor>();

            BindingContext bindingContext = trace.getBindingContext();
            for (JetDeclaration jetDeclaration : declaration.getDeclarations()) {
                if (jetDeclaration instanceof JetProperty) {
                    if (!DataPackage.shouldBeScriptClassMember(jetDeclaration)) continue;

                    PropertyDescriptorImpl propertyDescriptor = (PropertyDescriptorImpl) bindingContext.get(BindingContext.VARIABLE, jetDeclaration);
                    properties.add(propertyDescriptor);
                }
                else if (jetDeclaration instanceof JetNamedFunction) {
                    if (!DataPackage.shouldBeScriptClassMember(jetDeclaration)) continue;

                    SimpleFunctionDescriptor function = bindingContext.get(BindingContext.FUNCTION, jetDeclaration);
                    assert function != null;
                    functions.add(function.copy(descriptor.getClassDescriptor(), function.getModality(), function.getVisibility(),
                                                CallableMemberDescriptor.Kind.DECLARATION, false));
                }
            }

            descriptorImpl.initialize(returnType, properties, functions);
        }
    }

    @NotNull
    public JetType resolveScriptReturnType(
            @NotNull JetScript script,
            @NotNull ScriptDescriptor scriptDescriptor,
            @NotNull BindingTrace trace
    ) {
        // Resolve all contents of the script
        ExpressionTypingContext context = ExpressionTypingContext.newContext(
                expressionTypingServices,
                trace,
                scriptDescriptor.getScopeForBodyResolution(),
                DataFlowInfo.EMPTY,
                NO_EXPECTED_TYPE
        );
        JetType returnType = expressionTypingServices.getBlockReturnedType(script.getBlockExpression(), CoercionStrategy.NO_COERCION, context).getType();
        if (returnType == null) {
            returnType = ErrorUtils.createErrorType("getBlockReturnedType returned null");
        }
        return returnType;
    }
}
