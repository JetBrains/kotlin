package org.jetbrains.jet.lang.resolve;

import com.google.common.collect.Lists;
import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.JetSemanticServices;
import org.jetbrains.jet.lang.cfg.JetControlFlowProcessor;
import org.jetbrains.jet.lang.cfg.JetFlowInformationProvider;
import org.jetbrains.jet.lang.cfg.pseudocode.*;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.types.*;
import org.jetbrains.jet.lexer.JetTokens;

import java.util.*;

/**
 * @author abreslav
 */
public class ClassDescriptorResolver {

    private static final MemberModifiers DEFAULT_MODIFIERS = new MemberModifiers(false, false, false);

    private final JetSemanticServices semanticServices;
    private final TypeResolver typeResolver;
    private final TypeResolver typeResolverNotCheckingBounds;
    private final BindingTrace trace;
    private final JetControlFlowDataTraceFactory flowDataTraceFactory;

    public ClassDescriptorResolver(JetSemanticServices semanticServices, BindingTrace trace, JetControlFlowDataTraceFactory flowDataTraceFactory) {
        this.semanticServices = semanticServices;
        this.typeResolver = new TypeResolver(semanticServices, trace, true);
        this.typeResolverNotCheckingBounds = new TypeResolver(semanticServices, trace, false);
        this.trace = trace;
        this.flowDataTraceFactory = flowDataTraceFactory;
    }

    @Nullable
    public ClassDescriptor resolveClassDescriptor(@NotNull JetScope scope, @NotNull JetClass classElement) {
        final ClassDescriptorImpl classDescriptor = new ClassDescriptorImpl(
                scope.getContainingDeclaration(),
                AnnotationResolver.INSTANCE.resolveAnnotations(classElement.getModifierList()),
                JetPsiUtil.safeName(classElement.getName()));

        trace.recordDeclarationResolution(classElement, classDescriptor);

        final WritableScope parameterScope = new WritableScopeImpl(scope, classDescriptor, trace.getErrorHandler());

        // This call has side-effects on the parameterScope (fills it in)
        List<TypeParameterDescriptor> typeParameters
                = resolveTypeParameters(classDescriptor, parameterScope, classElement.getTypeParameters());

        List<JetDelegationSpecifier> delegationSpecifiers = classElement.getDelegationSpecifiers();
        // TODO : assuming that the hierarchy is acyclic
        Collection<JetType> supertypes = delegationSpecifiers.isEmpty()
                ? Collections.singleton(JetStandardClasses.getAnyType())
                : resolveDelegationSpecifiers(parameterScope, delegationSpecifiers, typeResolver);
        boolean open = classElement.hasModifier(JetTokens.OPEN_KEYWORD);

        final WritableScope memberDeclarations = new WritableScopeImpl(parameterScope, classDescriptor, trace.getErrorHandler());

        List<JetDeclaration> declarations = classElement.getDeclarations();
        for (JetDeclaration declaration : declarations) {
            declaration.accept(new JetVisitor() {
                @Override
                public void visitProperty(JetProperty property) {
                    if (property.getPropertyTypeRef() != null) {
                        memberDeclarations.addVariableDescriptor(resolvePropertyDescriptor(classDescriptor, parameterScope, property));
                    } else {
                        // TODO : Caution: a cyclic dependency possible
                        throw new UnsupportedOperationException();
                    }
                }

                @Override
                public void visitFunction(JetFunction function) {
                    if (function.getReturnTypeRef() != null) {
                        memberDeclarations.addFunctionDescriptor(resolveFunctionDescriptor(classDescriptor, parameterScope, function));
                    } else {
                        // TODO : Caution: a cyclic dependency possible
                        throw new UnsupportedOperationException();
                    }
                }

                @Override
                public void visitJetElement(JetElement elem) {
                    throw new UnsupportedOperationException(elem.toString());
                }
            });
        }

        WritableFunctionGroup constructors = new WritableFunctionGroup("<init>");
        for (JetConstructor constructor : classElement.getSecondaryConstructors()) {
            constructors.addFunction(resolveSecondaryConstructorDescriptor(memberDeclarations, classDescriptor, constructor));
        }
        ConstructorDescriptor primaryConstructorDescriptor = resolvePrimaryConstructorDescriptor(scope, classDescriptor, classElement);
        if (primaryConstructorDescriptor != null) {
            constructors.addFunction(primaryConstructorDescriptor);
        }
        return classDescriptor.initialize(
                !open,
                typeParameters,
                supertypes,
                memberDeclarations,
                constructors,
                primaryConstructorDescriptor
        );
    }

    public void resolveMutableClassDescriptor(@NotNull JetClass classElement, @NotNull MutableClassDescriptor descriptor) {
        // TODO : Where-clause
        List<TypeParameterDescriptor> typeParameters = Lists.newArrayList();
        int index = 0;
        for (JetTypeParameter typeParameter : classElement.getTypeParameters()) {
            TypeParameterDescriptor typeParameterDescriptor = TypeParameterDescriptor.createForFurtherModification(
                    descriptor,
                    AnnotationResolver.INSTANCE.resolveAnnotations(typeParameter.getModifierList()),
                    typeParameter.getVariance(),
                    JetPsiUtil.safeName(typeParameter.getName()),
                    index
            );
            trace.recordDeclarationResolution(typeParameter, typeParameterDescriptor);
            typeParameters.add(typeParameterDescriptor);
            index++;
        }
        descriptor.setTypeParameterDescriptors(typeParameters);

        descriptor.setOpen(classElement.hasModifier(JetTokens.OPEN_KEYWORD));

        trace.recordDeclarationResolution(classElement, descriptor);
    }

    public void resolveGenericBounds(@NotNull JetClass jetClass, @NotNull MutableClassDescriptor classDescriptor) {
        List<JetTypeParameter> typeParameters = jetClass.getTypeParameters();
        List<TypeParameterDescriptor> parameters = classDescriptor.getTypeConstructor().getParameters();
        for (int i = 0, typeParametersSize = typeParameters.size(); i < typeParametersSize; i++) {
            JetTypeParameter jetTypeParameter = typeParameters.get(i);
            TypeParameterDescriptor typeParameterDescriptor = parameters.get(i);
            JetTypeReference extendsBound = jetTypeParameter.getExtendsBound();
            if (extendsBound != null) {
                typeParameterDescriptor.addUpperBound(typeResolverNotCheckingBounds.resolveType(classDescriptor.getScopeForSupertypeResolution(), extendsBound));
            }
            else {
                typeParameterDescriptor.addUpperBound(JetStandardClasses.getDefaultBound());
            }
        }
        // TODO : Bounds from with
    }

    public void resolveSupertypes(@NotNull JetClass jetClass, @NotNull MutableClassDescriptor descriptor) {
        List<JetDelegationSpecifier> delegationSpecifiers = jetClass.getDelegationSpecifiers();
//        TODO : assuming that the hierarchy is acyclic
        Collection<? extends JetType> superclasses = delegationSpecifiers.isEmpty()
                ? Collections.singleton(JetStandardClasses.getAnyType())
                : resolveDelegationSpecifiers(
                    descriptor.getScopeForSupertypeResolution(),
                    delegationSpecifiers,
                    typeResolverNotCheckingBounds);

        // TODO : remove the importing
        for (JetType superclass : superclasses) {
            descriptor.addSupertype(superclass);
        }
    }

    @NotNull
    public FunctionDescriptorImpl resolveFunctionDescriptor(DeclarationDescriptor containingDescriptor, final JetScope scope, final JetFunction function) {
        final FunctionDescriptorImpl functionDescriptor = new FunctionDescriptorImpl(
                containingDescriptor,
                AnnotationResolver.INSTANCE.resolveAnnotations(function.getModifierList()),
                JetPsiUtil.safeName(function.getName())
        );
        WritableScope innerScope = new WritableScopeImpl(scope, functionDescriptor, trace.getErrorHandler());
        innerScope.addLabeledDeclaration(functionDescriptor);


        List<TypeParameterDescriptor> typeParameterDescriptors = resolveTypeParameters(functionDescriptor, innerScope, function.getTypeParameters());

        JetType receiverType = null;
        JetTypeReference receiverTypeRef = function.getReceiverTypeRef();
        if (receiverTypeRef != null) {
            JetScope scopeForReceiver =
                    function.hasTypeParameterListBeforeFunctionName()
                    ? innerScope
                    : scope;
            receiverType = typeResolver.resolveType(scopeForReceiver, receiverTypeRef);
        }

        List<ValueParameterDescriptor> valueParameterDescriptors = resolveValueParameters(functionDescriptor, innerScope, function.getValueParameters());

        JetTypeReference returnTypeRef = function.getReturnTypeRef();
        JetType returnType;
        if (returnTypeRef != null) {
            returnType = typeResolver.resolveType(innerScope, returnTypeRef);
        }
        else {
            final JetExpression bodyExpression = function.getBodyExpression();
            if (bodyExpression != null) {
                returnType = new DeferredType(new LazyValue<JetType>() {
                    @Override
                    protected JetType compute() {
                        JetFlowInformationProvider flowInformationProvider = computeFlowData(function, bodyExpression);
                        return semanticServices.getTypeInferrer(trace, flowInformationProvider).getFunctionReturnType(scope, function, functionDescriptor);
                    }
                });
            }
            else {
                trace.getErrorHandler().genericError(function.asElement().getNode(), "This function must either declare a return type or have a body element");
                returnType = ErrorUtils.createErrorType("No type, no body");
            }
        }

        functionDescriptor.initialize(
                receiverType,
                typeParameterDescriptors,
                valueParameterDescriptors,
                returnType);

        trace.recordDeclarationResolution(function, functionDescriptor);
        return functionDescriptor;
    }

    @NotNull
    private List<ValueParameterDescriptor> resolveValueParameters(FunctionDescriptor functionDescriptor, WritableScope parameterScope, List<JetParameter> valueParameters) {
        List<ValueParameterDescriptor> result = new ArrayList<ValueParameterDescriptor>();
        for (int i = 0, valueParametersSize = valueParameters.size(); i < valueParametersSize; i++) {
            JetParameter valueParameter = valueParameters.get(i);
            JetTypeReference typeReference = valueParameter.getTypeReference();

            ASTNode valOrVarNode = valueParameter.getValOrVarNode();
            if (valueParameter.isRef() && valOrVarNode != null) {
                trace.getErrorHandler().genericError(valOrVarNode, "'val' and 'var' are not allowed on ref-parameters");
            }

            JetType type;
            if (typeReference == null) {
                trace.getErrorHandler().genericError(valueParameter.getNode(), "A type annotation is required on a value parameter");
                type = ErrorUtils.createErrorType("Type annotation was missing");
            } else {
                type = typeResolver.resolveType(parameterScope, typeReference);
            }

            ValueParameterDescriptor valueParameterDescriptor = resolveValueParameterDescriptor(functionDescriptor, valueParameter, i, type);
            parameterScope.addVariableDescriptor(valueParameterDescriptor);
            result.add(valueParameterDescriptor);
        }
        return result;
    }

    @NotNull
    private MutableValueParameterDescriptor resolveValueParameterDescriptor(DeclarationDescriptor declarationDescriptor, JetParameter valueParameter, int index, JetType type) {
        MutableValueParameterDescriptor valueParameterDescriptor = new ValueParameterDescriptorImpl(
            declarationDescriptor,
            index,
            AnnotationResolver.INSTANCE.resolveAnnotations(valueParameter.getModifierList()),
            JetPsiUtil.safeName(valueParameter.getName()),
            valueParameter.isMutable() ? type : null,
            type,
            valueParameter.getDefaultValue() != null,
            false // TODO : varargs
    );
        // TODO : Default values???

        trace.recordDeclarationResolution(valueParameter, valueParameterDescriptor);
        return valueParameterDescriptor;
    }

    public List<TypeParameterDescriptor> resolveTypeParameters(DeclarationDescriptor containingDescriptor, WritableScope extensibleScope, List<JetTypeParameter> typeParameters) {
        // TODO : Where-clause
        List<TypeParameterDescriptor> result = new ArrayList<TypeParameterDescriptor>();
        for (int i = 0, typeParametersSize = typeParameters.size(); i < typeParametersSize; i++) {
            JetTypeParameter typeParameter = typeParameters.get(i);
            result.add(resolveTypeParameter(containingDescriptor, extensibleScope, typeParameter, i));
        }
        return result;
    }

    private TypeParameterDescriptor resolveTypeParameter(DeclarationDescriptor containingDescriptor, WritableScope extensibleScope, JetTypeParameter typeParameter, int index) {
        // TODO: other bounds from where-clause
        JetTypeReference extendsBound = typeParameter.getExtendsBound();
        JetType bound = extendsBound == null
                ? JetStandardClasses.getDefaultBound()
                : typeResolver.resolveType(extensibleScope, extendsBound);
        TypeParameterDescriptor typeParameterDescriptor = TypeParameterDescriptor.createForFurtherModification(
                containingDescriptor,
                AnnotationResolver.INSTANCE.resolveAnnotations(typeParameter.getModifierList()),
                typeParameter.getVariance(),
                JetPsiUtil.safeName(typeParameter.getName()),
                index
        );
        typeParameterDescriptor.addUpperBound(bound);
        extensibleScope.addTypeParameterDescriptor(typeParameterDescriptor);
        trace.recordDeclarationResolution(typeParameter, typeParameterDescriptor);
        return typeParameterDescriptor;
    }

    private Collection<JetType> resolveDelegationSpecifiers(JetScope extensibleScope, List<JetDelegationSpecifier> delegationSpecifiers, @NotNull TypeResolver resolver) {
        if (delegationSpecifiers.isEmpty()) {
            return Collections.emptyList();
        }
        Collection<JetType> result = new ArrayList<JetType>();
        for (JetDelegationSpecifier delegationSpecifier : delegationSpecifiers) {
            JetTypeReference typeReference = delegationSpecifier.getTypeReference();
            if (typeReference != null) {
                result.add(resolver.resolveType(extensibleScope, typeReference));
            }
            else {
                result.add(ErrorUtils.createErrorType("No type reference"));
            }
        }
        return result;
    }

    @NotNull
    public VariableDescriptor resolveLocalVariableDescriptor(@NotNull DeclarationDescriptor containingDeclaration, @NotNull JetScope scope, @NotNull JetParameter parameter) {
        JetType type = resolveParameterType(scope, parameter);
        return resolveLocalVariableDescriptor(containingDeclaration, parameter, type);
    }

    private JetType resolveParameterType(JetScope scope, JetParameter parameter) {
        JetTypeReference typeReference = parameter.getTypeReference();
        JetType type;
        if (typeReference != null) {
            type = typeResolver.resolveType(scope, typeReference);
        }
        else {
            // Error is reported by the parser
            type = ErrorUtils.createErrorType("Annotation is absent");
        }
        return type;
    }

    public VariableDescriptor resolveLocalVariableDescriptor(@NotNull DeclarationDescriptor containingDeclaration, @NotNull JetParameter parameter, @NotNull JetType type) {
        VariableDescriptor variableDescriptor = new LocalVariableDescriptor(
                containingDeclaration,
                AnnotationResolver.INSTANCE.resolveAnnotations(parameter.getModifierList()),
                JetPsiUtil.safeName(parameter.getName()),
                type,
                parameter.isMutable());
        trace.recordDeclarationResolution(parameter, variableDescriptor);
        return variableDescriptor;
    }

    @NotNull
    public VariableDescriptor resolveLocalVariableDescriptor(DeclarationDescriptor containingDeclaration, WritableScope scope, JetProperty property) {
        JetType type = getType(scope, property, false); // For a local variable the type must not be deferred

        VariableDescriptorImpl variableDescriptor = new LocalVariableDescriptor(
                containingDeclaration,
                AnnotationResolver.INSTANCE.resolveAnnotations(property.getModifierList()),
                JetPsiUtil.safeName(property.getName()),
                type,
                property.isVar());
        trace.recordDeclarationResolution(property, variableDescriptor);
        return variableDescriptor;
    }

    @NotNull
    public PropertyDescriptor resolvePropertyDescriptor(@NotNull DeclarationDescriptor containingDeclaration, @NotNull JetScope scope, JetProperty property) {
        JetType type = getType(scope, property, true);

        boolean isVar = property.isVar();
        JetModifierList modifierList = property.getModifierList();
        PropertyDescriptor propertyDescriptor = new PropertyDescriptor(
                containingDeclaration,
                AnnotationResolver.INSTANCE.resolveAnnotations(modifierList),
                resolveModifiers(modifierList, DEFAULT_MODIFIERS), // TODO : default modifiers differ in different contexts
                isVar,
                JetPsiUtil.safeName(property.getName()),
                isVar ? type : null,
                type);

        propertyDescriptor.initialize(
                resolvePropertyGetterDescriptor(scope, property, propertyDescriptor),
                resolvePropertySetterDescriptor(scope, property, propertyDescriptor));

        trace.recordDeclarationResolution(property, propertyDescriptor);
        return propertyDescriptor;
    }

    @NotNull
    private JetType getType(@NotNull final JetScope scope, @NotNull JetProperty property, boolean allowDeferred) {
        // TODO : receiver?
        JetTypeReference propertyTypeRef = property.getPropertyTypeRef();

        JetType type;
        if (propertyTypeRef == null) {
            final JetExpression initializer = property.getInitializer();
            if (initializer == null) {
                trace.getErrorHandler().genericError(property.getNode(), "This property must either have a type annotation or be initialized");
                type = ErrorUtils.createErrorType("No type, no body");
            } else {
                // TODO : ??? Fix-point here: what if we have something like "val a = foo {a.bar()}"
                // TODO : a risk of a memory leak
                LazyValue<JetType> lazyValue = new LazyValue<JetType>() {
                    @Override
                    protected JetType compute() {
                        return semanticServices.getTypeInferrer(trace, JetFlowInformationProvider.THROW_EXCEPTION).safeGetType(scope, initializer, false);
                    }
                };
                if (allowDeferred) {
                    type = new DeferredType(lazyValue);
                }
                else {
                    type = lazyValue.get();
                }
            }
        } else {
            type = typeResolver.resolveType(scope, propertyTypeRef);
        }
        return type;
    }

    @NotNull
    private MemberModifiers resolveModifiers(@Nullable JetModifierList modifierList, @NotNull MemberModifiers defaultModifiers) {
        if (modifierList == null) return defaultModifiers;
        return new MemberModifiers(
                modifierList.hasModifier(JetTokens.ABSTRACT_KEYWORD),
                modifierList.hasModifier(JetTokens.VIRTUAL_KEYWORD),
                modifierList.hasModifier(JetTokens.OVERRIDE_KEYWORD)
        );
    }

    @Nullable
    private PropertySetterDescriptor resolvePropertySetterDescriptor(@NotNull JetScope scope, @NotNull JetProperty property, @NotNull PropertyDescriptor propertyDescriptor) {
        JetPropertyAccessor setter = property.getSetter();
        if (setter != null && !property.isVar()) {
            trace.getErrorHandler().genericError(setter.asElement().getNode(), "A 'val'-property cannot have a setter");
            return null;
        }
        PropertySetterDescriptor setterDescriptor = null;
        if (setter != null) {
            List<Annotation> annotations = AnnotationResolver.INSTANCE.resolveAnnotations(setter.getModifierList());
            JetParameter parameter = setter.getParameter();

            setterDescriptor = new PropertySetterDescriptor(propertyDescriptor, annotations, setter.getBodyExpression() != null);
            if (parameter != null) {
                if (parameter.isRef()) {
                    trace.getErrorHandler().genericError(parameter.getRefNode(), "Setter parameters can not be 'ref'");
                }

                // This check is redundant: the parser does not allow a default value, but we'll keep it just in case
                JetExpression defaultValue = parameter.getDefaultValue();
                if (defaultValue != null) {
                    trace.getErrorHandler().genericError(defaultValue.getNode(), "Setter parameters can not have default values");
                }

                JetType type;
                JetTypeReference typeReference = parameter.getTypeReference();
                if (typeReference == null) {
                    type = propertyDescriptor.getInType(); // TODO : this maybe unknown at this point
                }
                else {
                    type = typeResolver.resolveType(scope, typeReference);
                    JetType inType = propertyDescriptor.getInType();
                    if (inType != null) {
                        if (!semanticServices.getTypeChecker().isSubtypeOf(type, inType)) {
                            trace.getErrorHandler().genericError(typeReference.getNode(), "Setter parameter type must be a subtype of the type of the property, i.e. " + inType);
                        }
                    }
                    else {
                        // TODO : the same check may be needed later???
                    }
                }

                MutableValueParameterDescriptor valueParameterDescriptor = resolveValueParameterDescriptor(setterDescriptor, parameter, 0, type);
                setterDescriptor.initialize(valueParameterDescriptor);
            }
            trace.recordDeclarationResolution(setter, setterDescriptor);
        }
        return setterDescriptor;
    }

    @Nullable
    private PropertyGetterDescriptor resolvePropertyGetterDescriptor(@NotNull JetScope scope, @NotNull JetProperty property, @NotNull PropertyDescriptor propertyDescriptor) {
        PropertyGetterDescriptor getterDescriptor = null;
        JetPropertyAccessor getter = property.getGetter();
        if (getter != null) {
            List<Annotation> annotations = AnnotationResolver.INSTANCE.resolveAnnotations(getter.getModifierList());

            JetType returnType = null;
            JetTypeReference returnTypeReference = getter.getReturnTypeReference();
            if (returnTypeReference != null) {
                returnType = typeResolver.resolveType(scope, returnTypeReference);
            }

            getterDescriptor = new PropertyGetterDescriptor(propertyDescriptor, annotations, returnType, getter.getBodyExpression() != null);
            trace.recordDeclarationResolution(getter, getterDescriptor);
        }
        return getterDescriptor;
    }

    @NotNull
    public ConstructorDescriptor resolveSecondaryConstructorDescriptor(@NotNull JetScope scope, @NotNull ClassDescriptor classDescriptor, @NotNull JetConstructor constructor) {
        return createConstructorDescriptor(scope, classDescriptor, false, constructor.getModifierList(), constructor, constructor.getParameters());
    }

    @NotNull
    private ConstructorDescriptor createConstructorDescriptor(
            @NotNull JetScope scope,
            @NotNull ClassDescriptor classDescriptor,
            boolean isPrimary,
            @Nullable JetModifierList modifierList,
            @NotNull JetDeclaration declarationToTrace,
            @NotNull List<JetParameter> valueParameters) {
        ConstructorDescriptorImpl constructorDescriptor = new ConstructorDescriptorImpl(
                classDescriptor,
                AnnotationResolver.INSTANCE.resolveAnnotations(modifierList),
                isPrimary
        );
        trace.recordDeclarationResolution(declarationToTrace, constructorDescriptor);
        return constructorDescriptor.initialize(
                resolveValueParameters(
                        constructorDescriptor,
                        new WritableScopeImpl(scope, classDescriptor, trace.getErrorHandler()),
                        valueParameters));
    }

    @Nullable
    public ConstructorDescriptor resolvePrimaryConstructorDescriptor(@NotNull JetScope scope, @NotNull ClassDescriptor classDescriptor, @NotNull JetClass classElement) {
        if (!classElement.hasPrimaryConstructor()) return null;
        return createConstructorDescriptor(
                scope,
                classDescriptor,
                true,
                classElement.getPrimaryConstructorModifierList(),
                classElement,
                classElement.getPrimaryConstructorParameters());
    }

    @NotNull
    public PropertyDescriptor resolvePrimaryConstructorParameterToAProperty(
            @NotNull ClassDescriptor classDescriptor,
            @NotNull JetScope scope,
            @NotNull JetParameter parameter) {
        JetType type = resolveParameterType(scope, parameter);
        String name = parameter.getName();
        boolean isMutable = parameter.isMutable();
        JetModifierList modifierList = parameter.getModifierList();

        if (modifierList != null) {
            ASTNode abstractNode = modifierList.getModifierNode(JetTokens.ABSTRACT_KEYWORD);
            if (abstractNode != null) {
                trace.getErrorHandler().genericError(abstractNode, "This property cannot be declared abstract");
            }
        }

        PropertyDescriptor propertyDescriptor = new PropertyDescriptor(
                classDescriptor,
                AnnotationResolver.INSTANCE.resolveAnnotations(modifierList),
                resolveModifiers(modifierList, DEFAULT_MODIFIERS),
                isMutable,
                name == null ? "<no name>" : name,
                isMutable ? type : null,
                type);
        propertyDescriptor.initialize(null, null);
        trace.recordValueParameterAsPropertyResolution(parameter, propertyDescriptor);
        return propertyDescriptor;
    }

    public void checkBounds(JetTypeReference typeReference, JetType type) {
        if (ErrorUtils.isErrorType(type)) return;

        JetTypeElement typeElement = typeReference.getTypeElement();
        if (typeElement == null) return;

        List<TypeParameterDescriptor> parameters = type.getConstructor().getParameters();
        List<TypeProjection> arguments = type.getArguments();
        assert parameters.size() == arguments.size();

        List<JetTypeReference> typeReferences = typeElement.getTypeArgumentsAsTypes();
        assert typeReferences.size() == arguments.size() : typeElement.getText();

        TypeSubstitutor substitutor = TypeSubstitutor.create(type);
        for (int i = 0, projectionsSize = typeReferences.size(); i < projectionsSize; i++) {
            JetTypeReference argumentTypeReference = typeReferences.get(i);

            if (argumentTypeReference == null) continue;

            JetType typeArgument = arguments.get(i).getType();
            checkBounds(argumentTypeReference, typeArgument);

            TypeParameterDescriptor typeParameterDescriptor = parameters.get(i);
            checkBounds(argumentTypeReference, typeArgument, typeParameterDescriptor, substitutor);
        }
    }

    public void checkBounds(
            @NotNull JetTypeReference argumentTypeReference,
            @NotNull JetType typeArgument,
            @NotNull TypeParameterDescriptor typeParameterDescriptor,
            @NotNull TypeSubstitutor substitutor) {
        for (JetType bound : typeParameterDescriptor.getUpperBounds()) {
            JetType substitutedBound = substitutor.safeSubstitute(bound, Variance.INVARIANT);
            if (!semanticServices.getTypeChecker().isSubtypeOf(typeArgument, substitutedBound)) {
                trace.getErrorHandler().genericError(argumentTypeReference.getNode(), "An upper bound " + substitutedBound + " is violated"); // TODO : Message
            }
        }
    }

    public JetFlowInformationProvider computeFlowData(@NotNull JetElement declaration, @NotNull JetExpression bodyExpression) {
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
                trace.getErrorHandler().genericError(instruction.getElement().getNode(), "Unsupported by control-flow builder " + instruction.getElement());
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
                    trace.getErrorHandler().genericError(elementInstruction.getElement().getNode(), "Unsupported by control-flow builder " + elementInstruction.getElement());
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
