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

package org.jetbrains.jet.lang.types.expressions;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.resolve.BindingTrace;
import org.jetbrains.jet.lang.resolve.calls.CallResolverExtension;
import org.jetbrains.jet.lang.resolve.calls.autocasts.DataFlowInfo;
import org.jetbrains.jet.lang.resolve.calls.context.ContextDependency;
import org.jetbrains.jet.lang.resolve.calls.context.ResolutionContext;
import org.jetbrains.jet.lang.resolve.calls.context.ResolutionResultsCache;
import org.jetbrains.jet.lang.resolve.calls.context.ResolutionResultsCacheImpl;
import org.jetbrains.jet.lang.resolve.constants.CompileTimeConstantChecker;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.types.JetType;

public class ExpressionTypingContext extends ResolutionContext<ExpressionTypingContext> {

    @NotNull
    public static ExpressionTypingContext newContext(
            @NotNull ExpressionTypingServices expressionTypingServices,
            @NotNull BindingTrace trace,
            @NotNull JetScope scope,
            @NotNull DataFlowInfo dataFlowInfo,
            @NotNull JetType expectedType
    ) {
        return newContext(trace, scope, dataFlowInfo, expectedType,
                          ContextDependency.INDEPENDENT, ResolutionResultsCacheImpl.create(), LabelResolver.create(),
                          expressionTypingServices.createExtension(scope, false), false, false);
    }

    @NotNull
    public static ExpressionTypingContext newContext(@NotNull ResolutionContext resolutionContext) {
        return newContext(resolutionContext.trace, resolutionContext.scope, resolutionContext.dataFlowInfo,
                          resolutionContext.expectedType, resolutionContext.contextDependency,
                          resolutionContext.resolutionResultsCache, resolutionContext.labelResolver,
                          resolutionContext.callResolverExtension, resolutionContext.isAnnotationContext,
                          resolutionContext.collectAllCandidates);
    }

    @NotNull
    public static ExpressionTypingContext newContext(
            @NotNull BindingTrace trace,
            @NotNull JetScope scope,
            @NotNull DataFlowInfo dataFlowInfo,
            @NotNull JetType expectedType,
            @NotNull ContextDependency contextDependency,
            @NotNull ResolutionResultsCache resolutionResultsCache,
            @NotNull LabelResolver labelResolver,
            @NotNull CallResolverExtension callResolverExtension,
            boolean isAnnotationContext,
            boolean collectAllCandidates
    ) {
        return new ExpressionTypingContext(labelResolver, trace, scope, scope.getContainingDeclaration(), dataFlowInfo,
                                           expectedType, contextDependency, resolutionResultsCache, callResolverExtension, 
                                           isAnnotationContext, collectAllCandidates);
    }

    public final DeclarationDescriptor containingDeclaration;
    private CompileTimeConstantChecker compileTimeConstantChecker;

    private ExpressionTypingContext(
            @NotNull LabelResolver labelResolver,
            @NotNull BindingTrace trace,
            @NotNull JetScope scope,
            @NotNull DeclarationDescriptor containingDeclaration,
            @NotNull DataFlowInfo dataFlowInfo,
            @NotNull JetType expectedType,
            @NotNull ContextDependency contextDependency,
            @NotNull ResolutionResultsCache resolutionResultsCache,
            @NotNull CallResolverExtension callResolverExtension,
            boolean isAnnotationContext,
            boolean collectAllCandidates
    ) {
        super(trace, scope, expectedType, dataFlowInfo, contextDependency, resolutionResultsCache, labelResolver, callResolverExtension,
              isAnnotationContext, collectAllCandidates);
        this.containingDeclaration = containingDeclaration;
    }

    @Override
    protected ExpressionTypingContext create(
            @NotNull BindingTrace trace,
            @NotNull JetScope scope,
            @NotNull DataFlowInfo dataFlowInfo,
            @NotNull JetType expectedType,
            @NotNull ContextDependency contextDependency,
            @NotNull ResolutionResultsCache resolutionResultsCache,
            @NotNull LabelResolver labelResolver
    ) {
        return new ExpressionTypingContext(this.labelResolver, trace, scope, containingDeclaration, dataFlowInfo,
                                           expectedType, contextDependency, resolutionResultsCache, callResolverExtension,
                                           isAnnotationContext, collectAllCandidates);
    }

///////////// LAZY ACCESSORS

    public CompileTimeConstantChecker getCompileTimeConstantChecker() {
        if (compileTimeConstantChecker == null) {
            compileTimeConstantChecker = new CompileTimeConstantChecker(trace, false);
        }
        return compileTimeConstantChecker;
    }
}
