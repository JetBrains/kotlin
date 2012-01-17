package org.jetbrains.jet.lang.resolve;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.JetSemanticServices;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.ClassifierDescriptor;
import org.jetbrains.jet.lang.descriptors.NamespaceDescriptor;
import org.jetbrains.jet.lang.descriptors.TypeParameterDescriptor;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.resolve.scopes.LazyScopeAdapter;
import org.jetbrains.jet.lang.types.*;
import org.jetbrains.jet.util.lazy.LazyValue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.jetbrains.jet.lang.diagnostics.Errors.UNRESOLVED_REFERENCE;
import static org.jetbrains.jet.lang.diagnostics.Errors.UNSUPPORTED;
import static org.jetbrains.jet.lang.diagnostics.Errors.WRONG_NUMBER_OF_TYPE_ARGUMENTS;
import static org.jetbrains.jet.lang.resolve.BindingContext.REFERENCE_TARGET;

/**
 * @author abreslav
 */
public class TypeResolver {

    private final JetSemanticServices semanticServices;
    private final BindingTrace trace;
    private final boolean checkBounds;
    private final AnnotationResolver annotationResolver;

    public TypeResolver(JetSemanticServices semanticServices, BindingTrace trace, boolean checkBounds) {
        this.semanticServices = semanticServices;
        this.trace = trace;
        this.checkBounds = checkBounds;
        this.annotationResolver = new AnnotationResolver(semanticServices, trace);
    }

    @NotNull
    public JetType resolveType(@NotNull final JetScope scope, @NotNull final JetTypeReference typeReference) {
        JetType cachedType = trace.getBindingContext().get(BindingContext.TYPE, typeReference);
        if (cachedType != null) return cachedType;

        final List<AnnotationDescriptor> annotations = annotationResolver.createAnnotationStubs(typeReference.getAnnotations());

        JetTypeElement typeElement = typeReference.getTypeElement();
        JetType type = resolveTypeElement(scope, annotations, typeElement, false);
        trace.record(BindingContext.TYPE, typeReference, type);

        return type;
    }

    @NotNull
    private JetType resolveTypeElement(final JetScope scope, final List<AnnotationDescriptor> annotations,
                                       JetTypeElement typeElement, final boolean nullable) {

        final JetType[] result = new JetType[1];
        if (typeElement != null) {
            typeElement.accept(new JetVisitorVoid() {
                @Override
                public void visitUserType(JetUserType type) {
                    JetSimpleNameExpression referenceExpression = type.getReferenceExpression();
                    String referencedName = type.getReferencedName();
                    if (referenceExpression == null || referencedName == null) {
                        return;
                    }

                    ClassifierDescriptor classifierDescriptor = resolveClass(scope, type);
                    if (classifierDescriptor == null) {
                        resolveTypeProjections(scope, ErrorUtils.createErrorType("No type").getConstructor(), type.getTypeArguments());
                        return;
                    }

                    if (classifierDescriptor instanceof TypeParameterDescriptor) {
                        TypeParameterDescriptor typeParameterDescriptor = (TypeParameterDescriptor) classifierDescriptor;

                        trace.record(BindingContext.REFERENCE_TARGET, referenceExpression, typeParameterDescriptor);

                        result[0] = new JetTypeImpl(
                                annotations,
                                typeParameterDescriptor.getTypeConstructor(),
                                nullable || TypeUtils.hasNullableLowerBound(typeParameterDescriptor),
                                Collections.<TypeProjection>emptyList(),
                                getScopeForTypeParameter(typeParameterDescriptor)
                        );

                        resolveTypeProjections(scope, ErrorUtils.createErrorType("No type").getConstructor(), type.getTypeArguments());
                    }
                    else if (classifierDescriptor instanceof ClassDescriptor) {
                        ClassDescriptor classDescriptor = (ClassDescriptor) classifierDescriptor;

                        trace.record(BindingContext.REFERENCE_TARGET, referenceExpression, classifierDescriptor);
                        TypeConstructor typeConstructor = classifierDescriptor.getTypeConstructor();
                        List<TypeProjection> arguments = resolveTypeProjections(scope, typeConstructor, type.getTypeArguments());
                        List<TypeParameterDescriptor> parameters = typeConstructor.getParameters();
                        int expectedArgumentCount = parameters.size();
                        int actualArgumentCount = arguments.size();
                        if (ErrorUtils.isError(typeConstructor)) {
                            result[0] = ErrorUtils.createErrorType("??");
                        }
                        else {
                            if (actualArgumentCount != expectedArgumentCount) {
                                if (actualArgumentCount == 0) {
                                    trace.report(WRONG_NUMBER_OF_TYPE_ARGUMENTS.on(type, expectedArgumentCount));
                                } else {
                                    trace.report(WRONG_NUMBER_OF_TYPE_ARGUMENTS.on(type.getTypeArgumentList(), expectedArgumentCount));
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
                    JetType returnType;
                    if (returnTypeRef != null) {
                        returnType = resolveType(scope, returnTypeRef);
                    }
                    else {
                        returnType = JetStandardClasses.getUnitType();
                    }
                    result[0] = JetStandardClasses.getFunctionType(annotations, receiverType, parameterTypes, returnType);
                }

                @Override
                public void visitJetElement(JetElement element) {
                    trace.report(UNSUPPORTED.on(element, "Self-types are not supported yet"));
//                    throw new IllegalArgumentException("Unsupported type: " + element);
                }
            });
        }
        if (result[0] == null) {
            return ErrorUtils.createErrorType(typeElement == null ? "No type element" : typeElement.getText());
        }
        if (nullable) {
            return TypeUtils.makeNullable(result[0]);
        }
        return result[0];
    }

    private JetScope getScopeForTypeParameter(final TypeParameterDescriptor typeParameterDescriptor) {
        if (checkBounds) {
            return typeParameterDescriptor.getUpperBoundsAsType().getMemberScope();
        }
        else {
            return new LazyScopeAdapter(new LazyValue<JetScope>() {
                @Override
                protected JetScope compute() {
                    return typeParameterDescriptor.getUpperBoundsAsType().getMemberScope();
                }
            });
        }
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
                List<TypeParameterDescriptor> parameters = constructor.getParameters();
                if (parameters.size() > i) {
                    TypeParameterDescriptor parameterDescriptor = parameters.get(i);
                    arguments.add(TypeUtils.makeStarProjection(parameterDescriptor));
                }
                else {
                    arguments.add(new TypeProjection(Variance.OUT_VARIANCE, ErrorUtils.createErrorType("*")));
                }
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
            trace.report(UNRESOLVED_REFERENCE.on(userType.getReferenceExpression()));
        }
        else {
            trace.record(REFERENCE_TARGET, userType.getReferenceExpression(), classifierDescriptor);
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
            trace.record(BindingContext.RESOLUTION_SCOPE, userType.getReferenceExpression(),
                         JetModuleUtil.getRootNamespaceType(userType).getMemberScope());
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
            trace.record(BindingContext.RESOLUTION_SCOPE, userType.getReferenceExpression(), scope);
        }

        return classifierDescriptor;
    }

    @Nullable
    private JetScope resolveClassLookupScope(JetScope scope, JetUserType userType) {
        ClassifierDescriptor classifierDescriptor = resolveClassWithoutErrorReporting(scope, userType);
        if (classifierDescriptor instanceof ClassDescriptor) {
            ClassDescriptor classDescriptor = (ClassDescriptor) classifierDescriptor;
            JetType classObjectType = classDescriptor.getClassObjectType();
            if (classObjectType != null) {
                return classObjectType.getMemberScope();
            }
        }

        NamespaceDescriptor namespaceDescriptor = resolveNamespace(scope, userType);
        if (namespaceDescriptor == null) {
            trace.report(UNRESOLVED_REFERENCE.on(userType.getReferenceExpression()));
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
        NamespaceDescriptor namespace;
        if (qualifier != null) {
            NamespaceDescriptor domain = resolveNamespace(scope, qualifier);
            if (domain == null) {
                return null;
            }
            namespace = domain.getMemberScope().getNamespace(userType.getReferencedName());
        }
        else {
            namespace = scope.getNamespace(userType.getReferencedName());
        }
        if (namespace != null) {
            trace.record(BindingContext.REFERENCE_TARGET, userType.getReferenceExpression(), namespace);
        }
        return namespace;
    }

}
