/*
 * Copyright 2010-2017 JetBrains s.r.o.
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
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import kotlin.Unit;
import kotlin.collections.CollectionsKt;
import kotlin.jvm.functions.Function1;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.builtins.FunctionTypesKt;
import org.jetbrains.kotlin.builtins.KotlinBuiltIns;
import org.jetbrains.kotlin.config.LanguageFeature;
import org.jetbrains.kotlin.config.LanguageVersionSettings;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.descriptors.impl.SyntheticFieldDescriptor;
import org.jetbrains.kotlin.diagnostics.Errors;
import org.jetbrains.kotlin.lexer.KtTokens;
import org.jetbrains.kotlin.name.SpecialNames;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.psi.psiUtil.PsiUtilsKt;
import org.jetbrains.kotlin.resolve.calls.CallResolver;
import org.jetbrains.kotlin.resolve.calls.components.InferenceSession;
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall;
import org.jetbrains.kotlin.resolve.calls.results.OverloadResolutionResults;
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowInfo;
import org.jetbrains.kotlin.resolve.calls.util.CallMaker;
import org.jetbrains.kotlin.resolve.descriptorUtil.DescriptorUtilsKt;
import org.jetbrains.kotlin.resolve.lazy.ForceResolveUtil;
import org.jetbrains.kotlin.resolve.multiplatform.ExpectedActualResolver;
import org.jetbrains.kotlin.resolve.scopes.*;
import org.jetbrains.kotlin.resolve.source.KotlinSourceElementKt;
import org.jetbrains.kotlin.types.*;
import org.jetbrains.kotlin.types.expressions.ExpressionTypingContext;
import org.jetbrains.kotlin.types.expressions.ExpressionTypingServices;
import org.jetbrains.kotlin.types.expressions.PreliminaryDeclarationVisitor;
import org.jetbrains.kotlin.types.expressions.ValueParameterResolver;
import org.jetbrains.kotlin.types.expressions.typeInfoFactory.TypeInfoFactoryKt;
import org.jetbrains.kotlin.util.Box;
import org.jetbrains.kotlin.util.ReenteringLazyValueComputationException;

import java.util.*;

import static org.jetbrains.kotlin.config.LanguageFeature.AllowSealedInheritorsInDifferentFilesOfSamePackage;
import static org.jetbrains.kotlin.config.LanguageFeature.TopLevelSealedInheritance;
import static org.jetbrains.kotlin.diagnostics.Errors.*;
import static org.jetbrains.kotlin.resolve.BindingContext.*;
import static org.jetbrains.kotlin.resolve.descriptorUtil.DescriptorUtilsKt.isEffectivelyExternal;
import static org.jetbrains.kotlin.types.TypeUtils.NO_EXPECTED_TYPE;

public class BodyResolver {
    @NotNull private final Project project;
    @NotNull private final AnnotationChecker annotationChecker;
    @NotNull private final ExpressionTypingServices expressionTypingServices;
    @NotNull private final CallResolver callResolver;
    @NotNull private final ObservableBindingTrace trace;
    @NotNull private final ControlFlowAnalyzer controlFlowAnalyzer;
    @NotNull private final DeclarationsChecker declarationsChecker;
    @NotNull private final AnnotationResolver annotationResolver;
    @NotNull private final DelegatedPropertyResolver delegatedPropertyResolver;
    @NotNull private final AnalyzerExtensions analyzerExtensions;
    @NotNull private final ValueParameterResolver valueParameterResolver;
    @NotNull private final BodyResolveCache bodyResolveCache;
    @NotNull private final KotlinBuiltIns builtIns;
    @NotNull private final OverloadChecker overloadChecker;
    @NotNull private final LanguageVersionSettings languageVersionSettings;

    public BodyResolver(
            @NotNull Project project,
            @NotNull AnnotationResolver annotationResolver,
            @NotNull BodyResolveCache bodyResolveCache,
            @NotNull CallResolver callResolver,
            @NotNull ControlFlowAnalyzer controlFlowAnalyzer,
            @NotNull DeclarationsChecker declarationsChecker,
            @NotNull DelegatedPropertyResolver delegatedPropertyResolver,
            @NotNull ExpressionTypingServices expressionTypingServices,
            @NotNull AnalyzerExtensions analyzerExtensions,
            @NotNull BindingTrace trace,
            @NotNull ValueParameterResolver valueParameterResolver,
            @NotNull AnnotationChecker annotationChecker,
            @NotNull KotlinBuiltIns builtIns,
            @NotNull OverloadChecker overloadChecker,
            @NotNull LanguageVersionSettings languageVersionSettings
    ) {
        this.project = project;
        this.annotationResolver = annotationResolver;
        this.bodyResolveCache = bodyResolveCache;
        this.callResolver = callResolver;
        this.controlFlowAnalyzer = controlFlowAnalyzer;
        this.declarationsChecker = declarationsChecker;
        this.delegatedPropertyResolver = delegatedPropertyResolver;
        this.expressionTypingServices = expressionTypingServices;
        this.analyzerExtensions = analyzerExtensions;
        this.annotationChecker = annotationChecker;
        this.overloadChecker = overloadChecker;
        this.trace = new ObservableBindingTrace(trace);
        this.valueParameterResolver = valueParameterResolver;
        this.builtIns = builtIns;
        this.languageVersionSettings = languageVersionSettings;
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
        for (Map.Entry<KtSecondaryConstructor, ClassConstructorDescriptor> entry : c.getSecondaryConstructors().entrySet()) {
            LexicalScope declaringScope = c.getDeclaringScope(entry.getKey());
            assert declaringScope != null : "Declaring scope should be registered before body resolve";
            resolveSecondaryConstructorBody(c.getOuterDataFlowInfo(), trace, entry.getKey(), entry.getValue(), declaringScope, c.getLocalContext());
        }
        if (c.getSecondaryConstructors().isEmpty()) return;
        Set<ConstructorDescriptor> visitedConstructors = new HashSet<>();
        for (Map.Entry<KtSecondaryConstructor, ClassConstructorDescriptor> entry : c.getSecondaryConstructors().entrySet()) {
            checkCyclicConstructorDelegationCall(entry.getValue(), visitedConstructors);
        }
    }

    public void resolveSecondaryConstructorBody(
            @NotNull DataFlowInfo outerDataFlowInfo,
            @NotNull BindingTrace trace,
            @NotNull KtSecondaryConstructor constructor,
            @NotNull ClassConstructorDescriptor descriptor,
            @NotNull LexicalScope declaringScope,
            @Nullable ExpressionTypingContext localContext
    ) {
        ForceResolveUtil.forceResolveAllContents(descriptor.getAnnotations());

        resolveFunctionBody(
                outerDataFlowInfo, trace, constructor, descriptor, declaringScope,
                headerInnerScope -> resolveSecondaryConstructorDelegationCall(
                        outerDataFlowInfo, trace, headerInnerScope, constructor,
                        descriptor, localContext != null ? localContext.inferenceSession : null
                ),
                scope -> new LexicalScopeImpl(
                        scope, descriptor, scope.isOwnerDescriptorAccessibleByLabel(), scope.getImplicitReceivers(),
                        LexicalScopeKind.CONSTRUCTOR_HEADER
                ),
                localContext
        );
    }

    @Nullable
    private DataFlowInfo resolveSecondaryConstructorDelegationCall(
            @NotNull DataFlowInfo outerDataFlowInfo,
            @NotNull BindingTrace trace,
            @NotNull LexicalScope scope,
            @NotNull KtSecondaryConstructor constructor,
            @NotNull ClassConstructorDescriptor descriptor,
            @Nullable InferenceSession inferenceSession
    ) {
        if (descriptor.isExpect() || isEffectivelyExternal(descriptor)) {
            // For expected and external classes, we do not resolve constructor delegation calls because they are prohibited
            return DataFlowInfo.Companion.getEMPTY();
        }

        OverloadResolutionResults<?> results = callResolver.resolveConstructorDelegationCall(
                trace, scope, outerDataFlowInfo,
                descriptor, constructor.getDelegationCall(), inferenceSession);

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
        Set<ConstructorDescriptor> visitedInCurrentChain = new HashSet<>();
        ConstructorDescriptor currentConstructorDescriptor = constructorDescriptor;
        while (true) {
            ProgressManager.checkCanceled();

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
        analyzerExtensions.process(c);
    }

    private void resolveSuperTypeEntryLists(@NotNull BodiesResolveContext c) {
        // TODO : Make sure the same thing is not initialized twice
        for (Map.Entry<KtClassOrObject, ClassDescriptorWithResolutionScopes> entry : c.getDeclaredClasses().entrySet()) {
            KtClassOrObject classOrObject = entry.getKey();
            ClassDescriptorWithResolutionScopes descriptor = entry.getValue();
            ExpressionTypingContext localContext = c.getLocalContext();

            resolveSuperTypeEntryList(c.getOuterDataFlowInfo(), classOrObject, descriptor,
                                      descriptor.getUnsubstitutedPrimaryConstructor(),
                                      descriptor.getScopeForConstructorHeaderResolution(),
                                      descriptor.getScopeForMemberDeclarationResolution(),
                                      localContext != null ? localContext.inferenceSession : null);
        }
    }

    public void resolveSuperTypeEntryList(
            @NotNull DataFlowInfo outerDataFlowInfo,
            @NotNull KtClassOrObject ktClass,
            @NotNull ClassDescriptor descriptor,
            @Nullable ConstructorDescriptor primaryConstructor,
            @NotNull LexicalScope scopeForConstructorResolution,
            @NotNull LexicalScope scopeForMemberResolution,
            @Nullable InferenceSession inferenceSession
    ) {
        ProgressManager.checkCanceled();

        LexicalScope scopeForConstructor =
                primaryConstructor == null
                ? null
                : FunctionDescriptorUtil.getFunctionInnerScope(scopeForConstructorResolution, primaryConstructor, trace, overloadChecker);
        if (primaryConstructor == null) {
            checkRedeclarationsInClassHeaderWithoutPrimaryConstructor(descriptor, scopeForConstructorResolution);
        }
        ExpressionTypingServices typeInferrer = expressionTypingServices; // TODO : flow

        Map<KtTypeReference, KotlinType> supertypes = Maps.newLinkedHashMap();
        ResolvedCall<?>[] primaryConstructorDelegationCall = new ResolvedCall[1];
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
                    typeInferrer.getType(
                            scope, delegateExpression, expectedType, outerDataFlowInfo,
                            inferenceSession != null ? inferenceSession : InferenceSession.Companion.getDefault(), trace
                    );
                }

                if (descriptor.isExpect()) {
                    trace.report(IMPLEMENTATION_BY_DELEGATION_IN_EXPECT_CLASS.on(specifier));
                }
                else if (primaryConstructor == null) {
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
                if (descriptor.isExpect()) {
                    trace.report(SUPERTYPE_INITIALIZED_IN_EXPECTED_CLASS.on(elementToMark));
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
                        trace, scopeForConstructor, CallMaker.makeConstructorCallWithoutTypeArguments(call),
                        NO_EXPECTED_TYPE, outerDataFlowInfo, false, inferenceSession
                );
                if (results.isSingleResult()) {
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
                    !descriptor.isExpect() && !isEffectivelyExternal(descriptor) &&
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

        if (ktClass instanceof KtEnumEntry && DescriptorUtils.isEnumEntry(descriptor) && ktClass.getSuperTypeListEntries().isEmpty()) {
            assert scopeForConstructor != null : "Scope for enum class constructor should be non-null: " + descriptor;
            resolveConstructorCallForEnumEntryWithoutInitializer(
                    (KtEnumEntry) ktClass, descriptor, scopeForConstructor,
                    outerDataFlowInfo, primaryConstructorDelegationCall, inferenceSession
            );
        }

        for (KtSuperTypeListEntry delegationSpecifier : ktClass.getSuperTypeListEntries()) {
            ProgressManager.checkCanceled();

            delegationSpecifier.accept(visitor);
        }

        if (DescriptorUtils.isAnnotationClass(descriptor) && ktClass.getSuperTypeList() != null) {
            trace.report(SUPERTYPES_FOR_ANNOTATION_CLASS.on(ktClass.getSuperTypeList()));
        }

        if (primaryConstructorDelegationCall[0] != null && primaryConstructor != null) {
            recordConstructorDelegationCall(trace, primaryConstructor, primaryConstructorDelegationCall[0]);
        }

        checkSupertypeList(descriptor, supertypes, ktClass);
    }

    private void checkRedeclarationsInClassHeaderWithoutPrimaryConstructor(
            @NotNull final ClassDescriptor descriptor, @NotNull LexicalScope scopeForConstructorResolution
    ) {
        // Initializing a scope will report errors if any.
        new LexicalScopeImpl(
                scopeForConstructorResolution, descriptor, true, Collections.emptyList(), LexicalScopeKind.CLASS_HEADER,
                new TraceBasedLocalRedeclarationChecker(trace, overloadChecker),
                new Function1<LexicalScopeImpl.InitializeHandler, Unit>() {
                    @Override
                    public Unit invoke(LexicalScopeImpl.InitializeHandler handler) {
                        // If a class has no primary constructor, it still can have type parameters declared in header.
                        for (TypeParameterDescriptor typeParameter : descriptor.getDeclaredTypeParameters()) {
                            handler.addClassifierDescriptor(typeParameter);
                        }
                        return Unit.INSTANCE;
                    }
                });
    }

    private void resolveConstructorCallForEnumEntryWithoutInitializer(
            @NotNull KtEnumEntry ktEnumEntry,
            @NotNull ClassDescriptor enumEntryDescriptor,
            @NotNull LexicalScope scopeForConstructor,
            @NotNull DataFlowInfo outerDataFlowInfo,
            @NotNull ResolvedCall<?>[] primaryConstructorDelegationCall,
            @Nullable InferenceSession inferenceSession
    ) {
        assert enumEntryDescriptor.getKind() == ClassKind.ENUM_ENTRY : "Enum entry expected: " + enumEntryDescriptor;
        ClassDescriptor enumClassDescriptor = (ClassDescriptor) enumEntryDescriptor.getContainingDeclaration();
        if (enumClassDescriptor.getKind() != ClassKind.ENUM_CLASS) return;
        if (enumClassDescriptor.isExpect()) return;

        List<ClassConstructorDescriptor> applicableConstructors = getConstructorForEmptyArgumentsList(enumClassDescriptor);
        if (applicableConstructors.size() != 1) {
            trace.report(ENUM_ENTRY_SHOULD_BE_INITIALIZED.on(ktEnumEntry));
            return;
        }

        KtInitializerList ktInitializerList = new KtPsiFactory(project, false).createEnumEntryInitializerList();
        KtSuperTypeCallEntry ktCallEntry = (KtSuperTypeCallEntry) ktInitializerList.getInitializers().get(0);
        Call call = CallMaker.makeConstructorCallWithoutTypeArguments(ktCallEntry);
        trace.record(BindingContext.TYPE, ktCallEntry.getTypeReference(), enumClassDescriptor.getDefaultType());
        trace.record(BindingContext.CALL, ktEnumEntry, call);
        OverloadResolutionResults<FunctionDescriptor> results = callResolver.resolveFunctionCall(
                trace,
                scopeForConstructor,
                call,
                NO_EXPECTED_TYPE,
                outerDataFlowInfo,
                false,
                inferenceSession
        );
        if (primaryConstructorDelegationCall[0] == null) {
            primaryConstructorDelegationCall[0] = results.getResultingCall();
        }
    }

    @NotNull
    private static List<ClassConstructorDescriptor> getConstructorForEmptyArgumentsList(@NotNull ClassDescriptor descriptor) {
        return CollectionsKt.filter(
                descriptor.getConstructors(),
                (constructor) -> CollectionsKt.all(
                        constructor.getValueParameters(),
                        (parameter) -> parameter.declaresDefaultValue() || parameter.getVarargElementType() != null
                )
        );
    }

    // Returns a set of enum or sealed types of which supertypeOwner is an entry or a member
    @NotNull
    private Set<TypeConstructor> getAllowedFinalSupertypes(
            @NotNull ClassDescriptor descriptor,
            @NotNull Map<KtTypeReference, KotlinType> supertypes,
            @NotNull KtClassOrObject ktClassOrObject
    ) {
        Set<TypeConstructor> parentEnumOrSealed = Collections.emptySet();
        if (ktClassOrObject instanceof KtEnumEntry) {
            parentEnumOrSealed = Collections.singleton(((ClassDescriptor) descriptor.getContainingDeclaration()).getTypeConstructor());
        }
        else if (languageVersionSettings.supportsFeature(TopLevelSealedInheritance) && DescriptorUtils.isTopLevelDeclaration(descriptor)) {
            // TODO: improve diagnostic when top level sealed inheritance is disabled
            for (KotlinType supertype : supertypes.values()) {
                ClassifierDescriptor classifierDescriptor = supertype.getConstructor().getDeclarationDescriptor();
                if (DescriptorUtils.isSealedClass(classifierDescriptor) && DescriptorUtils.isTopLevelDeclaration(classifierDescriptor)) {
                    parentEnumOrSealed = Collections.singleton(classifierDescriptor.getTypeConstructor());
                }
            }
        }
        else {
            ClassDescriptor currentDescriptor = descriptor;
            while (currentDescriptor.getContainingDeclaration() instanceof ClassDescriptor) {
                currentDescriptor = (ClassDescriptor) currentDescriptor.getContainingDeclaration();
                if (DescriptorUtils.isSealedClass(currentDescriptor)) {
                    if (parentEnumOrSealed.isEmpty()) {
                        parentEnumOrSealed = new HashSet<>();
                    }
                    parentEnumOrSealed.add(currentDescriptor.getTypeConstructor());
                    if (currentDescriptor.isExpect()) {
                        List<MemberDescriptor> actualDescriptors = ExpectedActualResolver.INSTANCE.findCompatibleActualForExpected(
                                currentDescriptor, DescriptorUtilsKt.getModule( currentDescriptor)
                        );
                        for (MemberDescriptor actualDescriptor: actualDescriptors) {
                            if (actualDescriptor instanceof TypeAliasDescriptor) {
                                parentEnumOrSealed.add(((TypeAliasDescriptor) actualDescriptor).getExpandedType().getConstructor());
                            }
                        }
                    }
                }
            }
        }
        return parentEnumOrSealed;
    }

    @SuppressWarnings("unchecked")
    private static void recordConstructorDelegationCall(
            @NotNull BindingTrace trace,
            @NotNull ConstructorDescriptor constructor,
            @NotNull ResolvedCall<?> call
    ) {
        trace.record(CONSTRUCTOR_RESOLVED_DELEGATION_CALL, constructor, (ResolvedCall<ConstructorDescriptor>) call);
    }

    private void checkSupertypeList(
            @NotNull ClassDescriptor supertypeOwner,
            @NotNull Map<KtTypeReference, KotlinType> supertypes,
            @NotNull KtClassOrObject ktClassOrObject
    ) {
        Set<TypeConstructor> allowedFinalSupertypes = getAllowedFinalSupertypes(supertypeOwner, supertypes, ktClassOrObject);
        Set<TypeConstructor> typeConstructors = new HashSet<>();
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

                if (FunctionTypesKt.isExtensionFunctionType(supertype) &&
                    !languageVersionSettings.supportsFeature(LanguageFeature.FunctionalTypeWithExtensionAsSupertype)
                ) {
                    trace.report(SUPERTYPE_IS_EXTENSION_FUNCTION_TYPE.on(typeReference));
                }
                else if (FunctionTypesKt.isSuspendFunctionType(supertype) &&
                         !languageVersionSettings.supportsFeature(LanguageFeature.SuspendFunctionAsSupertype)
                ) {
                    trace.report(SUPERTYPE_IS_SUSPEND_FUNCTION_TYPE.on(typeReference));
                }
                else if (FunctionTypesKt.isKSuspendFunctionType(supertype) &&
                         !languageVersionSettings.supportsFeature(LanguageFeature.SuspendFunctionAsSupertype)) {
                    trace.report(SUPERTYPE_IS_KSUSPEND_FUNCTION_TYPE.on(typeReference));
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
                    else if (ktClassOrObject.hasModifier(KtTokens.DATA_KEYWORD) &&
                             !languageVersionSettings.supportsFeature(LanguageFeature.DataClassInheritance)) {
                        trace.report(DATA_CLASS_CANNOT_HAVE_CLASS_SUPERTYPES.on(typeReference));
                        addSupertype = false;
                    }
                    else if (DescriptorUtils.isSubclass(classDescriptor, builtIns.getThrowable())) {
                        if (!supertypeOwner.getDeclaredTypeParameters().isEmpty()) {
                            trace.report(GENERIC_THROWABLE_SUBCLASS.on(ktClassOrObject.getTypeParameterList()));
                            addSupertype = false;
                        }
                        else if (!supertypeOwner.getTypeConstructor().getParameters().isEmpty()) {
                            if (languageVersionSettings
                                    .supportsFeature(LanguageFeature.ProhibitInnerClassesOfGenericClassExtendingThrowable)) {
                                trace.report(INNER_CLASS_OF_GENERIC_THROWABLE_SUBCLASS.on(ktClassOrObject));
                                addSupertype = false;
                            }
                            else {
                                trace.report(INNER_CLASS_OF_GENERIC_THROWABLE_SUBCLASS_WARNING.on(ktClassOrObject));
                            }
                        }
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
                if (DescriptorUtils.isSealedClass(classDescriptor)) {
                    DeclarationDescriptor containingDescriptor = supertypeOwner.getContainingDeclaration();
                    while (containingDescriptor != null && containingDescriptor != classDescriptor) {
                        containingDescriptor = containingDescriptor.getContainingDeclaration();
                    }
                    if (containingDescriptor == null) {
                        if (
                            !languageVersionSettings.supportsFeature(AllowSealedInheritorsInDifferentFilesOfSamePackage) ||
                            DescriptorUtils.isLocal(supertypeOwner)
                        ) {
                            trace.report(SEALED_SUPERTYPE.on(typeReference));
                        }
                    }
                    else {
                        String declarationName;
                        if (supertypeOwner.getName() == SpecialNames.NO_NAME_PROVIDED) {
                            declarationName = "Anonymous object";
                        } else {
                            declarationName = "Local class";
                        }
                        trace.report(SEALED_SUPERTYPE_IN_LOCAL_CLASS.on(typeReference, declarationName, classDescriptor.getKind()));
                    }
                }
                else if (ModalityUtilsKt.isFinalOrEnum(classDescriptor)) {
                    trace.report(FINAL_SUPERTYPE.on(typeReference));
                }
                else if (KotlinBuiltIns.isEnum(classDescriptor)) {
                    trace.report(CLASS_CANNOT_BE_EXTENDED_DIRECTLY.on(typeReference, classDescriptor));
                }
            }
        }
    }

    private void resolveAnonymousInitializers(@NotNull BodiesResolveContext c) {
        for (Map.Entry<KtAnonymousInitializer, ClassDescriptorWithResolutionScopes> entry : c.getAnonymousInitializers().entrySet()) {
            KtAnonymousInitializer initializer = entry.getKey();
            ClassDescriptorWithResolutionScopes descriptor = entry.getValue();
            ExpressionTypingContext context = c.getLocalContext();
            resolveAnonymousInitializer(c.getOuterDataFlowInfo(), initializer, descriptor, context != null ? context.inferenceSession : null);
        }
    }

    public void resolveAnonymousInitializer(
            @NotNull DataFlowInfo outerDataFlowInfo,
            @NotNull KtAnonymousInitializer anonymousInitializer,
            @NotNull ClassDescriptorWithResolutionScopes classDescriptor,
            @Nullable InferenceSession inferenceSession
    ) {
        ProgressManager.checkCanceled();

        LexicalScope scopeForInitializers = classDescriptor.getScopeForInitializerResolution();
        KtExpression body = anonymousInitializer.getBody();
        if (body != null) {
            PreliminaryDeclarationVisitor.Companion.createForDeclaration(
                    (KtDeclaration) anonymousInitializer.getParent().getParent(), trace, languageVersionSettings);
            expressionTypingServices.getTypeInfo(
                    scopeForInitializers, body, NO_EXPECTED_TYPE, outerDataFlowInfo,
                    inferenceSession != null ? inferenceSession : InferenceSession.Companion.getDefault(), trace, /*isStatement = */true
            );
        }
        processModifiersOnInitializer(anonymousInitializer, scopeForInitializers);
        if (classDescriptor.getConstructors().isEmpty()) {
            trace.report(ANONYMOUS_INITIALIZER_IN_INTERFACE.on(anonymousInitializer));
        }
        if (classDescriptor.isExpect()) {
            trace.report(EXPECTED_DECLARATION_WITH_BODY.on(anonymousInitializer));
        }
    }

    private void processModifiersOnInitializer(@NotNull KtModifierListOwner owner, @NotNull LexicalScope scope) {
        annotationChecker.check(owner, trace, null);
        ModifierCheckerCore.INSTANCE.check(owner, trace, null, languageVersionSettings);
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

                ExpressionTypingContext localContext = c.getLocalContext();
                LexicalScope parameterScope = getPrimaryConstructorParametersScope(classDescriptor.getScopeForConstructorHeaderResolution(),
                                                                                   unsubstitutedPrimaryConstructor);
                valueParameterResolver.resolveValueParameters(
                        klass.getPrimaryConstructorParameters(), unsubstitutedPrimaryConstructor.getValueParameters(),
                        parameterScope, c.getOuterDataFlowInfo(), trace, localContext != null ? localContext.inferenceSession : null
                );
                // Annotations on value parameter and constructor parameter could be splitted
                resolveConstructorPropertyDescriptors(klass);
            }
        }
    }

    private void resolveConstructorPropertyDescriptors(KtClassOrObject ktClassOrObject) {
        for (KtParameter parameter : ktClassOrObject.getPrimaryConstructorParameters()) {
            PropertyDescriptor descriptor = trace.getBindingContext().get(BindingContext.PRIMARY_CONSTRUCTOR_PARAMETER, parameter);
            if (descriptor != null) {
                ForceResolveUtil.forceResolveAllContents(descriptor.getAnnotations());

                if (languageVersionSettings.supportsFeature(LanguageFeature.ProhibitErroneousExpressionsInAnnotationsWithUseSiteTargets)) {
                    PropertyGetterDescriptor getterDescriptor = descriptor.getGetter();
                    if (getterDescriptor != null) {
                        ForceResolveUtil.forceResolveAllContents(getterDescriptor.getAnnotations());
                    }

                    PropertySetterDescriptor setterDescriptor = descriptor.getSetter();
                    if (setterDescriptor != null) {
                        ForceResolveUtil.forceResolveAllContents(setterDescriptor.getAnnotations());
                    }
                }
            }
        }
    }

    private static LexicalScope getPrimaryConstructorParametersScope(
            LexicalScope originalScope,
            ConstructorDescriptor unsubstitutedPrimaryConstructor
    ) {
        return new LexicalScopeImpl(originalScope, unsubstitutedPrimaryConstructor, false, Collections.emptyList(),
                                    LexicalScopeKind.DEFAULT_VALUE, LocalRedeclarationChecker.DO_NOTHING.INSTANCE,
                                    handler -> {
                                        for (ValueParameterDescriptor valueParameter : unsubstitutedPrimaryConstructor.getValueParameters()) {
                                            handler.addVariableDescriptor(valueParameter);
                                        }
                                        return Unit.INSTANCE;
                                    });
    }

    public void resolveProperty(
            @NotNull BodiesResolveContext c,
            @NotNull KtProperty property,
            @NotNull PropertyDescriptor propertyDescriptor
    ) {
        computeDeferredType(propertyDescriptor.getReturnType());

        PreliminaryDeclarationVisitor.Companion.createForDeclaration(property, trace, languageVersionSettings);
        KtExpression initializer = property.getInitializer();
        LexicalScope propertyHeaderScope = ScopeUtils.makeScopeForPropertyHeader(getScopeForProperty(c, property), propertyDescriptor);
        ExpressionTypingContext context = c.getLocalContext();

        if (initializer != null) {
            resolvePropertyInitializer(
                    c.getOuterDataFlowInfo(), property, propertyDescriptor,
                    initializer, propertyHeaderScope, context != null ? context.inferenceSession : null
            );
        }

        KtExpression delegateExpression = property.getDelegateExpression();
        if (delegateExpression != null) {
            assert initializer == null : "Initializer should be null for delegated property : " + property.getText();
            resolvePropertyDelegate(
                    c.getOuterDataFlowInfo(), property, propertyDescriptor,
                    delegateExpression, propertyHeaderScope, context != null ? context.inferenceSession : null
            );
        }

        resolvePropertyAccessors(c, property, propertyDescriptor);

        ForceResolveUtil.forceResolveAllContents(propertyDescriptor.getAnnotations());

        FieldDescriptor backingField = propertyDescriptor.getBackingField();
        if (backingField != null) {
            ForceResolveUtil.forceResolveAllContents(backingField.getAnnotations());
        }
    }

    private void resolvePropertyDeclarationBodies(@NotNull BodiesResolveContext c) {

        // Member properties
        Set<KtProperty> processed = new HashSet<>();
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
        List<ReceiverParameterDescriptor> implicitReceivers = new ArrayList<>();
        ReceiverParameterDescriptor extensionReceiverParameter = descriptor.getExtensionReceiverParameter();
        if (extensionReceiverParameter != null) {
            implicitReceivers.add(extensionReceiverParameter);
        }
        return new LexicalScopeImpl(headerScope, descriptor, true, implicitReceivers,
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

        boolean forceResolveAnnotations =
                languageVersionSettings.supportsFeature(LanguageFeature.ProhibitErroneousExpressionsInAnnotationsWithUseSiteTargets);

        if (getterDescriptor != null) {
            if (getter != null) {
                LexicalScope accessorScope = makeScopeForPropertyAccessor(c, getter, propertyDescriptor);
                resolveFunctionBody(c.getOuterDataFlowInfo(), fieldAccessTrackingTrace, getter, getterDescriptor, accessorScope, c.getLocalContext());
            }

            if (getter != null || forceResolveAnnotations) {
                ForceResolveUtil.forceResolveAllContents(getterDescriptor.getAnnotations());
            }
        }

        KtPropertyAccessor setter = property.getSetter();
        PropertySetterDescriptor setterDescriptor = propertyDescriptor.getSetter();

        if (setterDescriptor != null) {
            if (setter != null) {
                LexicalScope accessorScope = makeScopeForPropertyAccessor(c, setter, propertyDescriptor);
                resolveFunctionBody(c.getOuterDataFlowInfo(), fieldAccessTrackingTrace, setter, setterDescriptor, accessorScope, c.getLocalContext());
            }

            if (setter != null || forceResolveAnnotations) {
                ForceResolveUtil.forceResolveAllContents(setterDescriptor.getAnnotations());
            }
        }
    }

    private ObservableBindingTrace createFieldTrackingTrace(PropertyDescriptor propertyDescriptor) {
        return new ObservableBindingTrace(trace).addHandler(
                BindingContext.REFERENCE_TARGET,
                (slice, expression, descriptor) -> {
                    if (expression instanceof KtSimpleNameExpression &&
                        descriptor instanceof SyntheticFieldDescriptor) {
                        trace.record(BindingContext.BACKING_FIELD_REQUIRED,
                                     propertyDescriptor);
                    }
                }
        );
    }

    private void resolvePropertyDelegate(
            @NotNull DataFlowInfo outerDataFlowInfo,
            @NotNull KtProperty property,
            @NotNull PropertyDescriptor propertyDescriptor,
            @NotNull KtExpression delegateExpression,
            @NotNull LexicalScope propertyHeaderScope,
            @Nullable InferenceSession inferenceSession
    ) {
        delegatedPropertyResolver.resolvePropertyDelegate(outerDataFlowInfo,
                                                          property,
                                                          propertyDescriptor,
                                                          delegateExpression,
                                                          propertyHeaderScope,
                                                          inferenceSession != null ? inferenceSession : InferenceSession.Companion.getDefault(),
                                                          trace);
    }

    private void resolvePropertyInitializer(
            @NotNull DataFlowInfo outerDataFlowInfo,
            @NotNull KtProperty property,
            @NotNull PropertyDescriptor propertyDescriptor,
            @NotNull KtExpression initializer,
            @NotNull LexicalScope propertyHeader,
            @Nullable InferenceSession inferenceSession
    ) {
        LexicalScope propertyDeclarationInnerScope = ScopeUtils.makeScopeForPropertyInitializer(propertyHeader, propertyDescriptor);
        KotlinType expectedTypeForInitializer = property.getTypeReference() != null ? propertyDescriptor.getType() : NO_EXPECTED_TYPE;
        if (propertyDescriptor.getCompileTimeInitializer() == null) {
            expressionTypingServices.getType(
                    propertyDeclarationInnerScope, initializer, expectedTypeForInitializer,
                    outerDataFlowInfo, inferenceSession != null ? inferenceSession : InferenceSession.Companion.getDefault(), trace
            );
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
                resolveFunctionBody(c.getOuterDataFlowInfo(), trace, declaration, entry.getValue(), scope, c.getLocalContext());
            }
        }
    }

    public void resolveFunctionBody(
            @NotNull DataFlowInfo outerDataFlowInfo,
            @NotNull BindingTrace trace,
            @NotNull KtDeclarationWithBody function,
            @NotNull FunctionDescriptor functionDescriptor,
            @NotNull LexicalScope declaringScope,
            @Nullable ExpressionTypingContext localContext
    ) {
        computeDeferredType(functionDescriptor.getReturnType());

        resolveFunctionBody(outerDataFlowInfo, trace, function, functionDescriptor, declaringScope, null, null, localContext);

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
            @Nullable Function1<LexicalScope, LexicalScope> headerScopeFactory,
            @Nullable ExpressionTypingContext localContext
    ) {
        ProgressManager.checkCanceled();

        PreliminaryDeclarationVisitor.Companion.createForDeclaration(function, trace, languageVersionSettings);
        LexicalScope innerScope = FunctionDescriptorUtil.getFunctionInnerScope(scope, functionDescriptor, trace, overloadChecker);
        List<KtParameter> valueParameters = function.getValueParameters();
        List<ValueParameterDescriptor> valueParameterDescriptors = functionDescriptor.getValueParameters();

        LexicalScope headerScope = headerScopeFactory != null ? headerScopeFactory.invoke(innerScope) : innerScope;
        valueParameterResolver.resolveValueParameters(
                valueParameters, valueParameterDescriptors, headerScope, outerDataFlowInfo, trace,
                localContext != null ? localContext.inferenceSession : null
        );

        // Synthetic "field" creation
        if (functionDescriptor instanceof PropertyAccessorDescriptor && functionDescriptor.getExtensionReceiverParameter() == null
            && functionDescriptor.getContextReceiverParameters().isEmpty()) {
            PropertyAccessorDescriptor accessorDescriptor = (PropertyAccessorDescriptor) functionDescriptor;
            KtProperty property = (KtProperty) function.getParent();
            SourceElement propertySourceElement = KotlinSourceElementKt.toSourceElement(property);
            SyntheticFieldDescriptor fieldDescriptor = new SyntheticFieldDescriptor(accessorDescriptor, propertySourceElement);
            innerScope = new LexicalScopeImpl(innerScope, functionDescriptor, true, Collections.emptyList(),
                                              LexicalScopeKind.PROPERTY_ACCESSOR_BODY,
                                              LocalRedeclarationChecker.DO_NOTHING.INSTANCE, handler -> {
                                                  handler.addVariableDescriptor(fieldDescriptor);
                                                  return Unit.INSTANCE;
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
                    innerScope, function, functionDescriptor, dataFlowInfo != null ? dataFlowInfo : outerDataFlowInfo, null, trace, localContext
            );
        }

        assert functionDescriptor.getReturnType() != null;
    }

    public void resolveConstructorParameterDefaultValues(
            @NotNull DataFlowInfo outerDataFlowInfo,
            @NotNull BindingTrace trace,
            @NotNull KtPrimaryConstructor constructor,
            @NotNull ConstructorDescriptor constructorDescriptor,
            @NotNull LexicalScope declaringScope,
            @Nullable InferenceSession inferenceSession
    ) {
        List<KtParameter> valueParameters = constructor.getValueParameters();
        List<ValueParameterDescriptor> valueParameterDescriptors = constructorDescriptor.getValueParameters();

        LexicalScope scope = getPrimaryConstructorParametersScope(declaringScope, constructorDescriptor);

        valueParameterResolver.resolveValueParameters(valueParameters, valueParameterDescriptors, scope, outerDataFlowInfo, trace, inferenceSession);
    }

    public static void computeDeferredType(KotlinType type) {
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
        Deque<DeferredType> queue = new ArrayDeque<>(deferredTypes.size() + 1);
        trace.addHandler(DEFERRED_TYPE, (deferredTypeKeyDeferredTypeWritableSlice, key, value) -> queue.offerLast(key.getData()));
        for (Box<DeferredType> deferredType : deferredTypes) {
            queue.offerLast(deferredType.getData());
        }
        while (!queue.isEmpty()) {
            DeferredType deferredType = queue.pollFirst();
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
