package org.jetbrains.jet.lang.resolve;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.JetSemanticServices;
import org.jetbrains.jet.lang.cfg.JetFlowInformationProvider;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.types.*;
import org.jetbrains.jet.lexer.JetTokens;
import org.jetbrains.jet.util.slicedmap.WritableSlice;

import java.util.*;

import static org.jetbrains.jet.lang.resolve.BindingContext.ANNOTATION;
import static org.jetbrains.jet.lang.types.JetTypeInferrer.NO_EXPECTED_TYPE;

/**
 * @author abreslav
 */
public class TopDownAnalyzer {

    private final Map<JetClass, MutableClassDescriptor> classes = Maps.newLinkedHashMap();
    private final Map<JetObjectDeclaration, MutableClassDescriptor> objects = Maps.newLinkedHashMap();
    protected final Map<JetNamespace, WritableScope> namespaceScopes = Maps.newHashMap();
    protected final Map<JetNamespace, NamespaceDescriptorImpl> namespaceDescriptors = Maps.newHashMap();

    private final Map<JetNamedFunction, FunctionDescriptorImpl> functions = Maps.newLinkedHashMap();
    private final Map<JetDeclaration, ConstructorDescriptor> constructors = Maps.newLinkedHashMap();
    private final Map<JetProperty, PropertyDescriptor> properties = Maps.newLinkedHashMap();
    private final Map<JetDeclaration, JetScope> declaringScopes = Maps.newHashMap();
    private final Set<PropertyDescriptor> primaryConstructorParameterProperties = Sets.newHashSet();

    private final JetSemanticServices semanticServices;
    private final ClassDescriptorResolver classDescriptorResolver;
    private final BindingTrace trace;
    private final BindingTraceAdapter traceForConstructors;
    private final BindingTraceAdapter traceForMembers;
    private final AnnotationResolver annotationResolver;

    public TopDownAnalyzer(JetSemanticServices semanticServices, @NotNull BindingTrace bindingTrace) {
        this.semanticServices = semanticServices;
        this.classDescriptorResolver = semanticServices.getClassDescriptorResolver(bindingTrace);
        this.trace = bindingTrace;

        // This allows access to backing fields
        this.traceForConstructors = new BindingTraceAdapter(bindingTrace).addHandler(BindingContext.REFERENCE_TARGET, new BindingTraceAdapter.RecordHandler<JetReferenceExpression, DeclarationDescriptor>() {
            @Override
            public void handleRecord(WritableSlice<JetReferenceExpression, DeclarationDescriptor> slice, JetReferenceExpression expression, DeclarationDescriptor descriptor) {
                if (expression instanceof JetSimpleNameExpression) {
                    JetSimpleNameExpression simpleNameExpression = (JetSimpleNameExpression) expression;
                    if (simpleNameExpression.getReferencedNameElementType() == JetTokens.FIELD_IDENTIFIER) {
                        if (!trace.getBindingContext().get(BindingContext.BACKING_FIELD_REQUIRED, (PropertyDescriptor) descriptor)) {
                            TopDownAnalyzer.this.trace.getErrorHandler().genericError(expression.getNode(), "This property does not have a backing field");
                        }
                    }
                }
            }
        });

        // This tracks access to properties in order to register primary constructor parameters that yield real fields (JET-9)
        this.traceForMembers = new BindingTraceAdapter(bindingTrace).addHandler(BindingContext.REFERENCE_TARGET, new BindingTraceAdapter.RecordHandler<JetReferenceExpression, DeclarationDescriptor>() {
            @Override
            public void handleRecord(WritableSlice<JetReferenceExpression, DeclarationDescriptor> slice, JetReferenceExpression expression, DeclarationDescriptor descriptor) {
                if (descriptor instanceof PropertyDescriptor) {
                    PropertyDescriptor propertyDescriptor = (PropertyDescriptor) descriptor;
                    if (primaryConstructorParameterProperties.contains(propertyDescriptor)) {
                        traceForMembers.record(BindingContext.BACKING_FIELD_REQUIRED, propertyDescriptor);
                    }
                }
            }
        });

        this.annotationResolver = new AnnotationResolver(semanticServices, trace);
    }

    public void processObject(@NotNull JetScope outerScope, @NotNull DeclarationDescriptor containingDeclaration, @NotNull JetObjectDeclaration object) {
        process(outerScope, new NamespaceLike.Adapter(containingDeclaration) {

                    @Override
                    public NamespaceDescriptorImpl getNamespace(String name) {
                        throw new UnsupportedOperationException();
                    }

                    @Override
                    public void addNamespace(@NotNull NamespaceDescriptor namespaceDescriptor) {
                        throw new UnsupportedOperationException();
                    }

                    @Override
                    public void addClassifierDescriptor(@NotNull MutableClassDescriptor classDescriptor) {

                    }

                    @Override
                    public void addFunctionDescriptor(@NotNull FunctionDescriptor functionDescriptor) {
                        throw new UnsupportedOperationException();
                    }

                    @Override
                    public void addPropertyDescriptor(@NotNull PropertyDescriptor propertyDescriptor) {

                    }

                    @Override
                    public ClassObjectStatus setClassObjectDescriptor(@NotNull MutableClassDescriptor classObjectDescriptor) {
                        return ClassObjectStatus.NOT_ALLOWED;
                    }
                }, Collections.<JetDeclaration>singletonList(object));
    }

    public void process(@NotNull JetScope outerScope, NamespaceLike owner, @NotNull List<JetDeclaration> declarations) {
        collectNamespacesAndClassifiers(outerScope, owner, declarations); // namespaceScopes, classes

        createTypeConstructors(); // create type constructors for classes and generic parameters
        resolveTypesInClassHeaders(); // Generic bounds and types in supertype lists (no expressions or constructor resolution)
        checkGenericBoundsInClassHeaders(); // For the types resolved so far

        resolveConstructorHeaders();

        resolveAnnotationStubsOnClassesAndConstructors();

        resolveFunctionAndPropertyHeaders();

        resolveBehaviorDeclarationBodies();
    }

    private void collectNamespacesAndClassifiers(
            @NotNull final JetScope outerScope,
            @NotNull final NamespaceLike owner,
            @NotNull Collection<JetDeclaration> declarations) {
        for (JetDeclaration declaration : declarations) {
            declaration.accept(new JetVisitorVoid() {
                @Override
                public void visitNamespace(JetNamespace namespace) {

                    String name = namespace.getName();
                    if (name == null) {
                        name = "<no name provided>";
                    }
                    NamespaceDescriptorImpl namespaceDescriptor = owner.getNamespace(name);
                    if (namespaceDescriptor == null) {
                        namespaceDescriptor = new NamespaceDescriptorImpl(
                                owner.getOriginal(),
                                Collections.<AnnotationDescriptor>emptyList(), // TODO
                                name
                        );
                        namespaceDescriptor.initialize(new WritableScopeImpl(JetScope.EMPTY, namespaceDescriptor, trace.getErrorHandler()).setDebugName("Namespace member scope"));
                        owner.addNamespace(namespaceDescriptor);
                        trace.record(BindingContext.NAMESPACE, namespace, namespaceDescriptor);
                    }
                    namespaceDescriptors.put(namespace, namespaceDescriptor);

                    WriteThroughScope namespaceScope = new WriteThroughScope(outerScope, namespaceDescriptor.getMemberScope(), trace.getErrorHandler());
                    namespaceScopes.put(namespace, namespaceScope);

                    processImports(namespace, namespaceScope, outerScope);

                    collectNamespacesAndClassifiers(namespaceScope, namespaceDescriptor, namespace.getDeclarations());
                }

                @Override
                public void visitClass(JetClass klass) {
                    MutableClassDescriptor mutableClassDescriptor = new MutableClassDescriptor(trace, owner, outerScope);

                    if (klass.hasModifier(JetTokens.ENUM_KEYWORD)) {
                        MutableClassDescriptor classObjectDescriptor = new MutableClassDescriptor(trace, mutableClassDescriptor, outerScope, true);
                        classObjectDescriptor.setName("class-object-for-" + klass.getName());
                        classObjectDescriptor.createTypeConstructor();
                        createPrimaryConstructor(classObjectDescriptor);
                        mutableClassDescriptor.setClassObjectDescriptor(classObjectDescriptor);
                    }
                    visitClassOrObject(
                            klass,
                            (Map) classes,
                            owner,
                            outerScope,
                            mutableClassDescriptor);
                    owner.addClassifierDescriptor(mutableClassDescriptor);
                }

                @Override
                public void visitObjectDeclaration(JetObjectDeclaration declaration) {
                    createClassDescriptorForObject(declaration, owner);
                }

                @Override
                public void visitEnumEntry(JetEnumEntry enumEntry) {
                    MutableClassDescriptor classObjectDescriptor = ((MutableClassDescriptor) owner).getClassObjectDescriptor();
                    assert classObjectDescriptor != null : enumEntry.getParent().getText();
                    if (enumEntry.getPrimaryConstructorParameterList() == null) {
                        MutableClassDescriptor classDescriptor = createClassDescriptorForObject(enumEntry, classObjectDescriptor);
                        objects.remove(enumEntry);
                        classes.put(enumEntry, classDescriptor);
                    }
                    else {
                        MutableClassDescriptor mutableClassDescriptor = new MutableClassDescriptor(trace, classObjectDescriptor, outerScope);
                        visitClassOrObject(
                                enumEntry,
                                (Map) classes,
                                classObjectDescriptor,
                                outerScope,
                                mutableClassDescriptor);
                        classObjectDescriptor.addClassifierDescriptor(mutableClassDescriptor);
                    }
                }

                private MutableClassDescriptor createClassDescriptorForObject(@NotNull JetClassOrObject declaration, @NotNull NamespaceLike owner) {
                    MutableClassDescriptor mutableClassDescriptor = new MutableClassDescriptor(trace, owner, outerScope, true) {
                        @Override
                        public ClassObjectStatus setClassObjectDescriptor(@NotNull MutableClassDescriptor classObjectDescriptor) {
                            return ClassObjectStatus.NOT_ALLOWED;
                        }
                    };
                    visitClassOrObject(declaration, (Map) objects, owner, outerScope, mutableClassDescriptor);
                    createPrimaryConstructor(mutableClassDescriptor);
                    trace.record(BindingContext.CLASS, declaration, mutableClassDescriptor);
                    return mutableClassDescriptor;
                }

                private void createPrimaryConstructor(MutableClassDescriptor mutableClassDescriptor) {
                    ConstructorDescriptorImpl constructorDescriptor = new ConstructorDescriptorImpl(mutableClassDescriptor, Collections.<AnnotationDescriptor>emptyList(), true);
                    constructorDescriptor.initialize(Collections.<TypeParameterDescriptor>emptyList(), Collections.<ValueParameterDescriptor>emptyList());
                    // TODO : make the constructor private?
                    mutableClassDescriptor.setPrimaryConstructor(constructorDescriptor);
                }

                private void visitClassOrObject(@NotNull JetClassOrObject declaration, Map<JetClassOrObject, MutableClassDescriptor> map, NamespaceLike owner, JetScope outerScope, MutableClassDescriptor mutableClassDescriptor) {
                    mutableClassDescriptor.setName(JetPsiUtil.safeName(declaration.getName()));

                    map.put(declaration, mutableClassDescriptor);
                    declaringScopes.put((JetDeclaration) declaration, outerScope);

                    JetScope classScope = mutableClassDescriptor.getScopeForMemberResolution();
                    collectNamespacesAndClassifiers(classScope, mutableClassDescriptor, declaration.getDeclarations());
                }

                @Override
                public void visitTypedef(JetTypedef typedef) {
                    trace.getErrorHandler().genericError(typedef.getNode(), "Unsupported [TopDownAnalyzer]");
                }

                @Override
                public void visitClassObject(JetClassObject classObject) {
                    JetObjectDeclaration objectDeclaration = classObject.getObjectDeclaration();
                    if (objectDeclaration != null) {
                        NamespaceLike.ClassObjectStatus status = owner.setClassObjectDescriptor(createClassDescriptorForObject(objectDeclaration, owner));
                        switch (status) {
                            case DUPLICATE:
                                trace.getErrorHandler().genericError(classObject.getNode(), "Only one class object is allowed per class");
                                break;
                            case NOT_ALLOWED:
                                trace.getErrorHandler().genericError(classObject.getNode(), "A class object is not allowed here");
                                break;
                        }
                    }
                }
            });
        }
    }

    private void processImports(@NotNull JetNamespace namespace, @NotNull WriteThroughScope namespaceScope, @NotNull JetScope outerScope) {
        List<JetImportDirective> importDirectives = namespace.getImportDirectives();
        for (JetImportDirective importDirective : importDirectives) {
            if (importDirective.isAbsoluteInRootNamespace()) {
                trace.getErrorHandler().genericError(namespace.getNode(), "Unsupported by TDA"); // TODO
                continue;
            }
            if (importDirective.isAllUnder()) {
                JetExpression importedReference = importDirective.getImportedReference();
                if (importedReference != null) {
                    JetTypeInferrer.Services typeInferrerServices = semanticServices.getTypeInferrerServices(trace, JetFlowInformationProvider.THROW_EXCEPTION);
                    JetType type = typeInferrerServices.getTypeWithNamespaces(namespaceScope, importedReference);
                    if (type != null) {
                        namespaceScope.importScope(type.getMemberScope());
                    }
                }
            }
            else {
                ClassifierDescriptor classifierDescriptor = null;
                JetSimpleNameExpression referenceExpression = null;

                JetExpression importedReference = importDirective.getImportedReference();
                if (importedReference instanceof JetDotQualifiedExpression) {
                    JetDotQualifiedExpression reference = (JetDotQualifiedExpression) importedReference;
                    JetType type = semanticServices.getTypeInferrerServices(trace, JetFlowInformationProvider.THROW_EXCEPTION).getTypeWithNamespaces(namespaceScope, reference.getReceiverExpression());
                    JetExpression selectorExpression = reference.getSelectorExpression();
                    if (selectorExpression != null) {
                        referenceExpression = (JetSimpleNameExpression) selectorExpression;
                        String referencedName = referenceExpression.getReferencedName();
                        if (type != null && referencedName != null) {
                            classifierDescriptor = type.getMemberScope().getClassifier(referencedName);
                        }
                    }
                }
                else {
                    assert importedReference instanceof JetSimpleNameExpression;
                    referenceExpression = (JetSimpleNameExpression) importedReference;

                    String referencedName = referenceExpression.getReferencedName();
                    if (referencedName != null) {
                        classifierDescriptor = outerScope.getClassifier(referencedName);
                    }
                }

                if (classifierDescriptor != null) {
                    trace.record(BindingContext.REFERENCE_TARGET, referenceExpression, classifierDescriptor);

                    String aliasName = importDirective.getAliasName();
                    String importedClassifierName = aliasName != null ? aliasName : classifierDescriptor.getName();
                    namespaceScope.importClassifierAlias(importedClassifierName, classifierDescriptor);
                }
            }
        }
    }

    private void createTypeConstructors() {
        for (Map.Entry<JetClass, MutableClassDescriptor> entry : classes.entrySet()) {
            JetClass jetClass = entry.getKey();
            MutableClassDescriptor descriptor = entry.getValue();
            classDescriptorResolver.resolveMutableClassDescriptor(jetClass, descriptor);
            descriptor.createTypeConstructor();
        }
        for (Map.Entry<JetObjectDeclaration, MutableClassDescriptor> entry : objects.entrySet()) {
            MutableClassDescriptor descriptor = entry.getValue();
            descriptor.createTypeConstructor();
        }
    }

    private void resolveTypesInClassHeaders() {
        for (Map.Entry<JetClass, MutableClassDescriptor> entry : classes.entrySet()) {
            JetClass jetClass = entry.getKey();
            MutableClassDescriptor descriptor = entry.getValue();
            classDescriptorResolver.resolveGenericBounds(jetClass, descriptor.getScopeForSupertypeResolution(), descriptor.getTypeConstructor().getParameters());
            classDescriptorResolver.resolveSupertypes(jetClass, descriptor);
        }
        for (Map.Entry<JetObjectDeclaration, MutableClassDescriptor> entry : objects.entrySet()) {
            JetClassOrObject jetClass = entry.getKey();
            MutableClassDescriptor descriptor = entry.getValue();
            classDescriptorResolver.resolveSupertypes(jetClass, descriptor);
        }
    }

    private void checkGenericBoundsInClassHeaders() {
        for (Map.Entry<JetClass, MutableClassDescriptor> entry : classes.entrySet()) {
            JetClass jetClass = entry.getKey();

            for (JetDelegationSpecifier delegationSpecifier : jetClass.getDelegationSpecifiers()) {
                JetTypeReference typeReference = delegationSpecifier.getTypeReference();
                if (typeReference != null) {
                    JetType type = trace.getBindingContext().get(BindingContext.TYPE, typeReference);
                    classDescriptorResolver.checkBounds(typeReference, type);
                }
            }

            for (JetTypeParameter jetTypeParameter : jetClass.getTypeParameters()) {
                JetTypeReference extendsBound = jetTypeParameter.getExtendsBound();
                if (extendsBound != null) {
                    JetType type = trace.getBindingContext().get(BindingContext.TYPE, extendsBound);
                    if (type != null) {
                        classDescriptorResolver.checkBounds(extendsBound, type);
                    }
                }
            }

            for (JetTypeConstraint constraint : jetClass.getTypeConstaints()) {
                JetTypeReference extendsBound = constraint.getBoundTypeReference();
                if (extendsBound != null) {
                    JetType type = trace.getBindingContext().get(BindingContext.TYPE, extendsBound);
                    if (type != null) {
                        classDescriptorResolver.checkBounds(extendsBound, type);
                    }
                }
            }
        }
    }

    private void resolveConstructorHeaders() {
        for (Map.Entry<JetClass, MutableClassDescriptor> entry : classes.entrySet()) {
            JetClass jetClass = entry.getKey();
            MutableClassDescriptor classDescriptor = entry.getValue();

            processPrimaryConstructor(classDescriptor, jetClass);
            for (JetConstructor jetConstructor : jetClass.getSecondaryConstructors()) {
                processSecondaryConstructor(classDescriptor, jetConstructor);
            }
        }

    }

    private void resolveAnnotationStubsOnClassesAndConstructors() {
        for (Map.Entry<JetClass, MutableClassDescriptor> entry : classes.entrySet()) {
            JetClass jetClass = entry.getKey();
            MutableClassDescriptor mutableClassDescriptor = entry.getValue();

            JetModifierList modifierList = jetClass.getModifierList();
            if (modifierList != null) {
                List<JetAnnotationEntry> annotationEntries = modifierList.getAnnotationEntries();
                for (JetAnnotationEntry annotationEntry : annotationEntries) {
                    AnnotationDescriptor annotationDescriptor = trace.get(ANNOTATION, annotationEntry);
                    annotationResolver.resolveAnnotationStub(mutableClassDescriptor.getScopeForSupertypeResolution(), annotationEntry, annotationDescriptor);
                }
            }
        }
    }

    private void resolveFunctionAndPropertyHeaders() {
        for (Map.Entry<JetNamespace, WritableScope> entry : namespaceScopes.entrySet()) {
            JetNamespace namespace = entry.getKey();
            WritableScope namespaceScope = entry.getValue();
            NamespaceLike namespaceDescriptor = namespaceDescriptors.get(namespace);

            resolveFunctionAndPropertyHeaders(namespace.getDeclarations(), namespaceScope, namespaceDescriptor);
        }
        for (Map.Entry<JetClass, MutableClassDescriptor> entry : classes.entrySet()) {
            JetClass jetClass = entry.getKey();
            MutableClassDescriptor classDescriptor = entry.getValue();

            resolveFunctionAndPropertyHeaders(jetClass.getDeclarations(), classDescriptor.getScopeForMemberResolution(), classDescriptor);
//            processPrimaryConstructor(classDescriptor, jetClass);
//            for (JetConstructor jetConstructor : jetClass.getSecondaryConstructors()) {
//                processSecondaryConstructor(classDescriptor, jetConstructor);
//            }
        }
        for (Map.Entry<JetObjectDeclaration, MutableClassDescriptor> entry : objects.entrySet()) {
            JetObjectDeclaration object = entry.getKey();
            MutableClassDescriptor classDescriptor = entry.getValue();

            resolveFunctionAndPropertyHeaders(object.getDeclarations(), classDescriptor.getScopeForMemberResolution(), classDescriptor);
        }

        // TODO : Extensions
    }

    private void resolveFunctionAndPropertyHeaders(@NotNull List<JetDeclaration> declarations, final @NotNull JetScope scope, final @NotNull NamespaceLike namespaceLike) {
        for (JetDeclaration declaration : declarations) {
            declaration.accept(new JetVisitorVoid() {
                @Override
                public void visitNamedFunction(JetNamedFunction function) {
                    FunctionDescriptorImpl functionDescriptor = classDescriptorResolver.resolveFunctionDescriptor(namespaceLike, scope, function);
                    checkFunctionCorrectness(function, functionDescriptor, namespaceLike);
                    namespaceLike.addFunctionDescriptor(functionDescriptor);
                    functions.put(function, functionDescriptor);
                    declaringScopes.put(function, scope);
                }

                @Override
                public void visitProperty(JetProperty property) {
                    PropertyDescriptor propertyDescriptor = classDescriptorResolver.resolvePropertyDescriptor(namespaceLike, scope, property);
                    namespaceLike.addPropertyDescriptor(propertyDescriptor);
                    properties.put(property, propertyDescriptor);
                    declaringScopes.put(property, scope);
                }

                @Override
                public void visitObjectDeclaration(JetObjectDeclaration declaration) {
                    PropertyDescriptor propertyDescriptor = classDescriptorResolver.resolveObjectDeclarationAsPropertyDescriptor(namespaceLike, declaration, objects.get(declaration));
                    namespaceLike.addPropertyDescriptor(propertyDescriptor);
                }

                @Override
                public void visitEnumEntry(JetEnumEntry enumEntry) {
                    if (enumEntry.getPrimaryConstructorParameterList() == null) {
                        PropertyDescriptor propertyDescriptor = classDescriptorResolver.resolveObjectDeclarationAsPropertyDescriptor(namespaceLike, enumEntry, classes.get(enumEntry));
                        MutableClassDescriptor classObjectDescriptor = ((MutableClassDescriptor) namespaceLike).getClassObjectDescriptor();
                        assert classObjectDescriptor != null;
                        classObjectDescriptor.addPropertyDescriptor(propertyDescriptor);
                    }
                }
            });
        }
    }

    private void processPrimaryConstructor(MutableClassDescriptor classDescriptor, JetClass klass) {
        if (!klass.hasPrimaryConstructor()) return;

        // TODO : not all the parameters are real properties
        JetScope memberScope = classDescriptor.getScopeForSupertypeResolution();
        ConstructorDescriptor constructorDescriptor = classDescriptorResolver.resolvePrimaryConstructorDescriptor(memberScope, classDescriptor, klass);
        for (JetParameter parameter : klass.getPrimaryConstructorParameters()) {
            PropertyDescriptor propertyDescriptor = classDescriptorResolver.resolvePrimaryConstructorParameterToAProperty(
                    classDescriptor,
                    memberScope,
                    parameter
            );
            classDescriptor.addPropertyDescriptor(propertyDescriptor);
            primaryConstructorParameterProperties.add(propertyDescriptor);
        }
        if (constructorDescriptor != null) {
            classDescriptor.setPrimaryConstructor(constructorDescriptor);
        }
    }

    private void processSecondaryConstructor(MutableClassDescriptor classDescriptor, JetConstructor constructor) {
        ConstructorDescriptor constructorDescriptor = classDescriptorResolver.resolveSecondaryConstructorDescriptor(
                classDescriptor.getScopeForMemberResolution(),
                classDescriptor,
                constructor);
        classDescriptor.addConstructor(constructorDescriptor);
        constructors.put(constructor, constructorDescriptor);
        declaringScopes.put(constructor, classDescriptor.getScopeForMemberLookup());
    }

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private void resolveBehaviorDeclarationBodies() {
        bindOverrides();

        resolveDelegationSpecifierLists();
        resolveClassAnnotations();

        resolveAnonymousInitializers();
        resolvePropertyDeclarationBodies();

        resolveSecondaryConstructorBodies();
        resolveFunctionBodies();

        checkIfPrimaryConstructorIsNecessary();
    }

    private void bindOverrides() {
        for (Map.Entry<JetClass, MutableClassDescriptor> entry : classes.entrySet()) {
            bindOverridesInAClass(entry.getValue());
        }
        for (Map.Entry<JetObjectDeclaration, MutableClassDescriptor> entry : objects.entrySet()) {
            bindOverridesInAClass(entry.getValue());
        }
    }

    private void bindOverridesInAClass(MutableClassDescriptor classDescriptor) {

        for (FunctionDescriptor declaredFunction : classDescriptor.getFunctions()) {
            for (JetType supertype : classDescriptor.getTypeConstructor().getSupertypes()) {
                FunctionDescriptor overridden = findFunctionOverridableBy(declaredFunction, supertype);
                if (overridden != null) {
                    ((FunctionDescriptorImpl) declaredFunction).addOverriddenFunction(overridden);
                }
            }
        }
    }

    @Nullable
    private FunctionDescriptor findFunctionOverridableBy(@NotNull FunctionDescriptor declaredFunction, @NotNull JetType supertype) {
        FunctionGroup functionGroup = supertype.getMemberScope().getFunctionGroup(declaredFunction.getName());
        for (FunctionDescriptor functionDescriptor : functionGroup.getFunctionDescriptors()) {
            if (FunctionDescriptorUtil.isOverridableBy(semanticServices.getTypeChecker(), functionDescriptor, declaredFunction).isSuccess()) {
                return functionDescriptor;
            }
        }
        return null;
    }

    private void checkIfPrimaryConstructorIsNecessary() {
        for (Map.Entry<JetClass, MutableClassDescriptor> entry : classes.entrySet()) {
            MutableClassDescriptor classDescriptor = entry.getValue();
            JetClass jetClass = entry.getKey();
            if (classDescriptor.getUnsubstitutedPrimaryConstructor() == null) {
                for (PropertyDescriptor propertyDescriptor : classDescriptor.getProperties()) {
                    if (trace.getBindingContext().get(BindingContext.BACKING_FIELD_REQUIRED, propertyDescriptor)) {
                        PsiElement nameIdentifier = jetClass.getNameIdentifier();
                        if (nameIdentifier != null) {
                            trace.getErrorHandler().genericError(nameIdentifier.getNode(),
                                    "This class must have a primary constructor, because property " + propertyDescriptor.getName() + " has a backing field");
                        }
                        break;
                    }
                }
            }
        }
    }

    private void resolveDelegationSpecifierLists() {
        // TODO : Make sure the same thing is not initialized twice
        for (Map.Entry<JetClass, MutableClassDescriptor> entry : classes.entrySet()) {
            resolveDelegationSpecifierList(entry.getKey(), entry.getValue());
        }
        for (Map.Entry<JetObjectDeclaration, MutableClassDescriptor> entry : objects.entrySet()) {
            resolveDelegationSpecifierList(entry.getKey(), entry.getValue());
        }
    }

    private void resolveDelegationSpecifierList(final JetClassOrObject jetClass, final MutableClassDescriptor descriptor) {
        final ConstructorDescriptor primaryConstructor = descriptor.getUnsubstitutedPrimaryConstructor();
        final JetScope scopeForConstructor = primaryConstructor == null
                ? null
                : getInnerScopeForConstructor(primaryConstructor, descriptor.getScopeForMemberResolution(), true);
        final JetTypeInferrer.Services typeInferrer = semanticServices.getTypeInferrerServices(traceForConstructors, JetFlowInformationProvider.NONE); // TODO : flow

        for (JetDelegationSpecifier delegationSpecifier : jetClass.getDelegationSpecifiers()) {
            delegationSpecifier.accept(new JetVisitorVoid() {
                @Override
                public void visitDelegationByExpressionSpecifier(JetDelegatorByExpressionSpecifier specifier) {
                    JetExpression delegateExpression = specifier.getDelegateExpression();
                    if (delegateExpression != null) {
                        JetScope scope = scopeForConstructor == null ? descriptor.getScopeForMemberResolution() : scopeForConstructor;
                        JetType type = typeInferrer.getType(scope, delegateExpression, NO_EXPECTED_TYPE);
                        JetType supertype = trace.getBindingContext().get(BindingContext.TYPE, specifier.getTypeReference());
                        if (type != null && !semanticServices.getTypeChecker().isSubtypeOf(type, supertype)) { // TODO : Convertible?
                            trace.getErrorHandler().typeMismatch(delegateExpression, supertype, type);
                        }
                    }
                }

                @Override
                public void visitDelegationToSuperCallSpecifier(JetDelegatorToSuperCall call) {
                    JetTypeReference typeReference = call.getTypeReference();
                    if (typeReference != null) {
                        if (descriptor.getUnsubstitutedPrimaryConstructor() != null) {
                            typeInferrer.getCallResolver().resolveCall(trace, scopeForConstructor, null, call, NO_EXPECTED_TYPE);
                        }
                        else {
                            JetValueArgumentList valueArgumentList = call.getValueArgumentList();
                            assert valueArgumentList != null;
                            trace.getErrorHandler().genericError(valueArgumentList.getNode(),
                                    "Class " + JetPsiUtil.safeName(jetClass.getName()) + " must have a constructor in order to be able to initialize supertypes");
                        }
                    }
                }

                @Override
                public void visitDelegationToSuperClassSpecifier(JetDelegatorToSuperClass specifier) {
                    JetType supertype = trace.getBindingContext().get(BindingContext.TYPE, specifier.getTypeReference());
                    if (supertype != null) {
                        DeclarationDescriptor declarationDescriptor = supertype.getConstructor().getDeclarationDescriptor();
                        if (declarationDescriptor instanceof ClassDescriptor) {
                            ClassDescriptor classDescriptor = (ClassDescriptor) declarationDescriptor;
                            if (classDescriptor.hasConstructors() && !ErrorUtils.isError(classDescriptor.getTypeConstructor())) {
                                trace.getErrorHandler().genericError(specifier.getNode(), "This type has a constructor, and thus must be initialized here");
                            }
                        }
                        else {
                            trace.getErrorHandler().genericError(specifier.getNode(), "Only classes may serve as supertypes");
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
            });
        }
    }

    private void resolveClassAnnotations() {

    }

    private void resolveAnonymousInitializers() {
        for (Map.Entry<JetClass, MutableClassDescriptor> entry : classes.entrySet()) {
            resolveAnonymousInitializers(entry.getKey(), entry.getValue());
        }
        for (Map.Entry<JetObjectDeclaration, MutableClassDescriptor> entry : objects.entrySet()) {
            resolveAnonymousInitializers(entry.getKey(), entry.getValue());
        }
    }

    private void resolveAnonymousInitializers(JetClassOrObject jetClassOrObject, MutableClassDescriptor classDescriptor) {
        List<JetClassInitializer> anonymousInitializers = jetClassOrObject.getAnonymousInitializers();
        if (jetClassOrObject.hasPrimaryConstructor()) {
            ConstructorDescriptor primaryConstructor = classDescriptor.getUnsubstitutedPrimaryConstructor();
            assert primaryConstructor != null;
            final JetScope scopeForConstructor = getInnerScopeForConstructor(primaryConstructor, classDescriptor.getScopeForMemberResolution(), true);
            JetTypeInferrer.Services typeInferrer = semanticServices.getTypeInferrerServices(createFieldAssignTrackingTrace(), JetFlowInformationProvider.NONE); // TODO : flow
            for (JetClassInitializer anonymousInitializer : anonymousInitializers) {
                typeInferrer.getType(scopeForConstructor, anonymousInitializer.getBody(), NO_EXPECTED_TYPE);
            }
        }
        else {
            for (JetClassInitializer anonymousInitializer : anonymousInitializers) {
                trace.getErrorHandler().genericError(anonymousInitializer.getNode(), "Anonymous initializers are only allowed in the presence of a primary constructor");
            }
        }
    }

    private void resolveSecondaryConstructorBodies() {
        for (Map.Entry<JetDeclaration, ConstructorDescriptor> entry : constructors.entrySet()) {
            JetDeclaration declaration = entry.getKey();
            ConstructorDescriptor descriptor = entry.getValue();

            resolveSecondaryConstructorBody((JetConstructor) declaration, descriptor, ((MutableClassDescriptor) descriptor.getContainingDeclaration()).getScopeForMemberResolution());

            assert descriptor.getReturnType() != null;
        }
    }

    private void resolveSecondaryConstructorBody(JetConstructor declaration, final ConstructorDescriptor descriptor, final JetScope declaringScope) {
        final JetScope functionInnerScope = getInnerScopeForConstructor(descriptor, declaringScope, false);

        final JetTypeInferrer.Services typeInferrerForInitializers = semanticServices.getTypeInferrerServices(traceForConstructors, JetFlowInformationProvider.NONE);

        JetClass containingClass = PsiTreeUtil.getParentOfType(declaration, JetClass.class);
        assert containingClass != null : "This must be guaranteed by the parser";
        if (!containingClass.hasPrimaryConstructor()) {
            trace.getErrorHandler().genericError(declaration.getNameNode(), "A secondary constructor may appear only in a class that has a primary constructor");
        }
        else {
            List<JetDelegationSpecifier> initializers = declaration.getInitializers();
            if (initializers.isEmpty()) {
                trace.getErrorHandler().genericError(declaration.getNameNode(), "Secondary constructors must have an initializer list");
            }
            else {
                initializers.get(0).accept(new JetVisitorVoid() {
                    @Override
                    public void visitDelegationToSuperCallSpecifier(JetDelegatorToSuperCall call) {
                        JetTypeReference typeReference = call.getTypeReference();
                        if (typeReference != null) {
                            typeInferrerForInitializers.getCallResolver().resolveCall(trace, functionInnerScope, null, call, NO_EXPECTED_TYPE);
                        }
                    }

                    @Override
                    public void visitDelegationToThisCall(JetDelegatorToThisCall call) {
                        // TODO : check that there's no recursion in this() calls
                        // TODO : check: if a this() call is present, no other initializers are allowed
                        ClassDescriptor classDescriptor = descriptor.getContainingDeclaration();

                        typeInferrerForInitializers.getCallResolver().resolveCall(trace,
                                functionInnerScope,
                                null, call, NO_EXPECTED_TYPE);
//                                call.getThisReference(),
//                                classDescriptor,
//                                classDescriptor.getDefaultType(),
//                                call);
//                        trace.getErrorHandler().genericError(call.getNode(), "this-calls are not supported");
                    }

                    @Override
                    public void visitDelegationByExpressionSpecifier(JetDelegatorByExpressionSpecifier specifier) {
                        trace.getErrorHandler().genericError(specifier.getNode(), "'by'-clause is only supported for primary constructors");
                    }

                    @Override
                    public void visitDelegationToSuperClassSpecifier(JetDelegatorToSuperClass specifier) {
                        trace.getErrorHandler().genericError(specifier.getNode(), "Constructor parameters required");
                    }

                    @Override
                    public void visitDelegationSpecifier(JetDelegationSpecifier specifier) {
                        throw new IllegalStateException();
                    }
                });
                for (int i = 1, initializersSize = initializers.size(); i < initializersSize; i++) {
                    JetDelegationSpecifier initializer = initializers.get(i);
                    trace.getErrorHandler().genericError(initializer.getNode(), "Only one call to 'this(...)' is allowed");
                }
            }
        }
        JetExpression bodyExpression = declaration.getBodyExpression();
        if (bodyExpression != null) {
            classDescriptorResolver.computeFlowData(declaration, bodyExpression);
            JetFlowInformationProvider flowInformationProvider = classDescriptorResolver.computeFlowData(declaration, bodyExpression);
            JetTypeInferrer.Services typeInferrer = semanticServices.getTypeInferrerServices(traceForConstructors, flowInformationProvider);

            typeInferrer.checkFunctionReturnType(functionInnerScope, declaration, JetStandardClasses.getUnitType());
        }
    }

    @NotNull
    private JetScope getInnerScopeForConstructor(@NotNull ConstructorDescriptor descriptor, @NotNull JetScope declaringScope, boolean primary) {
        WritableScope constructorScope = new WritableScopeImpl(declaringScope, declaringScope.getContainingDeclaration(), trace.getErrorHandler()).setDebugName("Inner scope for constructor");
        for (PropertyDescriptor propertyDescriptor : ((MutableClassDescriptor) descriptor.getContainingDeclaration()).getProperties()) {
            constructorScope.addPropertyDescriptorByFieldName("$" + propertyDescriptor.getName(), propertyDescriptor);
        }

        constructorScope.setThisType(descriptor.getContainingDeclaration().getDefaultType());

        for (ValueParameterDescriptor valueParameterDescriptor : descriptor.getValueParameters()) {
            JetParameter parameter = (JetParameter) trace.getBindingContext().get(BindingContext.DESCRIPTOR_TO_DECLARATION, valueParameterDescriptor);
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
        for (Map.Entry<JetClass, MutableClassDescriptor> entry : classes.entrySet()) {
            JetClass jetClass = entry.getKey();
            MutableClassDescriptor classDescriptor = entry.getValue();

            for (JetProperty property : jetClass.getProperties()) {
                final PropertyDescriptor propertyDescriptor = properties.get(property);
                assert propertyDescriptor != null;

                JetScope declaringScope = declaringScopes.get(property);

                JetExpression initializer = property.getInitializer();
                if (initializer != null) {
                    ConstructorDescriptor primaryConstructor = classDescriptor.getUnsubstitutedPrimaryConstructor();
                    if (primaryConstructor == null) {
                        trace.getErrorHandler().genericError(initializer.getNode(), "Property initializers are not allowed when no primary constructor is present");
                    }
                    else {
                        JetScope scope = getInnerScopeForConstructor(primaryConstructor, classDescriptor.getScopeForMemberResolution(), true);
                        resolvePropertyInitializer(property, propertyDescriptor, initializer, scope);
                    }
                }

                resolvePropertyAccessors(property, propertyDescriptor, declaringScope);
                checkPropertyCorrectness(property, propertyDescriptor, classDescriptor);
                processed.add(property);
            }
        }

        // Top-level properties & properties of objects
        for (Map.Entry<JetProperty, PropertyDescriptor> entry : properties.entrySet()) {
            JetProperty property = entry.getKey();
            if (processed.contains(property)) continue;

            final PropertyDescriptor propertyDescriptor = entry.getValue();
            JetScope declaringScope = declaringScopes.get(property);

            JetExpression initializer = property.getInitializer();
            if (initializer != null) {
                resolvePropertyInitializer(property, propertyDescriptor, initializer, declaringScope);
            }

            resolvePropertyAccessors(property, propertyDescriptor, declaringScope);
            checkPropertyCorrectness(property, propertyDescriptor, null);
        }
    }

    private JetScope getPropertyDeclarationInnerScope(@NotNull JetScope outerScope, @NotNull PropertyDescriptor propertyDescriptor) {
        WritableScopeImpl result = new WritableScopeImpl(outerScope, propertyDescriptor, trace.getErrorHandler()).setDebugName("Property declaration inner scope");
        for (TypeParameterDescriptor typeParameterDescriptor : propertyDescriptor.getTypeParameters()) {
            result.addTypeParameterDescriptor(typeParameterDescriptor);
        }
        JetType receiverType = propertyDescriptor.getReceiverType();
        if (receiverType != null) {
            result.setThisType(receiverType);
        }
        return result;
    }

    private void resolvePropertyAccessors(JetProperty property, PropertyDescriptor propertyDescriptor, JetScope declaringScope) {
        BindingTraceAdapter fieldAccessTrackingTrace = createFieldTrackingTrace(propertyDescriptor);

        WritableScope accessorScope = new WritableScopeImpl(getPropertyDeclarationInnerScope(declaringScope, propertyDescriptor), declaringScope.getContainingDeclaration(), trace.getErrorHandler()).setDebugName("Accessor scope");
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

//        JetExpression initializer = property.getInitializer();
//        if (!property.isVar() && initializer != null && !trace.getBindingContext().get(BindingContext.BACKING_FIELD_REQUIRED, propertyDescriptor)) {
//            trace.getErrorHandler().genericError(initializer.getNode(), "Initializer is not allowed here because this property has no setter and no backing field either");
//        }
    }

    protected void checkPropertyCorrectness(JetProperty property, PropertyDescriptor propertyDescriptor, @Nullable ClassDescriptor classDescriptor) {
        JetExpression initializer = property.getInitializer();
        JetPropertyAccessor getter = property.getGetter();
        JetPropertyAccessor setter = property.getSetter();
        if (propertyDescriptor.getModifiers().isAbstract()) {
            if (classDescriptor == null) {
                trace.getErrorHandler().genericError(property.getModifierList().getModifierNode(JetTokens.ABSTRACT_KEYWORD),
                                                     "Global property can not be abstract");
                return;
            }
            if (! classDescriptor.isAbstract()) {
                trace.getErrorHandler().genericError(property.getModifierList().getModifierNode(JetTokens.ABSTRACT_KEYWORD),
                                                     "Abstract property " + property.getName() + " in non-abstract class " + classDescriptor.getName());
                return;
            }
            if (initializer != null) {
                trace.getErrorHandler().genericError(initializer.getNode(), "Property with initializer can not be abstract");
            }
            if (getter != null && getter.getBodyExpression() != null) {
                trace.getErrorHandler().genericError(getter.getNode(), "Property with getter implementation can not be abstract");
            }
            if (setter != null && setter.getBodyExpression() != null) {
                trace.getErrorHandler().genericError(setter.getNode(), "Property with setter implementation can not be abstract");
            }
            return;
        }
        boolean backingFieldRequired = trace.getBindingContext().get(BindingContext.BACKING_FIELD_REQUIRED, propertyDescriptor);
        if (initializer != null && ! backingFieldRequired) {
            trace.getErrorHandler().genericError(initializer.getNode(), "Initializer is not allowed here because this property has no backing field");
        }
        if (initializer == null && backingFieldRequired && ! trace.getBindingContext().get(BindingContext.IS_INITIALIZED, propertyDescriptor)) {
            if (classDescriptor == null || (getter != null && getter.getBodyExpression() != null) || (setter != null && setter.getBodyExpression() != null)) {
                trace.getErrorHandler().genericError(property.getNameIdentifier().getNode(), "Property must be initialized");
            } else {
                trace.getErrorHandler().genericError(property.getNameIdentifier().getNode(), "Property must be initialized or be abstract");
            }
        }
    }

    protected void checkFunctionCorrectness(JetNamedFunction function, FunctionDescriptor functionDescriptor, DeclarationDescriptor containingDescriptor) {
        if (containingDescriptor instanceof ClassDescriptor) {
            ClassDescriptor classDescriptor = (ClassDescriptor) containingDescriptor;
            if (functionDescriptor.getModifiers().isAbstract() && !classDescriptor.isAbstract()) {
                trace.getErrorHandler().genericError(function.getModifierList().getModifierNode(JetTokens.ABSTRACT_KEYWORD),
                                                     "Abstract method " + function.getName() + " in non-abstract class " + classDescriptor.getName());
            }
            if (function.getBodyExpression() == null && !functionDescriptor.getModifiers().isAbstract()) {
                trace.getErrorHandler().genericError(function.getNameIdentifier().getNode(), "Method without body must be abstract");
            }
            return;
        }
        if (functionDescriptor.getModifiers().isAbstract()) {
            trace.getErrorHandler().genericError(function.getModifierList().getModifierNode(JetTokens.ABSTRACT_KEYWORD),
                                                 "Global function " + function.getName() + " can not be abstract");
        }
        if (function.getBodyExpression() == null && !functionDescriptor.getModifiers().isAbstract()) {
            trace.getErrorHandler().genericError(function.getNameIdentifier().getNode(), "Global function must have body");
        }
    }

    private BindingTraceAdapter createFieldTrackingTrace(final PropertyDescriptor propertyDescriptor) {
        return new BindingTraceAdapter(traceForMembers).addHandler(BindingContext.REFERENCE_TARGET, new BindingTraceAdapter.RecordHandler<JetReferenceExpression, DeclarationDescriptor>() {
            @Override
            public void handleRecord(WritableSlice<JetReferenceExpression, DeclarationDescriptor> slice, JetReferenceExpression expression, DeclarationDescriptor descriptor) {
                if (expression instanceof JetSimpleNameExpression) {
                    JetSimpleNameExpression simpleNameExpression = (JetSimpleNameExpression) expression;
                    if (simpleNameExpression.getReferencedNameElementType() == JetTokens.FIELD_IDENTIFIER) {
                        // This check may be considered redundant as long as $x is only accessible from accessors to $x
                        if (descriptor == propertyDescriptor) { // TODO : original?
                            traceForMembers.record(BindingContext.BACKING_FIELD_REQUIRED, propertyDescriptor); // TODO: this trace?
                        }
                    }
                }
            }
        });
    }

    private BindingTraceAdapter createFieldAssignTrackingTrace() {
        return new BindingTraceAdapter(traceForConstructors).addHandler(BindingContext.VARIABLE_ASSIGNMENT, new BindingTraceAdapter.RecordHandler<JetExpression, DeclarationDescriptor>() {
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
        JetFlowInformationProvider flowInformationProvider = classDescriptorResolver.computeFlowData(property, initializer); // TODO : flow JET-15
        JetTypeInferrer.Services typeInferrer = semanticServices.getTypeInferrerServices(traceForConstructors, flowInformationProvider);
        JetType type = typeInferrer.getType(getPropertyDeclarationInnerScope(scope, propertyDescriptor), initializer, NO_EXPECTED_TYPE);

        JetType expectedType = propertyDescriptor.getInType();
        if (expectedType == null) {
            expectedType = propertyDescriptor.getOutType();
        }
        if (type != null && expectedType != null
            && !semanticServices.getTypeChecker().isSubtypeOf(type, expectedType)) {
            trace.getErrorHandler().typeMismatch(initializer, expectedType, type);
        }
    }

    private void resolveFunctionBodies() {
        for (Map.Entry<JetNamedFunction, FunctionDescriptorImpl> entry : functions.entrySet()) {
            JetDeclaration declaration = entry.getKey();
            FunctionDescriptor descriptor = entry.getValue();

            JetScope declaringScope = declaringScopes.get(declaration);
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
            JetFlowInformationProvider flowInformationProvider = classDescriptorResolver.computeFlowData(function.asElement(), bodyExpression);
            JetTypeInferrer.Services typeInferrer = semanticServices.getTypeInferrerServices(trace, flowInformationProvider);

            typeInferrer.checkFunctionReturnType(declaringScope, function, functionDescriptor);
        }

        assert functionDescriptor.getReturnType() != null;
    }



}
