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

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.intellij.psi.PsiElement;
import com.intellij.util.containers.Queue;
import kotlin.jvm.functions.Function1;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.lexer.JetTokens;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.psi.psiUtil.PsiUtilPackage;
import org.jetbrains.kotlin.resolve.calls.CallResolver;
import org.jetbrains.kotlin.resolve.calls.checkers.CallChecker;
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall;
import org.jetbrains.kotlin.resolve.calls.results.OverloadResolutionResults;
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowInfo;
import org.jetbrains.kotlin.resolve.calls.util.CallMaker;
import org.jetbrains.kotlin.resolve.lazy.ForceResolveUtil;
import org.jetbrains.kotlin.resolve.scopes.*;
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue;
import org.jetbrains.kotlin.types.*;
import org.jetbrains.kotlin.types.expressions.ExpressionTypingContext;
import org.jetbrains.kotlin.types.expressions.ExpressionTypingServices;
import org.jetbrains.kotlin.types.expressions.ValueParameterResolver;
import org.jetbrains.kotlin.types.expressions.typeInfoFactory.TypeInfoFactoryPackage;
import org.jetbrains.kotlin.util.Box;
import org.jetbrains.kotlin.util.ReenteringLazyValueComputationException;
import org.jetbrains.kotlin.util.slicedMap.WritableSlice;

import java.util.*;

import static org.jetbrains.kotlin.descriptors.ReceiverParameterDescriptor.NO_RECEIVER_PARAMETER;
import static org.jetbrains.kotlin.diagnostics.Errors.*;
import static org.jetbrains.kotlin.resolve.BindingContext.*;
import static org.jetbrains.kotlin.types.TypeUtils.NO_EXPECTED_TYPE;

public class BodyResolver {
    @NotNull private final ScriptBodyResolver scriptBodyResolverResolver;
    @NotNull private final AnnotationChecker annotationChecker;
    @NotNull private final ExpressionTypingServices expressionTypingServices;
    @NotNull private final CallResolver callResolver;
    @NotNull private final ObservableBindingTrace trace;
    @NotNull private final ControlFlowAnalyzer controlFlowAnalyzer;
    @NotNull private final DeclarationsChecker declarationsChecker;
    @NotNull private final AnnotationResolver annotationResolver;
    @NotNull private final DelegatedPropertyResolver delegatedPropertyResolver;
    @NotNull private final FunctionAnalyzerExtension functionAnalyzerExtension;
    @NotNull private final ValueParameterResolver valueParameterResolver;
    @NotNull private final BodyResolveCache bodyResolveCache;

    public BodyResolver(
            @NotNull AnnotationResolver annotationResolver,
            @NotNull BodyResolveCache bodyResolveCache,
            @NotNull CallResolver callResolver,
            @NotNull ControlFlowAnalyzer controlFlowAnalyzer,
            @NotNull DeclarationsChecker declarationsChecker,
            @NotNull DelegatedPropertyResolver delegatedPropertyResolver,
            @NotNull ExpressionTypingServices expressionTypingServices,
            @NotNull FunctionAnalyzerExtension functionAnalyzerExtension,
            @NotNull ScriptBodyResolver scriptBodyResolverResolver,
            @NotNull BindingTrace trace,
            @NotNull ValueParameterResolver valueParameterResolver,
            @NotNull AnnotationChecker annotationChecker
    ) {
        this.annotationResolver = annotationResolver;
        this.bodyResolveCache = bodyResolveCache;
        this.callResolver = callResolver;
        this.controlFlowAnalyzer = controlFlowAnalyzer;
        this.declarationsChecker = declarationsChecker;
        this.delegatedPropertyResolver = delegatedPropertyResolver;
        this.expressionTypingServices = expressionTypingServices;
        this.functionAnalyzerExtension = functionAnalyzerExtension;
        this.scriptBodyResolverResolver = scriptBodyResolverResolver;
        this.annotationChecker = annotationChecker;
        this.trace = new ObservableBindingTrace(trace);
        this.valueParameterResolver = valueParameterResolver;
    }

    private void resolveBehaviorDeclarationBodies(@NotNull BodiesResolveContext c) {
        resolveDelegationSpecifierLists(c);

        resolvePropertyDeclarationBodies(c);

        resolveAnonymousInitializers(c);
        resolvePrimaryConstructorParameters(c);
        resolveSecondaryConstructors(c);

        resolveFunctionBodies(c);

        // SCRIPT: resolve script bodies
        scriptBodyResolverResolver.resolveScriptBodies(c);

        if (!c.getTopDownAnalysisMode().getIsLocalDeclarations()) {
            computeDeferredTypes();
        }
    }

    private void resolveSecondaryConstructors(@NotNull BodiesResolveContext c) {
        for (Map.Entry<JetSecondaryConstructor, ConstructorDescriptor> entry : c.getSecondaryConstructors().entrySet()) {
            JetScope declaringScope = c.getDeclaringScope(entry.getKey());
            assert declaringScope != null : "Declaring scope should be registered before body resolve";
            resolveSecondaryConstructorBody(c.getOuterDataFlowInfo(), trace, entry.getKey(), entry.getValue(), declaringScope);
        }
        if (c.getSecondaryConstructors().isEmpty()) return;
        Set<ConstructorDescriptor> visitedConstructors = Sets.newHashSet();
        for (Map.Entry<JetSecondaryConstructor, ConstructorDescriptor> entry : c.getSecondaryConstructors().entrySet()) {
            checkCyclicConstructorDelegationCall(entry.getValue(), visitedConstructors);
        }
    }

    public void resolveSecondaryConstructorBody(
            @NotNull final DataFlowInfo outerDataFlowInfo,
            @NotNull final BindingTrace trace,
            @NotNull final JetSecondaryConstructor constructor,
            @NotNull final ConstructorDescriptor descriptor,
            @NotNull JetScope declaringScope
    ) {
        ForceResolveUtil.forceResolveAllContents(descriptor.getAnnotations());

        final CallChecker callChecker = new ConstructorHeaderCallChecker(descriptor);
        resolveFunctionBody(outerDataFlowInfo, trace, constructor, descriptor, declaringScope,
                            new Function1<JetScope, DataFlowInfo>() {
                                @Override
                                public DataFlowInfo invoke(@NotNull JetScope headerInnerScope) {
                                    return resolveSecondaryConstructorDelegationCall(outerDataFlowInfo, trace, headerInnerScope, constructor, descriptor,
                                                                                     callChecker);
                                }
                            },
                            callChecker);
    }

    @Nullable
    private DataFlowInfo resolveSecondaryConstructorDelegationCall(
            @NotNull DataFlowInfo outerDataFlowInfo,
            @NotNull BindingTrace trace,
            @NotNull JetScope scope,
            @NotNull JetSecondaryConstructor constructor,
            @NotNull ConstructorDescriptor descriptor,
            @NotNull CallChecker callChecker
    ) {
        OverloadResolutionResults<?> results = callResolver.resolveConstructorDelegationCall(
                trace, scope, outerDataFlowInfo,
                descriptor, constructor.getDelegationCall(),
                callChecker);

        if (results != null && results.isSingleResult()) {
            ResolvedCall<? extends CallableDescriptor> resolvedCall = results.getResultingCall();
            recordConstructorDelegationCall(trace, descriptor, resolvedCall);
            return resolvedCall.getDataFlowInfoForArguments().getResultInfo();
        }
        return null;
    }

    private void checkCyclicConstructorDelegationCall(
            @NotNull ConstructorDescriptor constructorDescriptor,
            @NotNull Set<ConstructorDescriptor> visitedConstructors
    ) {
        if (visitedConstructors.contains(constructorDescriptor)) return;

        // if visit constructor that is already in current chain
        // such constructor is on cycle
        Set<ConstructorDescriptor> visitedInCurrentChain = Sets.newHashSet();
        ConstructorDescriptor currentConstructorDescriptor = constructorDescriptor;
        while (true) {
            visitedInCurrentChain.add(currentConstructorDescriptor);
            ConstructorDescriptor delegatedConstructorDescriptor = getDelegatedConstructor(currentConstructorDescriptor);
            if (delegatedConstructorDescriptor == null) break;

            // if next delegation call is super or primary constructor or already visited
            if (!constructorDescriptor.getContainingDeclaration().equals(delegatedConstructorDescriptor.getContainingDeclaration()) ||
                    delegatedConstructorDescriptor.isPrimary() ||
                    visitedConstructors.contains(delegatedConstructorDescriptor)) {
                 break;
            }

            if (visitedInCurrentChain.contains(delegatedConstructorDescriptor)) {
                reportEachConstructorOnCycle(delegatedConstructorDescriptor);
                break;
            }
            currentConstructorDescriptor = delegatedConstructorDescriptor;
        }
        visitedConstructors.addAll(visitedInCurrentChain);
    }

    private void reportEachConstructorOnCycle(@NotNull ConstructorDescriptor startConstructor) {
        ConstructorDescriptor currentConstructor = startConstructor;
        do {
            PsiElement constructorToReport = DescriptorToSourceUtils.descriptorToDeclaration(currentConstructor);
            if (constructorToReport != null) {
                JetConstructorDelegationCall call = ((JetSecondaryConstructor) constructorToReport).getDelegationCall();
                assert call.getCalleeExpression() != null
                        : "Callee expression of delegation call should not be null on cycle as there should be explicit 'this' calls";
                trace.report(CYCLIC_CONSTRUCTOR_DELEGATION_CALL.on(call.getCalleeExpression()));
            }

            currentConstructor = getDelegatedConstructor(currentConstructor);
            assert currentConstructor != null : "Delegated constructor should not be null in cycle";
        } while (startConstructor != currentConstructor);
    }

    @Nullable
    private ConstructorDescriptor getDelegatedConstructor(@NotNull ConstructorDescriptor constructor) {
        ResolvedCall<ConstructorDescriptor> call = trace.get(CONSTRUCTOR_RESOLVED_DELEGATION_CALL, constructor);
        return call == null || !call.getStatus().isSuccess() ? null : call.getResultingDescriptor().getOriginal();
    }

    public void resolveBodies(@NotNull BodiesResolveContext c) {
        resolveBehaviorDeclarationBodies(c);
        controlFlowAnalyzer.process(c);
        declarationsChecker.process(c);
        functionAnalyzerExtension.process(c);
    }

    private void resolveDelegationSpecifierLists(@NotNull BodiesResolveContext c) {
        // TODO : Make sure the same thing is not initialized twice
        for (Map.Entry<JetClassOrObject, ClassDescriptorWithResolutionScopes> entry : c.getDeclaredClasses().entrySet()) {
            JetClassOrObject classOrObject = entry.getKey();
            ClassDescriptorWithResolutionScopes descriptor = entry.getValue();

            resolveDelegationSpecifierList(c.getOuterDataFlowInfo(), classOrObject, descriptor,
                                           descriptor.getUnsubstitutedPrimaryConstructor(),
                                           descriptor.getScopeForClassHeaderResolution(),
                                           descriptor.getScopeForMemberDeclarationResolution());
        }
    }

    public void resolveDelegationSpecifierList(
            @NotNull final DataFlowInfo outerDataFlowInfo,
            @NotNull JetClassOrObject jetClass,
            @NotNull final ClassDescriptor descriptor,
            @Nullable final ConstructorDescriptor primaryConstructor,
            @NotNull JetScope scopeForSupertypeResolution,
            @NotNull final JetScope scopeForMemberResolution
    ) {
        final JetScope scopeForConstructor = primaryConstructor == null
                ? null
                : FunctionDescriptorUtil.getFunctionInnerScope(scopeForSupertypeResolution, primaryConstructor, trace);
        final ExpressionTypingServices typeInferrer = expressionTypingServices; // TODO : flow

        final Map<JetTypeReference, JetType> supertypes = Maps.newLinkedHashMap();
        final ResolvedCall<?>[] primaryConstructorDelegationCall = new ResolvedCall[1];
        JetVisitorVoid visitor = new JetVisitorVoid() {
            private void recordSupertype(JetTypeReference typeReference, JetType supertype) {
                if (supertype == null) return;
                supertypes.put(typeReference, supertype);
            }

            @Override
            public void visitDelegationByExpressionSpecifier(@NotNull JetDelegatorByExpressionSpecifier specifier) {
                if (descriptor.getKind() == ClassKind.INTERFACE) {
                    trace.report(DELEGATION_IN_TRAIT.on(specifier));
                }
                JetType supertype = trace.getBindingContext().get(BindingContext.TYPE, specifier.getTypeReference());
                recordSupertype(specifier.getTypeReference(), supertype);
                if (supertype != null) {
                    DeclarationDescriptor declarationDescriptor = supertype.getConstructor().getDeclarationDescriptor();
                    if (declarationDescriptor instanceof ClassDescriptor) {
                        ClassDescriptor classDescriptor = (ClassDescriptor) declarationDescriptor;
                        if (classDescriptor.getKind() != ClassKind.INTERFACE) {
                            trace.report(DELEGATION_NOT_TO_TRAIT.on(specifier.getTypeReference()));
                        }
                    }
                }
                JetExpression delegateExpression = specifier.getDelegateExpression();
                if (delegateExpression != null) {
                    JetScope scope = scopeForConstructor == null ? scopeForMemberResolution : scopeForConstructor;
                    JetType expectedType = supertype != null ? supertype : NO_EXPECTED_TYPE;
                    typeInferrer.getType(scope, delegateExpression, expectedType, outerDataFlowInfo, trace);
                }
                if (primaryConstructor == null) {
                    trace.report(UNSUPPORTED.on(specifier, "Delegation without primary constructor is not supported"));
                }
            }

            @Override
            public void visitDelegationToSuperCallSpecifier(@NotNull JetDelegatorToSuperCall call) {
                JetValueArgumentList valueArgumentList = call.getValueArgumentList();
                PsiElement elementToMark = valueArgumentList == null ? call : valueArgumentList;
                if (descriptor.getKind() == ClassKind.INTERFACE) {
                    trace.report(SUPERTYPE_INITIALIZED_IN_TRAIT.on(elementToMark));
                }
                JetTypeReference typeReference = call.getTypeReference();
                if (typeReference == null) return;
                if (primaryConstructor == null) {
                    if (descriptor.getKind() != ClassKind.INTERFACE) {
                        trace.report(SUPERTYPE_INITIALIZED_WITHOUT_PRIMARY_CONSTRUCTOR.on(call));
                    }
                    recordSupertype(typeReference, trace.getBindingContext().get(BindingContext.TYPE, typeReference));
                    return;
                }
                OverloadResolutionResults<FunctionDescriptor> results = callResolver.resolveFunctionCall(
                        trace, scopeForConstructor,
                        CallMaker.makeCall(ReceiverValue.NO_RECEIVER, null, call), NO_EXPECTED_TYPE, outerDataFlowInfo, false);
                if (results.isSuccess()) {
                    JetType supertype = results.getResultingDescriptor().getReturnType();
                    recordSupertype(typeReference, supertype);
                    ClassDescriptor classDescriptor = TypeUtils.getClassDescriptor(supertype);
                    if (classDescriptor != null) {
                        // allow only one delegating constructor
                        if (primaryConstructorDelegationCall[0] == null) {
                            primaryConstructorDelegationCall[0] = results.getResultingCall();
                        }
                        else {
                            primaryConstructorDelegationCall[0] = null;
                        }
                    }
                    // Recording type info for callee to use later in JetObjectLiteralExpression
                    trace.record(PROCESSED, call.getCalleeExpression(), true);
                    trace.record(EXPRESSION_TYPE_INFO, call.getCalleeExpression(),
                                 TypeInfoFactoryPackage.noTypeInfo(results.getResultingCall().getDataFlowInfoForArguments().getResultInfo()));
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
                if (descriptor.getKind() != ClassKind.INTERFACE &&
                    descriptor.getUnsubstitutedPrimaryConstructor() != null &&
                    superClass.getKind() != ClassKind.INTERFACE &&
                    !superClass.getConstructors().isEmpty() &&
                    !ErrorUtils.isError(superClass)
                ) {
                    trace.report(SUPERTYPE_NOT_INITIALIZED.on(specifier));
                }
            }

            @Override
            public void visitJetElement(@NotNull JetElement element) {
                throw new UnsupportedOperationException(element.getText() + " : " + element);
            }
        };

        for (JetDelegationSpecifier delegationSpecifier : jetClass.getDelegationSpecifiers()) {
            delegationSpecifier.accept(visitor);
        }

        if (DescriptorUtils.isAnnotationClass(descriptor) && jetClass.getDelegationSpecifierList() != null) {
            trace.report(SUPERTYPES_FOR_ANNOTATION_CLASS.on(jetClass.getDelegationSpecifierList()));
        }

        if (primaryConstructorDelegationCall[0] != null && primaryConstructor != null) {
            recordConstructorDelegationCall(trace, primaryConstructor, primaryConstructorDelegationCall[0]);
        }

        checkSupertypeList(descriptor, supertypes, jetClass);
    }

    // Returns a set of enum or sealed types of which supertypeOwner is an entry or a member
    @NotNull
    private static Set<TypeConstructor> getAllowedFinalSupertypes(
            @NotNull ClassDescriptor descriptor,
            @NotNull JetClassOrObject jetClass
    ) {
        Set<TypeConstructor> parentEnumOrSealed;
        if (jetClass instanceof JetEnumEntry) {
            parentEnumOrSealed = Collections.singleton(((ClassDescriptor) descriptor.getContainingDeclaration()).getTypeConstructor());
        }
        else {
            parentEnumOrSealed = Collections.emptySet();
            ClassDescriptor currentDescriptor = descriptor;
            while (currentDescriptor.getContainingDeclaration() instanceof ClassDescriptor) {
                currentDescriptor = (ClassDescriptor) currentDescriptor.getContainingDeclaration();
                if (currentDescriptor.getModality() == Modality.SEALED) {
                    if (parentEnumOrSealed.isEmpty()) {
                        parentEnumOrSealed = new HashSet<TypeConstructor>();
                    }
                    parentEnumOrSealed.add(currentDescriptor.getTypeConstructor());
                }
            }
        }
        return parentEnumOrSealed;
    }

    private static void recordConstructorDelegationCall(
            @NotNull BindingTrace trace,
            @NotNull ConstructorDescriptor constructor,
            @NotNull ResolvedCall<?> call
    ) {
        //noinspection unchecked
        trace.record(CONSTRUCTOR_RESOLVED_DELEGATION_CALL, constructor, (ResolvedCall<ConstructorDescriptor>) call);
    }

    private void checkSupertypeList(
            @NotNull ClassDescriptor supertypeOwner,
            @NotNull Map<JetTypeReference, JetType> supertypes,
            @NotNull JetClassOrObject jetClass
    ) {
        Set<TypeConstructor> allowedFinalSupertypes = getAllowedFinalSupertypes(supertypeOwner, jetClass);
        Set<TypeConstructor> typeConstructors = Sets.newHashSet();
        boolean classAppeared = false;
        for (Map.Entry<JetTypeReference, JetType> entry : supertypes.entrySet()) {
            JetTypeReference typeReference = entry.getKey();
            JetType supertype = entry.getValue();

            boolean addSupertype = true;

            ClassDescriptor classDescriptor = TypeUtils.getClassDescriptor(supertype);
            if (classDescriptor != null) {
                if (ErrorUtils.isError(classDescriptor)) continue;

                if (classDescriptor.getKind() != ClassKind.INTERFACE) {
                    if (supertypeOwner.getKind() == ClassKind.ENUM_CLASS) {
                        trace.report(CLASS_IN_SUPERTYPE_FOR_ENUM.on(typeReference));
                        addSupertype = false;
                    }
                    else if (supertypeOwner.getKind() == ClassKind.INTERFACE &&
                             !classAppeared && !TypesPackage.isDynamic(supertype) /* avoid duplicate diagnostics */) {
                        trace.report(TRAIT_WITH_SUPERCLASS.on(typeReference));
                        addSupertype = false;
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
            if (addSupertype && !typeConstructors.add(constructor)) {
                trace.report(SUPERTYPE_APPEARS_TWICE.on(typeReference));
            }

            if (DescriptorUtils.isSingleton(classDescriptor)) {
                trace.report(SINGLETON_IN_SUPERTYPE.on(typeReference));
            }
            else if (constructor.isFinal() && !allowedFinalSupertypes.contains(constructor)) {
                if (classDescriptor.getModality() == Modality.SEALED) {
                    DeclarationDescriptor containingDescriptor = supertypeOwner.getContainingDeclaration();
                    while (containingDescriptor != null && containingDescriptor != classDescriptor) {
                        containingDescriptor = containingDescriptor.getContainingDeclaration();
                    }
                    if (containingDescriptor == null) {
                        trace.report(SEALED_SUPERTYPE.on(typeReference));
                    }
                    else {
                        trace.report(SEALED_SUPERTYPE_IN_LOCAL_CLASS.on(typeReference));
                    }
                }
                else {
                    trace.report(FINAL_SUPERTYPE.on(typeReference));
                }
            }
        }
    }

    private void resolveAnonymousInitializers(@NotNull BodiesResolveContext c) {
        for (Map.Entry<JetClassInitializer, ClassDescriptorWithResolutionScopes> entry : c.getAnonymousInitializers().entrySet()) {
            JetClassInitializer initializer = entry.getKey();
            ClassDescriptorWithResolutionScopes descriptor = entry.getValue();
            resolveAnonymousInitializer(c.getOuterDataFlowInfo(), initializer, descriptor);
        }
    }

    public void resolveAnonymousInitializer(
            @NotNull DataFlowInfo outerDataFlowInfo,
            @NotNull JetClassInitializer anonymousInitializer,
            @NotNull ClassDescriptorWithResolutionScopes classDescriptor
    ) {
        JetScope scopeForInitializers = classDescriptor.getScopeForInitializerResolution();
        if (!classDescriptor.getConstructors().isEmpty()) {
            JetExpression body = anonymousInitializer.getBody();
            if (body != null) {
                expressionTypingServices.getType(scopeForInitializers, body, NO_EXPECTED_TYPE, outerDataFlowInfo, trace);
            }
            processModifiersOnInitializer(anonymousInitializer, scopeForInitializers);
        }
        else {
            trace.report(ANONYMOUS_INITIALIZER_IN_TRAIT.on(anonymousInitializer));
            processModifiersOnInitializer(anonymousInitializer, scopeForInitializers);
        }
    }

    private void processModifiersOnInitializer(@NotNull JetModifierListOwner owner, @NotNull JetScope scope) {
        annotationChecker.check(owner, trace, null);
        ModifierCheckerCore.INSTANCE$.check(owner, trace, null);
        JetModifierList modifierList = owner.getModifierList();
        if (modifierList == null) return;

        annotationResolver.resolveAnnotationsWithArguments(scope, modifierList, trace);
    }

    private void resolvePrimaryConstructorParameters(@NotNull BodiesResolveContext c) {
        for (Map.Entry<JetClassOrObject, ClassDescriptorWithResolutionScopes> entry : c.getDeclaredClasses().entrySet()) {
            JetClassOrObject klass = entry.getKey();
            ClassDescriptorWithResolutionScopes classDescriptor = entry.getValue();
            ConstructorDescriptor unsubstitutedPrimaryConstructor = classDescriptor.getUnsubstitutedPrimaryConstructor();
            if (unsubstitutedPrimaryConstructor != null) {
                ForceResolveUtil.forceResolveAllContents(unsubstitutedPrimaryConstructor.getAnnotations());

                JetScope parameterScope = getPrimaryConstructorParametersScope(classDescriptor.getScopeForClassHeaderResolution(),
                                                                                    unsubstitutedPrimaryConstructor);
                valueParameterResolver.resolveValueParameters(klass.getPrimaryConstructorParameters(),
                                                              unsubstitutedPrimaryConstructor.getValueParameters(),
                                                              parameterScope, c.getOuterDataFlowInfo(), trace);
            }
        }
    }

    private static JetScope getPrimaryConstructorParametersScope(
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

    private void resolveProperty(
            @NotNull BodiesResolveContext c,
            @Nullable JetScope parentScope,
            @NotNull JetProperty property,
            @NotNull PropertyDescriptor propertyDescriptor
    ) {
        computeDeferredType(propertyDescriptor.getReturnType());

        JetExpression initializer = property.getInitializer();
        JetScope propertyScope = getScopeForProperty(c, property);
        if (parentScope == null) {
            parentScope = propertyScope;
        }
        if (initializer != null) {
            resolvePropertyInitializer(c.getOuterDataFlowInfo(), property, propertyDescriptor, initializer, propertyScope);
        }

        JetExpression delegateExpression = property.getDelegateExpression();
        if (delegateExpression != null) {
            assert initializer == null : "Initializer should be null for delegated property : " + property.getText();
            resolvePropertyDelegate(c.getOuterDataFlowInfo(), property, propertyDescriptor, delegateExpression, parentScope, propertyScope);
        }

        resolvePropertyAccessors(c, property, propertyDescriptor);
    }

    private void resolvePropertyDeclarationBodies(@NotNull BodiesResolveContext c) {

        // Member properties
        Set<JetProperty> processed = Sets.newHashSet();
        for (Map.Entry<JetClassOrObject, ClassDescriptorWithResolutionScopes> entry : c.getDeclaredClasses().entrySet()) {
            if (!(entry.getKey() instanceof JetClass)) continue;
            JetClass jetClass = (JetClass) entry.getKey();
            ClassDescriptorWithResolutionScopes classDescriptor = entry.getValue();

            for (JetProperty property : jetClass.getProperties()) {
                PropertyDescriptor propertyDescriptor = c.getProperties().get(property);
                assert propertyDescriptor != null;

                resolveProperty(c, classDescriptor.getScopeForMemberDeclarationResolution(), property, propertyDescriptor);
                processed.add(property);
            }
        }

        // Top-level properties & properties of objects
        for (Map.Entry<JetProperty, PropertyDescriptor> entry : c.getProperties().entrySet()) {
            JetProperty property = entry.getKey();
            if (processed.contains(property)) continue;

            PropertyDescriptor propertyDescriptor = entry.getValue();

            resolveProperty(c, null, property, propertyDescriptor);
        }
    }

    private JetScope makeScopeForPropertyAccessor(@NotNull BodiesResolveContext c, @NotNull JetPropertyAccessor accessor, @NotNull PropertyDescriptor descriptor) {
        JetScope accessorDeclaringScope = c.getDeclaringScope(accessor);
        assert accessorDeclaringScope != null : "Scope for accessor " + accessor.getText() + " should exists";
        return JetScopeUtils.makeScopeForPropertyAccessor(descriptor, accessorDeclaringScope, trace);
    }

    public void resolvePropertyAccessors(
            @NotNull BodiesResolveContext c,
            @NotNull JetProperty property,
            @NotNull PropertyDescriptor propertyDescriptor
    ) {
        ObservableBindingTrace fieldAccessTrackingTrace = createFieldTrackingTrace(propertyDescriptor);

        JetPropertyAccessor getter = property.getGetter();
        PropertyGetterDescriptor getterDescriptor = propertyDescriptor.getGetter();
        if (getter != null && getterDescriptor != null) {
            JetScope accessorScope = makeScopeForPropertyAccessor(c, getter, propertyDescriptor);
            ForceResolveUtil.forceResolveAllContents(getterDescriptor.getAnnotations());
            resolveFunctionBody(c.getOuterDataFlowInfo(), fieldAccessTrackingTrace, getter, getterDescriptor, accessorScope);
        }

        JetPropertyAccessor setter = property.getSetter();
        PropertySetterDescriptor setterDescriptor = propertyDescriptor.getSetter();
        if (setter != null && setterDescriptor != null) {
            JetScope accessorScope = makeScopeForPropertyAccessor(c, setter, propertyDescriptor);
            ForceResolveUtil.forceResolveAllContents(setterDescriptor.getAnnotations());
            resolveFunctionBody(c.getOuterDataFlowInfo(), fieldAccessTrackingTrace, setter, setterDescriptor, accessorScope);
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
            @NotNull DataFlowInfo outerDataFlowInfo,
            @NotNull JetProperty jetProperty,
            @NotNull PropertyDescriptor propertyDescriptor,
            @NotNull JetExpression delegateExpression,
            @NotNull JetScope parentScopeForAccessor,
            @NotNull JetScope propertyScope
    ) {
        JetPropertyAccessor getter = jetProperty.getGetter();
        if (getter != null && getter.hasBody()) {
            trace.report(ACCESSOR_FOR_DELEGATED_PROPERTY.on(getter));
        }

        JetPropertyAccessor setter = jetProperty.getSetter();
        if (setter != null && setter.hasBody()) {
            trace.report(ACCESSOR_FOR_DELEGATED_PROPERTY.on(setter));
        }

        JetScope propertyDeclarationInnerScope = JetScopeUtils.getPropertyDeclarationInnerScopeForInitializer(
                propertyDescriptor, propertyScope, propertyDescriptor.getTypeParameters(), NO_RECEIVER_PARAMETER, trace);
        JetScope accessorScope = JetScopeUtils.makeScopeForPropertyAccessor(
                propertyDescriptor, parentScopeForAccessor, trace);

        JetType delegateType = delegatedPropertyResolver.resolveDelegateExpression(
                delegateExpression, jetProperty, propertyDescriptor, propertyDeclarationInnerScope, accessorScope, trace,
                outerDataFlowInfo);

        delegatedPropertyResolver.resolveDelegatedPropertyGetMethod(propertyDescriptor, delegateExpression, delegateType,
                                                                    trace, accessorScope);

        if (jetProperty.isVar()) {
            delegatedPropertyResolver.resolveDelegatedPropertySetMethod(propertyDescriptor, delegateExpression, delegateType,
                                                                        trace, accessorScope);
        }

        delegatedPropertyResolver.resolveDelegatedPropertyPDMethod(propertyDescriptor, delegateExpression, delegateType,
                                                                   trace, accessorScope);
    }

    public void resolvePropertyInitializer(
            @NotNull DataFlowInfo outerDataFlowInfo,
            @NotNull JetProperty property,
            @NotNull PropertyDescriptor propertyDescriptor,
            @NotNull JetExpression initializer,
            @NotNull JetScope scope
    ) {
        JetScope propertyDeclarationInnerScope = JetScopeUtils.getPropertyDeclarationInnerScopeForInitializer(
                propertyDescriptor, scope, propertyDescriptor.getTypeParameters(), NO_RECEIVER_PARAMETER, trace);
        JetType expectedTypeForInitializer = property.getTypeReference() != null ? propertyDescriptor.getType() : NO_EXPECTED_TYPE;
        if (propertyDescriptor.getCompileTimeInitializer() == null) {
            expressionTypingServices.getType(propertyDeclarationInnerScope, initializer, expectedTypeForInitializer,
                                             outerDataFlowInfo, trace);
        }
    }

    @NotNull
    private static JetScope getScopeForProperty(@NotNull BodiesResolveContext c, @NotNull JetProperty property) {
        JetScope scope = c.getDeclaringScope(property);
        assert scope != null : "Scope for property " + property.getText() + " should exists";
        return scope;
    }

    private void resolveFunctionBodies(@NotNull BodiesResolveContext c) {
        for (Map.Entry<JetNamedFunction, SimpleFunctionDescriptor> entry : c.getFunctions().entrySet()) {
            JetNamedFunction declaration = entry.getKey();

            JetScope scope = c.getDeclaringScope(declaration);
            assert scope != null : "Scope is null: " + PsiUtilPackage.getElementTextWithContext(declaration);

            if (!c.getTopDownAnalysisMode().getIsLocalDeclarations() && !(bodyResolveCache instanceof BodyResolveCache.ThrowException) &&
                expressionTypingServices.getStatementFilter() != StatementFilter.NONE) {
                bodyResolveCache.resolveFunctionBody(declaration).addOwnDataTo(trace, true);
            }
            else {
                resolveFunctionBody(c.getOuterDataFlowInfo(), trace, declaration, entry.getValue(), scope);
            }
        }
    }

    public void resolveFunctionBody(
            @NotNull DataFlowInfo outerDataFlowInfo,
            @NotNull BindingTrace trace,
            @NotNull JetDeclarationWithBody function,
            @NotNull FunctionDescriptor functionDescriptor,
            @NotNull JetScope declaringScope
    ) {
        computeDeferredType(functionDescriptor.getReturnType());

        resolveFunctionBody(outerDataFlowInfo, trace, function, functionDescriptor, declaringScope, null, CallChecker.DoNothing.INSTANCE$);

        assert functionDescriptor.getReturnType() != null;
    }

    public void resolveFunctionBody(
            @NotNull DataFlowInfo outerDataFlowInfo,
            @NotNull BindingTrace trace,
            @NotNull JetDeclarationWithBody function,
            @NotNull FunctionDescriptor functionDescriptor,
            @NotNull JetScope scope,
            @Nullable Function1<JetScope, DataFlowInfo> beforeBlockBody,
            @NotNull CallChecker callChecker
    ) {
        JetScope innerScope = FunctionDescriptorUtil.getFunctionInnerScope(scope, functionDescriptor, trace);
        List<JetParameter> valueParameters = function.getValueParameters();
        List<ValueParameterDescriptor> valueParameterDescriptors = functionDescriptor.getValueParameters();

        valueParameterResolver.resolveValueParameters(
                valueParameters, valueParameterDescriptors,
                ExpressionTypingContext.newContext(
                        trace, innerScope, outerDataFlowInfo, NO_EXPECTED_TYPE, callChecker)
        );

        DataFlowInfo dataFlowInfo = null;

        if (beforeBlockBody != null) {
            dataFlowInfo = beforeBlockBody.invoke(innerScope);
        }

        if (function.hasBody()) {
            expressionTypingServices.checkFunctionReturnType(
                    innerScope, function, functionDescriptor, dataFlowInfo != null ? dataFlowInfo : outerDataFlowInfo, null, trace);
        }

        assert functionDescriptor.getReturnType() != null;
    }

    public void resolveConstructorParameterDefaultValuesAndAnnotations(
            @NotNull DataFlowInfo outerDataFlowInfo,
            @NotNull BindingTrace trace,
            @NotNull JetClass klass,
            @NotNull ConstructorDescriptor constructorDescriptor,
            @NotNull JetScope declaringScope
    ) {
        List<JetParameter> valueParameters = klass.getPrimaryConstructorParameters();
        List<ValueParameterDescriptor> valueParameterDescriptors = constructorDescriptor.getValueParameters();

        JetScope scope = getPrimaryConstructorParametersScope(declaringScope, constructorDescriptor);

        valueParameterResolver.resolveValueParameters(valueParameters, valueParameterDescriptors, scope, outerDataFlowInfo, trace);
    }

    private static void computeDeferredType(JetType type) {
        // handle type inference loop: function or property body contains a reference to itself
        // fun f() = { f() }
        // val x = x
        // type resolution must be started before body resolution
        if (type instanceof DeferredType) {
            DeferredType deferredType = (DeferredType) type;
            if (!deferredType.isComputed()) {
                deferredType.getDelegate();
            }
        }
    }

    private void computeDeferredTypes() {
        Collection<Box<DeferredType>> deferredTypes = trace.getKeys(DEFERRED_TYPE);
        if (deferredTypes.isEmpty()) {
            return;
        }
        // +1 is a work around against new Queue(0).addLast(...) bug // stepan.koltsov@ 2011-11-21
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
                    deferredType.getDelegate(); // to compute
                }
                catch (ReenteringLazyValueComputationException e) {
                    // A problem should be reported while computing the type
                }
            }
        }
    }
}
