package org.jetbrains.jet.lang.resolve;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.JetSemanticServices;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.types.*;
import org.jetbrains.jet.lexer.JetTokens;

import java.util.*;

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
                classElement.getName());
        WritableScope parameterScope = new WritableScope(scope, classDescriptor);

        // This call has side-effects on the parameterScope (fills it in)
        List<TypeParameterDescriptor> typeParameters
                = resolveTypeParameters(classDescriptor, parameterScope, classElement.getTypeParameters());

        List<JetDelegationSpecifier> delegationSpecifiers = classElement.getDelegationSpecifiers();
        // TODO : assuming that the hierarchy is acyclic
        Collection<? extends Type> superclasses = delegationSpecifiers.isEmpty()
                ? Collections.singleton(JetStandardClasses.getAnyType())
                : resolveTypes(parameterScope, delegationSpecifiers);
        boolean open = classElement.hasModifier(JetTokens.OPEN_KEYWORD);
        WritableScope members = resolveMembers(classDescriptor, classElement, typeParameters, scope, parameterScope, superclasses);

        return classDescriptor.initialize(
                !open,
                typeParameters,
                superclasses,
                members
        );
    }

    public void resolveMutableClassDescriptor(@NotNull JetScope scope, @NotNull JetClass classElement, @NotNull MutableClassDescriptor descriptor) {
        descriptor.setName(classElement.getName());

        WritableScope parameterScope = descriptor.getUnsubstitutedMemberScope();

        // This call has side-effects on the parameterScope (fills it in)
        List<TypeParameterDescriptor> typeParameters
                = resolveTypeParameters(descriptor, parameterScope, classElement.getTypeParameters());

        List<JetDelegationSpecifier> delegationSpecifiers = classElement.getDelegationSpecifiers();
        // TODO : assuming that the hierarchy is acyclic
        Collection<? extends Type> superclasses = delegationSpecifiers.isEmpty()
                ? Collections.singleton(JetStandardClasses.getAnyType())
                : resolveTypes(parameterScope, delegationSpecifiers);
        boolean open = classElement.hasModifier(JetTokens.OPEN_KEYWORD);

        descriptor.setTypeConstructor(
                new TypeConstructor(
                        AttributeResolver.INSTANCE.resolveAttributes(classElement.getModifierList()),
                        !open,
                        classElement.getName(),
                        typeParameters,
                        superclasses)
        );

        trace.recordDeclarationResolution(classElement, descriptor);
    }

    private WritableScope resolveMembers(
            final ClassDescriptor classDescriptor,
            final JetClass classElement,
            List<TypeParameterDescriptor> typeParameters,
            final JetScope outerScope,
            final JetScope typeParameterScope,
            final Collection<? extends Type> supertypes) {

        final WritableScope memberDeclarations = new WritableScope(typeParameterScope, classDescriptor);

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
                function.getName()
        );
        WritableScope parameterScope = new WritableScope(scope, functionDescriptor);

        // The two calls below have side-effects on parameterScope
        List<TypeParameterDescriptor> typeParameterDescriptors = resolveTypeParameters(functionDescriptor, parameterScope, function.getTypeParameters());
        List<ValueParameterDescriptor> valueParameterDescriptors = resolveValueParameters(functionDescriptor, parameterScope, function.getValueParameters());

        Type returnType;
        JetTypeReference returnTypeRef = function.getReturnTypeRef();
        if (returnTypeRef != null) {
            returnType = typeResolver.resolveType(parameterScope, returnTypeRef);
            // TODO : CHeck type of body
        } else {
            JetExpression bodyExpression = function.getBodyExpression();
            assert bodyExpression != null : "No type, no body"; // TODO
            // TODO : Recursion possible
            returnType = semanticServices.getTypeInferrer().safeGetType(parameterScope, bodyExpression, function.hasBlockBody());
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

            Type type;
            if (typeReference == null) {
                semanticServices.getErrorHandler().structuralError(valueParameter.getNode(), "A type annotation is required on a value parameter " + valueParameter.getName());
                type = ErrorType.createErrorType("Type annotation was missing");
            } else {
                type = typeResolver.resolveType(parameterScope, typeReference);
            }
            ValueParameterDescriptor valueParameterDescriptor = new ValueParameterDescriptorImpl(
                    functionDescriptor,
                    i,
                    AttributeResolver.INSTANCE.resolveAttributes(valueParameter.getModifierList()),
                    valueParameter.getName(),
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
        // TODO : When-clause
        List<TypeParameterDescriptor> result = new ArrayList<TypeParameterDescriptor>();
        for (JetTypeParameter typeParameter : typeParameters) {
            result.add(resolveTypeParameter(containingDescriptor, extensibleScope, typeParameter));
        }
        return result;
    }

    private TypeParameterDescriptor resolveTypeParameter(DeclarationDescriptor containingDescriptor, WritableScope extensibleScope, JetTypeParameter typeParameter) {
        JetTypeReference extendsBound = typeParameter.getExtendsBound();
        TypeParameterDescriptor typeParameterDescriptor = new TypeParameterDescriptor(
                containingDescriptor,
                AttributeResolver.INSTANCE.resolveAttributes(typeParameter.getModifierList()),
                typeParameter.getVariance(),
                typeParameter.getName(),
                extendsBound == null
                        ? Collections.<Type>singleton(JetStandardClasses.getAnyType())
                        : Collections.singleton(typeResolver.resolveType(extensibleScope, extendsBound))
        );
        extensibleScope.addTypeParameterDescriptor(typeParameterDescriptor);
        trace.recordDeclarationResolution(typeParameter, typeParameterDescriptor);
        return typeParameterDescriptor;
    }

    public Collection<? extends Type> resolveTypes(WritableScope extensibleScope, List<JetDelegationSpecifier> delegationSpecifiers) {
        if (delegationSpecifiers.isEmpty()) {
            return Collections.emptyList();
        }
        Collection<Type> result = new ArrayList<Type>();
        for (JetDelegationSpecifier delegationSpecifier : delegationSpecifiers) {
            result.add(resolveType(extensibleScope, delegationSpecifier));
        }
        return result;
    }

    private Type resolveType(JetScope scope, JetDelegationSpecifier delegationSpecifier) {
        JetTypeReference typeReference = delegationSpecifier.getTypeReference(); // TODO : make it not null
        return typeResolver.resolveType(scope, typeReference);
    }

    @NotNull
    public PropertyDescriptor resolvePropertyDescriptor(@NotNull DeclarationDescriptor containingDeclaration, @NotNull JetScope scope, @NotNull JetParameter parameter) {
        return new PropertyDescriptorImpl(
                containingDeclaration,
                AttributeResolver.INSTANCE.resolveAttributes(parameter.getModifierList()),
                parameter.getName(),
                typeResolver.resolveType(scope, parameter.getTypeReference()));
    }

    public PropertyDescriptor resolvePropertyDescriptor(@NotNull DeclarationDescriptor containingDeclaration, @NotNull JetScope scope, JetProperty property) {
        // TODO : receiver?
        JetTypeReference propertyTypeRef = property.getPropertyTypeRef();

        Type type;
        if (propertyTypeRef == null) {
            JetExpression initializer = property.getInitializer();
            assert initializer != null;
            // TODO : ??? Fix-point here: what if we have something like "val a = foo {a.bar()}"
            type = semanticServices.getTypeInferrer().getType(scope, initializer, false);
        } else {
            type = typeResolver.resolveType(scope, propertyTypeRef);
        }

        return new PropertyDescriptorImpl(
                containingDeclaration,
                AttributeResolver.INSTANCE.resolveAttributes(property.getModifierList()),
                property.getName(),
                type);
    }
}
