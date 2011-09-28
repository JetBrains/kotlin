package org.jetbrains.jet.lang.resolve;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.containers.Queue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
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
//                            BodyResolver.this.context.getTrace().getErrorHandler().genericError(expression.getNode(), "This property does not have a backing field");
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

        checkIfPrimaryConstructorIsNecessary();
        
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

    private void checkIfPrimaryConstructorIsNecessary() {
        for (Map.Entry<JetClass, MutableClassDescriptor> entry : context.getClasses().entrySet()) {
            MutableClassDescriptor classDescriptor = entry.getValue();
            JetClass jetClass = entry.getKey();
            if (classDescriptor.getUnsubstitutedPrimaryConstructor() == null && !(classDescriptor.getKind() == ClassKind.TRAIT)) {
                for (PropertyDescriptor propertyDescriptor : classDescriptor.getProperties()) {
                    if (context.getTrace().getBindingContext().get(BindingContext.BACKING_FIELD_REQUIRED, propertyDescriptor)) {
                        PsiElement nameIdentifier = jetClass.getNameIdentifier();
                        if (nameIdentifier != null) {
//                            context.getTrace().getErrorHandler().genericError(nameIdentifier.getNode(),
//                                    "This class must have a primary constructor, because property " + propertyDescriptor.getName() + " has a backing field");
                            context.getTrace().report(PRIMARY_CONSTRUCTOR_MISSING_STATEFUL_PROPERTY.on(jetClass, nameIdentifier, propertyDescriptor));
                        }
                        break;
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
        final JetTypeInferrer.Services typeInferrer = context.getSemanticServices().getTypeInferrerServices(traceForConstructors, JetFlowInformationProvider.NONE); // TODO : flow

        final Map<JetTypeReference, JetType> supertypes = Maps.newLinkedHashMap();
        JetVisitorVoid visitor = new JetVisitorVoid() {
            private void recordSupertype(JetTypeReference typeReference, JetType supertype) {
                if (supertype == null) return;
                supertypes.put(typeReference, supertype);
            }

            @Override
            public void visitDelegationByExpressionSpecifier(JetDelegatorByExpressionSpecifier specifier) {
                if (descriptor.getKind() == ClassKind.TRAIT) {
//                    context.getTrace().getErrorHandler().genericError(specifier.getNode(), "Traits cannot use delegation");
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
//                    context.getTrace().getErrorHandler().genericError(node, "Traits cannot initialize supertypes");
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
//                                    context.getTrace().getErrorHandler().genericError(node, "A trait may not have a constructor");
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
//                        context.getTrace().getErrorHandler().genericError(valueArgumentList.getNode(),
//                                                             "Class " + JetPsiUtil.safeName(jetClass.getName()) + " must have a constructor in order to be able to initialize supertypes");
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
//                                context.getTrace().getErrorHandler().genericError(specifier.getNode(), "This type has a constructor, and thus must be initialized here");
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
//                        context.getTrace().getErrorHandler().genericError(typeReference.getNode(), "Only one class may appear in a supertype list");
                        context.getTrace().report(MANY_CLASSES_IN_SUPERTYPE_LIST.on(typeReference));
                    }
                    else {
                        classAppeared = true;
                    }
                }
            }
            else {
//                context.getTrace().getErrorHandler().genericError(typeReference.getNode(), "Only classes and traits may serve as supertypes");
                context.getTrace().report(SUPERTYPE_NOT_A_CLASS_OR_TRAIT.on(typeReference));
            }

            TypeConstructor constructor = supertype.getConstructor();
            if (!typeConstructors.add(constructor)) {
//                context.getTrace().getErrorHandler().genericError(typeReference.getNode(), "A supertype appears twice");
                context.getTrace().report(SUPERTYPE_APPEARS_TWICE.on(typeReference));
            }

            if (constructor.isSealed() && !allowedFinalSupertypes.contains(constructor)) {
//                context.getTrace().getErrorHandler().genericError(typeReference.getNode(), "This type is final, so it cannot be inherited from");
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
            JetTypeInferrer.Services typeInferrer = context.getSemanticServices().getTypeInferrerServices(createFieldAssignTrackingTrace(), JetFlowInformationProvider.NONE); // TODO : flow
            for (JetClassInitializer anonymousInitializer : anonymousInitializers) {
                typeInferrer.getType(scopeForConstructor, anonymousInitializer.getBody(), NO_EXPECTED_TYPE);
            }
        }
        else {
            for (JetClassInitializer anonymousInitializer : anonymousInitializers) {
//                context.getTrace().getErrorHandler().genericError(anonymousInitializer.getNode(), "Anonymous initializers are only allowed in the presence of a primary constructor");
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

        final JetTypeInferrer.Services typeInferrerForInitializers = context.getSemanticServices().getTypeInferrerServices(traceForConstructors, JetFlowInformationProvider.NONE);

        JetClass containingClass = PsiTreeUtil.getParentOfType(declaration, JetClass.class);
        assert containingClass != null : "This must be guaranteed by the parser";
        if (!containingClass.hasPrimaryConstructor()) {
//            context.getTrace().getErrorHandler().genericError(declaration.getNameNode(), "A secondary constructor may appear only in a class that has a primary constructor");
            context.getTrace().report(SECONDARY_CONSTRUCTOR_BUT_NO_PRIMARY.on(declaration.getNameNode()));
        }
        else {
            List<JetDelegationSpecifier> initializers = declaration.getInitializers();
            if (initializers.isEmpty()) {
//                context.getTrace().getErrorHandler().genericError(declaration.getNameNode(), "Secondary constructors must have an initializer list");
                context.getTrace().report(SECONDARY_CONSTRUCTOR_NO_INITIALIZER_LIST.on(declaration.getNameNode()));
            }
            else {
                initializers.get(0).accept(new JetVisitorVoid() {
                    @Override
                    public void visitDelegationToSuperCallSpecifier(JetDelegatorToSuperCall call) {
                        JetTypeReference typeReference = call.getTypeReference();
                        if (typeReference != null) {
                            typeInferrerForInitializers.getCallResolver().resolveCall(context.getTrace(), functionInnerScope, null, call, NO_EXPECTED_TYPE);
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
//                        context.getTrace().getErrorHandler().genericError(specifier.getNode(), "'by'-clause is only supported for primary constructors");
                        context.getTrace().report(BY_IN_SECONDARY_CONSTRUCTOR.on(specifier));
                    }

                    @Override
                    public void visitDelegationToSuperClassSpecifier(JetDelegatorToSuperClass specifier) {
//                        context.getTrace().getErrorHandler().genericError(specifier.getNode(), "Constructor parameters required");
                        context.getTrace().report(INITIALIZER_WITH_NO_ARGUMENTS.on(specifier));
                    }

                    @Override
                    public void visitDelegationSpecifier(JetDelegationSpecifier specifier) {
                        throw new IllegalStateException();
                    }
                });
                for (int i = 1, initializersSize = initializers.size(); i < initializersSize; i++) {
                    JetDelegationSpecifier initializer = initializers.get(i);
//                    context.getTrace().getErrorHandler().genericError(initializer.getNode(), "Only one call to 'this(...)' is allowed");
                    context.getTrace().report(MANY_CALLS_TO_THIS.on(initializer));
                }
            }
        }
        JetExpression bodyExpression = declaration.getBodyExpression();
        if (bodyExpression != null) {
            context.getClassDescriptorResolver().computeFlowData(declaration, bodyExpression);
            JetFlowInformationProvider flowInformationProvider = context.getClassDescriptorResolver().computeFlowData(declaration, bodyExpression);
            JetTypeInferrer.Services typeInferrer = context.getSemanticServices().getTypeInferrerServices(traceForConstructors, flowInformationProvider);

            typeInferrer.checkFunctionReturnType(functionInnerScope, declaration, JetStandardClasses.getUnitType());
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
                checkProperty(property, propertyDescriptor, classDescriptor);
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
            checkProperty(property, propertyDescriptor, null);
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

    protected void checkProperty(JetProperty property, PropertyDescriptor propertyDescriptor, @Nullable ClassDescriptor classDescriptor) {
        checkPropertyAbstractness(property, propertyDescriptor, classDescriptor);
        checkPropertyInitializer(property, propertyDescriptor, classDescriptor);
    }

    private void checkPropertyAbstractness(JetProperty property, PropertyDescriptor propertyDescriptor, ClassDescriptor classDescriptor) {
        JetPropertyAccessor getter = property.getGetter();
        JetPropertyAccessor setter = property.getSetter();
        JetModifierList modifierList = property.getModifierList();
        ASTNode abstractNode = modifierList != null ? modifierList.getModifierNode(JetTokens.ABSTRACT_KEYWORD) : null;

        if (abstractNode != null) { //has abstract modifier
            if (classDescriptor == null) {
//                context.getTrace().getErrorHandler().genericError(abstractNode, "A property may be abstract only when defined in a class or trait");
                context.getTrace().report(ABSTRACT_PROPERTY_NOT_IN_CLASS.on(property, abstractNode));
                return;
            }
            if (!(classDescriptor.getModality() == Modality.ABSTRACT) && classDescriptor.getKind() != ClassKind.ENUM_CLASS) {
//                context.getTrace().getErrorHandler().genericError(abstractNode, "Abstract property " + property.getName() + " in non-abstract class " + classDescriptor.getName());
                PsiElement classElement = context.getTrace().get(BindingContext.DESCRIPTOR_TO_DECLARATION, classDescriptor);
                assert classElement instanceof JetClass;
                context.getTrace().report(ABSTRACT_PROPERTY_IN_NON_ABSTRACT_CLASS.on(property, abstractNode, property.getName(), classDescriptor, (JetClass) classElement));
                return;
            }
            if (classDescriptor.getKind() == ClassKind.TRAIT) {
//                context.getTrace().getErrorHandler().genericWarning(abstractNode, "Abstract modifier is redundant in traits");
                context.getTrace().report(REDUNDANT_ABSTRACT.on(property, abstractNode));
            }
        }

        if (propertyDescriptor.getModality() == Modality.ABSTRACT) {
            JetType returnType = propertyDescriptor.getReturnType();
            if (returnType instanceof DeferredType) {
                returnType = ((DeferredType) returnType).getActualType();
            }

            JetExpression initializer = property.getInitializer();
            if (initializer != null) {
//                context.getTrace().getErrorHandler().genericError(initializer.getNode(), "Property with initializer cannot be abstract");
                context.getTrace().report(ABSTRACT_PROPERTY_WITH_INITIALIZER.on(property, initializer, returnType));
            }
            if (getter != null && getter.getBodyExpression() != null) {
//                context.getTrace().getErrorHandler().genericError(getter.getNode(), "Property with getter implementation cannot be abstract");
                context.getTrace().report(ABSTRACT_PROPERTY_WITH_GETTER.on(property, getter, returnType));
            }
            if (setter != null && setter.getBodyExpression() != null) {
//                context.getTrace().getErrorHandler().genericError(setter.getNode(), "Property with setter implementation cannot be abstract");
                context.getTrace().report(ABSTRACT_PROPERTY_WITH_SETTER.on(property, setter, returnType));
            }
        }
    }

    private void checkPropertyInitializer(JetProperty property, PropertyDescriptor propertyDescriptor, ClassDescriptor classDescriptor) {
        JetPropertyAccessor getter = property.getGetter();
        JetPropertyAccessor setter = property.getSetter();
        boolean hasAccessorImplementation = (getter != null && getter.getBodyExpression() != null) ||
                                            (setter != null && setter.getBodyExpression() != null);
        if (propertyDescriptor.getModality() == Modality.ABSTRACT) return;

        boolean inTrait = classDescriptor != null && classDescriptor.getKind() == ClassKind.TRAIT;
        JetExpression initializer = property.getInitializer();
        boolean backingFieldRequired = context.getTrace().getBindingContext().get(BindingContext.BACKING_FIELD_REQUIRED, propertyDescriptor);

        PsiElement nameIdentifier = property.getNameIdentifier();
        ASTNode nameNode = nameIdentifier == null ? property.getNode() : nameIdentifier.getNode();

        if (inTrait && backingFieldRequired && hasAccessorImplementation) {
//            context.getTrace().getErrorHandler().genericError(nameNode, "Property in a trait cannot have a backing field");
            context.getTrace().report(BACKING_FIELD_IN_TRAIT.on(nameNode));
        }
        if (initializer == null) {
            if (backingFieldRequired && !inTrait && !context.getTrace().getBindingContext().get(BindingContext.IS_INITIALIZED, propertyDescriptor)) {
                if (classDescriptor == null || hasAccessorImplementation) {
//                    context.getTrace().getErrorHandler().genericError(nameNode, "Property must be initialized");
                    context.getTrace().report(MUST_BE_INITIALIZED.on(nameNode));
                } else {
//                    context.getTrace().getErrorHandler().genericError(nameNode, "Property must be initialized or be abstract");
                    context.getTrace().report(MUST_BE_INITIALIZED_OR_BE_ABSTRACT.on(property, nameNode));
                }
            }
            return;
        }
        if (inTrait) {
//            context.getTrace().getErrorHandler().genericError(initializer.getNode(), "Property initializers are not allowed in traits");
            JetType returnType = propertyDescriptor.getReturnType();
            if (returnType instanceof DeferredType) {
                returnType = ((DeferredType) returnType).getActualType();
            }
            context.getTrace().report(PROPERTY_INITIALIZER_IN_TRAIT.on(property, initializer, returnType));
        }
        else if (!backingFieldRequired) {
//            context.getTrace().getErrorHandler().genericError(initializer.getNode(), "Initializer is not allowed here because this property has no backing field");
            context.getTrace().report(PROPERTY_INITIALIZER_NO_BACKING_FIELD.on(initializer));
        }
        else if (classDescriptor != null && classDescriptor.getUnsubstitutedPrimaryConstructor() == null) {
//            context.getTrace().getErrorHandler().genericError(initializer.getNode(), "Property initializers are not allowed when no primary constructor is present");
            PsiElement classElement = context.getTrace().get(BindingContext.DESCRIPTOR_TO_DECLARATION, classDescriptor);
            assert classElement instanceof JetClass;

            context.getTrace().report(PROPERTY_INITIALIZER_NO_PRIMARY_CONSTRUCTOR.on(property, initializer, (JetClass) classElement));
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
        JetFlowInformationProvider flowInformationProvider = context.getClassDescriptorResolver().computeFlowData(property, initializer); // TODO : flow JET-15
        JetTypeInferrer.Services typeInferrer = context.getSemanticServices().getTypeInferrerServices(traceForConstructors, flowInformationProvider);
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
            JetDeclaration declaration = entry.getKey();
            FunctionDescriptor descriptor = entry.getValue();

            JetScope declaringScope = this.context.getDeclaringScopes().get(declaration);
            assert declaringScope != null;

            resolveFunctionBody(traceForMembers, (JetNamedFunction) declaration, descriptor, declaringScope);

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
            JetFlowInformationProvider flowInformationProvider = context.getClassDescriptorResolver().computeFlowData(function.asElement(), bodyExpression);
            JetTypeInferrer.Services typeInferrer = context.getSemanticServices().getTypeInferrerServices(trace, flowInformationProvider);

            typeInferrer.checkFunctionReturnType(declaringScope, function, functionDescriptor);
        }

        checkFunction(function, functionDescriptor);
        assert functionDescriptor.getReturnType() != null;
    }

    protected void checkFunction(JetDeclarationWithBody function, FunctionDescriptor functionDescriptor) {
        DeclarationDescriptor containingDescriptor = functionDescriptor.getContainingDeclaration();
        PsiElement nameIdentifier;
        boolean isPropertyAccessor = false;
        if (function instanceof JetNamedFunction) {
            nameIdentifier = ((JetNamedFunction) function).getNameIdentifier();
        }
        else if (function instanceof JetPropertyAccessor) {
            isPropertyAccessor = true;
            nameIdentifier = ((JetPropertyAccessor) function).getNamePlaceholder();
        }
        else {
            throw new UnsupportedOperationException();
        }
        JetFunctionOrPropertyAccessor functionOrPropertyAccessor = (JetFunctionOrPropertyAccessor) function;
        JetModifierList modifierList = functionOrPropertyAccessor.getModifierList();
        ASTNode abstractNode = modifierList != null ? modifierList.getModifierNode(JetTokens.ABSTRACT_KEYWORD) : null;
        boolean hasAbstractModifier = abstractNode != null;
        if (containingDescriptor instanceof ClassDescriptor) {
            ClassDescriptor classDescriptor = (ClassDescriptor) containingDescriptor;
            boolean inTrait = classDescriptor.getKind() == ClassKind.TRAIT;
            boolean inEnum = classDescriptor.getKind() == ClassKind.ENUM_CLASS;
            boolean inAbstractClass = classDescriptor.getModality() == Modality.ABSTRACT;
            if (hasAbstractModifier && !inAbstractClass && !inTrait && !inEnum) {
                PsiElement classElement = context.getTrace().get(BindingContext.DESCRIPTOR_TO_DECLARATION, classDescriptor);
                assert classElement instanceof JetClass;
                context.getTrace().report(ABSTRACT_FUNCTION_IN_NON_ABSTRACT_CLASS.on(functionOrPropertyAccessor, abstractNode, functionDescriptor.getName(), classDescriptor, (JetClass) classElement));
            }
            if (hasAbstractModifier && inTrait && !isPropertyAccessor) {
                context.getTrace().report(REDUNDANT_ABSTRACT.on(functionOrPropertyAccessor, abstractNode));
            }
            if (function.getBodyExpression() != null && hasAbstractModifier) {
                context.getTrace().report(ABSTRACT_FUNCTION_WITH_BODY.on(functionOrPropertyAccessor, abstractNode, functionDescriptor));
            }
            if (function.getBodyExpression() == null && !hasAbstractModifier && !inTrait && nameIdentifier != null && !isPropertyAccessor) {
                context.getTrace().report(NON_ABSTRACT_FUNCTION_WITH_NO_BODY.on(functionOrPropertyAccessor, nameIdentifier, functionDescriptor));
            }
            return;
        }
        if (hasAbstractModifier) {
            if (!isPropertyAccessor) {
                context.getTrace().report(NON_MEMBER_ABSTRACT_FUNCTION.on(functionOrPropertyAccessor, abstractNode, functionDescriptor));
            }
            else {
                context.getTrace().report(NON_MEMBER_ABSTRACT_ACCESSOR.on(functionOrPropertyAccessor, abstractNode));
            }
        }
        if (function.getBodyExpression() == null && !hasAbstractModifier && nameIdentifier != null && !isPropertyAccessor) {
            context.getTrace().report(NON_MEMBER_FUNCTION_NO_BODY.on(functionOrPropertyAccessor, nameIdentifier, functionDescriptor));
        }
    }

}
