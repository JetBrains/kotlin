package org.jetbrains.jet.lang.resolve;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.JetSemanticServices;
import org.jetbrains.jet.lang.cfg.JetFlowInformationProvider;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.types.ErrorUtils;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.JetTypeInferrer;
import org.jetbrains.jet.lexer.JetTokens;

import java.util.*;

/**
 * @author abreslav
 */
public class TopDownAnalyzer {

    private final Map<JetClass, MutableClassDescriptor> classes = Maps.newLinkedHashMap();
    private final Map<JetObjectDeclaration, MutableClassDescriptor> objects = Maps.newLinkedHashMap();
    private final Map<JetNamespace, WritableScope> namespaceScopes = Maps.newHashMap();
    private final Map<JetNamespace, NamespaceDescriptorImpl> namespaceDescriptors = Maps.newHashMap();

    private final Map<JetFunction, FunctionDescriptorImpl> functions = Maps.newLinkedHashMap();
    private final Map<JetDeclaration, ConstructorDescriptor> constructors = Maps.newLinkedHashMap();
    private final Map<JetProperty, PropertyDescriptor> properties = new LinkedHashMap<JetProperty, PropertyDescriptor>();
    private final Map<JetDeclaration, JetScope> declaringScopes = Maps.newHashMap();
    private final Multimap<DeclarationDescriptor, PropertyDescriptor> declaringScopesToProperties = ArrayListMultimap.create();
    private final Set<PropertyDescriptor> primaryConstructorParameterProperties = Sets.newHashSet();

    private final JetSemanticServices semanticServices;
    private final ClassDescriptorResolver classDescriptorResolver;
    private final BindingTrace trace;
    private final BindingTraceAdapter traceForConstructors;
    private final BindingTraceAdapter traceForMembers;

    public TopDownAnalyzer(JetSemanticServices semanticServices, @NotNull BindingTrace bindingTrace) {
        this.semanticServices = semanticServices;
        this.classDescriptorResolver = semanticServices.getClassDescriptorResolver(bindingTrace);
        this.trace = bindingTrace;

        // This allows access to backing fields
        this.traceForConstructors = new BindingTraceAdapter(bindingTrace) {
            @Override
            public void recordReferenceResolution(@NotNull JetReferenceExpression expression, @NotNull DeclarationDescriptor descriptor) {
                super.recordReferenceResolution(expression, descriptor);
                if (expression instanceof JetSimpleNameExpression) {
                    JetSimpleNameExpression simpleNameExpression = (JetSimpleNameExpression) expression;
                    if (simpleNameExpression.getReferencedNameElementType() == JetTokens.FIELD_IDENTIFIER) {
                        if (!trace.getBindingContext().hasBackingField((PropertyDescriptor) descriptor)) {
                            TopDownAnalyzer.this.trace.getErrorHandler().genericError(expression.getNode(), "This property does not have a backing field");
                        }
                    }
                }
            }
        };

        // This tracks access to properties in order to register primary constructor parameters that yield real fields (JET-9)
        this.traceForMembers = new BindingTraceAdapter(bindingTrace) {
            @Override
            public void recordReferenceResolution(@NotNull JetReferenceExpression expression, @NotNull DeclarationDescriptor descriptor) {
                super.recordReferenceResolution(expression, descriptor);
                if (descriptor instanceof PropertyDescriptor) {
                    PropertyDescriptor propertyDescriptor = (PropertyDescriptor) descriptor;
                    if (primaryConstructorParameterProperties.contains(propertyDescriptor)) {
                        requireBackingField(propertyDescriptor);
                    }
                }
            }
        };
    }

    @Deprecated // For JetStandardLibraryOnly
    public void processStandardLibraryNamespace(@NotNull WritableScope outerScope, @NotNull NamespaceDescriptorImpl standardLibraryNamespace, @NotNull JetNamespace namespace) {
        namespaceScopes.put(namespace, standardLibraryNamespace.getMemberScope());
        namespaceDescriptors.put(namespace, standardLibraryNamespace);
        process(outerScope, standardLibraryNamespace, namespace.getDeclarations());
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

        resolveFunctionAndPropertyHeaders(); // Constructor headers are resolved as well

        resolveBehaviorDeclarationBodies();
    }

    private void collectNamespacesAndClassifiers(
            @NotNull final JetScope outerScope,
            @NotNull final NamespaceLike owner,
            @NotNull Collection<JetDeclaration> declarations) {
        for (JetDeclaration declaration : declarations) {
            declaration.accept(new JetVisitor() {
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
                                Collections.<Annotation>emptyList(), // TODO
                                name
                        );
                        namespaceDescriptor.initialize(new WritableScopeImpl(JetScope.EMPTY, namespaceDescriptor, trace.getErrorHandler()));
                        owner.addNamespace(namespaceDescriptor);
                        trace.recordDeclarationResolution(namespace, namespaceDescriptor);
                    }
                    namespaceDescriptors.put(namespace, namespaceDescriptor);

                    WriteThroughScope namespaceScope = new WriteThroughScope(outerScope, namespaceDescriptor.getMemberScope(), trace.getErrorHandler());
                    namespaceScopes.put(namespace, namespaceScope);

                    processImports(namespace, namespaceScope, outerScope);

                    collectNamespacesAndClassifiers(namespaceScope, namespaceDescriptor, namespace.getDeclarations());
                }

                @Override
                public void visitClass(JetClass klass) {
                    visitClassOrObject(
                            klass,
                            (Map) classes,
                            owner,
                            outerScope,
                            new MutableClassDescriptor(trace, owner, outerScope));
                }

                @Override
                public void visitObjectDeclaration(JetObjectDeclaration declaration) {
                    createClassDescriptorForObject(declaration);
                }

                private MutableClassDescriptor createClassDescriptorForObject(@NotNull JetObjectDeclaration declaration) {
                    MutableClassDescriptor mutableClassDescriptor = new MutableClassDescriptor(trace, owner, outerScope) {
                        @Override
                        public ClassObjectStatus setClassObjectDescriptor(@NotNull MutableClassDescriptor classObjectDescriptor) {
                            return ClassObjectStatus.NOT_ALLOWED;
                        }
                    };
                    visitClassOrObject(declaration, (Map) objects, owner, outerScope, mutableClassDescriptor);
                    ConstructorDescriptorImpl constructorDescriptor = new ConstructorDescriptorImpl(mutableClassDescriptor, Collections.<Annotation>emptyList(), true);
                    constructorDescriptor.initialize(Collections.<ValueParameterDescriptor>emptyList());
                    // TODO : make the constructor private?
                    mutableClassDescriptor.setPrimaryConstructor(constructorDescriptor);
                    trace.recordDeclarationResolution(declaration, mutableClassDescriptor);
                    return mutableClassDescriptor;
                }

                private void visitClassOrObject(@NotNull JetClassOrObject declaration, Map<JetClassOrObject, MutableClassDescriptor> map, NamespaceLike owner, JetScope outerScope, MutableClassDescriptor mutableClassDescriptor) {
                    mutableClassDescriptor.setName(JetPsiUtil.safeName(declaration.getName()));

                    if (declaration instanceof JetClass) {
                        owner.addClassifierDescriptor(mutableClassDescriptor);
                    }

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
                public void visitExtension(JetExtension extension) {
                    trace.getErrorHandler().genericError(extension.getNode(), "Unsupported [TopDownAnalyzer]");
                }

                @Override
                public void visitClassObject(JetClassObject classObject) {
                    JetObjectDeclaration objectDeclaration = classObject.getObjectDeclaration();
                    if (objectDeclaration != null) {
                        NamespaceLike.ClassObjectStatus status = owner.setClassObjectDescriptor(createClassDescriptorForObject(objectDeclaration));
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
                throw new UnsupportedOperationException();
            }
            if (importDirective.isAllUnder()) {
                JetExpression importedReference = importDirective.getImportedReference();
                if (importedReference != null) {
                    JetType type = semanticServices.getTypeInferrer(trace, JetFlowInformationProvider.THROW_EXCEPTION).getTypeWithNamespaces(namespaceScope, importedReference, false);
                    if (type != null) {
                        namespaceScope.importScope(type.getMemberScope());
                    }
                }
            } else {
                ClassifierDescriptor classifierDescriptor = null;
                JetSimpleNameExpression referenceExpression = null;

                JetExpression importedReference = importDirective.getImportedReference();
                if (importedReference instanceof JetDotQualifiedExpression) {
                    JetDotQualifiedExpression reference = (JetDotQualifiedExpression) importedReference;
                    JetType type = semanticServices.getTypeInferrer(trace, JetFlowInformationProvider.THROW_EXCEPTION).getTypeWithNamespaces(namespaceScope, reference.getReceiverExpression(), false);
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
                    trace.recordReferenceResolution(referenceExpression, classifierDescriptor);

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
        }
    }

    private void resolveTypesInClassHeaders() {
        for (Map.Entry<JetClass, MutableClassDescriptor> entry : classes.entrySet()) {
            JetClass jetClass = entry.getKey();
            MutableClassDescriptor descriptor = entry.getValue();
            classDescriptorResolver.resolveGenericBounds(jetClass, descriptor);
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
                JetType type = trace.getBindingContext().resolveTypeReference(delegationSpecifier.getTypeReference());
                classDescriptorResolver.checkBounds(delegationSpecifier.getTypeReference(), type);
            }

            for (JetTypeParameter jetTypeParameter : jetClass.getTypeParameters()) {
                JetTypeReference extendsBound = jetTypeParameter.getExtendsBound();
                if (extendsBound != null) {
                    JetType type = trace.getBindingContext().resolveTypeReference(extendsBound);
                    classDescriptorResolver.checkBounds(extendsBound, type);
                }
            }

            // TODO : where-bounds
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
            processPrimaryConstructor(classDescriptor, jetClass);
            for (JetConstructor jetConstructor : jetClass.getSecondaryConstructors()) {
                processSecondaryConstructor(classDescriptor, jetConstructor);
            }

            // TODO : Constructors
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
            declaration.accept(new JetVisitor() {
                @Override
                public void visitFunction(JetFunction function) {
                    FunctionDescriptorImpl functionDescriptor = classDescriptorResolver.resolveFunctionDescriptor(namespaceLike, scope, function);
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
            });
        }
    }

    private void processPrimaryConstructor(MutableClassDescriptor classDescriptor, JetClass klass) {
        if (!klass.hasPrimaryConstructor()) return;

        // TODO : not all the parameters are real properties
        JetScope memberScope = classDescriptor.getScopeForMemberResolution(); // TODO : this is REALLY questionable
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
        resolveDelegationSpecifierLists();

        resolveAnonymousInitializers();
        resolvePropertyDeclarationBodies();

        resolveSecondaryConstructorBodies();
        resolveFunctionBodies();

        checkIfPrimaryConstructorIsNecessary();

        bindOverrides();

        checkGenericBoundsInClassHeaders();
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
            if (!jetClass.hasPrimaryConstructor()) {
                for (PropertyDescriptor propertyDescriptor : classDescriptor.getProperties()) {
                    if (trace.getBindingContext().hasBackingField(propertyDescriptor)) {
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
        final JetTypeInferrer typeInferrer = semanticServices.getTypeInferrer(traceForConstructors, JetFlowInformationProvider.NONE); // TODO : flow

        for (JetDelegationSpecifier delegationSpecifier : jetClass.getDelegationSpecifiers()) {
            delegationSpecifier.accept(new JetVisitor() {
                @Override
                public void visitDelegationByExpressionSpecifier(JetDelegatorByExpressionSpecifier specifier) {
                    JetExpression delegateExpression = specifier.getDelegateExpression();
                    if (delegateExpression != null) {
                        JetScope scope = scopeForConstructor == null ? descriptor.getScopeForMemberResolution() : scopeForConstructor;
                        JetType type = typeInferrer.getType(scope, delegateExpression, false);
                        JetType supertype = trace.getBindingContext().resolveTypeReference(specifier.getTypeReference());
                        if (type != null && !semanticServices.getTypeChecker().isSubtypeOf(type, supertype)) { // TODO : Convertible?
                            trace.getErrorHandler().typeMismatch(delegateExpression, supertype, type);
                        }
                    }
                }

                @Override
                public void visitDelegationToSuperCallSpecifier(JetDelegatorToSuperCall call) {
                    JetTypeReference typeReference = call.getTypeReference();
                    if (typeReference != null) {
                        if (jetClass.hasPrimaryConstructor()) {
                            typeInferrer.checkConstructorCall(scopeForConstructor, typeReference, call);
                        }
                        else {
                            JetArgumentList valueArgumentList = call.getValueArgumentList();
                            assert valueArgumentList != null;
                            trace.getErrorHandler().genericError(valueArgumentList.getNode(),
                                    "Class " + JetPsiUtil.safeName(jetClass.getName()) + " must have a constructor in order to be able to initialize supertypes");
                        }
                    }
                }

                @Override
                public void visitDelegationToSuperClassSpecifier(JetDelegatorToSuperClass specifier) {
                    JetType supertype = trace.getBindingContext().resolveTypeReference(specifier.getTypeReference());
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
            JetTypeInferrer typeInferrer = semanticServices.getTypeInferrer(traceForConstructors, JetFlowInformationProvider.NONE); // TODO : flow
            for (JetClassInitializer anonymousInitializer : anonymousInitializers) {
                typeInferrer.getType(scopeForConstructor, anonymousInitializer.getBody(), true);
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

            assert descriptor.getUnsubstitutedReturnType() != null;
        }
    }

    private void resolveSecondaryConstructorBody(JetConstructor declaration, final ConstructorDescriptor descriptor, final JetScope declaringScope) {
        final JetScope functionInnerScope = getInnerScopeForConstructor(descriptor, declaringScope, false);

        final JetTypeInferrer typeInferrerForInitializers = semanticServices.getTypeInferrer(traceForConstructors, JetFlowInformationProvider.NONE);

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
                initializers.get(0).accept(new JetVisitor() {
                    @Override
                    public void visitDelegationToSuperCallSpecifier(JetDelegatorToSuperCall call) {
                        JetTypeReference typeReference = call.getTypeReference();
                        if (typeReference != null) {
                            typeInferrerForInitializers.checkConstructorCall(functionInnerScope, typeReference, call);
                        }
                    }

                    @Override
                    public void visitDelegationToThisCall(JetDelegatorToThisCall call) {
                        // TODO : check that there's no recursion in this() calls
                        // TODO : check: if a this() call is present, no other initializers are allowed
                        ClassDescriptor classDescriptor = descriptor.getContainingDeclaration();
                        typeInferrerForInitializers.checkClassConstructorCall(
                                functionInnerScope,
                                call.getThisReference(),
                                classDescriptor,
                                classDescriptor.getDefaultType(),
                                call);
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
            JetTypeInferrer typeInferrer = semanticServices.getTypeInferrer(traceForConstructors, flowInformationProvider);

            typeInferrer.getType(functionInnerScope, bodyExpression, true);
        }
    }

    @NotNull
    private JetScope getInnerScopeForConstructor(@NotNull ConstructorDescriptor descriptor, @NotNull JetScope declaringScope, boolean primary) {
        WritableScope constructorScope = new WritableScopeImpl(declaringScope, declaringScope.getContainingDeclaration(), trace.getErrorHandler());
        for (PropertyDescriptor propertyDescriptor : ((MutableClassDescriptor) descriptor.getContainingDeclaration()).getProperties()) {
            constructorScope.addPropertyDescriptorByFieldName("$" + propertyDescriptor.getName(), propertyDescriptor);
        }

        constructorScope.setThisType(descriptor.getContainingDeclaration().getDefaultType());

        for (ValueParameterDescriptor valueParameterDescriptor : descriptor.getUnsubstitutedValueParameters()) {
            JetParameter parameter = (JetParameter) trace.getBindingContext().getDeclarationPsiElement(valueParameterDescriptor);
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
        }
    }

    private JetScope getPropertyDeclarationInnerScope(@NotNull JetScope outerScope, @NotNull PropertyDescriptor propertyDescriptor) {
        WritableScopeImpl result = new WritableScopeImpl(outerScope, propertyDescriptor, trace.getErrorHandler());
        for (TypeParameterDescriptor typeParameterDescriptor : propertyDescriptor.getTypeParemeters()) {
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

        WritableScope accessorScope = new WritableScopeImpl(getPropertyDeclarationInnerScope(declaringScope, propertyDescriptor), declaringScope.getContainingDeclaration(), trace.getErrorHandler());
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

        JetExpression initializer = property.getInitializer();
        if (!property.isVar() && initializer != null && !trace.getBindingContext().hasBackingField(propertyDescriptor)) {
            trace.getErrorHandler().genericError(initializer.getNode(), "Initializer is not allowed here because this property has no setter and no backing field either");
        }
    }

    private BindingTraceAdapter createFieldTrackingTrace(final PropertyDescriptor propertyDescriptor) {
        return new BindingTraceAdapter(traceForMembers) {
            @Override
            public void recordReferenceResolution(@NotNull JetReferenceExpression expression, @NotNull DeclarationDescriptor descriptor) {
                super.recordReferenceResolution(expression, descriptor);
                if (expression instanceof JetSimpleNameExpression) {
                    JetSimpleNameExpression simpleNameExpression = (JetSimpleNameExpression) expression;
                    if (simpleNameExpression.getReferencedNameElementType() == JetTokens.FIELD_IDENTIFIER) {
                        // This check may be considered redundant as long as $x is only accessible from accessors to $x
                        if (descriptor == propertyDescriptor) { // TODO : original?
                            requireBackingField(propertyDescriptor);
                        }
                    }
                }
            }
        };
    }

    private void resolvePropertyInitializer(JetProperty property, PropertyDescriptor propertyDescriptor, JetExpression initializer, JetScope scope) {
        JetFlowInformationProvider flowInformationProvider = classDescriptorResolver.computeFlowData(property, initializer); // TODO : flow JET-15
        JetTypeInferrer typeInferrer = semanticServices.getTypeInferrer(traceForConstructors, flowInformationProvider);
        JetType type = typeInferrer.getType(getPropertyDeclarationInnerScope(scope, propertyDescriptor), initializer, false);

        JetType expectedType;
        PropertySetterDescriptor setter = propertyDescriptor.getSetter();
        if (setter != null) {
            expectedType = setter.getUnsubstitutedReturnType();
        }
        else {
            expectedType = propertyDescriptor.getInType();
            if (expectedType == null) {
                expectedType = propertyDescriptor.getOutType();
            }
        }
        if (type != null && expectedType != null
            && !semanticServices.getTypeChecker().isConvertibleTo(type, expectedType)) {
            trace.getErrorHandler().typeMismatch(initializer, expectedType, type);
        }
    }

    private void resolveFunctionBodies() {
        for (Map.Entry<JetFunction, FunctionDescriptorImpl> entry : functions.entrySet()) {
            JetDeclaration declaration = entry.getKey();
            FunctionDescriptor descriptor = entry.getValue();

            JetScope declaringScope = declaringScopes.get(declaration);
            assert declaringScope != null;

            resolveFunctionBody(traceForMembers, (JetFunction) declaration, (FunctionDescriptorImpl) descriptor, declaringScope);

            assert descriptor.getUnsubstitutedReturnType() != null;
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
            JetTypeInferrer typeInferrer = semanticServices.getTypeInferrer(trace, flowInformationProvider);

            typeInferrer.checkFunctionReturnType(declaringScope, function, functionDescriptor);

            List<JetElement> unreachableElements = new ArrayList<JetElement>();
            flowInformationProvider.collectUnreachableExpressions(function.asElement(), unreachableElements);

            // This is needed in order to highlight only '1 < 2' and not '1', '<' and '2' as well
            Set<JetElement> rootElements = JetPsiUtil.findRootExpressions(unreachableElements);

            // TODO : (return 1) || (return 2) -- only || and right of it is unreachable
            // TODO : try {return 1} finally {return 2}. Currently 'return 1' is reported as unreachable,
            //        though it'd better be reported more specifically

            for (JetElement element : rootElements) {
                trace.getErrorHandler().genericError(element.getNode(), "Unreachable code");
            }
        }

        assert functionDescriptor.getUnsubstitutedReturnType() != null;
    }

}
