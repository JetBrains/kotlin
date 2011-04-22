package org.jetbrains.jet.lang.resolve;

import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.JetSemanticServices;
import org.jetbrains.jet.lang.cfg.JetFlowInformationProvider;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.types.*;
import org.jetbrains.jet.lexer.JetTokens;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @author abreslav
 */
public class ClassDescriptorResolver {

    private static final MemberModifiers DEFAULT_MODIFIERS = new MemberModifiers(false, false, false);

    private final JetSemanticServices semanticServices;
    private final TypeResolver typeResolver;
    private final BindingTrace trace;

    public ClassDescriptorResolver(JetSemanticServices semanticServices, BindingTrace trace) {
        this.semanticServices = semanticServices;
        this.typeResolver = new TypeResolver(trace, semanticServices);
        this.trace = trace;
    }

    @Nullable
    public ClassDescriptor resolveClassDescriptor(@NotNull JetScope scope, @NotNull JetClass classElement) {
        ClassDescriptorImpl classDescriptor = new ClassDescriptorImpl(
                scope.getContainingDeclaration(),
                AttributeResolver.INSTANCE.resolveAttributes(classElement.getModifierList()),
                JetPsiUtil.safeName(classElement.getName()));

        trace.recordDeclarationResolution(classElement, classDescriptor);

        WritableScope parameterScope = semanticServices.createWritableScope(scope, classDescriptor);

        // This call has side-effects on the parameterScope (fills it in)
        List<TypeParameterDescriptor> typeParameters
                = resolveTypeParameters(classDescriptor, parameterScope, classElement.getTypeParameters());

        List<JetDelegationSpecifier> delegationSpecifiers = classElement.getDelegationSpecifiers();
        // TODO : assuming that the hierarchy is acyclic
        Collection<? extends JetType> supertypes = delegationSpecifiers.isEmpty()
                ? Collections.singleton(JetStandardClasses.getAnyType())
                : resolveDelegationSpecifiers(parameterScope, delegationSpecifiers);
        boolean open = classElement.hasModifier(JetTokens.OPEN_KEYWORD);
        WritableScope members = resolveMembers(classDescriptor, classElement, typeParameters, scope, parameterScope, supertypes);

        WritableFunctionGroup constructors = new WritableFunctionGroup("<init>");
        for (JetConstructor constructor : classElement.getSecondaryConstructors()) {
            constructors.addFunction(resolveConstructorDescriptor(members, classDescriptor, constructor, false));
        }
        ConstructorDescriptor primaryConstructorDescriptor = resolvePrimaryConstructor(scope, classDescriptor, classElement);
        if (primaryConstructorDescriptor != null) {
            constructors.addFunction(primaryConstructorDescriptor);
        }
        return classDescriptor.initialize(
                !open,
                typeParameters,
                supertypes,
                members,
                constructors
        );
    }

    public void resolveMutableClassDescriptor(@NotNull JetScope scope, @NotNull JetClass classElement, @NotNull MutableClassDescriptor descriptor) {
        descriptor.setName(JetPsiUtil.safeName(classElement.getName()));
        descriptor.getClassHeaderScope().addLabeledDeclaration(descriptor);

        WritableScope parameterScope = descriptor.getClassHeaderScope();

        // This call has side-effects on the parameterScope (fills it in)
        List<TypeParameterDescriptor> typeParameters
                = resolveTypeParameters(descriptor, parameterScope, classElement.getTypeParameters());

        boolean open = classElement.hasModifier(JetTokens.OPEN_KEYWORD);
        List<JetType> supertypes = new ArrayList<JetType>();
        TypeConstructorImpl typeConstructor = new TypeConstructorImpl(
                descriptor,
                AttributeResolver.INSTANCE.resolveAttributes(classElement.getModifierList()),
                !open,
                JetPsiUtil.safeName(classElement.getName()),
                typeParameters,
                supertypes);
        descriptor.setTypeConstructor(
                typeConstructor
        );

        List<JetDelegationSpecifier> delegationSpecifiers = classElement.getDelegationSpecifiers();
        // TODO : assuming that the hierarchy is acyclic
        Collection<? extends JetType> superclasses = delegationSpecifiers.isEmpty()
                ? Collections.singleton(JetStandardClasses.getAnyType())
                : resolveDelegationSpecifiers(parameterScope, delegationSpecifiers);

        // TODO : UGLY HACK
        supertypes.addAll(superclasses);


        // TODO : importing may be a bad idea
        for (JetType supertype : superclasses) {
            assert supertype != null : classElement.getName();
            parameterScope.importScope(supertype.getMemberScope());
        }

        descriptor.getClassHeaderScope().setThisType(descriptor.getDefaultType());

        trace.recordDeclarationResolution(classElement, descriptor);
    }

    private WritableScope resolveMembers(
            final ClassDescriptor classDescriptor,
            final JetClass classElement,
            List<TypeParameterDescriptor> typeParameters,
            final JetScope outerScope,
            final JetScope typeParameterScope,
            final Collection<? extends JetType> supertypes) {

        final WritableScope memberDeclarations = semanticServices.createWritableScope(typeParameterScope, classDescriptor);

        List<JetDeclaration> declarations = classElement.getDeclarations();
        for (JetDeclaration declaration : declarations) {
            declaration.accept(new JetVisitor() {
                @Override
                public void visitProperty(JetProperty property) {
                    if (property.getPropertyTypeRef() != null) {
                        memberDeclarations.addVariableDescriptor(resolvePropertyDescriptor(classDescriptor, typeParameterScope, property));
                    } else {
                        // TODO : Caution: a cyclic dependency possible
                        throw new UnsupportedOperationException();
                    }
                }

                @Override
                public void visitFunction(JetFunction function) {
                    if (function.getReturnTypeRef() != null) {
                        memberDeclarations.addFunctionDescriptor(resolveFunctionDescriptor(classDescriptor, typeParameterScope, function));
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

        return memberDeclarations;
    }

    @NotNull
    public FunctionDescriptor resolveFunctionDescriptor(DeclarationDescriptor containingDescriptor, JetScope scope, JetFunction function) {
        FunctionDescriptorImpl functionDescriptor = new FunctionDescriptorImpl(
                containingDescriptor,
                AttributeResolver.INSTANCE.resolveAttributes(function.getModifierList()),
                JetPsiUtil.safeName(function.getName())
        );
        WritableScope innerScope = semanticServices.createWritableScope(scope, functionDescriptor);
        innerScope.addLabeledDeclaration(functionDescriptor);

        // The two calls below have side-effects on parameterScope
        List<TypeParameterDescriptor> typeParameterDescriptors = resolveTypeParameters(functionDescriptor, innerScope, function.getTypeParameters());
        List<ValueParameterDescriptor> valueParameterDescriptors = resolveValueParameters(functionDescriptor, innerScope, function.getValueParameters());

        JetTypeReference returnTypeRef = function.getReturnTypeRef();
        JetType returnType = null;
        if (returnTypeRef != null) {
            returnType = typeResolver.resolveType(innerScope, returnTypeRef);
        }

        functionDescriptor.initialize(
                typeParameterDescriptors,
                valueParameterDescriptors,
                returnType);

        trace.recordDeclarationResolution(function, functionDescriptor);
        return functionDescriptor;
    }

    @NotNull
    private List<ValueParameterDescriptor> resolveValueParameters(MutableFunctionDescriptor functionDescriptor, WritableScope parameterScope, List<JetParameter> valueParameters) {
        List<ValueParameterDescriptor> result = new ArrayList<ValueParameterDescriptor>();
        for (int i = 0, valueParametersSize = valueParameters.size(); i < valueParametersSize; i++) {
            JetParameter valueParameter = valueParameters.get(i);
            JetTypeReference typeReference = valueParameter.getTypeReference();

            ASTNode valOrVarNode = valueParameter.getValOrVarNode();
            if (valueParameter.isRef() && valOrVarNode != null) {
                semanticServices.getErrorHandler().genericError(valOrVarNode, "'val' and 'var' are not allowed on ref-parameters");
            }

            JetType type;
            if (typeReference == null) {
                semanticServices.getErrorHandler().genericError(valueParameter.getNode(), "A type annotation is required on a value parameter");
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
            AttributeResolver.INSTANCE.resolveAttributes(valueParameter.getModifierList()),
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
        for (JetTypeParameter typeParameter : typeParameters) {
            result.add(resolveTypeParameter(containingDescriptor, extensibleScope, typeParameter));
        }
        return result;
    }

    private TypeParameterDescriptor resolveTypeParameter(DeclarationDescriptor containingDescriptor, WritableScope extensibleScope, JetTypeParameter typeParameter) {
        // TODO: other bounds from where-clause
        JetTypeReference extendsBound = typeParameter.getExtendsBound();
        JetType bound = extendsBound == null
                ? JetStandardClasses.getDefaultBound()
                : typeResolver.resolveType(extensibleScope, extendsBound);
        TypeParameterDescriptor typeParameterDescriptor = new TypeParameterDescriptor(
                containingDescriptor,
                AttributeResolver.INSTANCE.resolveAttributes(typeParameter.getModifierList()),
                typeParameter.getVariance(),
                JetPsiUtil.safeName(typeParameter.getName()),
                Collections.singleton(bound),
                bound
        );
        extensibleScope.addTypeParameterDescriptor(typeParameterDescriptor);
        trace.recordDeclarationResolution(typeParameter, typeParameterDescriptor);
        return typeParameterDescriptor;
    }

    public Collection<? extends JetType> resolveDelegationSpecifiers(WritableScope extensibleScope, List<JetDelegationSpecifier> delegationSpecifiers) {
        if (delegationSpecifiers.isEmpty()) {
            return Collections.emptyList();
        }
        Collection<JetType> result = new ArrayList<JetType>();
        for (JetDelegationSpecifier delegationSpecifier : delegationSpecifiers) {
            JetTypeReference typeReference = delegationSpecifier.getTypeReference();
            if (typeReference != null) {
                result.add(typeResolver.resolveType(extensibleScope, typeReference));
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
                AttributeResolver.INSTANCE.resolveAttributes(parameter.getModifierList()),
                JetPsiUtil.safeName(parameter.getName()),
                type,
                parameter.isMutable());
        trace.recordDeclarationResolution(parameter, variableDescriptor);
        return variableDescriptor;
    }

    public VariableDescriptor resolveLocalVariableDescriptor(DeclarationDescriptor containingDeclaration, WritableScope scope, JetProperty property) {
        JetType type = getType(scope, property);

        VariableDescriptorImpl propertyDescriptor = new LocalVariableDescriptor(
                containingDeclaration,
                AttributeResolver.INSTANCE.resolveAttributes(property.getModifierList()),
                JetPsiUtil.safeName(property.getName()),
                type,
                property.isVar());
        trace.recordDeclarationResolution(property, propertyDescriptor);
        return propertyDescriptor;
    }

    public PropertyDescriptor resolvePropertyDescriptor(@NotNull DeclarationDescriptor containingDeclaration, @NotNull JetScope scope, JetProperty property) {
        JetType type = getType(scope, property);

        boolean isVar = property.isVar();
        JetModifierList modifierList = property.getModifierList();
        PropertyDescriptor propertyDescriptor = new PropertyDescriptor(
                containingDeclaration,
                AttributeResolver.INSTANCE.resolveAttributes(modifierList),
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
            semanticServices.getErrorHandler().genericError(setter.asElement().getNode(), "A 'val'-property cannot have a setter");
            return null;
        }
        PropertySetterDescriptor setterDescriptor = null;
        if (setter != null) {
            List<Attribute> attributes = AttributeResolver.INSTANCE.resolveAttributes(setter.getModifierList());
            JetParameter parameter = setter.getParameter();

            setterDescriptor = new PropertySetterDescriptor(propertyDescriptor, attributes, setter.getBodyExpression() != null);
            if (parameter != null) {
                if (parameter.isRef()) {
                    semanticServices.getErrorHandler().genericError(parameter.getRefNode(), "Setter parameters can not be 'ref'");
                }

                // This check is redundant: the parser does not allow a default value, but we'll keep it just in case
                JetExpression defaultValue = parameter.getDefaultValue();
                if (defaultValue != null) {
                    semanticServices.getErrorHandler().genericError(defaultValue.getNode(), "Setter parameters can not have default values");
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
                            semanticServices.getErrorHandler().genericError(typeReference.getNode(), "Setter parameter type must be a subtype of the type of the property, i.e. " + inType);
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
            List<Attribute> attributes = AttributeResolver.INSTANCE.resolveAttributes(getter.getModifierList());

            JetType returnType = null;
            JetTypeReference returnTypeReference = getter.getReturnTypeReference();
            if (returnTypeReference != null) {
                returnType = typeResolver.resolveType(scope, returnTypeReference);
            }

            getterDescriptor = new PropertyGetterDescriptor(propertyDescriptor, attributes, returnType, getter.getBodyExpression() != null);
            trace.recordDeclarationResolution(getter, getterDescriptor);
        }
        return getterDescriptor;
    }

    @NotNull
    private JetType getType(@NotNull JetScope scope, @NotNull JetProperty property) {
        // TODO : receiver?
        JetTypeReference propertyTypeRef = property.getPropertyTypeRef();

        JetType type;
        if (propertyTypeRef == null) {
            JetExpression initializer = property.getInitializer();
            if (initializer == null) {
                semanticServices.getErrorHandler().genericError(property.getNode(), "This property must either have a type annotation or be initialized");
                type = ErrorUtils.createErrorType("No type, no body");
            } else {
                // TODO : ??? Fix-point here: what if we have something like "val a = foo {a.bar()}"
                type = semanticServices.getTypeInferrer(trace, JetFlowInformationProvider.THROW_EXCEPTION).safeGetType(scope, initializer, false);
            }
        } else {
            type = typeResolver.resolveType(scope, propertyTypeRef);
        }
        return type;
    }

    @NotNull
    public ConstructorDescriptor resolveConstructorDescriptor(@NotNull JetScope scope, ClassDescriptor classDescriptor, JetConstructor constructor, boolean isPrimary) {
        return createConstructorDescriptor(scope, classDescriptor, isPrimary, constructor.getModifierList(), constructor, constructor.getParameters());
    }

    @NotNull
    private ConstructorDescriptor createConstructorDescriptor(JetScope scope, ClassDescriptor classDescriptor, boolean isPrimary, JetModifierList modifierList, JetDeclaration declarationToTrace, List<JetParameter> valueParameters) {
        ConstructorDescriptorImpl constructorDescriptor = new ConstructorDescriptorImpl(
                classDescriptor,
                AttributeResolver.INSTANCE.resolveAttributes(modifierList),
                isPrimary
        );
        trace.recordDeclarationResolution(declarationToTrace, constructorDescriptor);
        return constructorDescriptor.initialize(
                resolveValueParameters(
                        constructorDescriptor,
                        semanticServices.createWritableScope(scope, classDescriptor),
                        valueParameters));
    }

    @Nullable
    public ConstructorDescriptor resolvePrimaryConstructor(@NotNull JetScope scope, @NotNull ClassDescriptor classDescriptor, @NotNull JetClass classElement) {
        JetParameterList primaryConstructorParameterList = classElement.getPrimaryConstructorParameterList();
        if (primaryConstructorParameterList != null) {
            return createConstructorDescriptor(
                    scope,
                    classDescriptor,
                    true,
                    classElement.getModifierList(), // TODO
                    classElement,
                    primaryConstructorParameterList.getParameters());
        }
        else {
            List<JetConstructor> secondaryConstructors = classElement.getSecondaryConstructors();
            if (secondaryConstructors.isEmpty()) {
                return createConstructorDescriptor(
                        scope,
                        classDescriptor,
                        true,
                        classElement.getModifierList(), // TODO
                        classElement,
                        Collections.<JetParameter>emptyList());
            }
            else return null;
        }
    }

    public VariableDescriptor resolvePrimaryConstructorParameterToAProperty(
            @NotNull ClassDescriptor classDescriptor,
            @NotNull JetScope scope,
            @NotNull JetParameter parameter) {
        JetType type = resolveParameterType(scope, parameter);
        String name = parameter.getName();
        boolean isMutable = parameter.isMutable();
        JetModifierList modifierList = parameter.getModifierList();
        PropertyDescriptor propertyDescriptor = new PropertyDescriptor(
                classDescriptor,
                AttributeResolver.INSTANCE.resolveAttributes(modifierList),
                resolveModifiers(modifierList, DEFAULT_MODIFIERS),
                isMutable,
                name == null ? "<no name>" : name,
                isMutable ? type : null,
                type);
        propertyDescriptor.initialize(null, null);
        trace.recordDeclarationResolution(parameter, propertyDescriptor);
        return propertyDescriptor;
    }

}
