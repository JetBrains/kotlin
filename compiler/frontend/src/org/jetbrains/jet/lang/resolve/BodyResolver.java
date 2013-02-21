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
import org.jetbrains.jet.lang.descriptors.impl.FunctionDescriptorUtil;
import org.jetbrains.jet.lang.descriptors.impl.MutableClassDescriptor;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.calls.util.CallMaker;
import org.jetbrains.jet.lang.resolve.calls.CallResolver;
import org.jetbrains.jet.lang.resolve.calls.results.OverloadResolutionResults;
import org.jetbrains.jet.lang.resolve.calls.autocasts.DataFlowInfo;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.resolve.scopes.RedeclarationHandler;
import org.jetbrains.jet.lang.resolve.scopes.WritableScope;
import org.jetbrains.jet.lang.resolve.scopes.WritableScopeImpl;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverValue;
import org.jetbrains.jet.lang.types.*;
import org.jetbrains.jet.lang.types.checker.JetTypeChecker;
import org.jetbrains.jet.lang.types.expressions.ExpressionTypingServices;
import org.jetbrains.jet.lexer.JetTokens;
import org.jetbrains.jet.util.Box;
import org.jetbrains.jet.util.lazy.ReenteringLazyValueComputationException;
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
    private DescriptorResolver descriptorResolver;
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

    @Inject
    public void setTopDownAnalysisParameters(@NotNull TopDownAnalysisParameters topDownAnalysisParameters) {
        this.topDownAnalysisParameters = topDownAnalysisParameters;
    }

    @Inject
    public void setDescriptorResolver(@NotNull DescriptorResolver descriptorResolver) {
        this.descriptorResolver = descriptorResolver;
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

    private void resolveBehaviorDeclarationBodies(@NotNull BodiesResolveContext bodiesResolveContext) {
        // Initialize context
        context = bodiesResolveContext;

        resolveDelegationSpecifierLists();
        resolveClassAnnotations();

        resolvePropertyDeclarationBodies();
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
    }

    private void resolveDelegationSpecifierLists() {
        // TODO : Make sure the same thing is not initialized twice
        for (Map.Entry<JetClass, MutableClassDescriptor> entry : context.getClasses().entrySet()) {
            resolveDelegationSpecifierList(entry.getKey(), entry.getValue());
        }
        for (Map.Entry<JetObjectDeclaration, MutableClassDescriptor> entry : context.getObjects().entrySet()) {
            resolveDelegationSpecifierList(entry.getKey(), entry.getValue());
        }
    }

    private void resolveDelegationSpecifierList(JetClassOrObject jetClass, final MutableClassDescriptor descriptor) {
        resolveDelegationSpecifierList(jetClass, descriptor,
                                       descriptor.getUnsubstitutedPrimaryConstructor(),
                                       descriptor.getScopeForSupertypeResolution(),
                                       descriptor.getScopeForMemberResolution());
    }

    public void resolveDelegationSpecifierList(@NotNull final JetClassOrObject jetClass, @NotNull final ClassDescriptor descriptor,
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
            public void visitDelegationByExpressionSpecifier(JetDelegatorByExpressionSpecifier specifier) {
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
                    JetType type = typeInferrer.getType(scope, delegateExpression, NO_EXPECTED_TYPE, DataFlowInfo.EMPTY, trace);
                    if (type != null && supertype != null && !JetTypeChecker.INSTANCE.isSubtypeOf(type, supertype)) {
                        trace.report(TYPE_MISMATCH.on(delegateExpression, supertype, type));
                    }
                }
            }

            @Override
            public void visitDelegationToSuperCallSpecifier(JetDelegatorToSuperCall call) {
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
                        CallMaker.makeCall(ReceiverValue.NO_RECEIVER, null, call), NO_EXPECTED_TYPE, DataFlowInfo.EMPTY);
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
            public void visitDelegationToSuperClassSpecifier(JetDelegatorToSuperClass specifier) {
                JetTypeReference typeReference = specifier.getTypeReference();
                JetType supertype = trace.getBindingContext().get(BindingContext.TYPE, typeReference);
                recordSupertype(typeReference, supertype);
                if (supertype == null) return;
                ClassDescriptor classDescriptor = TypeUtils.getClassDescriptor(supertype);
                if (classDescriptor == null) return;
                if (descriptor.getKind() != ClassKind.TRAIT && !classDescriptor.getConstructors().isEmpty() &&
                    !ErrorUtils.isError(classDescriptor.getTypeConstructor()) && classDescriptor.getKind() != ClassKind.TRAIT) {
                    trace.report(SUPERTYPE_NOT_INITIALIZED.on(specifier));
                }
            }

            @Override
            public void visitDelegationToThisCall(JetDelegatorToThisCall thisCall) {
                throw new IllegalStateException("This-calls should be prohibited by the parser");
            }

            @Override
            public void visitJetElement(JetElement element) {
                throw new UnsupportedOperationException(element.getText() + " : " + element);
            }
        };

        for (JetDelegationSpecifier delegationSpecifier : jetClass.getDelegationSpecifiers()) {
            delegationSpecifier.accept(visitor);
        }


        Set<TypeConstructor> parentEnum = Collections.emptySet();
        if (jetClass instanceof JetEnumEntry) {
            parentEnum = Collections.singleton(((ClassDescriptor) descriptor.getContainingDeclaration().getContainingDeclaration()).getTypeConstructor());
        }

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

            if (constructor.isSealed() && !allowedFinalSupertypes.contains(constructor)) {
                trace.report(FINAL_SUPERTYPE.on(typeReference));
            }
        }
    }

    private void resolveClassAnnotations() {
    }

    private void resolveAnonymousInitializers() {
        for (Map.Entry<JetClass, MutableClassDescriptor> entry : context.getClasses().entrySet()) {
            resolveAnonymousInitializers(entry.getKey(), entry.getValue());
        }
        for (Map.Entry<JetObjectDeclaration, MutableClassDescriptor> entry : context.getObjects().entrySet()) {
            resolveAnonymousInitializers(entry.getKey(), entry.getValue());
        }
    }

    private void resolveAnonymousInitializers(JetClassOrObject jetClassOrObject, MutableClassDescriptor classDescriptor) {
        resolveAnonymousInitializers(jetClassOrObject, classDescriptor.getUnsubstitutedPrimaryConstructor(),
                                     classDescriptor.getScopeForInitializers());
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
                expressionTypingServices.getType(scopeForInitializers, anonymousInitializer.getBody(), NO_EXPECTED_TYPE, DataFlowInfo.EMPTY, trace);
            }
        }
        else {
            for (JetClassInitializer anonymousInitializer : anonymousInitializers) {
                trace.report(ANONYMOUS_INITIALIZER_WITHOUT_CONSTRUCTOR.on(anonymousInitializer));
            }
        }
    }

    private void resolvePrimaryConstructorParameters() {
        for (Map.Entry<JetClass, MutableClassDescriptor> entry : context.getClasses().entrySet()) {
            JetClass klass = entry.getKey();
            MutableClassDescriptor classDescriptor = entry.getValue();
            ConstructorDescriptor unsubstitutedPrimaryConstructor = classDescriptor.getUnsubstitutedPrimaryConstructor();
            if (unsubstitutedPrimaryConstructor != null) {
                WritableScope parameterScope = new WritableScopeImpl(classDescriptor.getScopeForSupertypeResolution(), unsubstitutedPrimaryConstructor,
                                                                     RedeclarationHandler.DO_NOTHING, "Scope with value parameters of a constructor");
                for (ValueParameterDescriptor valueParameterDescriptor : unsubstitutedPrimaryConstructor.getValueParameters()) {
                    parameterScope.addVariableDescriptor(valueParameterDescriptor);
                }
                parameterScope.changeLockLevel(WritableScope.LockLevel.READING);
                checkDefaultParameterValues(klass.getPrimaryConstructorParameters(), unsubstitutedPrimaryConstructor.getValueParameters(), parameterScope);
            }
        }
    }

    private void resolvePropertyDeclarationBodies() {

        // Member properties
        Set<JetProperty> processed = Sets.newHashSet();
        for (Map.Entry<JetClass, MutableClassDescriptor> entry : context.getClasses().entrySet()) {
            JetClass jetClass = entry.getKey();
            if (!context.completeAnalysisNeeded(jetClass)) continue;
            MutableClassDescriptor classDescriptor = entry.getValue();

            for (JetProperty property : jetClass.getProperties()) {
                final PropertyDescriptor propertyDescriptor = this.context.getProperties().get(property);
                assert propertyDescriptor != null;

                computeDeferredType(propertyDescriptor.getReturnType());

                JetExpression initializer = property.getInitializer();
                if (initializer != null) {
                    ConstructorDescriptor primaryConstructor = classDescriptor.getUnsubstitutedPrimaryConstructor();
                    if (primaryConstructor != null) {
                        JetScope declaringScopeForPropertyInitializer = this.context.getDeclaringScopes().apply(property);
                        resolvePropertyInitializer(property, propertyDescriptor, initializer, declaringScopeForPropertyInitializer);
                    }
                }

                resolvePropertyAccessors(property, propertyDescriptor);
                processed.add(property);
            }
        }

        // Top-level properties & properties of objects
        for (Map.Entry<JetProperty, PropertyDescriptor> entry : this.context.getProperties().entrySet()) {
            JetProperty property = entry.getKey();
            if (!context.completeAnalysisNeeded(property)) continue;
            if (processed.contains(property)) continue;

            final PropertyDescriptor propertyDescriptor = entry.getValue();

            computeDeferredType(propertyDescriptor.getReturnType());

            JetScope declaringScope = this.context.getDeclaringScopes().apply(property);

            JetExpression initializer = property.getInitializer();
            if (initializer != null) {
                resolvePropertyInitializer(property, propertyDescriptor, initializer, declaringScope);
            }

            resolvePropertyAccessors(property, propertyDescriptor);
        }
    }

    private JetScope makeScopeForPropertyAccessor(@NotNull JetPropertyAccessor accessor, PropertyDescriptor propertyDescriptor) {
        JetScope declaringScope = context.getDeclaringScopes().apply(accessor);

        JetScope propertyDeclarationInnerScope = descriptorResolver.getPropertyDeclarationInnerScope(
                propertyDescriptor, declaringScope, propertyDescriptor.getTypeParameters(), propertyDescriptor.getReceiverParameter(), trace);
        WritableScope accessorScope = new WritableScopeImpl(
                propertyDeclarationInnerScope, declaringScope.getContainingDeclaration(), new TraceBasedRedeclarationHandler(trace), "Accessor scope");
        accessorScope.changeLockLevel(WritableScope.LockLevel.READING);

        return accessorScope;
    }

    public void resolvePropertyAccessors(JetProperty property, PropertyDescriptor propertyDescriptor) {
        ObservableBindingTrace fieldAccessTrackingTrace = createFieldTrackingTrace(propertyDescriptor);

        JetPropertyAccessor getter = property.getGetter();
        PropertyGetterDescriptor getterDescriptor = propertyDescriptor.getGetter();
        if (getter != null && getterDescriptor != null) {
            JetScope accessorScope = makeScopeForPropertyAccessor(getter, propertyDescriptor);
            resolveFunctionBody(fieldAccessTrackingTrace, getter, getterDescriptor, accessorScope);
        }

        JetPropertyAccessor setter = property.getSetter();
        PropertySetterDescriptor setterDescriptor = propertyDescriptor.getSetter();
        if (setter != null && setterDescriptor != null) {
            JetScope accessorScope = makeScopeForPropertyAccessor(setter, propertyDescriptor);
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

    public void resolvePropertyInitializer(JetProperty property, PropertyDescriptor propertyDescriptor, JetExpression initializer, JetScope scope) {
        JetType expectedTypeForInitializer = property.getTypeRef() != null ? propertyDescriptor.getType() : NO_EXPECTED_TYPE;
        JetScope propertyDeclarationInnerScope = descriptorResolver.getPropertyDeclarationInnerScopeForInitializer(
                scope, propertyDescriptor.getTypeParameters(), NO_RECEIVER_PARAMETER, trace);
        expressionTypingServices.getType(propertyDeclarationInnerScope, initializer, expectedTypeForInitializer, DataFlowInfo.EMPTY, trace);
    }

    private void resolveFunctionBodies() {
        for (Map.Entry<JetNamedFunction, SimpleFunctionDescriptor> entry : this.context.getFunctions().entrySet()) {
            JetNamedFunction declaration = entry.getKey();
            SimpleFunctionDescriptor descriptor = entry.getValue();

            computeDeferredType(descriptor.getReturnType());

            JetScope declaringScope = this.context.getDeclaringScopes().apply(declaration);
            assert declaringScope != null;

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
            expressionTypingServices.checkFunctionReturnType(functionInnerScope, function, functionDescriptor, DataFlowInfo.EMPTY, null, trace);
        }

        List<JetParameter> valueParameters = function.getValueParameters();
        List<ValueParameterDescriptor> valueParameterDescriptors = functionDescriptor.getValueParameters();

        checkDefaultParameterValues(valueParameters, valueParameterDescriptors, functionInnerScope);

        assert functionDescriptor.getReturnType() != null;
    }

    private void checkDefaultParameterValues(List<JetParameter> valueParameters, List<ValueParameterDescriptor> valueParameterDescriptors, JetScope declaringScope) {
        for (int i = 0; i < valueParameters.size(); i++) {
            ValueParameterDescriptor valueParameterDescriptor = valueParameterDescriptors.get(i);
            if (valueParameterDescriptor.hasDefaultValue()) {
                JetParameter jetParameter = valueParameters.get(i);
                JetExpression defaultValue = jetParameter.getDefaultValue();
                if (defaultValue != null) {
                    expressionTypingServices.getType(declaringScope, defaultValue, valueParameterDescriptor.getType(), DataFlowInfo.EMPTY, trace);
                }
            }
        }
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
