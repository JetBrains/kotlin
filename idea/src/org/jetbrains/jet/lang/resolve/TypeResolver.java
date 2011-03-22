package org.jetbrains.jet.lang.resolve;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.JetSemanticServices;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.types.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author abreslav
 */
public class TypeResolver {

    private final BindingTrace trace;
    private final JetSemanticServices semanticServices;

    public TypeResolver(BindingTrace trace, JetSemanticServices semanticServices) {
        this.trace = trace;
        this.semanticServices = semanticServices;
    }

    @NotNull
    public JetType resolveType(@NotNull final JetScope scope, @NotNull final JetTypeReference typeReference) {
        final List<Attribute> attributes = AttributeResolver.INSTANCE.resolveAttributes(typeReference.getAttributes());

        JetTypeElement typeElement = typeReference.getTypeElement();
        JetType type = resolveTypeElement(scope, attributes, typeElement, false);
        trace.recordTypeResolution(typeReference, type);
        return type;
    }

    @NotNull
    private JetType resolveTypeElement(final JetScope scope, final List<Attribute> attributes, JetTypeElement typeElement, final boolean nullable) {
        final JetType[] result = new JetType[1];
        if (typeElement != null) {
            typeElement.accept(new JetVisitor() {
                @Override
                public void visitUserType(JetUserType type) {
                    JetSimpleNameExpression referenceExpression = type.getReferenceExpression();
                    String referencedName = type.getReferencedName();
                    if (referenceExpression == null || referencedName == null) {
                        return;
                    }
                    ClassDescriptor classDescriptor = resolveClass(scope, type);
                    if (classDescriptor != null) {
                        trace.recordReferenceResolution(referenceExpression, classDescriptor);
                        TypeConstructor typeConstructor = classDescriptor.getTypeConstructor();
                        List<TypeProjection> arguments = resolveTypeProjections(scope, typeConstructor, type.getTypeArguments());
                        if (arguments.size() != typeConstructor.getParameters().size()) {
                            semanticServices.getErrorHandler().genericError(type.getNode(), typeConstructor.getParameters().size() + " type parameters expected"); // TODO : message
                        } else {
                            result[0] = new JetTypeImpl(
                                    attributes,
                                    typeConstructor,
                                    nullable,
                                    arguments,
                                    classDescriptor.getMemberScope(arguments)
                            );
                        }
                    }
                    else if (type.getTypeArguments().isEmpty()) {
                        TypeParameterDescriptor typeParameterDescriptor = scope.getTypeParameter(referencedName);
                        if (typeParameterDescriptor != null) {
                            trace.recordReferenceResolution(referenceExpression, typeParameterDescriptor);
                            result[0] = new JetTypeImpl(
                                    attributes,
                                    typeParameterDescriptor.getTypeConstructor(),
                                    nullable || hasNullableBound(typeParameterDescriptor),
                                    Collections.<TypeProjection>emptyList(),
                                    // TODO : joint domain
                                    JetStandardClasses.STUB
                            );
                        } else {
                            semanticServices.getErrorHandler().unresolvedReference(referenceExpression);
                        }
                    }
                    else {
                        semanticServices.getErrorHandler().unresolvedReference(referenceExpression);
                    }
                }

                @Override
                public void visitNullableType(JetNullableType nullableType) {
                    result[0] = resolveTypeElement(scope, attributes, nullableType.getInnerType(), true);
                }

                @Override
                public void visitTupleType(JetTupleType type) {
                    // TODO labels
                    result[0] = JetStandardClasses.getTupleType(resolveTypes(scope, type.getComponentTypeRefs()));
                }

                @Override
                public void visitFunctionType(JetFunctionType type) {
                    JetTypeReference receiverTypeRef = type.getReceiverTypeRef();
                    JetType receiverType = receiverTypeRef == null ? null : resolveType(scope, receiverTypeRef);

                    List<JetType> parameterTypes = new ArrayList<JetType>();
                    for (JetParameter parameter : type.getParameters()) {
                        parameterTypes.add(resolveType(scope, parameter.getTypeReference()));
                    }

                    JetType returnType = resolveType(scope, type.getReturnTypeRef());

                    result[0] = JetStandardClasses.getFunctionType(attributes, receiverType, parameterTypes, returnType);
                }

                @Override
                public void visitJetElement(JetElement elem) {
                    throw new IllegalArgumentException("Unsupported type: " + elem);
                }
            });
        }
        if (result[0] == null) {
            return ErrorType.createErrorType(typeElement == null ? "No type element": typeElement.getText());
        }
        return result[0];
    }

    private boolean hasNullableBound(TypeParameterDescriptor typeParameterDescriptor) {
        for (JetType bound : typeParameterDescriptor.getUpperBounds()) {
            if (bound.isNullable()) {
                return true;
            }
        }
        return false;
    }

    private List<JetType> resolveTypes(JetScope scope, List<JetTypeReference> argumentElements) {
        final List<JetType> arguments = new ArrayList<JetType>();
        for (JetTypeReference argumentElement : argumentElements) {
            arguments.add(resolveType(scope, argumentElement));
        }
        return arguments;
    }

    @NotNull
    private List<TypeProjection> resolveTypeProjections(JetScope scope, TypeConstructor constructor, List<JetTypeProjection> argumentElements) {
        final List<TypeProjection> arguments = new ArrayList<TypeProjection>();
        for (int i = 0, argumentElementsSize = argumentElements.size(); i < argumentElementsSize; i++) {
            JetTypeProjection argumentElement = argumentElements.get(i);

            JetProjectionKind projectionKind = argumentElement.getProjectionKind();
            JetType type;
            if (projectionKind == JetProjectionKind.STAR) {
                TypeParameterDescriptor parameterDescriptor = constructor.getParameters().get(i);
                arguments.add(TypeUtils.makeStarProjection(parameterDescriptor));
            }
            else {
                // TODO : handle the Foo<in *> case
                type = resolveType(scope, argumentElement.getTypeReference());
                Variance kind = null;
                switch (projectionKind) {
                    case IN:
                        kind = Variance.IN_VARIANCE;
                        break;
                    case OUT:
                        kind = Variance.OUT_VARIANCE;
                        break;
                    case NONE:
                        kind = Variance.INVARIANT;
                        break;
                }
                assert kind != null;
                arguments.add(new TypeProjection(kind, type));
            }
        }
        return arguments;
    }

    @Nullable
    public ClassDescriptor resolveClass(JetScope scope, JetUserType userType) {
        JetSimpleNameExpression expression = userType.getReferenceExpression();
        if (expression == null) {
            return null;
        }
        String referencedName = expression.getReferencedName();
        if (referencedName == null) {
            return null;
        }
        if (userType.isAbsoluteInRootNamespace()) {
            return JetModuleUtil.getRootNamespaceScope(userType).getClass(referencedName);
        }
        JetUserType qualifier = userType.getQualifier();
        if (qualifier != null) {
            scope = resolveClassLookupScope(scope, qualifier);
        }
        return scope.getClass(referencedName);
    }

    private JetScope resolveClassLookupScope(JetScope scope, JetUserType userType) {
        ClassDescriptor classDescriptor = resolveClass(scope, userType);
        if (classDescriptor != null) {
            return classDescriptor.getMemberScope(resolveTypeProjections(scope, classDescriptor.getTypeConstructor(), userType.getTypeArguments()));
        }

        NamespaceDescriptor namespaceDescriptor = resolveNamespace(scope, userType);
        if (namespaceDescriptor == null) {
            return JetScope.EMPTY;
        }
        return namespaceDescriptor.getMemberScope();
    }

    @Nullable
    private NamespaceDescriptor resolveNamespace(JetScope scope, JetUserType userType) {
        if (userType.isAbsoluteInRootNamespace()) {
            return resolveNamespace(JetModuleUtil.getRootNamespaceScope(userType), userType);
        }

        JetUserType qualifier = userType.getQualifier();
        if (qualifier != null) {
            NamespaceDescriptor domain = resolveNamespace(scope, qualifier);
            if (domain == null) {
                return null;
            }
            return domain.getMemberScope().getNamespace(userType.getReferencedName());
        }

        NamespaceDescriptor namespace = scope.getNamespace(userType.getReferencedName());
        if (namespace != null) {
            trace.recordReferenceResolution(userType.getReferenceExpression(), namespace);
        }
        return namespace;
    }

}
