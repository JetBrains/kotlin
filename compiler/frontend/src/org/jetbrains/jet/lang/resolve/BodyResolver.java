package org.jetbrains.jet.lang.resolve;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.intellij.lang.ASTNode;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.containers.Queue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.cfg.JetFlowInformationProvider;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.resolve.scopes.WritableScope;
import org.jetbrains.jet.lang.resolve.scopes.WritableScopeImpl;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverDescriptor;
import org.jetbrains.jet.lang.types.*;
import org.jetbrains.jet.lexer.JetTokens;
import org.jetbrains.jet.util.slicedmap.WritableSlice;

import java.util.*;

import static org.jetbrains.jet.lang.diagnostics.Errors.*;
import static org.jetbrains.jet.lang.resolve.BindingContext.DEFERRED_TYPE;
import static org.jetbrains.jet.lang.resolve.BindingContext.DEFERRED_TYPES;
import static org.jetbrains.jet.lang.resolve.BindingContext.DeferredTypeKey.DEFERRED_TYPE_KEY;
import static org.jetbrains.jet.lang.types.JetTypeInferrer.NO_EXPECTED_TYPE;

/**
* @author abreslav
*/
public class BodyResolver {
    private final TopDownAnalysisContext context;

    private final ObservableBindingTrace traceForConstructors;
    private final ObservableBindingTrace traceForMembers;

    public BodyResolver(TopDownAnalysisContext context) {
        this.context = context;

        // This allows access to backing fields
        this.traceForConstructors = new ObservableBindingTrace(context.getTrace()).addHandler(BindingContext.REFERENCE_TARGET, new ObservableBindingTrace.RecordHandler<JetReferenceExpression, DeclarationDescriptor>() {
            @Override
            public void handleRecord(WritableSlice<JetReferenceExpression, DeclarationDescriptor> slice, JetReferenceExpression expression, DeclarationDescriptor descriptor) {
                if (expression instanceof JetSimpleNameExpression) {
                    JetSimpleNameExpression simpleNameExpression = (JetSimpleNameExpression) expression;
                    if (simpleNameExpression.getReferencedNameElementType() == JetTokens.FIELD_IDENTIFIER) {
                        if (!BodyResolver.this.context.getTrace().getBindingContext().get(BindingContext.BACKING_FIELD_REQUIRED, (PropertyDescriptor) descriptor)) {
                            BodyResolver.this.context.getTrace().report(NO_BACKING_FIELD.on(expression));
                        }
                    }
                }
            }
        });

        // This tracks access to properties in order to register primary constructor parameters that yield real fields (JET-9)
        this.traceForMembers = new ObservableBindingTrace(context.getTrace()).addHandler(BindingContext.REFERENCE_TARGET, new ObservableBindingTrace.RecordHandler<JetReferenceExpression, DeclarationDescriptor>() {
            @Override
            public void handleRecord(WritableSlice<JetReferenceExpression, DeclarationDescriptor> slice, JetReferenceExpression expression, DeclarationDescriptor descriptor) {
                if (descriptor instanceof PropertyDescriptor) {
                    PropertyDescriptor propertyDescriptor = (PropertyDescriptor) descriptor;
                    if (BodyResolver.this.context.getPrimaryConstructorParameterProperties().contains(propertyDescriptor)) {
                        traceForMembers.record(BindingContext.BACKING_FIELD_REQUIRED, propertyDescriptor);
                    }
                }
            }
        });

    }


    public void resolveBehaviorDeclarationBodies() {

        resolveDelegationSpecifierLists();
        resolveClassAnnotations();

        resolveAnonymousInitializers();
        resolvePropertyDeclarationBodies();

        resolveSecondaryConstructorBodies();
        resolveFunctionBodies();

        computeDeferredTypes();        
    }
    
    private void computeDeferredTypes() {
        Collection<DeferredType> deferredTypes = context.getTrace().get(DEFERRED_TYPES, DEFERRED_TYPE_KEY);
        if (deferredTypes != null) {
            final Queue<DeferredType> queue = new Queue<DeferredType>(deferredTypes.size());
            context.getTrace().addHandler(DEFERRED_TYPE, new ObservableBindingTrace.RecordHandler<BindingContext.DeferredTypeKey, DeferredType>() {
                @Override
                public void handleRecord(WritableSlice<BindingContext.DeferredTypeKey, DeferredType> deferredTypeKeyDeferredTypeWritableSlice, BindingContext.DeferredTypeKey key, DeferredType value) {
                    queue.addLast(value);
                }
            });
            for (DeferredType deferredType : deferredTypes) {
                queue.addLast(deferredType);
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


    private void resolveDelegationSpecifierLists() {
        // TODO : Make sure the same thing is not initialized twice
        for (Map.Entry<JetClass, MutableClassDescriptor> entry : context.getClasses().entrySet()) {
            resolveDelegationSpecifierList(entry.getKey(), entry.getValue());
        }
        for (Map.Entry<JetObjectDeclaration, MutableClassDescriptor> entry : context.getObjects().entrySet()) {
            resolveDelegationSpecifierList(entry.getKey(), entry.getValue());
        }
    }

    private void resolveDelegationSpecifierList(final JetClassOrObject jetClass, final MutableClassDescriptor descriptor) {
        final ConstructorDescriptor primaryConstructor = descriptor.getUnsubstitutedPrimaryConstructor();
        final JetScope scopeForConstructor = primaryConstructor == null
                ? null
                : getInnerScopeForConstructor(primaryConstructor, descriptor.getScopeForMemberResolution(), true);
        final JetTypeInferrer.Services typeInferrer = context.getSemanticServices().getTypeInferrerServices(traceForConstructors); // TODO : flow

        final Map<JetTypeReference, JetType> supertypes = Maps.newLinkedHashMap();
        JetVisitorVoid visitor = new JetVisitorVoid() {
            private void recordSupertype(JetTypeReference typeReference, JetType supertype) {
                if (supertype == null) return;
                supertypes.put(typeReference, supertype);
            }

            @Override
            public void visitDelegationByExpressionSpecifier(JetDelegatorByExpressionSpecifier specifier) {
                if (descriptor.getKind() == ClassKind.TRAIT) {
                    context.getTrace().report(DELEGATION_IN_TRAIT.on(specifier));
                }
                JetType supertype = context.getTrace().getBindingContext().get(BindingContext.TYPE, specifier.getTypeReference());
                recordSupertype(specifier.getTypeReference(), supertype);
                if (supertype != null) {
                    DeclarationDescriptor declarationDescriptor = supertype.getConstructor().getDeclarationDescriptor();
                    if (declarationDescriptor instanceof ClassDescriptor) {
                        ClassDescriptor classDescriptor = (ClassDescriptor) declarationDescriptor;
                        if (classDescriptor.getKind() != ClassKind.TRAIT) {
                            context.getTrace().report(DELEGATION_NOT_TO_TRAIT.on(specifier.getTypeReference()));
                        }
                    }
                }
                JetExpression delegateExpression = specifier.getDelegateExpression();
                if (delegateExpression != null) {
                    JetScope scope = scopeForConstructor == null
                                     ? descriptor.getScopeForMemberResolution()
                                     : scopeForConstructor;
                    JetType type = typeInferrer.getType(scope, delegateExpression, NO_EXPECTED_TYPE);
                    if (type != null && supertype != null && !context.getSemanticServices().getTypeChecker().isSubtypeOf(type, supertype)) {
                        context.getTrace().report(TYPE_MISMATCH.on(delegateExpression, supertype, type));
                    }
                }
            }

            @Override
            public void visitDelegationToSuperCallSpecifier(JetDelegatorToSuperCall call) {
                JetValueArgumentList valueArgumentList = call.getValueArgumentList();
                ASTNode node = valueArgumentList == null ? call.getNode() : valueArgumentList.getNode();
                if (descriptor.getKind() == ClassKind.TRAIT) {
                    context.getTrace().report(SUPERTYPE_INITIALIZED_IN_TRAIT.on(node));
                }
                JetTypeReference typeReference = call.getTypeReference();
                if (typeReference != null) {
                    if (descriptor.getUnsubstitutedPrimaryConstructor() != null) {
                        JetType supertype = typeInferrer.getCallResolver().resolveCall(context.getTrace(), scopeForConstructor, ReceiverDescriptor.NO_RECEIVER, call, NO_EXPECTED_TYPE);
                        if (supertype != null) {
                            recordSupertype(typeReference, supertype);
                            ClassDescriptor classDescriptor = TypeUtils.getClassDescriptor(supertype);
                            if (classDescriptor != null) {
                                if (classDescriptor.getKind() == ClassKind.TRAIT) {
                                    context.getTrace().report(CONSTRUCTOR_IN_TRAIT.on(node));
                                }
                            }
                        }
                        else {
                            recordSupertype(typeReference, context.getTrace().getBindingContext().get(BindingContext.TYPE, typeReference));
                        }
                    }
                    else if (descriptor.getKind() != ClassKind.TRAIT) {
                        JetType supertype = context.getTrace().getBindingContext().get(BindingContext.TYPE, typeReference);
                        recordSupertype(typeReference, supertype);

                        assert valueArgumentList != null;
                        context.getTrace().report(PRIMARY_CONSTRUCTOR_MISSING_SUPER_CONSTRUCTOR_CALL.on(valueArgumentList, jetClass));
                    }
                }
            }

            @Override
            public void visitDelegationToSuperClassSpecifier(JetDelegatorToSuperClass specifier) {
                JetTypeReference typeReference = specifier.getTypeReference();
                JetType supertype = context.getTrace().getBindingContext().get(BindingContext.TYPE, typeReference);
                recordSupertype(typeReference, supertype);
                if (supertype != null) {
                    ClassDescriptor classDescriptor = TypeUtils.getClassDescriptor(supertype);
                    if (classDescriptor != null) {
                        if (descriptor.getKind() != ClassKind.TRAIT) {
                            if (classDescriptor.hasConstructors() && !ErrorUtils.isError(classDescriptor.getTypeConstructor()) && classDescriptor.getKind() != ClassKind.TRAIT) {
                                context.getTrace().report(SUPERTYPE_NOT_INITIALIZED.on(specifier));
                            }
                        }
                    }
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
    private void checkSupertypeList(@NotNull MutableClassDescriptor supertypeOwner, @NotNull Map<JetTypeReference, JetType> supertypes, Set<TypeConstructor> allowedFinalSupertypes) {
        Set<TypeConstructor> typeConstructors = Sets.newHashSet();
        boolean classAppeared = false;
        for (Map.Entry<JetTypeReference, JetType> entry : supertypes.entrySet()) {
            JetTypeReference typeReference = entry.getKey();
            JetType supertype = entry.getValue();

            ClassDescriptor classDescriptor = TypeUtils.getClassDescriptor(supertype);
            if (classDescriptor != null) {
                if (classDescriptor.getKind() != ClassKind.TRAIT) {
                    if (classAppeared) {
                        context.getTrace().report(MANY_CLASSES_IN_SUPERTYPE_LIST.on(typeReference));
                    }
                    else {
                        classAppeared = true;
                    }
                }
            }
            else {
                context.getTrace().report(SUPERTYPE_NOT_A_CLASS_OR_TRAIT.on(typeReference));
            }

            TypeConstructor constructor = supertype.getConstructor();
            if (!typeConstructors.add(constructor)) {
                context.getTrace().report(SUPERTYPE_APPEARS_TWICE.on(typeReference));
            }

            if (constructor.isSealed() && !allowedFinalSupertypes.contains(constructor)) {
                context.getTrace().report(FINAL_SUPERTYPE.on(typeReference));
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
        List<JetClassInitializer> anonymousInitializers = jetClassOrObject.getAnonymousInitializers();
        if (jetClassOrObject.hasPrimaryConstructor()) {
            ConstructorDescriptor primaryConstructor = classDescriptor.getUnsubstitutedPrimaryConstructor();
            assert primaryConstructor != null;
            final JetScope scopeForConstructor = getInnerScopeForConstructor(primaryConstructor, classDescriptor.getScopeForMemberResolution(), true);
            JetTypeInferrer.Services typeInferrer = context.getSemanticServices().getTypeInferrerServices(createFieldAssignTrackingTrace()); // TODO : flow
            for (JetClassInitializer anonymousInitializer : anonymousInitializers) {
                typeInferrer.getType(scopeForConstructor, anonymousInitializer.getBody(), NO_EXPECTED_TYPE);
            }
        }
        else {
            for (JetClassInitializer anonymousInitializer : anonymousInitializers) {
                context.getTrace().report(ANONYMOUS_INITIALIZER_WITHOUT_CONSTRUCTOR.on(anonymousInitializer));
            }
        }
    }

    private void resolveSecondaryConstructorBodies() {
        for (Map.Entry<JetDeclaration, ConstructorDescriptor> entry : this.context.getConstructors().entrySet()) {
            JetDeclaration declaration = entry.getKey();
            ConstructorDescriptor descriptor = entry.getValue();

            resolveSecondaryConstructorBody((JetConstructor) declaration, descriptor, ((MutableClassDescriptor) descriptor.getContainingDeclaration()).getScopeForMemberResolution());

            assert descriptor.getReturnType() != null;
        }
    }

    private void resolveSecondaryConstructorBody(JetConstructor declaration, final ConstructorDescriptor descriptor, final JetScope declaringScope) {
        final JetScope functionInnerScope = getInnerScopeForConstructor(descriptor, declaringScope, false);

        final JetTypeInferrer.Services typeInferrerForInitializers = context.getSemanticServices().getTypeInferrerServices(traceForConstructors);

        JetClass containingClass = PsiTreeUtil.getParentOfType(declaration, JetClass.class);
        assert containingClass != null : "This must be guaranteed by the parser";
        if (!containingClass.hasPrimaryConstructor()) {
            context.getTrace().report(SECONDARY_CONSTRUCTOR_BUT_NO_PRIMARY.on(declaration.getNameNode()));
        }
        else {
            List<JetDelegationSpecifier> initializers = declaration.getInitializers();
            if (initializers.isEmpty()) {
                context.getTrace().report(SECONDARY_CONSTRUCTOR_NO_INITIALIZER_LIST.on(declaration.getNameNode()));
            }
            else {
                initializers.get(0).accept(new JetVisitorVoid() {
                    @Override
                    public void visitDelegationToSuperCallSpecifier(JetDelegatorToSuperCall call) {
                        JetTypeReference typeReference = call.getTypeReference();
                        if (typeReference != null) {
                            typeInferrerForInitializers.getCallResolver().resolveCall(context.getTrace(), functionInnerScope, ReceiverDescriptor.NO_RECEIVER, call, NO_EXPECTED_TYPE);
                        }
                    }

                    @Override
                    public void visitDelegationToThisCall(JetDelegatorToThisCall call) {
                        // TODO : check that there's no recursion in this() calls
                        // TODO : check: if a this() call is present, no other initializers are allowed
                        ClassDescriptor classDescriptor = descriptor.getContainingDeclaration();

                        typeInferrerForInitializers.getCallResolver().resolveCall(context.getTrace(),
                                functionInnerScope,
                                ReceiverDescriptor.NO_RECEIVER, call, NO_EXPECTED_TYPE);
//                                call.getThisReference(),
//                                classDescriptor,
//                                classDescriptor.getDefaultType(),
//                                call);
//                        context.getTrace().getErrorHandler().genericError(call.getNode(), "this-calls are not supported");
                    }

                    @Override
                    public void visitDelegationByExpressionSpecifier(JetDelegatorByExpressionSpecifier specifier) {
                        context.getTrace().report(BY_IN_SECONDARY_CONSTRUCTOR.on(specifier));
                    }

                    @Override
                    public void visitDelegationToSuperClassSpecifier(JetDelegatorToSuperClass specifier) {
                        context.getTrace().report(INITIALIZER_WITH_NO_ARGUMENTS.on(specifier));
                    }

                    @Override
                    public void visitDelegationSpecifier(JetDelegationSpecifier specifier) {
                        throw new IllegalStateException();
                    }
                });
                for (int i = 1, initializersSize = initializers.size(); i < initializersSize; i++) {
                    JetDelegationSpecifier initializer = initializers.get(i);
                    context.getTrace().report(MANY_CALLS_TO_THIS.on(initializer));
                }
            }
        }
        JetExpression bodyExpression = declaration.getBodyExpression();
        if (bodyExpression != null) {
            //context.getClassDescriptorResolver().computeFlowData(declaration, bodyExpression);
            //JetFlowInformationProvider flowInformationProvider = context.getClassDescriptorResolver().computeFlowData(declaration, bodyExpression);
            JetTypeInferrer.Services typeInferrer = context.getSemanticServices().getTypeInferrerServices(traceForConstructors);

            typeInferrer.checkFunctionReturnType(functionInnerScope, declaration, descriptor, JetStandardClasses.getUnitType());
        }
    }

    @NotNull
    private JetScope getInnerScopeForConstructor(@NotNull ConstructorDescriptor descriptor, @NotNull JetScope declaringScope, boolean primary) {
        WritableScope constructorScope = new WritableScopeImpl(declaringScope, declaringScope.getContainingDeclaration(), new TraceBasedRedeclarationHandler(context.getTrace())).setDebugName("Inner scope for constructor");
        for (PropertyDescriptor propertyDescriptor : ((MutableClassDescriptor) descriptor.getContainingDeclaration()).getProperties()) {
            constructorScope.addPropertyDescriptorByFieldName("$" + propertyDescriptor.getName(), propertyDescriptor);
        }

//        constructorScope.setImplicitReceiver(new ClassReceiver(descriptor.getContainingDeclaration()));

        for (ValueParameterDescriptor valueParameterDescriptor : descriptor.getValueParameters()) {
            JetParameter parameter = (JetParameter) context.getTrace().getBindingContext().get(BindingContext.DESCRIPTOR_TO_DECLARATION, valueParameterDescriptor);
            if (parameter.getValOrVarNode() == null || !primary) {
                constructorScope.addVariableDescriptor(valueParameterDescriptor);
            }
        }

        constructorScope.addLabeledDeclaration(descriptor); // TODO : Labels for constructors?!

        return constructorScope;
    }

    private void resolvePropertyDeclarationBodies() {

        // Member properties
        Set<JetProperty> processed = Sets.newHashSet();
        for (Map.Entry<JetClass, MutableClassDescriptor> entry : context.getClasses().entrySet()) {
            JetClass jetClass = entry.getKey();
            MutableClassDescriptor classDescriptor = entry.getValue();

            for (JetProperty property : jetClass.getProperties()) {
                final PropertyDescriptor propertyDescriptor = this.context.getProperties().get(property);
                assert propertyDescriptor != null;

                JetScope declaringScope = this.context.getDeclaringScopes().get(property);

                JetExpression initializer = property.getInitializer();
                if (initializer != null) {
                    ConstructorDescriptor primaryConstructor = classDescriptor.getUnsubstitutedPrimaryConstructor();
                    if (primaryConstructor != null) {
                        JetScope scope = getInnerScopeForConstructor(primaryConstructor, classDescriptor.getScopeForMemberResolution(), true);
                        resolvePropertyInitializer(property, propertyDescriptor, initializer, scope);
                    }
                }

                resolvePropertyAccessors(property, propertyDescriptor, declaringScope);
                processed.add(property);
            }
        }

        // Top-level properties & properties of objects
        for (Map.Entry<JetProperty, PropertyDescriptor> entry : this.context.getProperties().entrySet()) {
            JetProperty property = entry.getKey();
            if (processed.contains(property)) continue;

            final PropertyDescriptor propertyDescriptor = entry.getValue();
            JetScope declaringScope = this.context.getDeclaringScopes().get(property);

            JetExpression initializer = property.getInitializer();
            if (initializer != null) {
                resolvePropertyInitializer(property, propertyDescriptor, initializer, declaringScope);
            }

            resolvePropertyAccessors(property, propertyDescriptor, declaringScope);
        }
    }

    private JetScope getPropertyDeclarationInnerScope(@NotNull JetScope outerScope, @NotNull PropertyDescriptor propertyDescriptor) {
        WritableScopeImpl result = new WritableScopeImpl(outerScope, propertyDescriptor, new TraceBasedRedeclarationHandler(context.getTrace())).setDebugName("Property declaration inner scope");
        for (TypeParameterDescriptor typeParameterDescriptor : propertyDescriptor.getTypeParameters()) {
            result.addTypeParameterDescriptor(typeParameterDescriptor);
        }
        ReceiverDescriptor receiver = propertyDescriptor.getReceiver();
        if (receiver.exists()) {
            result.setImplicitReceiver(receiver);
        }
        return result;
    }

    private void resolvePropertyAccessors(JetProperty property, PropertyDescriptor propertyDescriptor, JetScope declaringScope) {
        ObservableBindingTrace fieldAccessTrackingTrace = createFieldTrackingTrace(propertyDescriptor);

        WritableScope accessorScope = new WritableScopeImpl(getPropertyDeclarationInnerScope(declaringScope, propertyDescriptor), declaringScope.getContainingDeclaration(), new TraceBasedRedeclarationHandler(context.getTrace())).setDebugName("Accessor scope");
        accessorScope.addPropertyDescriptorByFieldName("$" + propertyDescriptor.getName(), propertyDescriptor);

        JetPropertyAccessor getter = property.getGetter();
        PropertyGetterDescriptor getterDescriptor = propertyDescriptor.getGetter();
        if (getter != null && getterDescriptor != null) {
            resolveFunctionBody(fieldAccessTrackingTrace, getter, getterDescriptor, accessorScope);
        }

        JetPropertyAccessor setter = property.getSetter();
        PropertySetterDescriptor setterDescriptor = propertyDescriptor.getSetter();
        if (setter != null && setterDescriptor != null) {
            resolveFunctionBody(fieldAccessTrackingTrace, setter, setterDescriptor, accessorScope);
        }
    }

    private ObservableBindingTrace createFieldTrackingTrace(final PropertyDescriptor propertyDescriptor) {
        return new ObservableBindingTrace(traceForMembers).addHandler(BindingContext.REFERENCE_TARGET, new ObservableBindingTrace.RecordHandler<JetReferenceExpression, DeclarationDescriptor>() {
            @Override
            public void handleRecord(WritableSlice<JetReferenceExpression, DeclarationDescriptor> slice, JetReferenceExpression expression, DeclarationDescriptor descriptor) {
                if (expression instanceof JetSimpleNameExpression) {
                    JetSimpleNameExpression simpleNameExpression = (JetSimpleNameExpression) expression;
                    if (simpleNameExpression.getReferencedNameElementType() == JetTokens.FIELD_IDENTIFIER) {
                        // This check may be considered redundant as long as $x is only accessible from accessors to $x
                        if (descriptor == propertyDescriptor) { // TODO : original?
                            traceForMembers.record(BindingContext.BACKING_FIELD_REQUIRED, propertyDescriptor); // TODO: this context.getTrace()?
                        }
                    }
                }
            }
        });
    }

    private ObservableBindingTrace createFieldAssignTrackingTrace() {
        return new ObservableBindingTrace(traceForConstructors).addHandler(BindingContext.VARIABLE_ASSIGNMENT, new ObservableBindingTrace.RecordHandler<JetExpression, DeclarationDescriptor>() {
            @Override
            public void handleRecord(WritableSlice<JetExpression, DeclarationDescriptor> jetExpressionBooleanWritableSlice, JetExpression expression, DeclarationDescriptor descriptor) {
                if (expression instanceof JetSimpleNameExpression) {
                    JetSimpleNameExpression variable = (JetSimpleNameExpression) expression;
                    if (variable.getReferencedNameElementType() == JetTokens.FIELD_IDENTIFIER) {
                        if (descriptor instanceof PropertyDescriptor) {
                            traceForMembers.record(BindingContext.IS_INITIALIZED, (PropertyDescriptor) descriptor);
                        }
                    }
                }
            }
        });
    }

    private void resolvePropertyInitializer(JetProperty property, PropertyDescriptor propertyDescriptor, JetExpression initializer, JetScope scope) {
        //JetFlowInformationProvider flowInformationProvider = context.getClassDescriptorResolver().computeFlowData(property, initializer); // TODO : flow JET-15
        JetTypeInferrer.Services typeInferrer = context.getSemanticServices().getTypeInferrerServices(traceForConstructors);
        JetType type = typeInferrer.getType(getPropertyDeclarationInnerScope(scope, propertyDescriptor), initializer, NO_EXPECTED_TYPE);

        JetType expectedType = propertyDescriptor.getInType();
        if (expectedType == null) {
            expectedType = propertyDescriptor.getOutType();
        }
        if (type != null && expectedType != null
            && !context.getSemanticServices().getTypeChecker().isSubtypeOf(type, expectedType)) {
            context.getTrace().report(TYPE_MISMATCH.on(initializer, expectedType, type));
        }
    }

    private void resolveFunctionBodies() {
        for (Map.Entry<JetNamedFunction, FunctionDescriptorImpl> entry : this.context.getFunctions().entrySet()) {
            JetNamedFunction declaration = entry.getKey();
            FunctionDescriptor descriptor = entry.getValue();

            JetScope declaringScope = this.context.getDeclaringScopes().get(declaration);
            assert declaringScope != null;

            resolveFunctionBody(traceForMembers, declaration, descriptor, declaringScope);

            assert descriptor.getReturnType() != null;
        }
    }

    private void resolveFunctionBody(
            @NotNull BindingTrace trace,
            @NotNull JetDeclarationWithBody function,
            @NotNull FunctionDescriptor functionDescriptor,
            @NotNull JetScope declaringScope) {

        JetExpression bodyExpression = function.getBodyExpression();
        if (bodyExpression != null) {
            //JetFlowInformationProvider flowInformationProvider = context.getClassDescriptorResolver().computeFlowData(function.asElement(), bodyExpression);
            JetTypeInferrer.Services typeInferrer = context.getSemanticServices().getTypeInferrerServices(trace);

            typeInferrer.checkFunctionReturnType(declaringScope, function, functionDescriptor);
        }

        JetTypeInferrer.Services typeInferrer = context.getSemanticServices().getTypeInferrerServices(trace);
        List<JetParameter> valueParameters = function.getValueParameters();
        for (int i = 0; i < valueParameters.size(); i++) {
            ValueParameterDescriptor valueParameterDescriptor = functionDescriptor.getValueParameters().get(i);
            if (valueParameterDescriptor.hasDefaultValue()) {
                JetParameter jetParameter = valueParameters.get(i);
                JetExpression defaultValue = jetParameter.getDefaultValue();
                if (defaultValue != null) {
                    typeInferrer.getType(declaringScope, defaultValue, valueParameterDescriptor.getOutType());
                }
            }
        }

        assert functionDescriptor.getReturnType() != null;
    }
}
