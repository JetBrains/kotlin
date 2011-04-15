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
        Collection<? extends JetType> superclasses = delegationSpecifiers.isEmpty()
                ? Collections.singleton(JetStandardClasses.getAnyType())
                : resolveTypes(parameterScope, delegationSpecifiers);
        boolean open = classElement.hasModifier(JetTokens.OPEN_KEYWORD);
        WritableScope members = resolveMembers(classDescriptor, classElement, typeParameters, scope, parameterScope, superclasses);

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
                superclasses,
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
                : resolveTypes(parameterScope, delegationSpecifiers);

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
                        memberDeclarations.addPropertyDescriptor(resolvePropertyDescriptor(classDescriptor, typeParameterScope, property));
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

    private List<ValueParameterDescriptor> resolveValueParameters(FunctionDescriptorImpl functionDescriptor, WritableScope parameterScope, List<JetParameter> valueParameters) {
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
            ValueParameterDescriptor valueParameterDescriptor = new ValueParameterDescriptorImpl(
                    functionDescriptor,
                    i,
                    AttributeResolver.INSTANCE.resolveAttributes(valueParameter.getModifierList()),
                    JetPsiUtil.safeName(valueParameter.getName()),
                    valueParameter.isMutable() ? type : null,
                    type,
                    valueParameter.getDefaultValue() != null,
                    false // TODO : varargs
            );
            // TODO : Default values???

            result.add(valueParameterDescriptor);
            trace.recordDeclarationResolution(valueParameter, valueParameterDescriptor);
            parameterScope.addPropertyDescriptor(valueParameterDescriptor);
        }
        return result;
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

    public Collection<? extends JetType> resolveTypes(WritableScope extensibleScope, List<JetDelegationSpecifier> delegationSpecifiers) {
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
    public PropertyDescriptor resolveValueParameterDescriptor(@NotNull DeclarationDescriptor containingDeclaration, @NotNull JetScope scope, @NotNull JetParameter parameter) {
        JetType type = getParameterType(scope, parameter);
        return resolveValueParameterDescriptor(containingDeclaration, parameter, type);
    }

    private JetType getParameterType(JetScope scope, JetParameter parameter) {
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

    public PropertyDescriptor resolveValueParameterDescriptor(@NotNull DeclarationDescriptor containingDeclaration, @NotNull JetParameter parameter, @NotNull JetType type) {
        PropertyDescriptor propertyDescriptor = new PropertyDescriptorImpl(
                containingDeclaration,
                AttributeResolver.INSTANCE.resolveAttributes(parameter.getModifierList()),
                JetPsiUtil.safeName(parameter.getName()),
                parameter.isMutable() ? null : type,
                type);
        trace.recordDeclarationResolution(parameter, propertyDescriptor);
        return propertyDescriptor;
    }

    public PropertyDescriptor resolvePropertyDescriptor(@NotNull DeclarationDescriptor containingDeclaration, @NotNull JetScope scope, JetProperty property) {
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
                type = semanticServices.getTypeInferrer(trace, JetFlowInformationProvider.THROW_EXCEPTION).getType(scope, initializer, false);
            }
        } else {
            type = typeResolver.resolveType(scope, propertyTypeRef);
        }

        PropertyDescriptorImpl propertyDescriptor = new PropertyDescriptorImpl(
                containingDeclaration,
                AttributeResolver.INSTANCE.resolveAttributes(property.getModifierList()),
                JetPsiUtil.safeName(property.getName()),
                property.isVar() ? type : null,
                type);
        trace.recordDeclarationResolution(property, propertyDescriptor);
        return propertyDescriptor;
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

    public PropertyDescriptor resolvePrimaryConstructorParameterToAProperty(
            @NotNull ClassDescriptor classDescriptor,
            @NotNull JetScope scope,
            @NotNull JetParameter parameter) {
        JetType type = getParameterType(scope, parameter);
        String name = parameter.getName();
        PropertyDescriptorImpl propertyDescriptor = new PropertyDescriptorImpl(
                classDescriptor,
                AttributeResolver.INSTANCE.resolveAttributes(parameter.getModifierList()),
                name == null ? "<no name>" : name,
                parameter.isMutable() ? type : null,
                type);
        trace.recordDeclarationResolution(parameter, propertyDescriptor);
        return propertyDescriptor;
    }

}
