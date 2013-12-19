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

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.intellij.psi.PsiElement;
import com.intellij.util.containers.Queue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.descriptors.impl.MutableClassDescriptor;
import org.jetbrains.jet.lang.evaluate.ConstantExpressionEvaluator;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.calls.CallResolver;
import org.jetbrains.jet.lang.resolve.calls.context.ContextDependency;
import org.jetbrains.jet.lang.resolve.calls.context.ResolutionResultsCacheImpl;
import org.jetbrains.jet.lang.resolve.calls.context.SimpleResolutionContext;
import org.jetbrains.jet.lang.resolve.calls.results.OverloadResolutionResults;
import org.jetbrains.jet.lang.resolve.calls.util.CallMaker;
import org.jetbrains.jet.lang.resolve.constants.CompileTimeConstant;
import org.jetbrains.jet.lang.resolve.scopes.*;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverValue;
import org.jetbrains.jet.lang.types.*;
import org.jetbrains.jet.lang.types.expressions.DataFlowUtils;
import org.jetbrains.jet.lang.types.expressions.ExpressionTypingServices;
import org.jetbrains.jet.lang.types.expressions.LabelResolver;
import org.jetbrains.jet.lexer.JetTokens;
import org.jetbrains.jet.util.Box;
import org.jetbrains.jet.util.ReenteringLazyValueComputationException;
import org.jetbrains.jet.util.slicedmap.WritableSlice;

import javax.inject.Inject;
import java.util.*;

import static org.jetbrains.jet.lang.descriptors.ReceiverParameterDescriptor.NO_RECEIVER_PARAMETER;
import static org.jetbrains.jet.lang.diagnostics.Errors.*;
import static org.jetbrains.jet.lang.resolve.BindingContext.DEFERRED_TYPE;
import static org.jetbrains.jet.lang.types.TypeUtils.NO_EXPECTED_TYPE;

public class BodyResolver {
    @NotNull
    private BodiesResolveContext context;
    @NotNull
    private TopDownAnalysisParameters topDownAnalysisParameters;
    @NotNull
    private ScriptBodyResolver scriptBodyResolverResolver;
    @NotNull
    private ExpressionTypingServices expressionTypingServices;
    @NotNull
    private CallResolver callResolver;
    @NotNull
    private ObservableBindingTrace trace;
    @NotNull
    private ControlFlowAnalyzer controlFlowAnalyzer;
    @NotNull
    private DeclarationsChecker declarationsChecker;
    @NotNull
    private AnnotationResolver annotationResolver;
    @NotNull
    private DelegatedPropertyResolver delegatedPropertyResolver;
    @NotNull
    private FunctionAnalyzerExtension functionAnalyzerExtension;

    @Inject
    public void setTopDownAnalysisParameters(@NotNull TopDownAnalysisParameters topDownAnalysisParameters) {
        this.topDownAnalysisParameters = topDownAnalysisParameters;
    }

    @Inject
    public void setScriptBodyResolverResolver(@NotNull ScriptBodyResolver scriptBodyResolverResolver) {
        this.scriptBodyResolverResolver = scriptBodyResolverResolver;
    }

    @Inject
    public void setExpressionTypingServices(@NotNull ExpressionTypingServices expressionTypingServices) {
        this.expressionTypingServices = expressionTypingServices;
    }

    @Inject
    public void setCallResolver(@NotNull CallResolver callResolver) {
        this.callResolver = callResolver;
    }

    @Inject
    public void setTrace(@NotNull BindingTrace trace) {
        this.trace = new ObservableBindingTrace(trace);
    }

    @Inject
    public void setControlFlowAnalyzer(@NotNull ControlFlowAnalyzer controlFlowAnalyzer) {
        this.controlFlowAnalyzer = controlFlowAnalyzer;
    }

    @Inject
    public void setDeclarationsChecker(@NotNull DeclarationsChecker declarationsChecker) {
        this.declarationsChecker = declarationsChecker;
    }

    @Inject
    public void setContext(@NotNull BodiesResolveContext context) {
        this.context = context;
    }

    @Inject
    public void setAnnotationResolver(@NotNull AnnotationResolver annotationResolver) {
        this.annotationResolver = annotationResolver;
    }

    @Inject
    public void setDelegatedPropertyResolver(@NotNull DelegatedPropertyResolver delegatedPropertyResolver) {
        this.delegatedPropertyResolver = delegatedPropertyResolver;
    }

    @Inject
    public void setFunctionAnalyzerExtension(@NotNull FunctionAnalyzerExtension functionAnalyzerExtension) {
        this.functionAnalyzerExtension = functionAnalyzerExtension;
    }

    private void resolveBehaviorDeclarationBodies(@NotNull BodiesResolveContext bodiesResolveContext) {
        // Initialize context
        context = bodiesResolveContext;

        resolveDelegationSpecifierLists();

        resolvePropertyDeclarationBodies();
        resolveClassAnnotations();
        resolveAnonymousInitializers();
        resolvePrimaryConstructorParameters();

        resolveFunctionBodies();

        scriptBodyResolverResolver.resolveScriptBodies();

        if (!topDownAnalysisParameters.isDeclaredLocally()) {
            computeDeferredTypes();
        }
    }

    public void resolveBodies() {
        resolveBehaviorDeclarationBodies(context);
        controlFlowAnalyzer.process(context);
        declarationsChecker.process(context);
        functionAnalyzerExtension.process(context);
    }

    private void resolveDelegationSpecifierLists() {
        // TODO : Make sure the same thing is not initialized twice
        for (Map.Entry<JetClassOrObject, MutableClassDescriptor> entry : context.getClasses().entrySet()) {
            JetClassOrObject classOrObject = entry.getKey();
            MutableClassDescriptor descriptor = entry.getValue();
            resolveDelegationSpecifierList(classOrObject, descriptor,
                                           descriptor.getUnsubstitutedPrimaryConstructor(),
                                           descriptor.getScopeForSupertypeResolution(),
                                           descriptor.getScopeForMemberResolution());
        }
    }

    public void resolveDelegationSpecifierList(@NotNull JetClassOrObject jetClass, @NotNull final ClassDescriptor descriptor,
            @Nullable final ConstructorDescriptor primaryConstructor,
            @NotNull JetScope scopeForSupertypeResolution,
            @NotNull final JetScope scopeForMemberResolution) {
        if (!context.completeAnalysisNeeded(jetClass)) return;
        final JetScope scopeForConstructor = primaryConstructor == null
                ? null
                : FunctionDescriptorUtil.getFunctionInnerScope(scopeForSupertypeResolution, primaryConstructor, trace);
        final ExpressionTypingServices typeInferrer = expressionTypingServices; // TODO : flow

        final Map<JetTypeReference, JetType> supertypes = Maps.newLinkedHashMap();
        JetVisitorVoid visitor = new JetVisitorVoid() {
            private void recordSupertype(JetTypeReference typeReference, JetType supertype) {
                if (supertype == null) return;
                supertypes.put(typeReference, supertype);
            }

            @Override
            public void visitDelegationByExpressionSpecifier(@NotNull JetDelegatorByExpressionSpecifier specifier) {
                if (descriptor.getKind() == ClassKind.TRAIT) {
                    trace.report(DELEGATION_IN_TRAIT.on(specifier));
                }
                JetType supertype = trace.getBindingContext().get(BindingContext.TYPE, specifier.getTypeReference());
                recordSupertype(specifier.getTypeReference(), supertype);
                if (supertype != null) {
                    DeclarationDescriptor declarationDescriptor = supertype.getConstructor().getDeclarationDescriptor();
                    if (declarationDescriptor instanceof ClassDescriptor) {
                        ClassDescriptor classDescriptor = (ClassDescriptor) declarationDescriptor;
                        if (classDescriptor.getKind() != ClassKind.TRAIT) {
                            trace.report(DELEGATION_NOT_TO_TRAIT.on(specifier.getTypeReference()));
                        }
                    }
                }
                JetExpression delegateExpression = specifier.getDelegateExpression();
                if (delegateExpression != null) {
                    JetScope scope = scopeForConstructor == null
                                     ? scopeForMemberResolution
                                     : scopeForConstructor;
                    JetType type = typeInferrer.getType(scope, delegateExpression, NO_EXPECTED_TYPE, context.getOuterDataFlowInfo(), trace);
                    if (type != null && supertype != null) {
                        SimpleResolutionContext simpleResolutionContext = new SimpleResolutionContext(
                                trace, scope, supertype, context.getOuterDataFlowInfo(), ContextDependency.INDEPENDENT,
                                ResolutionResultsCacheImpl.create(), LabelResolver.create(),
                                expressionTypingServices.createExtension(scope, false), false);
                        DataFlowUtils.checkType(type, delegateExpression, simpleResolutionContext);
                    }
                }
            }

            @Override
            public void visitDelegationToSuperCallSpecifier(@NotNull JetDelegatorToSuperCall call) {
                JetValueArgumentList valueArgumentList = call.getValueArgumentList();
                PsiElement elementToMark = valueArgumentList == null ? call : valueArgumentList;
                if (descriptor.getKind() == ClassKind.TRAIT) {
                    trace.report(SUPERTYPE_INITIALIZED_IN_TRAIT.on(elementToMark));
                }
                JetTypeReference typeReference = call.getTypeReference();
                if (typeReference == null) return;
                if (primaryConstructor == null) {
                    assert descriptor.getKind() == ClassKind.TRAIT;
                    recordSupertype(typeReference, trace.getBindingContext().get(BindingContext.TYPE, typeReference));
                    return;
                }
                OverloadResolutionResults<FunctionDescriptor> results = callResolver.resolveFunctionCall(
                        trace, scopeForConstructor,
                        CallMaker.makeCall(ReceiverValue.NO_RECEIVER, null, call), NO_EXPECTED_TYPE, context.getOuterDataFlowInfo(), false);
                if (results.isSuccess()) {
                    JetType supertype = results.getResultingDescriptor().getReturnType();
                    recordSupertype(typeReference, supertype);
                    ClassDescriptor classDescriptor = TypeUtils.getClassDescriptor(supertype);
                    if (classDescriptor != null) {
                        if (classDescriptor.getKind() == ClassKind.TRAIT) {
                            trace.report(CONSTRUCTOR_IN_TRAIT.on(elementToMark));
                        }
                    }
                }
                else {
                    recordSupertype(typeReference, trace.getBindingContext().get(BindingContext.TYPE, typeReference));
                }
            }

            @Override
            public void visitDelegationToSuperClassSpecifier(@NotNull JetDelegatorToSuperClass specifier) {
                JetTypeReference typeReference = specifier.getTypeReference();
                JetType supertype = trace.getBindingContext().get(BindingContext.TYPE, typeReference);
                recordSupertype(typeReference, supertype);
                if (supertype == null) return;
                ClassDescriptor superClass = TypeUtils.getClassDescriptor(supertype);
                if (superClass == null) return;
                if (superClass.getKind().isSingleton()) {
                    // A "singleton in supertype" diagnostic will be reported later
                    return;
                }
                if (descriptor.getKind() != ClassKind.TRAIT && !superClass.getConstructors().isEmpty() && !ErrorUtils.isError(superClass)) {
                    trace.report(SUPERTYPE_NOT_INITIALIZED.on(specifier));
                }
            }

            @Override
            public void visitDelegationToThisCall(@NotNull JetDelegatorToThisCall thisCall) {
                throw new IllegalStateException("This-calls should be prohibited by the parser");
            }

            @Override
            public void visitJetElement(@NotNull JetElement element) {
                throw new UnsupportedOperationException(element.getText() + " : " + element);
            }
        };

        for (JetDelegationSpecifier delegationSpecifier : jetClass.getDelegationSpecifiers()) {
            delegationSpecifier.accept(visitor);
        }

        Set<TypeConstructor> parentEnum =
                jetClass instanceof JetEnumEntry
                ? Collections.singleton(((ClassDescriptor) descriptor.getContainingDeclaration()).getTypeConstructor())
                : Collections.<TypeConstructor>emptySet();

        checkSupertypeList(descriptor, supertypes, parentEnum);
    }

    // allowedFinalSupertypes typically contains a enum type of which supertypeOwner is an entry
    private void checkSupertypeList(
            @NotNull ClassDescriptor supertypeOwner,
            @NotNull Map<JetTypeReference, JetType> supertypes,
            @NotNull Set<TypeConstructor> allowedFinalSupertypes
    ) {
        Set<TypeConstructor> typeConstructors = Sets.newHashSet();
        boolean classAppeared = false;
        for (Map.Entry<JetTypeReference, JetType> entry : supertypes.entrySet()) {
            JetTypeReference typeReference = entry.getKey();
            JetType supertype = entry.getValue();

            ClassDescriptor classDescriptor = TypeUtils.getClassDescriptor(supertype);
            if (classDescriptor != null) {
                if (classDescriptor.getKind() != ClassKind.TRAIT) {
                    if (supertypeOwner.getKind() == ClassKind.ENUM_CLASS) {
                        trace.report(CLASS_IN_SUPERTYPE_FOR_ENUM.on(typeReference));
                    }
                    if (classAppeared) {
                        trace.report(MANY_CLASSES_IN_SUPERTYPE_LIST.on(typeReference));
                    }
                    else {
                        classAppeared = true;
                    }
                }
            }
            else {
                trace.report(SUPERTYPE_NOT_A_CLASS_OR_TRAIT.on(typeReference));
            }

            TypeConstructor constructor = supertype.getConstructor();
            if (!typeConstructors.add(constructor)) {
                trace.report(SUPERTYPE_APPEARS_TWICE.on(typeReference));
            }

            if (DescriptorUtils.isSingleton(classDescriptor)) {
                trace.report(SINGLETON_IN_SUPERTYPE.on(typeReference));
            }
            else if (constructor.isFinal() && !allowedFinalSupertypes.contains(constructor)) {
                trace.report(FINAL_SUPERTYPE.on(typeReference));
            }
        }
    }

    private void resolveClassAnnotations() {
        for (Map.Entry<JetClassOrObject, MutableClassDescriptor> entry : context.getClasses().entrySet()) {
            resolveAnnotationArguments(entry.getValue().getScopeForSupertypeResolution(), entry.getKey());
        }
    }

    private void resolveAnonymousInitializers() {
        for (Map.Entry<JetClassOrObject, MutableClassDescriptor> entry : context.getClasses().entrySet()) {
            JetClassOrObject classOrObject = entry.getKey();
            MutableClassDescriptor descriptor = entry.getValue();
            resolveAnonymousInitializers(classOrObject, descriptor.getUnsubstitutedPrimaryConstructor(),
                                         descriptor.getScopeForInitializers());
        }
    }

    public void resolveAnonymousInitializers(JetClassOrObject jetClassOrObject,
            @Nullable ConstructorDescriptor primaryConstructor,
            @NotNull JetScope scopeForInitializers) {
        if (!context.completeAnalysisNeeded(jetClassOrObject)) {
            return;
        }
        List<JetClassInitializer> anonymousInitializers = jetClassOrObject.getAnonymousInitializers();
        if (primaryConstructor != null) {
            for (JetClassInitializer anonymousInitializer : anonymousInitializers) {
                expressionTypingServices.getType(scopeForInitializers, anonymousInitializer.getBody(), NO_EXPECTED_TYPE, context.getOuterDataFlowInfo(), trace);
            }
        }
        else {
            for (JetClassInitializer anonymousInitializer : anonymousInitializers) {
                trace.report(ANONYMOUS_INITIALIZER_IN_TRAIT.on(anonymousInitializer));
            }
        }
    }

    private void resolvePrimaryConstructorParameters() {
        for (Map.Entry<JetClassOrObject, MutableClassDescriptor> entry : context.getClasses().entrySet()) {
            if (!(entry.getKey() instanceof JetClass)) continue;
            JetClass klass = (JetClass) entry.getKey();
            MutableClassDescriptor classDescriptor = entry.getValue();
            ConstructorDescriptor unsubstitutedPrimaryConstructor = classDescriptor.getUnsubstitutedPrimaryConstructor();

            annotationResolver.resolveAnnotationsArguments(classDescriptor.getScopeForSupertypeResolution(), klass.getPrimaryConstructorModifierList(), trace);

            if (unsubstitutedPrimaryConstructor != null) {
                WritableScope parameterScope = getPrimaryConstructorParametersScope(classDescriptor.getScopeForSupertypeResolution(), unsubstitutedPrimaryConstructor);
                expressionTypingServices.resolveValueParameters(klass.getPrimaryConstructorParameters(), unsubstitutedPrimaryConstructor.getValueParameters(),
                                       parameterScope, context.getOuterDataFlowInfo(), trace);
            }
        }
    }

    private static WritableScope getPrimaryConstructorParametersScope(
            JetScope originalScope,
            ConstructorDescriptor unsubstitutedPrimaryConstructor
    ) {
        WritableScope parameterScope = new WritableScopeImpl(
                originalScope,
                unsubstitutedPrimaryConstructor,
                RedeclarationHandler.DO_NOTHING, "Scope with value parameters of a constructor"
        );
        for (ValueParameterDescriptor valueParameterDescriptor : unsubstitutedPrimaryConstructor.getValueParameters()) {
            parameterScope.addVariableDescriptor(valueParameterDescriptor);
        }
        parameterScope.changeLockLevel(WritableScope.LockLevel.READING);
        return parameterScope;
    }

    private void resolvePropertyDeclarationBodies() {

        // Member properties
        Set<JetProperty> processed = Sets.newHashSet();
        for (Map.Entry<JetClassOrObject, MutableClassDescriptor> entry : context.getClasses().entrySet()) {
            if (!(entry.getKey() instanceof JetClass)) continue;
            JetClass jetClass = (JetClass) entry.getKey();
            if (!context.completeAnalysisNeeded(jetClass)) continue;
            MutableClassDescriptor classDescriptor = entry.getValue();

            for (JetProperty property : jetClass.getProperties()) {
                PropertyDescriptor propertyDescriptor = this.context.getProperties().get(property);
                assert propertyDescriptor != null;

                computeDeferredType(propertyDescriptor.getReturnType());

                JetExpression initializer = property.getInitializer();
                JetScope propertyScope = getScopeForProperty(property);
                if (initializer != null) {
                    ConstructorDescriptor primaryConstructor = classDescriptor.getUnsubstitutedPrimaryConstructor();
                    if (primaryConstructor != null) {
                        resolvePropertyInitializer(property, propertyDescriptor, initializer, propertyScope);
                    }
                }

                JetExpression delegateExpression = property.getDelegateExpression();
                if (delegateExpression != null) {
                    assert initializer == null : "Initializer should be null for delegated property : " + property.getText();
                    resolvePropertyDelegate(property, propertyDescriptor, delegateExpression, classDescriptor.getScopeForMemberResolution(), propertyScope);
                }

                resolveAnnotationArguments(propertyScope, property);

                resolvePropertyAccessors(property, propertyDescriptor);
                processed.add(property);
            }
        }

        // Top-level properties & properties of objects
        for (Map.Entry<JetProperty, PropertyDescriptor> entry : this.context.getProperties().entrySet()) {
            JetProperty property = entry.getKey();
            if (!context.completeAnalysisNeeded(property)) continue;
            if (processed.contains(property)) continue;

            PropertyDescriptor propertyDescriptor = entry.getValue();

            computeDeferredType(propertyDescriptor.getReturnType());

            JetExpression initializer = property.getInitializer();
            JetScope propertyScope = getScopeForProperty(property);
            if (initializer != null) {
                resolvePropertyInitializer(property, propertyDescriptor, initializer, propertyScope);
            }

            JetExpression delegateExpression = property.getDelegateExpression();
            if (delegateExpression != null) {
                assert initializer == null : "Initializer should be null for delegated property : " + property.getText();
                resolvePropertyDelegate(property, propertyDescriptor, delegateExpression, propertyScope, propertyScope);
            }

            resolveAnnotationArguments(propertyScope, property);

            resolvePropertyAccessors(property, propertyDescriptor);
        }
    }

    private JetScope makeScopeForPropertyAccessor(@NotNull JetPropertyAccessor accessor, @NotNull PropertyDescriptor descriptor) {
        JetScope accessorDeclaringScope = context.getDeclaringScopes().apply(accessor);
        assert accessorDeclaringScope != null : "Scope for accessor " + accessor.getText() + " should exists";
        return JetScopeUtils.makeScopeForPropertyAccessor(descriptor, accessorDeclaringScope, trace);
    }

    public void resolvePropertyAccessors(JetProperty property, PropertyDescriptor propertyDescriptor) {
        ObservableBindingTrace fieldAccessTrackingTrace = createFieldTrackingTrace(propertyDescriptor);

        JetPropertyAccessor getter = property.getGetter();
        PropertyGetterDescriptor getterDescriptor = propertyDescriptor.getGetter();
        if (getter != null && getterDescriptor != null) {
            JetScope accessorScope = makeScopeForPropertyAccessor(getter, propertyDescriptor);
            resolveAnnotationArguments(accessorScope, getter);
            resolveFunctionBody(fieldAccessTrackingTrace, getter, getterDescriptor, accessorScope);
        }

        JetPropertyAccessor setter = property.getSetter();
        PropertySetterDescriptor setterDescriptor = propertyDescriptor.getSetter();
        if (setter != null && setterDescriptor != null) {
            JetScope accessorScope = makeScopeForPropertyAccessor(setter, propertyDescriptor);
            resolveAnnotationArguments(accessorScope, setter);
            resolveFunctionBody(fieldAccessTrackingTrace, setter, setterDescriptor, accessorScope);
        }
    }

    private ObservableBindingTrace createFieldTrackingTrace(final PropertyDescriptor propertyDescriptor) {
        return new ObservableBindingTrace(trace).addHandler(BindingContext.REFERENCE_TARGET, new ObservableBindingTrace.RecordHandler<JetReferenceExpression, DeclarationDescriptor>() {
            @Override
            public void handleRecord(WritableSlice<JetReferenceExpression, DeclarationDescriptor> slice, JetReferenceExpression expression, DeclarationDescriptor descriptor) {
                if (expression instanceof JetSimpleNameExpression) {
                    JetSimpleNameExpression simpleNameExpression = (JetSimpleNameExpression) expression;
                    if (simpleNameExpression.getReferencedNameElementType() == JetTokens.FIELD_IDENTIFIER) {
                        // This check may be considered redundant as long as $x is only accessible from accessors to $x
                        if (descriptor == propertyDescriptor) { // TODO : original?
                            trace.record(BindingContext.BACKING_FIELD_REQUIRED, propertyDescriptor); // TODO: this trace?
                        }
                    }
                }
            }
        });
    }

    public void resolvePropertyDelegate(
            @NotNull JetProperty jetProperty,
            @NotNull PropertyDescriptor propertyDescriptor,
            @NotNull JetExpression delegateExpression,
            @NotNull JetScope parentScopeForAccessor,
            @NotNull JetScope propertyScope
    ) {
        JetPropertyAccessor getter = jetProperty.getGetter();
        if (getter != null) {
            trace.report(ACCESSOR_FOR_DELEGATED_PROPERTY.on(getter));
        }

        JetPropertyAccessor setter = jetProperty.getSetter();
        if (setter != null) {
            trace.report(ACCESSOR_FOR_DELEGATED_PROPERTY.on(setter));
        }

        JetScope propertyDeclarationInnerScope = JetScopeUtils.getPropertyDeclarationInnerScopeForInitializer(
                propertyScope, propertyDescriptor.getTypeParameters(), NO_RECEIVER_PARAMETER, trace);
        JetScope accessorScope = JetScopeUtils.makeScopeForPropertyAccessor(
                propertyDescriptor, parentScopeForAccessor, trace);

        JetType delegateType = delegatedPropertyResolver.resolveDelegateExpression(
                delegateExpression, jetProperty, propertyDescriptor, propertyDeclarationInnerScope, accessorScope, trace,
                context.getOuterDataFlowInfo());

        delegatedPropertyResolver.resolveDelegatedPropertyGetMethod(propertyDescriptor, delegateExpression, delegateType,
                                                                    trace, accessorScope);

        if (jetProperty.isVar()) {
            delegatedPropertyResolver.resolveDelegatedPropertySetMethod(propertyDescriptor, delegateExpression, delegateType,
                                                                        trace, accessorScope);
        }
    }

    public void resolvePropertyInitializer(
            @NotNull JetProperty property,
            @NotNull PropertyDescriptor propertyDescriptor,
            @NotNull JetExpression initializer,
            @NotNull JetScope scope
    ) {
        JetScope propertyDeclarationInnerScope = JetScopeUtils.getPropertyDeclarationInnerScopeForInitializer(
                scope, propertyDescriptor.getTypeParameters(), NO_RECEIVER_PARAMETER, trace);
        JetType expectedTypeForInitializer = property.getTypeRef() != null ? propertyDescriptor.getType() : NO_EXPECTED_TYPE;
        expressionTypingServices.getType(propertyDeclarationInnerScope, initializer, expectedTypeForInitializer, context.getOuterDataFlowInfo(), trace);
        if (AnnotationUtils.isPropertyCompileTimeConstant(propertyDescriptor)) {
            CompileTimeConstant<?> constant = ConstantExpressionEvaluator.object$.evaluate(initializer, trace, expectedTypeForInitializer);
            if (constant != null) {
                trace.record(BindingContext.COMPILE_TIME_INITIALIZER, propertyDescriptor, constant);
            }
        }
    }

    @NotNull
    private JetScope getScopeForProperty(@NotNull JetProperty property) {
        JetScope scope = this.context.getDeclaringScopes().apply(property);
        assert scope != null : "Scope for property " + property.getText() + " should exists";
        return scope;
    }

    private void resolveFunctionBodies() {
        for (Map.Entry<JetNamedFunction, SimpleFunctionDescriptor> entry : this.context.getFunctions().entrySet()) {
            JetNamedFunction declaration = entry.getKey();
            SimpleFunctionDescriptor descriptor = entry.getValue();

            computeDeferredType(descriptor.getReturnType());

            JetScope declaringScope = this.context.getDeclaringScopes().apply(declaration);
            assert declaringScope != null;

            resolveAnnotationArguments(declaringScope, declaration);
            resolveFunctionBody(trace, declaration, descriptor, declaringScope);

            assert descriptor.getReturnType() != null;
        }
    }

    public void resolveFunctionBody(
            @NotNull BindingTrace trace,
            @NotNull JetDeclarationWithBody function,
            @NotNull FunctionDescriptor functionDescriptor,
            @NotNull JetScope declaringScope) {
        if (!context.completeAnalysisNeeded(function)) return;

        JetExpression bodyExpression = function.getBodyExpression();
        JetScope functionInnerScope = FunctionDescriptorUtil.getFunctionInnerScope(declaringScope, functionDescriptor, trace);
        if (bodyExpression != null) {
            expressionTypingServices.checkFunctionReturnType(functionInnerScope, function, functionDescriptor, context.getOuterDataFlowInfo(), null, trace);
        }

        List<JetParameter> valueParameters = function.getValueParameters();
        List<ValueParameterDescriptor> valueParameterDescriptors = functionDescriptor.getValueParameters();

        expressionTypingServices.resolveValueParameters(valueParameters, valueParameterDescriptors, functionInnerScope, context.getOuterDataFlowInfo(), trace);

        assert functionDescriptor.getReturnType() != null;
    }

    public void resolveConstructorParameterDefaultValuesAndAnnotations(
            @NotNull BindingTrace trace,
            @NotNull JetClass klass,
            @NotNull ConstructorDescriptor constructorDescriptor,
            @NotNull JetScope declaringScope
    ) {
        if (!context.completeAnalysisNeeded(klass)) return;

        List<JetParameter> valueParameters = klass.getPrimaryConstructorParameters();
        List<ValueParameterDescriptor> valueParameterDescriptors = constructorDescriptor.getValueParameters();

        JetScope scope = getPrimaryConstructorParametersScope(declaringScope, constructorDescriptor);

        expressionTypingServices.resolveValueParameters(valueParameters, valueParameterDescriptors, scope, context.getOuterDataFlowInfo(), trace);
    }

    private void resolveAnnotationArguments(@NotNull JetScope scope, @NotNull JetModifierListOwner owner) {
        annotationResolver.resolveAnnotationsArguments(scope, owner.getModifierList(), trace);
    }

    private static void computeDeferredType(JetType type) {
        // handle type inference loop: function or property body contains a reference to itself
        // fun f() = { f() }
        // val x = x
        // type resolution must be started before body resolution
        if (type instanceof DeferredType) {
            DeferredType deferredType = (DeferredType) type;
            if (!deferredType.isComputed()) {
                deferredType.getActualType();
            }
        }
    }

    private void computeDeferredTypes() {
        Collection<Box<DeferredType>> deferredTypes = trace.getKeys(DEFERRED_TYPE);
        if (deferredTypes != null) {
            // +1 is a work around agains new Queue(0).addLast(...) bug // stepan.koltsov@ 2011-11-21
            final Queue<DeferredType> queue = new Queue<DeferredType>(deferredTypes.size() + 1);
            trace.addHandler(DEFERRED_TYPE, new ObservableBindingTrace.RecordHandler<Box<DeferredType>, Boolean>() {
                @Override
                public void handleRecord(WritableSlice<Box<DeferredType>, Boolean> deferredTypeKeyDeferredTypeWritableSlice, Box<DeferredType> key, Boolean value) {
                    queue.addLast(key.getData());
                }
            });
            for (Box<DeferredType> deferredType : deferredTypes) {
                queue.addLast(deferredType.getData());
            }
            while (!queue.isEmpty()) {
                DeferredType deferredType = queue.pullFirst();
                if (!deferredType.isComputed()) {
                    try {
                        deferredType.getActualType(); // to compute
                    }
                    catch (ReenteringLazyValueComputationException e) {
                        // A problem should be reported while computing the type
                    }
                }
            }
        }
    }

}
