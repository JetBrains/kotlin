package org.jetbrains.jet.lang.resolve;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.JetSemanticServices;
import org.jetbrains.jet.lang.cfg.JetControlFlowProcessor;
import org.jetbrains.jet.lang.cfg.JetFlowInformationProvider;
import org.jetbrains.jet.lang.cfg.pseudocode.*;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.types.*;
import org.jetbrains.jet.lexer.JetTokens;

import java.util.*;

/**
 * @author abreslav
 */
public class TopDownAnalyzer {

    private final Map<JetClass, MutableClassDescriptor> classes = new LinkedHashMap<JetClass, MutableClassDescriptor>();
    private final Map<JetNamespace, WritableScope> namespaceScopes = new LinkedHashMap<JetNamespace, WritableScope>();
    private final Map<JetDeclaration, FunctionDescriptorImpl> functions = Maps.newLinkedHashMap();
    private final Map<JetDeclaration, ConstructorDescriptor> constructors = Maps.newLinkedHashMap();
    private final Map<JetProperty, PropertyDescriptor> properties = new LinkedHashMap<JetProperty, PropertyDescriptor>();
    private final Map<JetDeclaration, WritableScope> declaringScopes = new HashMap<JetDeclaration, WritableScope>();
    private final Multimap<DeclarationDescriptor, PropertyDescriptor> declaringScopesToProperties = ArrayListMultimap.create();
    private final Set<PropertyDescriptor> primaryConstructorParameterProperties = Sets.newHashSet();

    private final JetSemanticServices semanticServices;
    private final ClassDescriptorResolver classDescriptorResolver;
    private final BindingTraceContext trace;
    private final JetControlFlowDataTraceFactory flowDataTraceFactory;
    private boolean readyToProcessExpressions = false;
    private final BindingTraceAdapter traceForConstructors;
    private final BindingTraceAdapter traceForMembers;

    public TopDownAnalyzer(JetSemanticServices semanticServices, @NotNull BindingTraceContext bindingTrace, @NotNull JetControlFlowDataTraceFactory flowDataTraceFactory) {
        this.semanticServices = semanticServices;
        this.classDescriptorResolver = new ClassDescriptorResolver(semanticServices, bindingTrace);
        this.trace = bindingTrace;
        this.flowDataTraceFactory = flowDataTraceFactory;

        // This allows access to backing fields
        this.traceForConstructors = new BindingTraceAdapter(bindingTrace) {
            @Override
            public void recordReferenceResolution(@NotNull JetReferenceExpression expression, @NotNull DeclarationDescriptor descriptor) {
                super.recordReferenceResolution(expression, descriptor);
                if (expression instanceof JetSimpleNameExpression) {
                    JetSimpleNameExpression simpleNameExpression = (JetSimpleNameExpression) expression;
                    if (simpleNameExpression.getReferencedNameElementType() == JetTokens.FIELD_IDENTIFIER) {
                        if (!trace.hasBackingField((PropertyDescriptor) descriptor)) {
                            TopDownAnalyzer.this.semanticServices.getErrorHandler().genericError(expression.getNode(), "This property does not have a backing field");
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

    public TopDownAnalyzer(JetSemanticServices semanticServices, @NotNull BindingTraceContext bindingTrace) {
        this(semanticServices, bindingTrace, JetControlFlowDataTraceFactory.EMPTY);
    }

    public void process(@NotNull JetScope outerScope, @NotNull JetDeclaration declaration) {
        process(outerScope, Collections.singletonList(declaration));
    }

    public void process(@NotNull JetScope outerScope, @NotNull List<JetDeclaration> declarations) {
        final WritableScope toplevelScope = semanticServices.createWritableScope(outerScope, outerScope.getContainingDeclaration()); // TODO ?!
        trace.setToplevelScope(toplevelScope); // TODO : this is a hack
        collectTypeDeclarators(toplevelScope, declarations);
        resolveTypeDeclarations();
        processBehaviorDeclarators(toplevelScope, declarations);
        readyToProcessExpressions = true;
        resolveBehaviorDeclarationBodies();
    }

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private void collectTypeDeclarators(
            @NotNull final WritableScope declaringScope,
            List<JetDeclaration> declarations) {
        for (JetDeclaration declaration : declarations) {
            declaration.accept(new JetVisitor() {
                @Override
                public void visitClass(JetClass klass) {
                    WritableScope classScope = processClass(declaringScope, klass);
                    collectTypeDeclarators(classScope, klass.getDeclarations());
                }

                @Override
                public void visitNamespace(JetNamespace namespace) {
                    List<JetImportDirective> importDirectives = namespace.getImportDirectives();

                    String name = namespace.getName();
                    if (name == null) {
                        name = "<no name provided>";
                    }
                    NamespaceDescriptor namespaceDescriptor = declaringScope.getDeclaredNamespace(name);
                    if (namespaceDescriptor == null) {
                        namespaceDescriptor = new NamespaceDescriptor(
                                declaringScope.getContainingDeclaration(),
                                Collections.<Attribute>emptyList(), // TODO
                                name
                        );
                        namespaceDescriptor.initialize(semanticServices.createWritableScope(JetScope.EMPTY, namespaceDescriptor));
                        declaringScope.addNamespace(namespaceDescriptor);
                        trace.recordDeclarationResolution(namespace, namespaceDescriptor);
                    }

                    WritableScope namespaceScope = new WriteThroughScope(declaringScope, (WritableScope) namespaceDescriptor.getMemberScope());
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

                    collectTypeDeclarators(namespaceScope, namespace.getDeclarations());
                }

                @Override
                public void visitTypedef(JetTypedef typedef) {
                    processTypeDef(typedef);
                }

                @Override
                public void visitDeclaration(JetDeclaration dcl) {
                    // Other declarations do not declare visible types
                }
            });
        }
    }

    private WritableScope processClass(@NotNull WritableScope declaringScope, JetClass klass) {
        MutableClassDescriptor mutableClassDescriptor = new MutableClassDescriptor(semanticServices, declaringScope.getContainingDeclaration(), declaringScope);
        mutableClassDescriptor.setName(JetPsiUtil.safeName(klass.getName()));

        declaringScope.addClassifierDescriptor(mutableClassDescriptor);

        classes.put(klass, mutableClassDescriptor);
        declaringScopes.put(klass, declaringScope);

        return mutableClassDescriptor.getWritableUnsubstitutedMemberScope();
    }

    private void processExtension(JetExtension extension) {
        throw new UnsupportedOperationException(extension.getText()); // TODO
    }

    private void processTypeDef(@NotNull JetTypedef typedef) {
        throw new UnsupportedOperationException(typedef.getText()); // TODO
    }

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private void resolveTypeDeclarations() {
        for (Map.Entry<JetClass, MutableClassDescriptor> entry : classes.entrySet()) {
            JetClass jetClass = entry.getKey();
            MutableClassDescriptor descriptor = entry.getValue();
            classDescriptorResolver.resolveMutableClassDescriptor(declaringScopes.get(jetClass), jetClass, descriptor);
        }
    }

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private void processBehaviorDeclarators(@NotNull final WritableScope declaringScope, List<JetDeclaration> declarations) {
        for (JetDeclaration declaration : declarations) {
            declaration.accept(new JetVisitor() {
                @Override
                public void visitClass(JetClass klass) {
                    MutableClassDescriptor mutableClassDescriptor = classes.get(klass);
                    processPrimaryConstructor(mutableClassDescriptor, klass);
                    processBehaviorDeclarators(mutableClassDescriptor.getWritableUnsubstitutedMemberScope(), klass.getDeclarations());
                }

                @Override
                public void visitClassObject(JetClassObject classObject) {
                    processClassObject(classObject);
                    processBehaviorDeclarators(declaringScope, classObject.getObject().getDeclarations());
                }

                @Override
                public void visitNamespace(JetNamespace namespace) {
                    WritableScope namespaceScope = namespaceScopes.get(namespace);
                    processBehaviorDeclarators(namespaceScope, namespace.getDeclarations());
                }

                @Override
                public void visitFunction(JetFunction function) {
                    processFunction(declaringScope, function);
                }

                @Override
                public void visitProperty(JetProperty property) {
                    processProperty(declaringScope, property);
                }

                @Override
                public void visitConstructor(JetConstructor constructor) {
                    DeclarationDescriptor containingDeclaration = declaringScope.getContainingDeclaration();
                    if (containingDeclaration instanceof ClassDescriptor) {
                        processSecondaryConstructor((MutableClassDescriptor) containingDeclaration, constructor);
                    }
                    else {
                        semanticServices.getErrorHandler().genericError(constructor.getNode(), "Constructors are only allowed inside classes");
                    }
                }

                @Override
                public void visitAnonymousInitializer(JetClassInitializer initializer) {
                    // Nothing
                }

                @Override
                public void visitDeclaration(JetDeclaration dcl) {
                    semanticServices.getErrorHandler().genericError(dcl.getNode(), "[TopDownAnalyzer] Unsupported declaration: " + dcl); // TODO
                }
            });
        }

    }

    private void processPrimaryConstructor(MutableClassDescriptor classDescriptor, JetClass klass) {
        if (!klass.hasPrimaryConstructor()) return;

        // TODO : not all the parameters are real properties
        WritableScope memberScope = classDescriptor.getWritableUnsubstitutedMemberScope(); // TODO : this is REALLY questionable
        ConstructorDescriptor constructorDescriptor = classDescriptorResolver.resolvePrimaryConstructorDescriptor(memberScope, classDescriptor, klass);
        for (JetParameter parameter : klass.getPrimaryConstructorParameters()) {
            PropertyDescriptor propertyDescriptor = classDescriptorResolver.resolvePrimaryConstructorParameterToAProperty(
                    classDescriptor,
                    memberScope,
                    parameter
            );
            memberScope.addVariableDescriptor(
                    propertyDescriptor);
            primaryConstructorParameterProperties.add(propertyDescriptor);
        }
        if (constructorDescriptor != null) {
            classDescriptor.setPrimaryConstructor(constructorDescriptor);
        }
    }

    private void processSecondaryConstructor(MutableClassDescriptor classDescriptor, JetConstructor constructor) {
        ConstructorDescriptor constructorDescriptor = classDescriptorResolver.resolveSecondaryConstructorDescriptor(
                classDescriptor.getWritableUnsubstitutedMemberScope(),
                classDescriptor,
                constructor);
        classDescriptor.addConstructor(constructorDescriptor);
        constructors.put(constructor, constructorDescriptor);
        declaringScopes.put(constructor, classDescriptor.getWritableUnsubstitutedMemberScope());
    }

    private void processFunction(@NotNull WritableScope declaringScope, JetFunction function) {
        declaringScopes.put(function, declaringScope);
        FunctionDescriptorImpl descriptor = classDescriptorResolver.resolveFunctionDescriptor(declaringScope.getContainingDeclaration(), declaringScope, function);
        declaringScope.addFunctionDescriptor(descriptor);
        functions.put(function, descriptor);
    }

    private void processProperty(WritableScope declaringScope, JetProperty property) {
        declaringScopes.put(property, declaringScope);
        // TODO : Do not infer the type from the initializer here: the scope is wrong, and not ready anyway
        PropertyDescriptor descriptor = classDescriptorResolver.resolvePropertyDescriptor(declaringScope.getContainingDeclaration(), declaringScope, property);
        declaringScope.addVariableDescriptor(descriptor);
        declaringScopesToProperties.put(declaringScope.getContainingDeclaration(), descriptor);
        properties.put(property, descriptor);
    }

    private void processClassObject(JetClassObject classObject) {
        semanticServices.getErrorHandler().genericError(classObject.getNode(), "Class objects are not supported yet"); // TODO
    }

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private void resolveBehaviorDeclarationBodies() {
        resolveDelegationSpecifierLists();

        resolveAnonymousInitializers();
        resolvePropertyDeclarationBodies();

        resolveSecondaryConstructorBodies();
        resolveFunctionBodies();
    }

    private void resolveDelegationSpecifierLists() {
        // TODO : Make sure the same thing is not initialized twice
        final JetTypeInferrer typeInferrer = semanticServices.getTypeInferrer(trace, JetFlowInformationProvider.NONE);
        for (Map.Entry<JetClass, MutableClassDescriptor> entry : classes.entrySet()) {
            final JetClass jetClass = entry.getKey();
            final MutableClassDescriptor descriptor = entry.getValue();

            for (JetDelegationSpecifier delegationSpecifier : jetClass.getDelegationSpecifiers()) {
                delegationSpecifier.accept(new JetVisitor() {
                    @Override
                    public void visitDelegationByExpressionSpecifier(JetDelegatorByExpressionSpecifier specifier) {
                        JetExpression delegateExpression = specifier.getDelegateExpression();
                        if (delegateExpression != null) {
                            JetType type = typeInferrer.getType(descriptor.getWritableUnsubstitutedMemberScope(), delegateExpression, false);
                            JetType supertype = trace.resolveTypeReference(specifier.getTypeReference());
                            if (type != null && !semanticServices.getTypeChecker().isSubtypeOf(type, supertype)) { // TODO : Convertible?
                                semanticServices.getErrorHandler().typeMismatch(delegateExpression, supertype, type);
                            }
                        }
                    }

                    @Override
                    public void visitDelegationToSuperCallSpecifier(JetDelegatorToSuperCall call) {
                        JetTypeReference typeReference = call.getTypeReference();
                        if (typeReference != null) {
                            typeInferrer.checkConstructorCall(descriptor.getWritableUnsubstitutedMemberScope(), typeReference, call);
                            if (!jetClass.hasPrimaryConstructor()) {
                                JetArgumentList valueArgumentList = call.getValueArgumentList();
                                assert valueArgumentList != null;
                                semanticServices.getErrorHandler().genericError(valueArgumentList.getNode(),
                                        "Class " + JetPsiUtil.safeName(jetClass.getName()) + " must have a constructor in order to be able to initialize supertypes");
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
                                    semanticServices.getErrorHandler().genericError(specifier.getNode(), "This type has a constructor, and thus must be initialized here");
                                }
                            }
                            else {
                                semanticServices.getErrorHandler().genericError(specifier.getNode(), "Only classes may serve as supertypes");
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
                final JetScope scopeForConstructor = getInnerScopeForConstructor(primaryConstructor, classDescriptor.getWritableUnsubstitutedMemberScope());
                JetTypeInferrer typeInferrer = semanticServices.getTypeInferrer(traceForConstructors, JetFlowInformationProvider.NONE); // TODO : flow
                for (JetClassInitializer anonymousInitializer : anonymousInitializers) {
                    typeInferrer.getType(scopeForConstructor, anonymousInitializer.getBody(), true);
                }
            }
            else {
                for (JetClassInitializer anonymousInitializer : anonymousInitializers) {
                    semanticServices.getErrorHandler().genericError(anonymousInitializer.getNode(), "Anonymous initializers are only allowed in the presence of a primary constructor");
                }
            }
        }
    }

    private void resolveSecondaryConstructorBodies() {
        for (Map.Entry<JetDeclaration, ConstructorDescriptor> entry : constructors.entrySet()) {
            JetDeclaration declaration = entry.getKey();
            ConstructorDescriptor descriptor = entry.getValue();

            WritableScope declaringScope = declaringScopes.get(declaration);
            assert declaringScope != null;

            resolveSecondaryConstructorBody((JetConstructor) declaration, descriptor, declaringScope);

            assert descriptor.getUnsubstitutedReturnType() != null;
        }
    }

    private void resolveSecondaryConstructorBody(JetConstructor declaration, final ConstructorDescriptor descriptor, final WritableScope declaringScope) {
        final JetScope functionInnerScope = getInnerScopeForConstructor(descriptor, declaringScope);

        final JetTypeInferrer typeInferrerForInitializers = semanticServices.getTypeInferrer(traceForConstructors, JetFlowInformationProvider.NONE);

        JetClass containingClass = PsiTreeUtil.getParentOfType(declaration, JetClass.class);
        assert containingClass != null : "This must be guaranteed by the parser";
        if (!containingClass.hasPrimaryConstructor()) {
            semanticServices.getErrorHandler().genericError(declaration.getNameNode(), "A secondary constructor may appear only in a class that has a primary constructor");
        }
        else {
            List<JetDelegationSpecifier> initializers = declaration.getInitializers();
            if (initializers.isEmpty()) {
                semanticServices.getErrorHandler().genericError(declaration.getNameNode(), "Secondary constructors must have an initializer list");
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
                        semanticServices.getErrorHandler().genericError(specifier.getNode(), "'by'-clause is only supported for primary constructors");
                    }

                    @Override
                    public void visitDelegationToSuperClassSpecifier(JetDelegatorToSuperClass specifier) {
                        semanticServices.getErrorHandler().genericError(specifier.getNode(), "Constructor parameters required");
                    }

                    @Override
                    public void visitDelegationSpecifier(JetDelegationSpecifier specifier) {
                        throw new IllegalStateException();
                    }
                });
                for (int i = 1, initializersSize = initializers.size(); i < initializersSize; i++) {
                    JetDelegationSpecifier initializer = initializers.get(i);
                    semanticServices.getErrorHandler().genericError(initializer.getNode(), "Only one call to 'this(...)' is allowed");
                }
            }
        }
        JetExpression bodyExpression = declaration.getBodyExpression();
        if (bodyExpression != null) {
            computeFlowData(declaration, bodyExpression);
            JetFlowInformationProvider flowInformationProvider = computeFlowData(declaration, bodyExpression);
            JetTypeInferrer typeInferrer = semanticServices.getTypeInferrer(traceForConstructors, flowInformationProvider);

            typeInferrer.getType(functionInnerScope, bodyExpression, true);
        }
    }

    @NotNull
    private JetScope getInnerScopeForConstructor(@NotNull ConstructorDescriptor descriptor, @NotNull JetScope declaringScope) {
        WritableScope constructorScope = semanticServices.createWritableScope(declaringScope, declaringScope.getContainingDeclaration());
        for (PropertyDescriptor propertyDescriptor : declaringScopesToProperties.get(descriptor.getContainingDeclaration())) {
            constructorScope.addPropertyDescriptorByFieldName("$" + propertyDescriptor.getName(), propertyDescriptor);
        }
        return FunctionDescriptorUtil.getFunctionInnerScope(constructorScope, descriptor, semanticServices);
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

                WritableScope declaringScope = declaringScopes.get(property);

                JetExpression initializer = property.getInitializer();
                if (initializer != null) {
                    ConstructorDescriptor primaryConstructor = classDescriptor.getUnsubstitutedPrimaryConstructor();
                    if (primaryConstructor == null) {
                        semanticServices.getErrorHandler().genericError(initializer.getNode(), "Property initializers are not allowed when no primary constructor is present");
                    }
                    else {
                        JetScope scope = getInnerScopeForConstructor(primaryConstructor, classDescriptor.getWritableUnsubstitutedMemberScope());
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
            WritableScope declaringScope = declaringScopes.get(property);

            JetExpression initializer = property.getInitializer();
            if (initializer != null) {
                resolvePropertyInitializer(property, propertyDescriptor, initializer, declaringScope);
            }

            resolvePropertyAccessors(property, propertyDescriptor, declaringScope);
        }
    }

    private void resolvePropertyAccessors(JetProperty property, PropertyDescriptor propertyDescriptor, WritableScope declaringScope) {
        BindingTraceAdapter fieldAccessTrackingTrace = createFieldTrackingTrace(propertyDescriptor);

        WritableScope accessorScope = semanticServices.createWritableScope(declaringScope, declaringScope.getContainingDeclaration());
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
            semanticServices.getErrorHandler().genericError(initializer.getNode(), "Initializer is not allowed here because this property has no setter and no backing field either");
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
        JetFlowInformationProvider flowInformationProvider = computeFlowData(property, initializer); // TODO : flow JET-15
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
            semanticServices.getErrorHandler().typeMismatch(initializer, expectedType, type);
        }
    }

    private void resolveFunctionBodies() {
        for (Map.Entry<JetDeclaration, FunctionDescriptorImpl> entry : functions.entrySet()) {
            JetDeclaration declaration = entry.getKey();
            FunctionDescriptor descriptor = entry.getValue();

            WritableScope declaringScope = declaringScopes.get(declaration);
            assert declaringScope != null;

            resolveFunctionBody(traceForMembers, (JetFunction) declaration, (FunctionDescriptorImpl) descriptor, declaringScope);

            assert descriptor.getUnsubstitutedReturnType() != null;
        }
    }

    private void resolveFunctionBody(
            @NotNull BindingTrace trace,
            @NotNull JetDeclarationWithBody function,
            @NotNull MutableFunctionDescriptor functionDescriptor,
            @NotNull WritableScope declaringScope) {
        JetExpression bodyExpression = function.getBodyExpression();
        if (bodyExpression != null) {
            JetFlowInformationProvider flowInformationProvider = computeFlowData(function.asElement(), bodyExpression);
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
                semanticServices.getErrorHandler().genericError(element.getNode(), "Unreachable code");
            }
        }
        else {
            if (!functionDescriptor.isReturnTypeSet()) {
                semanticServices.getErrorHandler().genericError(function.asElement().getNode(), "This function must either declare a return type or have a body element");
                functionDescriptor.setUnsubstitutedReturnType(ErrorUtils.createErrorType("No type, no body"));
            }
        }
    }

    private JetFlowInformationProvider computeFlowData(@NotNull JetElement declaration, @NotNull JetExpression bodyExpression) {
        final JetPseudocodeTrace pseudocodeTrace = flowDataTraceFactory.createTrace(declaration);
        final Map<JetElement, Pseudocode> pseudocodeMap = new HashMap<JetElement, Pseudocode>();
        final Map<JetElement, Instruction> representativeInstructions = new HashMap<JetElement, Instruction>();
        JetPseudocodeTrace wrappedTrace = new JetPseudocodeTrace() {
            @Override
            public void recordControlFlowData(@NotNull JetElement element, @NotNull Pseudocode pseudocode) {
                pseudocodeTrace.recordControlFlowData(element, pseudocode);
                pseudocodeMap.put(element, pseudocode);
            }

            @Override
            public void recordRepresentativeInstruction(@NotNull JetElement element, @NotNull Instruction instruction) {
                Instruction oldValue = representativeInstructions.put(element, instruction);
//                assert oldValue == null : element.getText();
            }

            @Override
            public void close() {
                pseudocodeTrace.close();
                for (Pseudocode pseudocode : pseudocodeMap.values()) {
                    pseudocode.postProcess();
                }
            }
        };
        JetControlFlowInstructionsGenerator instructionsGenerator = new JetControlFlowInstructionsGenerator(wrappedTrace);
        new JetControlFlowProcessor(semanticServices, trace, instructionsGenerator).generate(declaration, bodyExpression);
        wrappedTrace.close();
        return new JetFlowInformationProvider() {
            @Override
            public void collectReturnedInformation(@NotNull JetElement subroutine, @NotNull Collection<JetExpression> returnedExpressions, @NotNull Collection<JetElement> elementsReturningUnit) {
                Pseudocode pseudocode = pseudocodeMap.get(subroutine);
                assert pseudocode != null;

                SubroutineExitInstruction exitInstruction = pseudocode.getExitInstruction();
                processPreviousInstructions(exitInstruction, new HashSet<Instruction>(), returnedExpressions, elementsReturningUnit);
            }

            @Override
            public void collectUnreachableExpressions(@NotNull JetElement subroutine, @NotNull Collection<JetElement> unreachableElements) {
                Pseudocode pseudocode = pseudocodeMap.get(subroutine);
                assert pseudocode != null;

                SubroutineEnterInstruction enterInstruction = pseudocode.getEnterInstruction();
                Set<Instruction> visited = new HashSet<Instruction>();
                collectReachable(enterInstruction, visited);

                for (Instruction instruction : pseudocode.getInstructions()) {
                    if (!visited.contains(instruction) &&
                        instruction instanceof JetElementInstruction &&
                        // TODO : do {return} while (1 > a)
                        !(instruction instanceof ReadUnitValueInstruction)) {
                        unreachableElements.add(((JetElementInstruction) instruction).getElement());
                    }
                }
            }

            @Override
            public void collectDominatedExpressions(@NotNull JetExpression dominator, @NotNull Collection<JetElement> dominated) {
                Instruction dominatorInstruction = representativeInstructions.get(dominator);
                if (dominatorInstruction == null) {
                    return;
                }
                SubroutineEnterInstruction enterInstruction = dominatorInstruction.getOwner().getEnterInstruction();

                Set<Instruction> reachable = new HashSet<Instruction>();
                collectReachable(enterInstruction, reachable);

                Set<Instruction> reachableWithDominatorProhibited = new HashSet<Instruction>();
                reachableWithDominatorProhibited.add(dominatorInstruction);
                collectReachable(enterInstruction, reachableWithDominatorProhibited);

                for (Instruction instruction : reachable) {
                    if (instruction instanceof JetElementInstruction
                            && reachable.contains(instruction)
                            && !reachableWithDominatorProhibited.contains(instruction)) {
                        JetElementInstruction elementInstruction = (JetElementInstruction) instruction;
                        dominated.add(elementInstruction.getElement());
                    }
                }
            }
        };
    }

    private void collectReachable(Instruction current, Set<Instruction> visited) {
        if (!visited.add(current)) return;

        for (Instruction nextInstruction : current.getNextInstructions()) {
            collectReachable(nextInstruction, visited);
        }
    }

    private void processPreviousInstructions(Instruction previousFor, final Set<Instruction> visited, final Collection<JetExpression> returnedExpressions, final Collection<JetElement> elementsReturningUnit) {
        if (!visited.add(previousFor)) return;

        Collection<Instruction> previousInstructions = previousFor.getPreviousInstructions();
        InstructionVisitor visitor = new InstructionVisitor() {
            @Override
            public void visitReadValue(ReadValueInstruction instruction) {
                returnedExpressions.add((JetExpression) instruction.getElement());
            }

            @Override
            public void visitReturnValue(ReturnValueInstruction instruction) {
                processPreviousInstructions(instruction, visited, returnedExpressions, elementsReturningUnit);
            }

            @Override
            public void visitReturnNoValue(ReturnNoValueInstruction instruction) {
                elementsReturningUnit.add(instruction.getElement());
            }

            @Override
            public void visitSubroutineEnter(SubroutineEnterInstruction instruction) {
                elementsReturningUnit.add(instruction.getSubroutine());
            }

            @Override
            public void visitUnsupportedElementInstruction(UnsupportedElementInstruction instruction) {
                semanticServices.getErrorHandler().genericError(instruction.getElement().getNode(), "Unsupported by control-flow builder " + instruction.getElement());
            }

            @Override
            public void visitWriteValue(WriteValueInstruction writeValueInstruction) {
                elementsReturningUnit.add(writeValueInstruction.getElement());
            }

            @Override
            public void visitJump(AbstractJumpInstruction instruction) {
                processPreviousInstructions(instruction, visited, returnedExpressions, elementsReturningUnit);
            }

            @Override
            public void visitReadUnitValue(ReadUnitValueInstruction instruction) {
                returnedExpressions.add((JetExpression) instruction.getElement());
            }

            @Override
            public void visitInstruction(Instruction instruction) {
                if (instruction instanceof JetElementInstructionImpl) {
                    JetElementInstructionImpl elementInstruction = (JetElementInstructionImpl) instruction;
                    semanticServices.getErrorHandler().genericError(elementInstruction.getElement().getNode(), "Unsupported by control-flow builder " + elementInstruction.getElement());
                }
                else {
                    throw new UnsupportedOperationException(instruction.toString());
                }
            }
        };
        for (Instruction previousInstruction : previousInstructions) {
            previousInstruction.accept(visitor);
        }
    }

}
