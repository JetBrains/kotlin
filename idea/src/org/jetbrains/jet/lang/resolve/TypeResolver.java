package org.jetbrains.jet.lang.resolve;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.JetSemanticServices;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.types.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author abreslav
 */
public class TypeResolver {

    private final JetSemanticServices semanticServices;
    private final BindingTrace trace;
    private final boolean checkBounds;

    public TypeResolver(JetSemanticServices semanticServices, BindingTrace trace, boolean checkBounds) {
        this.semanticServices = semanticServices;
        this.trace = trace;
        this.checkBounds = checkBounds;
    }

    @NotNull
    public JetType resolveType(@NotNull final JetScope scope, @NotNull final JetTypeReference typeReference) {
        final List<Annotation> annotations = AnnotationResolver.INSTANCE.resolveAnnotations(typeReference.getAttributes());

        JetTypeElement typeElement = typeReference.getTypeElement();
        JetType type = resolveTypeElement(scope, annotations, typeElement, false);
        trace.recordTypeResolution(typeReference, type);
        return type;
    }

    @NotNull
    private JetType resolveTypeElement(final JetScope scope, final List<Annotation> annotations, JetTypeElement typeElement, final boolean nullable) {
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

                    ClassifierDescriptor classifierDescriptor = resolveClass(scope, type);
                    if (classifierDescriptor != null) {
                        if (classifierDescriptor instanceof TypeParameterDescriptor) {
                            TypeParameterDescriptor typeParameterDescriptor = (TypeParameterDescriptor) classifierDescriptor;

                            trace.recordReferenceResolution(referenceExpression, typeParameterDescriptor);

                            result[0] = new JetTypeImpl(
                                    annotations,
                                    typeParameterDescriptor.getTypeConstructor(),
                                    nullable || TypeUtils.hasNullableBound(typeParameterDescriptor),
                                    Collections.<TypeProjection>emptyList(),
                                    // TODO : joint domain
                                    JetStandardClasses.STUB
                            );
                        }
                        else if (classifierDescriptor instanceof ClassDescriptor) {
                            ClassDescriptor classDescriptor = (ClassDescriptor) classifierDescriptor;
                            
                            trace.recordReferenceResolution(referenceExpression, classifierDescriptor);
                            TypeConstructor typeConstructor = classifierDescriptor.getTypeConstructor();
                            List<TypeProjection> arguments = resolveTypeProjections(scope, typeConstructor, type.getTypeArguments());
                            List<TypeParameterDescriptor> parameters = typeConstructor.getParameters();
                            int expectedArgumentCount = parameters.size();
                            int actualArgumentCount = arguments.size();
                            if (ErrorUtils.isError(typeConstructor)) {
                                result[0] = new JetTypeImpl(
                                        annotations,
                                        typeConstructor,
                                        nullable,
                                        arguments, // TODO : review
                                        classDescriptor.getMemberScope(Collections.<TypeProjection>emptyList())
                                );
                            }
                            else {
                                if (actualArgumentCount != expectedArgumentCount) {
                                    String errorMessage = (expectedArgumentCount == 0 ? "No" : expectedArgumentCount) + " type arguments expected";
                                    if (actualArgumentCount == 0) {
                                        trace.getErrorHandler().genericError(type.getNode(), errorMessage);
                                    } else if (expectedArgumentCount == 0) {
                                        trace.getErrorHandler().genericError(type.getTypeArgumentList().getNode(), errorMessage);
                                    }
                                } else {
                                    result[0] = new JetTypeImpl(
                                            annotations,
                                            typeConstructor,
                                            nullable,
                                            arguments,
                                            classDescriptor.getMemberScope(arguments)
                                    );
                                    if (checkBounds) {
                                        TypeSubstitutor substitutor = TypeSubstitutor.create(result[0]);
                                        for (int i = 0, parametersSize = parameters.size(); i < parametersSize; i++) {
                                            TypeParameterDescriptor parameter = parameters.get(i);
                                            JetType argument = arguments.get(i).getType();
                                            JetTypeReference typeReference = type.getTypeArguments().get(i).getTypeReference();

                                            if (typeReference != null) {
                                                semanticServices.getClassDescriptorResolver(trace).checkBounds(typeReference, argument, parameter, substitutor);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                @Override
                public void visitNullableType(JetNullableType nullableType) {
                    result[0] = resolveTypeElement(scope, annotations, nullableType.getInnerType(), true);
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

                    JetTypeReference returnTypeRef = type.getReturnTypeRef();
                    if (returnTypeRef != null) {
                        JetType returnType = resolveType(scope, returnTypeRef);
                        result[0] = JetStandardClasses.getFunctionType(annotations, receiverType, parameterTypes, returnType);
                    }
                }

                @Override
                public void visitJetElement(JetElement elem) {
                    throw new IllegalArgumentException("Unsupported type: " + elem);
                }
            });
        }
        if (result[0] == null) {
            return ErrorUtils.createErrorType(typeElement == null ? "No type element" : typeElement.getText());
        }
        return result[0];
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
    public ClassifierDescriptor resolveClass(JetScope scope, JetUserType userType) {
        ClassifierDescriptor classifierDescriptor = resolveClassWithoutErrorReporting(scope, userType);

        if (classifierDescriptor == null) {
            trace.getErrorHandler().unresolvedReference(userType.getReferenceExpression());
        }

        return classifierDescriptor;
    }

    @Nullable
    private ClassifierDescriptor resolveClassWithoutErrorReporting(JetScope scope, JetUserType userType) {
        JetSimpleNameExpression expression = userType.getReferenceExpression();
        if (expression == null) {
            return null;
        }
        String referencedName = expression.getReferencedName();
        if (referencedName == null) {
            return null;
        }
        ClassifierDescriptor classifierDescriptor;
        if (userType.isAbsoluteInRootNamespace()) {
            classifierDescriptor = JetModuleUtil.getRootNamespaceType(userType).getMemberScope().getClassifier(referencedName);
        }
        else {
            JetUserType qualifier = userType.getQualifier();
            if (qualifier != null) {
                scope = resolveClassLookupScope(scope, qualifier);
            }
            if (scope == null) {
                return ErrorUtils.getErrorClass();
            }
            classifierDescriptor = scope.getClassifier(referencedName);
        }
        return classifierDescriptor;
    }

    @Nullable
    private JetScope resolveClassLookupScope(JetScope scope, JetUserType userType) {
        ClassifierDescriptor classifierDescriptor = resolveClassWithoutErrorReporting(scope, userType);
        if (classifierDescriptor instanceof ClassDescriptor) {
            List<TypeProjection> typeArguments = resolveTypeProjections(scope, classifierDescriptor.getTypeConstructor(), userType.getTypeArguments());
            return ((ClassDescriptor) classifierDescriptor).getMemberScope(typeArguments);
        }

        NamespaceDescriptor namespaceDescriptor = resolveNamespace(scope, userType);
        if (namespaceDescriptor == null) {
            trace.getErrorHandler().unresolvedReference(userType.getReferenceExpression());
            return null;
        }
        return namespaceDescriptor.getMemberScope();
    }

    @Nullable
    private NamespaceDescriptor resolveNamespace(JetScope scope, JetUserType userType) {
        if (userType.isAbsoluteInRootNamespace()) {
            return resolveNamespace(JetModuleUtil.getRootNamespaceType(userType).getMemberScope(), userType);
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
