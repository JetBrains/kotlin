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
import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.builtins.KotlinBuiltIns;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.descriptors.impl.SyntheticFieldDescriptor;
import org.jetbrains.kotlin.diagnostics.Errors;
import org.jetbrains.kotlin.lexer.KtTokens;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.psi.psiUtil.PsiUtilsKt;
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
import org.jetbrains.kotlin.types.expressions.ExpressionTypingServices;
import org.jetbrains.kotlin.types.expressions.PreliminaryDeclarationVisitor;
import org.jetbrains.kotlin.types.expressions.ValueParameterResolver;
import org.jetbrains.kotlin.types.expressions.typeInfoFactory.TypeInfoFactoryKt;
import org.jetbrains.kotlin.util.Box;
import org.jetbrains.kotlin.util.ReenteringLazyValueComputationException;
import org.jetbrains.kotlin.util.slicedMap.WritableSlice;

import java.util.*;

import static org.jetbrains.kotlin.diagnostics.Errors.*;
import static org.jetbrains.kotlin.resolve.BindingContext.*;
import static org.jetbrains.kotlin.types.TypeUtils.NO_EXPECTED_TYPE;

public class BodyResolver {
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
    @NotNull private final KotlinBuiltIns builtIns;

    public BodyResolver(
            @NotNull AnnotationResolver annotationResolver,
            @NotNull BodyResolveCache bodyResolveCache,
            @NotNull CallResolver callResolver,
            @NotNull ControlFlowAnalyzer controlFlowAnalyzer,
            @NotNull DeclarationsChecker declarationsChecker,
            @NotNull DelegatedPropertyResolver delegatedPropertyResolver,
            @NotNull ExpressionTypingServices expressionTypingServices,
            @NotNull FunctionAnalyzerExtension functionAnalyzerExtension,
            @NotNull BindingTrace trace,
            @NotNull ValueParameterResolver valueParameterResolver,
            @NotNull AnnotationChecker annotationChecker,
            @NotNull KotlinBuiltIns builtIns
    ) {
        this.annotationResolver = annotationResolver;
        this.bodyResolveCache = bodyResolveCache;
        this.callResolver = callResolver;
        this.controlFlowAnalyzer = controlFlowAnalyzer;
        this.declarationsChecker = declarationsChecker;
        this.delegatedPropertyResolver = delegatedPropertyResolver;
        this.expressionTypingServices = expressionTypingServices;
        this.functionAnalyzerExtension = functionAnalyzerExtension;
        this.annotationChecker = annotationChecker;
        this.trace = new ObservableBindingTrace(trace);
        this.valueParameterResolver = valueParameterResolver;
        this.builtIns = builtIns;
    }

    private void resolveBehaviorDeclarationBodies(@NotNull BodiesResolveContext c) {
        resolveSuperTypeEntryLists(c);

        resolvePropertyDeclarationBodies(c);

        resolveAnonymousInitializers(c);
        resolvePrimaryConstructorParameters(c);
        resolveSecondaryConstructors(c);

        resolveFunctionBodies(c);

        if (!c.getTopDownAnalysisMode().isLocalDeclarations()) {
            computeDeferredTypes();
        }
    }

    private void resolveSecondaryConstructors(@NotNull BodiesResolveContext c) {
        for (Map.Entry<KtSecondaryConstructor, ConstructorDescriptor> entry : c.getSecondaryConstructors().entrySet()) {
            LexicalScope declaringScope = c.getDeclaringScope(entry.getKey());
            assert declaringScope != null : "Declaring scope should be registered before body resolve";
            resolveSecondaryConstructorBody(c.getOuterDataFlowInfo(), trace, entry.getKey(), entry.getValue(), declaringScope);
        }
        if (c.getSecondaryConstructors().isEmpty()) return;
        Set<ConstructorDescriptor> visitedConstructors = Sets.newHashSet();
        for (Map.Entry<KtSecondaryConstructor, ConstructorDescriptor> entry : c.getSecondaryConstructors().entrySet()) {
            checkCyclicConstructorDelegationCall(entry.getValue(), visitedConstructors);
        }
    }

    public void resolveSecondaryConstructorBody(
            @NotNull final DataFlowInfo outerDataFlowInfo,
            @NotNull final BindingTrace trace,
            @NotNull final KtSecondaryConstructor constructor,
            @NotNull final ConstructorDescriptor descriptor,
            @NotNull LexicalScope declaringScope
    ) {
        ForceResolveUtil.forceResolveAllContents(descriptor.getAnnotations());

        resolveFunctionBody(outerDataFlowInfo, trace, constructor, descriptor, declaringScope,
                            new Function1<LexicalScope, DataFlowInfo>() {
                                @Override
                                public DataFlowInfo invoke(@NotNull LexicalScope headerInnerScope) {
                                    return resolveSecondaryConstructorDelegationCall(outerDataFlowInfo, trace, headerInnerScope,
                                                                                     constructor, descriptor);
                                }
                            },
                            new Function1<LexicalScope, LexicalScope>() {
                                @Override
                                public LexicalScope invoke(LexicalScope scope) {
                                    return new LexicalScopeImpl(
                                            scope, descriptor, scope.isOwnerDescriptorAccessibleByLabel(), scope.getImplicitReceiver(),
                                            LexicalScopeKind.CONSTRUCTOR_HEADER);
                                }
                            });
    }

    @Nullable
    private DataFlowInfo resolveSecondaryConstructorDelegationCall(
            @NotNull DataFlowInfo outerDataFlowInfo,
            @NotNull BindingTrace trace,
            @NotNull LexicalScope scope,
            @NotNull KtSecondaryConstructor constructor,
            @NotNull ConstructorDescriptor descriptor
    ) {
        OverloadResolutionResults<?> results = callResolver.resolveConstructorDelegationCall(
                trace, scope, outerDataFlowInfo,
                descriptor, constructor.getDelegationCall());

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
                KtConstructorDelegationCall call = ((KtSecondaryConstructor) constructorToReport).getDelegationCall();
                assert call.getCalleeExpression() != null
                        : "Callee expression of delegation call should not be null on cycle as there should be explicit 'this' calls";
                trace.report(CYCLIC_CONSTRUCTOR_DELEGATION_CALL.on(call.getCalleeExpression()));
            }

            currentConstructor = getDelegatedConstructor(currentConstructor);
            assert currentConstructor != null : "Delegated constructor should not be null in cycle";
        }
        while (startConstructor != currentConstructor);
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

    private void resolveSuperTypeEntryLists(@NotNull BodiesResolveContext c) {
        // TODO : Make sure the same thing is not initialized twice
        for (Map.Entry<KtClassOrObject, ClassDescriptorWithResolutionScopes> entry : c.getDeclaredClasses().entrySet()) {
            KtClassOrObject classOrObject = entry.getKey();
            ClassDescriptorWithResolutionScopes descriptor = entry.getValue();

            resolveSuperTypeEntryList(c.getOuterDataFlowInfo(), classOrObject, descriptor,
                                      descriptor.getUnsubstitutedPrimaryConstructor(),
                                      descriptor.getScopeForConstructorHeaderResolution(),
                                      descriptor.getScopeForMemberDeclarationResolution());
        }
    }

    public void resolveSuperTypeEntryList(
            @NotNull final DataFlowInfo outerDataFlowInfo,
            @NotNull KtClassOrObject jetClass,
            @NotNull final ClassDescriptor descriptor,
            @Nullable final ConstructorDescriptor primaryConstructor,
            @NotNull LexicalScope scopeForConstructorResolution,
            @NotNull final LexicalScope scopeForMemberResolution
    ) {
        final LexicalScope scopeForConstructor =
                primaryConstructor == null
                ? null
                : FunctionDescriptorUtil.getFunctionInnerScope(scopeForConstructorResolution, primaryConstructor, trace);
        final ExpressionTypingServices typeInferrer = expressionTypingServices; // TODO : flow

        final Map<KtTypeReference, KotlinType> supertypes = Maps.newLinkedHashMap();
        final ResolvedCall<?>[] primaryConstructorDelegationCall = new ResolvedCall[1];
        KtVisitorVoid visitor = new KtVisitorVoid() {
            private void recordSupertype(KtTypeReference typeReference, KotlinType supertype) {
                if (supertype == null) return;
                supertypes.put(typeReference, supertype);
            }

            @Override
            public void visitDelegatedSuperTypeEntry(@NotNull KtDelegatedSuperTypeEntry specifier) {
                if (descriptor.getKind() == ClassKind.INTERFACE) {
                    trace.report(DELEGATION_IN_INTERFACE.on(specifier));
                }
                KotlinType supertype = trace.getBindingContext().get(BindingContext.TYPE, specifier.getTypeReference());
                recordSupertype(specifier.getTypeReference(), supertype);
                if (supertype != null) {
                    DeclarationDescriptor declarationDescriptor = supertype.getConstructor().getDeclarationDescriptor();
                    if (declarationDescriptor instanceof ClassDescriptor) {
                        ClassDescriptor classDescriptor = (ClassDescriptor) declarationDescriptor;
                        if (classDescriptor.getKind() != ClassKind.INTERFACE) {
                            trace.report(DELEGATION_NOT_TO_INTERFACE.on(specifier.getTypeReference()));
                        }
                    }
                }
                KtExpression delegateExpression = specifier.getDelegateExpression();
                if (delegateExpression != null) {
                    LexicalScope scope = scopeForConstructor == null ? scopeForMemberResolution : scopeForConstructor;
                    KotlinType expectedType = supertype != null ? supertype : NO_EXPECTED_TYPE;
                    typeInferrer.getType(scope, delegateExpression, expectedType, outerDataFlowInfo, trace);
                }
                if (primaryConstructor == null) {
                    trace.report(UNSUPPORTED.on(specifier, "Delegation without primary constructor is not supported"));
                }
            }

            @Override
            public void visitSuperTypeCallEntry(@NotNull KtSuperTypeCallEntry call) {
                KtValueArgumentList valueArgumentList = call.getValueArgumentList();
                PsiElement elementToMark = valueArgumentList == null ? call : valueArgumentList;
                if (descriptor.getKind() == ClassKind.INTERFACE) {
                    trace.report(SUPERTYPE_INITIALIZED_IN_INTERFACE.on(elementToMark));
                }
                KtTypeReference typeReference = call.getTypeReference();
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
                        CallMaker.makeConstructorCallWithoutTypeArguments(call), NO_EXPECTED_TYPE, outerDataFlowInfo, false);
                if (results.isSuccess()) {
                    KotlinType supertype = results.getResultingDescriptor().getReturnType();
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
                                 TypeInfoFactoryKt.noTypeInfo(results.getResultingCall().getDataFlowInfoForArguments().getResultInfo()));
                }
                else {
                    recordSupertype(typeReference, trace.getBindingContext().get(BindingContext.TYPE, typeReference));
                }
            }

            @Override
            public void visitSuperTypeEntry(@NotNull KtSuperTypeEntry specifier) {
                KtTypeReference typeReference = specifier.getTypeReference();
                KotlinType supertype = trace.getBindingContext().get(BindingContext.TYPE, typeReference);
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
            public void visitKtElement(@NotNull KtElement element) {
                throw new UnsupportedOperationException(element.getText() + " : " + element);
            }
        };

        for (KtSuperTypeListEntry delegationSpecifier : jetClass.getSuperTypeListEntries()) {
            delegationSpecifier.accept(visitor);
        }

        if (DescriptorUtils.isAnnotationClass(descriptor) && jetClass.getSuperTypeList() != null) {
            trace.report(SUPERTYPES_FOR_ANNOTATION_CLASS.on(jetClass.getSuperTypeList()));
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
            @NotNull KtClassOrObject jetClass
    ) {
        Set<TypeConstructor> parentEnumOrSealed;
        if (jetClass instanceof KtEnumEntry) {
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
            @NotNull Map<KtTypeReference, KotlinType> supertypes,
            @NotNull KtClassOrObject jetClass
    ) {
        Set<TypeConstructor> allowedFinalSupertypes = getAllowedFinalSupertypes(supertypeOwner, jetClass);
        Set<TypeConstructor> typeConstructors = Sets.newHashSet();
        boolean classAppeared = false;
        for (Map.Entry<KtTypeReference, KotlinType> entry : supertypes.entrySet()) {
            KtTypeReference typeReference = entry.getKey();
            KotlinType supertype = entry.getValue();

            KtTypeElement typeElement = typeReference.getTypeElement();
            if (typeElement instanceof KtFunctionType) {
                for (KtParameter parameter : ((KtFunctionType) typeElement).getParameters()) {
                    PsiElement nameIdentifier = parameter.getNameIdentifier();

                    if (nameIdentifier != null) {
                        trace.report(Errors.UNSUPPORTED.on(nameIdentifier, "named parameter in function type in supertype position"));
                    }
                }
            }

            boolean addSupertype = true;

            ClassDescriptor classDescriptor = TypeUtils.getClassDescriptor(supertype);
            if (classDescriptor != null) {
                if (ErrorUtils.isError(classDescriptor)) continue;

                if (KotlinBuiltIns.isExactExtensionFunctionType(supertype)) {
                    trace.report(SUPERTYPE_IS_EXTENSION_FUNCTION_TYPE.on(typeReference));
                }

                if (classDescriptor.getKind() != ClassKind.INTERFACE) {
                    if (supertypeOwner.getKind() == ClassKind.ENUM_CLASS) {
                        trace.report(CLASS_IN_SUPERTYPE_FOR_ENUM.on(typeReference));
                        addSupertype = false;
                    }
                    else if (supertypeOwner.getKind() == ClassKind.INTERFACE &&
                             !classAppeared && !DynamicTypesKt.isDynamic(supertype) /* avoid duplicate diagnostics */) {
                        trace.report(INTERFACE_WITH_SUPERCLASS.on(typeReference));
                        addSupertype = false;
                    }
                    else if (jetClass.hasModifier(KtTokens.DATA_KEYWORD)) {
                        trace.report(DATA_CLASS_CANNOT_HAVE_CLASS_SUPERTYPES.on(typeReference));
                        addSupertype = false;
                    }
                    else if (DescriptorUtils.isSubclass(classDescriptor, builtIns.getThrowable()) &&
                             !supertypeOwner.getDeclaredTypeParameters().isEmpty()) {
                        trace.report(GENERIC_THROWABLE_SUBCLASS.on(jetClass.getTypeParameterList()));
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
                trace.report(SUPERTYPE_NOT_A_CLASS_OR_INTERFACE.on(typeReference));
            }

            TypeConstructor constructor = supertype.getConstructor();
            if (addSupertype && !typeConstructors.add(constructor)) {
                trace.report(SUPERTYPE_APPEARS_TWICE.on(typeReference));
            }

            if (classDescriptor == null) return;
            if (classDescriptor.getKind().isSingleton()) {
                if (!DescriptorUtils.isEnumEntry(classDescriptor)) {
                    trace.report(SINGLETON_IN_SUPERTYPE.on(typeReference));
                }
            }
            else if (!allowedFinalSupertypes.contains(constructor)) {
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
                else if (ModalityKt.isFinalOrEnum(classDescriptor)) {
                    trace.report(FINAL_SUPERTYPE.on(typeReference));
                }
            }
        }
    }

    private void resolveAnonymousInitializers(@NotNull BodiesResolveContext c) {
        for (Map.Entry<KtAnonymousInitializer, ClassDescriptorWithResolutionScopes> entry : c.getAnonymousInitializers().entrySet()) {
            KtAnonymousInitializer initializer = entry.getKey();
            ClassDescriptorWithResolutionScopes descriptor = entry.getValue();
            resolveAnonymousInitializer(c.getOuterDataFlowInfo(), initializer, descriptor);
        }
    }

    public void resolveAnonymousInitializer(
            @NotNull DataFlowInfo outerDataFlowInfo,
            @NotNull KtAnonymousInitializer anonymousInitializer,
            @NotNull ClassDescriptorWithResolutionScopes classDescriptor
    ) {
        LexicalScope scopeForInitializers = classDescriptor.getScopeForInitializerResolution();
        if (!classDescriptor.getConstructors().isEmpty()) {
            KtExpression body = anonymousInitializer.getBody();
            if (body != null) {
                PreliminaryDeclarationVisitor.Companion.createForDeclaration(
                        (KtDeclaration) anonymousInitializer.getParent().getParent(), trace);
                expressionTypingServices.getTypeInfo(
                        scopeForInitializers, body, NO_EXPECTED_TYPE, outerDataFlowInfo, trace, /*isStatement = */true
                );
            }
            processModifiersOnInitializer(anonymousInitializer, scopeForInitializers);
        }
        else {
            trace.report(ANONYMOUS_INITIALIZER_IN_INTERFACE.on(anonymousInitializer));
            processModifiersOnInitializer(anonymousInitializer, scopeForInitializers);
        }
    }

    private void processModifiersOnInitializer(@NotNull KtModifierListOwner owner, @NotNull LexicalScope scope) {
        annotationChecker.check(owner, trace, null);
        ModifierCheckerCore.INSTANCE.check(owner, trace, null);
        KtModifierList modifierList = owner.getModifierList();
        if (modifierList == null) return;

        annotationResolver.resolveAnnotationsWithArguments(scope, modifierList, trace);
    }

    private void resolvePrimaryConstructorParameters(@NotNull BodiesResolveContext c) {
        for (Map.Entry<KtClassOrObject, ClassDescriptorWithResolutionScopes> entry : c.getDeclaredClasses().entrySet()) {
            KtClassOrObject klass = entry.getKey();
            ClassDescriptorWithResolutionScopes classDescriptor = entry.getValue();
            ConstructorDescriptor unsubstitutedPrimaryConstructor = classDescriptor.getUnsubstitutedPrimaryConstructor();
            if (unsubstitutedPrimaryConstructor != null) {
                ForceResolveUtil.forceResolveAllContents(unsubstitutedPrimaryConstructor.getAnnotations());

                LexicalScope parameterScope = getPrimaryConstructorParametersScope(classDescriptor.getScopeForConstructorHeaderResolution(),
                                                                                   unsubstitutedPrimaryConstructor);
                valueParameterResolver.resolveValueParameters(klass.getPrimaryConstructorParameters(),
                                                              unsubstitutedPrimaryConstructor.getValueParameters(),
                                                              parameterScope, c.getOuterDataFlowInfo(), trace);
            }
        }
    }

    private static LexicalScope getPrimaryConstructorParametersScope(
            LexicalScope originalScope,
            final ConstructorDescriptor unsubstitutedPrimaryConstructor
    ) {
        return new LexicalScopeImpl(originalScope, unsubstitutedPrimaryConstructor, false, null,
                                    LexicalScopeKind.DEFAULT_VALUE, RedeclarationHandler.DO_NOTHING,
                                    new Function1<LexicalScopeImpl.InitializeHandler, Unit>() {
                                        @Override
                                        public Unit invoke(LexicalScopeImpl.InitializeHandler handler) {
                                            for (ValueParameterDescriptor
                                                    valueParameterDescriptor : unsubstitutedPrimaryConstructor.getValueParameters()) {
                                                handler.addVariableDescriptor(valueParameterDescriptor);
                                            }
                                            return Unit.INSTANCE;
                                        }
                                    });
    }

    public void resolveProperty(
            @NotNull BodiesResolveContext c,
            @NotNull KtProperty property,
            @NotNull PropertyDescriptor propertyDescriptor
    ) {
        computeDeferredType(propertyDescriptor.getReturnType());

        PreliminaryDeclarationVisitor.Companion.createForDeclaration(property, trace);
        KtExpression initializer = property.getInitializer();
        LexicalScope propertyHeaderScope = ScopeUtils.makeScopeForPropertyHeader(getScopeForProperty(c, property), propertyDescriptor);

        if (initializer != null) {
            resolvePropertyInitializer(c.getOuterDataFlowInfo(), property, propertyDescriptor, initializer, propertyHeaderScope);
        }

        KtExpression delegateExpression = property.getDelegateExpression();
        if (delegateExpression != null) {
            assert initializer == null : "Initializer should be null for delegated property : " + property.getText();
            resolvePropertyDelegate(c.getOuterDataFlowInfo(), property, propertyDescriptor, delegateExpression, propertyHeaderScope);
        }

        resolvePropertyAccessors(c, property, propertyDescriptor);
    }

    private void resolvePropertyDeclarationBodies(@NotNull BodiesResolveContext c) {

        // Member properties
        Set<KtProperty> processed = Sets.newHashSet();
        for (Map.Entry<KtClassOrObject, ClassDescriptorWithResolutionScopes> entry : c.getDeclaredClasses().entrySet()) {
            if (!(entry.getKey() instanceof KtClass)) continue;
            KtClass ktClass = (KtClass) entry.getKey();
            ClassDescriptorWithResolutionScopes classDescriptor = entry.getValue();

            for (KtProperty property : ktClass.getProperties()) {
                PropertyDescriptor propertyDescriptor = c.getProperties().get(property);
                assert propertyDescriptor != null;

                resolveProperty(c, property, propertyDescriptor);
                processed.add(property);
            }
        }

        // Top-level properties & properties of objects
        for (Map.Entry<KtProperty, PropertyDescriptor> entry : c.getProperties().entrySet()) {
            KtProperty property = entry.getKey();
            if (processed.contains(property)) continue;

            PropertyDescriptor propertyDescriptor = entry.getValue();

            resolveProperty(c, property, propertyDescriptor);
        }
    }

    private static LexicalScope makeScopeForPropertyAccessor(
            @NotNull BodiesResolveContext c, @NotNull KtPropertyAccessor accessor, @NotNull PropertyDescriptor descriptor
    ) {
        LexicalScope accessorDeclaringScope = c.getDeclaringScope(accessor);
        assert accessorDeclaringScope != null : "Scope for accessor " + accessor.getText() + " should exists";
        LexicalScope headerScope = ScopeUtils.makeScopeForPropertyHeader(accessorDeclaringScope, descriptor);
        return new LexicalScopeImpl(headerScope, descriptor, true, descriptor.getExtensionReceiverParameter(),
                                    LexicalScopeKind.PROPERTY_ACCESSOR_BODY);
    }

    private void resolvePropertyAccessors(
            @NotNull BodiesResolveContext c,
            @NotNull KtProperty property,
            @NotNull PropertyDescriptor propertyDescriptor
    ) {
        ObservableBindingTrace fieldAccessTrackingTrace = createFieldTrackingTrace(propertyDescriptor);

        KtPropertyAccessor getter = property.getGetter();
        PropertyGetterDescriptor getterDescriptor = propertyDescriptor.getGetter();
        if (getter != null && getterDescriptor != null) {
            LexicalScope accessorScope = makeScopeForPropertyAccessor(c, getter, propertyDescriptor);
            ForceResolveUtil.forceResolveAllContents(getterDescriptor.getAnnotations());
            resolveFunctionBody(c.getOuterDataFlowInfo(), fieldAccessTrackingTrace, getter, getterDescriptor, accessorScope);
        }

        KtPropertyAccessor setter = property.getSetter();
        PropertySetterDescriptor setterDescriptor = propertyDescriptor.getSetter();
        if (setter != null && setterDescriptor != null) {
            LexicalScope accessorScope = makeScopeForPropertyAccessor(c, setter, propertyDescriptor);
            ForceResolveUtil.forceResolveAllContents(setterDescriptor.getAnnotations());
            resolveFunctionBody(c.getOuterDataFlowInfo(), fieldAccessTrackingTrace, setter, setterDescriptor, accessorScope);
        }
    }

    private ObservableBindingTrace createFieldTrackingTrace(final PropertyDescriptor propertyDescriptor) {
        return new ObservableBindingTrace(trace).addHandler(
                BindingContext.REFERENCE_TARGET,
                new ObservableBindingTrace.RecordHandler<KtReferenceExpression, DeclarationDescriptor>() {
            @Override
            public void handleRecord(
                    WritableSlice<KtReferenceExpression, DeclarationDescriptor> slice,
                    KtReferenceExpression expression,
                    DeclarationDescriptor descriptor
            ) {
                if (expression instanceof KtSimpleNameExpression &&
                    descriptor instanceof SyntheticFieldDescriptor) {
                    trace.record(BindingContext.BACKING_FIELD_REQUIRED,
                                 propertyDescriptor);
                }
            }
        });
    }

    private void resolvePropertyDelegate(
            @NotNull DataFlowInfo outerDataFlowInfo,
            @NotNull KtProperty property,
            @NotNull PropertyDescriptor propertyDescriptor,
            @NotNull KtExpression delegateExpression,
            @NotNull LexicalScope propertyHeaderScope
    ) {
        KtPropertyAccessor getter = property.getGetter();
        if (getter != null && getter.hasBody()) {
            trace.report(ACCESSOR_FOR_DELEGATED_PROPERTY.on(getter));
        }

        KtPropertyAccessor setter = property.getSetter();
        if (setter != null && setter.hasBody()) {
            trace.report(ACCESSOR_FOR_DELEGATED_PROPERTY.on(setter));
        }

        LexicalScope delegateFunctionsScope = ScopeUtils.makeScopeForDelegateConventionFunctions(propertyHeaderScope, propertyDescriptor);

        LexicalScope initializerScope = ScopeUtils.makeScopeForPropertyInitializer(propertyHeaderScope, propertyDescriptor);

        KotlinType delegateType = delegatedPropertyResolver.resolveDelegateExpression(
                delegateExpression, property, propertyDescriptor, initializerScope, trace,
                outerDataFlowInfo);

        delegatedPropertyResolver.resolveDelegatedPropertyGetMethod(propertyDescriptor, delegateExpression, delegateType,
                                                                    trace, delegateFunctionsScope);

        if (property.isVar()) {
            delegatedPropertyResolver.resolveDelegatedPropertySetMethod(propertyDescriptor, delegateExpression, delegateType,
                                                                        trace, delegateFunctionsScope);
        }

        delegatedPropertyResolver.resolveDelegatedPropertyPDMethod(propertyDescriptor, delegateExpression, delegateType,
                                                                   trace, delegateFunctionsScope);
    }

    private void resolvePropertyInitializer(
            @NotNull DataFlowInfo outerDataFlowInfo,
            @NotNull KtProperty property,
            @NotNull PropertyDescriptor propertyDescriptor,
            @NotNull KtExpression initializer,
            @NotNull LexicalScope propertyHeader
    ) {
        LexicalScope propertyDeclarationInnerScope = ScopeUtils.makeScopeForPropertyInitializer(propertyHeader, propertyDescriptor);
        KotlinType expectedTypeForInitializer = property.getTypeReference() != null ? propertyDescriptor.getType() : NO_EXPECTED_TYPE;
        if (propertyDescriptor.getCompileTimeInitializer() == null) {
            expressionTypingServices.getType(propertyDeclarationInnerScope, initializer, expectedTypeForInitializer,
                                             outerDataFlowInfo, trace);
        }
    }

    @NotNull
    private static LexicalScope getScopeForProperty(@NotNull BodiesResolveContext c, @NotNull KtProperty property) {
        LexicalScope scope = c.getDeclaringScope(property);
        assert scope != null : "Scope for property " + property.getText() + " should exists";
        return scope;
    }

    private void resolveFunctionBodies(@NotNull BodiesResolveContext c) {
        for (Map.Entry<KtNamedFunction, SimpleFunctionDescriptor> entry : c.getFunctions().entrySet()) {
            KtNamedFunction declaration = entry.getKey();

            LexicalScope scope = c.getDeclaringScope(declaration);
            assert scope != null : "Scope is null: " + PsiUtilsKt.getElementTextWithContext(declaration);

            if (!c.getTopDownAnalysisMode().isLocalDeclarations() && !(bodyResolveCache instanceof BodyResolveCache.ThrowException) &&
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
            @NotNull KtDeclarationWithBody function,
            @NotNull FunctionDescriptor functionDescriptor,
            @NotNull LexicalScope declaringScope
    ) {
        computeDeferredType(functionDescriptor.getReturnType());

        resolveFunctionBody(outerDataFlowInfo, trace, function, functionDescriptor, declaringScope, null, null);

        assert functionDescriptor.getReturnType() != null;
    }

    private void resolveFunctionBody(
            @NotNull DataFlowInfo outerDataFlowInfo,
            @NotNull BindingTrace trace,
            @NotNull KtDeclarationWithBody function,
            @NotNull FunctionDescriptor functionDescriptor,
            @NotNull LexicalScope scope,
            @Nullable Function1<LexicalScope, DataFlowInfo> beforeBlockBody,
            // Creates wrapper scope for header resolution if necessary (see resolveSecondaryConstructorBody)
            @Nullable Function1<LexicalScope, LexicalScope> headerScopeFactory
    ) {
        PreliminaryDeclarationVisitor.Companion.createForDeclaration(function, trace);
        LexicalScope innerScope = FunctionDescriptorUtil.getFunctionInnerScope(scope, functionDescriptor, trace);
        List<KtParameter> valueParameters = function.getValueParameters();
        List<ValueParameterDescriptor> valueParameterDescriptors = functionDescriptor.getValueParameters();

        LexicalScope headerScope = headerScopeFactory != null ? headerScopeFactory.invoke(innerScope) : innerScope;
        valueParameterResolver.resolveValueParameters(
                valueParameters, valueParameterDescriptors, headerScope, outerDataFlowInfo, trace
        );

        // Synthetic "field" creation
        if (functionDescriptor instanceof PropertyAccessorDescriptor && functionDescriptor.getExtensionReceiverParameter() == null) {
            PropertyAccessorDescriptor accessorDescriptor = (PropertyAccessorDescriptor) functionDescriptor;
            KtProperty property = (KtProperty) function.getParent();
            final SyntheticFieldDescriptor fieldDescriptor = new SyntheticFieldDescriptor(accessorDescriptor, property);
            innerScope = new LexicalScopeImpl(innerScope, functionDescriptor, true, null,
                                              LexicalScopeKind.PROPERTY_ACCESSOR_BODY,
                                              RedeclarationHandler.DO_NOTHING, new Function1<LexicalScopeImpl.InitializeHandler, Unit>() {
                @Override
                public Unit invoke(LexicalScopeImpl.InitializeHandler handler) {
                    handler.addVariableDescriptor(fieldDescriptor);
                    return Unit.INSTANCE;
                }
            });
            // Check parameter name shadowing
            for (KtParameter parameter : function.getValueParameters()) {
                if (SyntheticFieldDescriptor.NAME.equals(parameter.getNameAsName())) {
                    trace.report(Errors.ACCESSOR_PARAMETER_NAME_SHADOWING.on(parameter));
                }
            }
        }

        DataFlowInfo dataFlowInfo = null;

        if (beforeBlockBody != null) {
            dataFlowInfo = beforeBlockBody.invoke(headerScope);
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
            @NotNull KtClass klass,
            @NotNull ConstructorDescriptor constructorDescriptor,
            @NotNull LexicalScope declaringScope
    ) {
        List<KtParameter> valueParameters = klass.getPrimaryConstructorParameters();
        List<ValueParameterDescriptor> valueParameterDescriptors = constructorDescriptor.getValueParameters();

        LexicalScope scope = getPrimaryConstructorParametersScope(declaringScope, constructorDescriptor);

        valueParameterResolver.resolveValueParameters(valueParameters, valueParameterDescriptors, scope, outerDataFlowInfo, trace);
    }

    private static void computeDeferredType(KotlinType type) {
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
            public void handleRecord(
                    WritableSlice<Box<DeferredType>, Boolean> deferredTypeKeyDeferredTypeWritableSlice,
                    Box<DeferredType> key,
                    Boolean value
            ) {
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
