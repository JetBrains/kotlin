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
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor;
import org.jetbrains.kotlin.descriptors.ScriptDescriptor;
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor;
import org.jetbrains.kotlin.descriptors.impl.PropertyDescriptorImpl;
import org.jetbrains.kotlin.descriptors.impl.ScriptDescriptorImpl;
import org.jetbrains.kotlin.psi.JetDeclaration;
import org.jetbrains.kotlin.psi.JetNamedFunction;
import org.jetbrains.kotlin.psi.JetProperty;
import org.jetbrains.kotlin.psi.JetScript;
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowInfo;
import org.jetbrains.kotlin.resolve.lazy.ForceResolveUtil;
import org.jetbrains.kotlin.resolve.lazy.data.DataPackage;
import org.jetbrains.kotlin.resolve.scopes.WritableScope;
import org.jetbrains.kotlin.types.ErrorUtils;
import org.jetbrains.kotlin.types.JetType;
import org.jetbrains.kotlin.types.expressions.CoercionStrategy;
import org.jetbrains.kotlin.types.expressions.ExpressionTypingContext;
import org.jetbrains.kotlin.types.expressions.ExpressionTypingServices;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.jetbrains.kotlin.types.TypeUtils.NO_EXPECTED_TYPE;


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

            if (c.getTopDownAnalysisParameters().isLazy()) {
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
