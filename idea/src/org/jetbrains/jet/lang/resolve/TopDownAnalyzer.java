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
import org.jetbrains.jet.lang.types.*;
import org.jetbrains.jet.lexer.JetTokens;

import java.util.*;

/**
 * @author abreslav
 */
public class TopDownAnalyzer {

    private final Map<JetClass, MutableClassDescriptor> classes = Maps.newLinkedHashMap();
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
    private final BindingTraceContext trace;
    private boolean readyToProcessExpressions = false;
    private final BindingTraceAdapter traceForConstructors;
    private final BindingTraceAdapter traceForMembers;

    public TopDownAnalyzer(JetSemanticServices semanticServices, @NotNull BindingTraceContext bindingTrace) {
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
                        if (!trace.hasBackingField((PropertyDescriptor) descriptor)) {
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

//    public void process(@NotNull JetScope outerScope, @NotNull JetDeclaration declaration) {
//        process(outerScope, Collections.singletonList(declaration));
//    }

    public void process(@NotNull WritableScope outerScope, NamespaceLike owner, @NotNull List<JetDeclaration> declarations) {
        collectNamespacesAndClassifiers(outerScope, owner, declarations); // namespaceScopes, classes

        createTypeConstructors(); // create type constructors for classes and generic parameters
        resolveTypesInClassHeaders(); // Generic bounds and types in supertype lists (no expressions or constructor resolution)
        checkGenericBoundsInClassHeaders(); // For the types resolved so far

        resolveFunctionAndPropertyHeaders(declarations); // TODO : for now, fail fast if something is unknown yet (i.e. some type annotation is omitted)
                                                         // Constructor headers are resolved as well

        resolveExecutableCode();
//        processBehaviorDeclarators(outerScope, declarations);

//        readyToProcessExpressions = true;
//        resolveBehaviorDeclarationBodies();

    }

    private void collectNamespacesAndClassifiers(
            @NotNull final JetScope outerScope,
            @NotNull final NamespaceLike owner,
            @NotNull Collection<JetDeclaration> declarations) {
        for (JetDeclaration declaration : declarations) {
            declaration.accept(new JetVisitor() {
                @Override
                public void visitNamespace(JetNamespace namespace) {
                    List<JetImportDirective> importDirectives = namespace.getImportDirectives();

                    String name = namespace.getName();
                    if (name == null) {
                        name = "<no name provided>";
                    }
                    NamespaceDescriptorImpl namespaceDescriptor = owner.getNamespace(name);
                    if (namespaceDescriptor == null) {
                        namespaceDescriptor = new NamespaceDescriptorImpl(
                                owner.getOriginal(), //declaringScope.getContainingDeclaration(),
                                Collections.<Annotation>emptyList(), // TODO
                                name
                        );
                        namespaceDescriptor.initialize(new WritableScopeImpl(JetScope.EMPTY, namespaceDescriptor, trace.getErrorHandler(), null));
                        owner.addNamespace(namespaceDescriptor);
                        trace.recordDeclarationResolution(namespace, namespaceDescriptor);
                    }
                    namespaceDescriptors.put(namespace, namespaceDescriptor);

                    WritableScope namespaceScope = new WriteThroughScope(outerScope, (WritableScope) namespaceDescriptor.getMemberScope());
                    namespaceScopes.put(namespace, namespaceScope);

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
                            throw new UnsupportedOperationException();
                        }
                    }

                    collectNamespacesAndClassifiers(namespaceScope, namespaceDescriptor, namespace.getDeclarations());
                }

                @Override
                public void visitClass(JetClass klass) {
                    MutableClassDescriptor mutableClassDescriptor = new MutableClassDescriptor(trace, owner, outerScope);
                    mutableClassDescriptor.setName(JetPsiUtil.safeName(klass.getName()));

                    owner.addClassifierDescriptor(mutableClassDescriptor);

                    classes.put(klass, mutableClassDescriptor);
                    declaringScopes.put(klass, outerScope);

                    WritableScope classScope = mutableClassDescriptor.getScopeForMemberResolution();
                    collectNamespacesAndClassifiers(classScope, mutableClassDescriptor, klass.getDeclarations());
                }

                @Override
                public void visitTypedef(JetTypedef typedef) {
                    trace.getErrorHandler().genericError(typedef.getNode(), "Unsupported [TopDownAnalyzer]");
                }

                @Override
                public void visitExtension(JetExtension extension) {
                    trace.getErrorHandler().genericError(extension.getNode(), "Unsupported [TopDownAnalyzer]");
                }
            });
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
    }

    private void checkGenericBoundsInClassHeaders() {
        for (Map.Entry<JetClass, MutableClassDescriptor> entry : classes.entrySet()) {
            JetClass jetClass = entry.getKey();

            for (JetDelegationSpecifier delegationSpecifier : jetClass.getDelegationSpecifiers()) {
                JetType type = trace.resolveTypeReference(delegationSpecifier.getTypeReference());
                classDescriptorResolver.checkBounds(delegationSpecifier.getTypeReference(), type);
            }

            for (JetTypeParameter jetTypeParameter : jetClass.getTypeParameters()) {
                JetTypeReference extendsBound = jetTypeParameter.getExtendsBound();
                if (extendsBound != null) {
                    JetType type = trace.resolveTypeReference(extendsBound);
                    classDescriptorResolver.checkBounds(extendsBound, type);
                }
            }

            // TODO : where-bounds
        }

    }

    private void resolveFunctionAndPropertyHeaders(@NotNull List<JetDeclaration> declarations) {
        for (Map.Entry<JetNamespace, WritableScope> entry : namespaceScopes.entrySet()) {
            JetNamespace namespace = entry.getKey();
            final WritableScope namespaceScope = entry.getValue();
            final NamespaceLike namespaceDescriptor = namespaceDescriptors.get(namespace);

            resolveFunctionAndPropertyHeaders(namespace.getDeclarations(), namespaceScope, namespaceDescriptor);
        }
        for (Map.Entry<JetClass, MutableClassDescriptor> entry : classes.entrySet()) {
            JetClass jetClass = entry.getKey();
            final MutableClassDescriptor classDescriptor = entry.getValue();

            resolveFunctionAndPropertyHeaders(jetClass.getDeclarations(), classDescriptor.getScopeForMemberResolution(), classDescriptor);
            processPrimaryConstructor(classDescriptor, jetClass);
            for (JetConstructor jetConstructor : jetClass.getSecondaryConstructors()) {
                processSecondaryConstructor(classDescriptor, jetConstructor);
            }
//            processBehaviorDeclarators(classDescriptor.getScopeForMemberLookup(), jetClass.getDeclarations());

            // TODO : Constructors
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
            });
        }
    }

    private void resolveExecutableCode() {
        readyToProcessExpressions = true;
        resolveBehaviorDeclarationBodies();
    }

//    @NotNull
//    public JetScope process(@NotNull JetScope outerScope, @NotNull List<JetDeclaration> declarations) {
//        final WritableScope toplevelScope = new WritableScopeImpl(outerScope, outerScope.getContainingDeclaration(), trace.getErrorHandler(), null); // TODO ?!
//
//        collectTypeDeclarators(toplevelScope, declarations);
//        resolveTypeDeclarations();
//        processBehaviorDeclarators(toplevelScope, declarations);
//        readyToProcessExpressions = true;
//        resolveBehaviorDeclarationBodies();
//        return toplevelScope;
//    }

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

//    private void collectTypeDeclarators(
//            @NotNull final WritableScope declaringScope,
//            List<JetDeclaration> declarations) {
//        for (JetDeclaration declaration : declarations) {
//            declaration.accept(new JetVisitor() {
//                @Override
//                public void visitNamespace(JetNamespace namespace) {
//                    List<JetImportDirective> importDirectives = namespace.getImportDirectives();
//
//                    String name = namespace.getName();
//                    if (name == null) {
//                        name = "<no name provided>";
//                    }
//                    NamespaceDescriptorImpl namespaceDescriptor = (NamespaceDescriptorImpl) declaringScope.getDeclaredNamespace(name);
//                    if (namespaceDescriptor == null) {
//                        namespaceDescriptor = new NamespaceDescriptorImpl(
//                                declaringScope.getContainingDeclaration(),
//                                Collections.<Annotation>emptyList(), // TODO
//                                name
//                        );
//                        namespaceDescriptor.initialize(new WritableScopeImpl(JetScope.EMPTY, namespaceDescriptor, trace.getErrorHandler(), null));
//                        declaringScope.addNamespace(namespaceDescriptor);
//                        trace.recordDeclarationResolution(namespace, namespaceDescriptor);
//                    }
//
//                    WritableScope namespaceScope = new WriteThroughScope(declaringScope, (WritableScope) namespaceDescriptor.getMemberScope());
//                    namespaceScopes.put(namespace, namespaceScope);
//
//                    for (JetImportDirective importDirective : importDirectives) {
//                        if (importDirective.isAbsoluteInRootNamespace()) {
//                            throw new UnsupportedOperationException();
//                        }
//                        if (importDirective.isAllUnder()) {
//                            JetExpression importedReference = importDirective.getImportedReference();
//                            if (importedReference != null) {
//                                JetType type = semanticServices.getTypeInferrer(trace, JetFlowInformationProvider.THROW_EXCEPTION).getTypeWithNamespaces(namespaceScope, importedReference, false);
//                                if (type != null) {
//                                    namespaceScope.importScope(type.getMemberScope());
//                                }
//                            }
//                        } else {
//                            throw new UnsupportedOperationException();
//                        }
//                    }
//
//                    collectTypeDeclarators(namespaceScope, namespace.getDeclarations());
//                }
//
//                @Override
//                public void visitClass(JetClass klass) {
//                    MutableClassDescriptor mutableClassDescriptor = new MutableClassDescriptor(trace, declaringScope.getContainingDeclaration(), declaringScope);
//                    mutableClassDescriptor.setName(JetPsiUtil.safeName(klass.getName()));
//
//                    declaringScope.addClassifierDescriptor(mutableClassDescriptor);
//
//                    classes.put(klass, mutableClassDescriptor);
//                    declaringScopes.put(klass, declaringScope);
//
//                    WritableScope classScope = mutableClassDescriptor.getScopeForMemberLookup();
//                    collectTypeDeclarators(classScope, klass.getDeclarations());
//                }
//
//                @Override
//                public void visitTypedef(JetTypedef typedef) {
//                    trace.getErrorHandler().genericError(typedef.getNode(), "Unsupported [TopDownAnalyzer]");
//                }
//
//                @Override
//                public void visitExtension(JetExtension extension) {
//                    trace.getErrorHandler().genericError(extension.getNode(), "Unsupported [TopDownAnalyzer]");
//                }
//
//                @Override
//                public void visitDeclaration(JetDeclaration dcl) {
//                    // Other declarations do not declare visible types
//                }
//            });
//        }
//    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

//    private void resolveTypeDeclarations() {
//        for (Map.Entry<JetClass, MutableClassDescriptor> entry : classes.entrySet()) {
//            JetClass jetClass = entry.getKey();
//            MutableClassDescriptor descriptor = entry.getValue();
//            classDescriptorResolver.resolveMutableClassDescriptor(declaringScopes.get(jetClass), jetClass, descriptor);
//        }
//    }

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

//    private void processBehaviorDeclarators(@NotNull final WritableScope declaringScope, List<JetDeclaration> declarations) {
//        for (JetDeclaration declaration : declarations) {
//            declaration.accept(new JetVisitor() {
//                @Override
//                public void visitClass(JetClass klass) {
//                    MutableClassDescriptor mutableClassDescriptor = classes.get(klass);
//                    processPrimaryConstructor(mutableClassDescriptor, klass);
//                    processBehaviorDeclarators(mutableClassDescriptor.getScopeForMemberLookup(), klass.getDeclarations());
//                }
//
//                @Override
//                public void visitClassObject(JetClassObject classObject) {
//                    processClassObject(classObject);
//                    processBehaviorDeclarators(declaringScope, classObject.getObject().getDeclarations());
//                }
//
//                @Override
//                public void visitNamespace(JetNamespace namespace) {
//                    WritableScope namespaceScope = namespaceScopes.get(namespace);
//                    processBehaviorDeclarators(namespaceScope, namespace.getDeclarations());
//                }
//
//                @Override
//                public void visitFunction(JetFunction function) {
//                    processFunction(declaringScope, function);
//                }
//
//                @Override
//                public void visitProperty(JetProperty property) {
//                    processProperty(declaringScope, property);
//                }
//
//                @Override
//                public void visitConstructor(JetConstructor constructor) {
//                    DeclarationDescriptor containingDeclaration = declaringScope.getContainingDeclaration();
//                    if (containingDeclaration instanceof ClassDescriptor) {
//                        processSecondaryConstructor((MutableClassDescriptor) containingDeclaration, constructor);
//                    }
//                    else {
//                        trace.getErrorHandler().genericError(constructor.getNode(), "Constructors are only allowed inside classes");
//                    }
//                }
//
//                @Override
//                public void visitAnonymousInitializer(JetClassInitializer initializer) {
//                    // Nothing
//                }
//
//                @Override
//                public void visitDeclaration(JetDeclaration dcl) {
//                    trace.getErrorHandler().genericError(dcl.getNode(), "[TopDownAnalyzer] Unsupported declaration: " + dcl); // TODO
//                }
//            });
//        }
//
//    }

    private void processPrimaryConstructor(MutableClassDescriptor classDescriptor, JetClass klass) {
        if (!klass.hasPrimaryConstructor()) return;

        // TODO : not all the parameters are real properties
        WritableScope memberScope = classDescriptor.getScopeForMemberResolution(); // TODO : this is REALLY questionable
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

//    private void processFunction(@NotNull WritableScope declaringScope, JetFunction function) {
//        declaringScopes.put(function, declaringScope);
//        FunctionDescriptorImpl descriptor = classDescriptorResolver.resolveFunctionDescriptor(declaringScope.getContainingDeclaration(), declaringScope, function);
//        declaringScope.addFunctionDescriptor(descriptor);
//        functions.put(function, descriptor);
//    }
//
//    private void processProperty(WritableScope declaringScope, JetProperty property) {
//        declaringScopes.put(property, declaringScope);
//        // TODO : Do not infer the type from the initializer here: the scope is wrong, and not ready anyway
//        PropertyDescriptor descriptor = classDescriptorResolver.resolvePropertyDescriptor(declaringScope.getContainingDeclaration(), declaringScope, property);
//        declaringScope.addVariableDescriptor(descriptor);
//        declaringScopesToProperties.put(declaringScope.getContainingDeclaration(), descriptor);
//        properties.put(property, descriptor);
//    }
//
//    private void processClassObject(JetClassObject classObject) {
//        trace.getErrorHandler().genericError(classObject.getNode(), "Class objects are not supported yet"); // TODO
//    }

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
            MutableClassDescriptor classDescriptor = entry.getValue();
//            JetClass jetClass = entry.getKey();

            for (FunctionDescriptor declaredFunction : classDescriptor.getFunctions()) {
                for (JetType supertype : classDescriptor.getTypeConstructor().getSupertypes()) {
                    FunctionDescriptor overridden = findFunctionOverridableBy(declaredFunction, supertype);
                    if (overridden != null) {
                        ((FunctionDescriptorImpl) declaredFunction).addOverriddenFunction(overridden);
                    }
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
                    if (trace.hasBackingField(propertyDescriptor)) {
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
            final JetClass jetClass = entry.getKey();
            final MutableClassDescriptor descriptor = entry.getValue();
            final ConstructorDescriptor primaryConstructor = descriptor.getUnsubstitutedPrimaryConstructor();
            final JetScope scopeForConstructor = primaryConstructor == null
                    ? null
                    : getInnerScopeForConstructor(primaryConstructor, descriptor.getScopeForMemberResolution());
            final JetTypeInferrer typeInferrer = semanticServices.getTypeInferrer(traceForConstructors, JetFlowInformationProvider.NONE); // TODO : flow

            for (JetDelegationSpecifier delegationSpecifier : jetClass.getDelegationSpecifiers()) {
                delegationSpecifier.accept(new JetVisitor() {
                    @Override
                    public void visitDelegationByExpressionSpecifier(JetDelegatorByExpressionSpecifier specifier) {
                        JetExpression delegateExpression = specifier.getDelegateExpression();
                        if (delegateExpression != null) {
                            JetScope scope = scopeForConstructor == null ? descriptor.getScopeForMemberResolution() : scopeForConstructor;
                            JetType type = typeInferrer.getType(scope, delegateExpression, false);
                            JetType supertype = trace.resolveTypeReference(specifier.getTypeReference());
                            if (type != null && !semanticServices.getTypeChecker().isSubtypeOf(type, supertype)) { // TODO : Convertible?
                                trace.getErrorHandler().typeMismatch(delegateExpression, supertype, type);
                            }
                        }
                    }

                    @Override
                    public void visitDelegationToSuperCallSpecifier(JetDelegatorToSuperCall call) {
                        JetTypeReference typeReference = call.getTypeReference();
                        if (typeReference != null) {
                            if (!jetClass.hasPrimaryConstructor()) {
                                JetArgumentList valueArgumentList = call.getValueArgumentList();
                                assert valueArgumentList != null;
                                trace.getErrorHandler().genericError(valueArgumentList.getNode(),
                                        "Class " + JetPsiUtil.safeName(jetClass.getName()) + " must have a constructor in order to be able to initialize supertypes");
                            }
                            else {
                                typeInferrer.checkConstructorCall(scopeForConstructor, typeReference, call);
                            }
                        }
                    }

                    @Override
                    public void visitDelegationToSuperClassSpecifier(JetDelegatorToSuperClass specifier) {
                        JetType supertype = trace.resolveTypeReference(specifier.getTypeReference());
                        if (supertype != null) {
                            DeclarationDescriptor declarationDescriptor = supertype.getConstructor().getDeclarationDescriptor();
                            if (declarationDescriptor instanceof ClassDescriptor) {
                                ClassDescriptor classDescriptor = (ClassDescriptor) declarationDescriptor;
                                if (classDescriptor.hasConstructors()) {
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
                    public void visitJetElement(JetElement elem) {
                        throw new UnsupportedOperationException(elem.getText() + " : " + elem);
                    }
                });
            }
        }
    }

    private void resolveAnonymousInitializers() {
        for (Map.Entry<JetClass, MutableClassDescriptor> entry : classes.entrySet()) {
            JetClass jetClass = entry.getKey();
            MutableClassDescriptor classDescriptor = entry.getValue();

            List<JetClassInitializer> anonymousInitializers = jetClass.getAnonymousInitializers();
            if (jetClass.hasPrimaryConstructor()) {
                ConstructorDescriptor primaryConstructor = classDescriptor.getUnsubstitutedPrimaryConstructor();
                assert primaryConstructor != null;
                final JetScope scopeForConstructor = getInnerScopeForConstructor(primaryConstructor, classDescriptor.getScopeForMemberResolution());
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
    }

    private void resolveSecondaryConstructorBodies() {
        for (Map.Entry<JetDeclaration, ConstructorDescriptor> entry : constructors.entrySet()) {
            JetDeclaration declaration = entry.getKey();
            ConstructorDescriptor descriptor = entry.getValue();

            JetScope declaringScope = declaringScopes.get(declaration);
            assert declaringScope != null;

            resolveSecondaryConstructorBody((JetConstructor) declaration, descriptor, declaringScope);

            assert descriptor.getUnsubstitutedReturnType() != null;
        }
    }

    private void resolveSecondaryConstructorBody(JetConstructor declaration, final ConstructorDescriptor descriptor, final JetScope declaringScope) {
        final JetScope functionInnerScope = getInnerScopeForConstructor(descriptor, declaringScope);

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
                        ClassDescriptor classDescriptor = (ClassDescriptor) descriptor.getContainingDeclaration();
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
    private JetScope getInnerScopeForConstructor(@NotNull ConstructorDescriptor descriptor, @NotNull JetScope declaringScope) {
        WritableScope constructorScope = new WritableScopeImpl(declaringScope, declaringScope.getContainingDeclaration(), trace.getErrorHandler(), null);
        for (PropertyDescriptor propertyDescriptor : ((MutableClassDescriptor) descriptor.getContainingDeclaration()).getProperties()) {
            constructorScope.addPropertyDescriptorByFieldName("$" + propertyDescriptor.getName(), propertyDescriptor);
        }
        return FunctionDescriptorUtil.getFunctionInnerScope(constructorScope, descriptor, trace);
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
                        JetScope scope = getInnerScopeForConstructor(primaryConstructor, classDescriptor.getScopeForMemberResolution());
                        resolvePropertyInitializer(property, propertyDescriptor, initializer, scope);
                    }
                }

                resolvePropertyAccessors(property, propertyDescriptor, declaringScope);
                processed.add(property);
            }
        }

        // Top-level properties
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

    private void resolvePropertyAccessors(JetProperty property, PropertyDescriptor propertyDescriptor, JetScope declaringScope) {
        BindingTraceAdapter fieldAccessTrackingTrace = createFieldTrackingTrace(propertyDescriptor);

        WritableScope accessorScope = new WritableScopeImpl(declaringScope, declaringScope.getContainingDeclaration(), trace.getErrorHandler(), null);
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
        if (!property.isVar() && initializer != null && !trace.hasBackingField(propertyDescriptor)) {
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
        JetType type = typeInferrer.getType(scope, initializer, false);

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
            @NotNull MutableFunctionDescriptor functionDescriptor,
            @NotNull JetScope declaringScope) {
        JetExpression bodyExpression = function.getBodyExpression();

        if (bodyExpression != null) {
            JetFlowInformationProvider flowInformationProvider = classDescriptorResolver.computeFlowData(function.asElement(), bodyExpression);
            JetTypeInferrer typeInferrer = semanticServices.getTypeInferrer(trace, flowInformationProvider);

            assert readyToProcessExpressions : "Must be ready collecting types";

            if (functionDescriptor.isReturnTypeSet()) {
                typeInferrer.checkFunctionReturnType(declaringScope, function, functionDescriptor);
            }
            else {
                JetType returnType = typeInferrer.getFunctionReturnType(declaringScope, function, functionDescriptor);
                if (returnType == null) {
                    returnType = ErrorUtils.createErrorType("Unable to infer body type");
                }
                functionDescriptor.setUnsubstitutedReturnType(returnType);
            }

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
        else {
            if (!functionDescriptor.isReturnTypeSet()) {
                trace.getErrorHandler().genericError(function.asElement().getNode(), "This function must either declare a return type or have a body element");
                functionDescriptor.setUnsubstitutedReturnType(ErrorUtils.createErrorType("No type, no body"));
            }
        }
    }

}
