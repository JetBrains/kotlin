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

import com.google.common.collect.Lists;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.psi.JetScript;
import org.jetbrains.jet.lang.resolve.calls.autocasts.DataFlowInfo;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.resolve.scopes.RedeclarationHandler;
import org.jetbrains.jet.lang.resolve.scopes.WritableScope;
import org.jetbrains.jet.lang.resolve.scopes.WritableScopeImpl;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ScriptReceiver;
import org.jetbrains.jet.lang.types.DependencyClassByQualifiedNameResolver;
import org.jetbrains.jet.lang.types.ErrorUtils;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.TypeUtils;
import org.jetbrains.jet.lang.types.expressions.CoercionStrategy;
import org.jetbrains.jet.lang.types.expressions.ExpressionTypingContext;
import org.jetbrains.jet.lang.types.expressions.ExpressionTypingServices;
import org.jetbrains.jet.lang.types.ref.JetTypeName;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.jetbrains.jet.lang.types.TypeUtils.NO_EXPECTED_TYPE;

/**
 * @author Stepan Koltsov
 */
public class ScriptResolver {

    @NotNull
    private NamespaceFactory namespaceFactory;
    @NotNull
    private DependencyClassByQualifiedNameResolver dependencyClassByQualifiedNameResolver;
    @NotNull
    private ModuleDescriptor moduleDescriptor;
    @NotNull
    private TopDownAnalysisContext context;
    @NotNull
    private ExpressionTypingServices expressionTypingServices;
    @NotNull
    private BindingTrace trace;
    @NotNull
    private TopDownAnalysisParameters topDownAnalysisParameters;

    @Inject
    public void setNamespaceFactory(@NotNull NamespaceFactory namespaceFactory) {
        this.namespaceFactory = namespaceFactory;
    }

    @Inject
    public void setDependencyClassByQualifiedNameResolver(@NotNull DependencyClassByQualifiedNameResolver dependencyClassByQualifiedNameResolver) {
        this.dependencyClassByQualifiedNameResolver = dependencyClassByQualifiedNameResolver;
    }

    @Inject
    public void setModuleDescriptor(@NotNull ModuleDescriptor moduleDescriptor) {
        this.moduleDescriptor = moduleDescriptor;
    }

    @Inject
    public void setContext(@NotNull TopDownAnalysisContext context) {
        this.context = context;
    }

    @Inject
    public void setExpressionTypingServices(@NotNull ExpressionTypingServices expressionTypingServices) {
        this.expressionTypingServices = expressionTypingServices;
    }

    @Inject
    public void setTrace(@NotNull BindingTrace trace) {
        this.trace = trace;
    }

    @Inject
    public void setTopDownAnalysisParameters(@NotNull TopDownAnalysisParameters topDownAnalysisParameters) {
        this.topDownAnalysisParameters = topDownAnalysisParameters;
    }


    @NotNull
    private ClassDescriptor resolveClass(@NotNull FqName className) {
        ClassDescriptor classDescriptor = dependencyClassByQualifiedNameResolver.resolveClass(className);
        if (classDescriptor == null) {
            throw new IllegalStateException("dependency class not found by name: " + className);
        }
        return classDescriptor;
    }

    @NotNull
    public JetType resolveTypeName(@NotNull JetTypeName typeName) {
        List<JetType> typeArguments = new ArrayList<JetType>();
        for (JetTypeName typeArgumentName : typeName.getArguments()) {
            typeArguments.add(resolveTypeName(typeArgumentName));
        }
        ClassDescriptor classDescriptor = resolveClass(typeName.getClassName());
        return TypeUtils.substituteParameters(classDescriptor, typeArguments);
    }


    @NotNull
    private ValueParameterDescriptor resolveScriptParameter(
            @NotNull AnalyzerScriptParameter scriptParameter,
            int index,
            @NotNull ScriptDescriptor script) {
        JetType type = resolveTypeName(scriptParameter.getType());
        return new ValueParameterDescriptorImpl(script, index, Collections.<AnnotationDescriptor>emptyList(), scriptParameter.getName(), false, type, false, null);
    }

    public void processScriptHierarchy(@NotNull JetScript script, @NotNull JetScope outerScope) {
        NamespaceDescriptorImpl ns = namespaceFactory.createNamespaceDescriptorPathIfNeeded(FqName.ROOT);
        ScriptDescriptor scriptDescriptor = new ScriptDescriptor(ns);
        //WriteThroughScope scriptScope = new WriteThroughScope(
        //        outerScope, ns.getMemberScope(), new TraceBasedRedeclarationHandler(trace));
        WritableScopeImpl scriptScope = new WritableScopeImpl(outerScope, scriptDescriptor, RedeclarationHandler.DO_NOTHING, "script");
        scriptScope.changeLockLevel(WritableScope.LockLevel.BOTH);

        context.getScriptScopes().put(script, scriptScope);
        context.getScripts().put(script, scriptDescriptor);
        trace.record(BindingContext.SCRIPT, script, scriptDescriptor);
    }



    public void resolveScripts() {
        for (Map.Entry<JetScript, ScriptDescriptor> e : context.getScripts().entrySet()) {
            JetScript declaration = e.getKey();
            ScriptDescriptor descriptor = e.getValue();
            WritableScope scope = context.getScriptScopes().get(declaration);

            List<ValueParameterDescriptor> valueParameters = Lists.newArrayList();

            scope.setImplicitReceiver(descriptor.getImplicitReceiver());

            int index = 0;
            for (AnalyzerScriptParameter scriptParameter : topDownAnalysisParameters.getScriptParameters()) {
                ValueParameterDescriptor parameter = resolveScriptParameter(scriptParameter, index, descriptor);
                valueParameters.add(parameter);
                scope.addVariableDescriptor(parameter);
                ++index;
            }

            scope.changeLockLevel(WritableScope.LockLevel.READING);

            ExpressionTypingContext context = ExpressionTypingContext.newContext(
                    expressionTypingServices,
                    trace,
                    scope,
                    DataFlowInfo.EMPTY,
                    NO_EXPECTED_TYPE,
                    false);
            JetType returnType = expressionTypingServices.getBlockReturnedType(scope, declaration.getBlockExpression(), CoercionStrategy.NO_COERCION, context, trace);
            if (returnType == null) {
                returnType = ErrorUtils.createErrorType("getBlockReturnedType returned null");
            }
            descriptor.initialize(returnType, valueParameters);
        }
    }



}
