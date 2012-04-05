/*
 * Copyright 2010-2012 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.jet.lang.resolve;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.resolve.scopes.LazyScopeAdapter;
import org.jetbrains.jet.lang.types.*;
import org.jetbrains.jet.lang.types.lang.JetStandardClasses;
import org.jetbrains.jet.util.lazy.LazyValue;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.jetbrains.jet.lang.diagnostics.Errors.*;

/**
 * @author abreslav
 */
public class TypeResolver {

    private AnnotationResolver annotationResolver;
    private DescriptorResolver descriptorResolver;
    private QualifiedExpressionResolver qualifiedExpressionResolver;

    @Inject
    public void setDescriptorResolver(DescriptorResolver descriptorResolver) {
        this.descriptorResolver = descriptorResolver;
    }

    @Inject
    public void setAnnotationResolver(AnnotationResolver annotationResolver) {
        this.annotationResolver = annotationResolver;
    }

    @Inject
    public void setQualifiedExpressionResolver(QualifiedExpressionResolver qualifiedExpressionResolver) {
        this.qualifiedExpressionResolver = qualifiedExpressionResolver;
    }

    @NotNull
    public JetType resolveType(@NotNull final JetScope scope, @NotNull final JetTypeReference typeReference, BindingTrace trace, boolean checkBounds) {
        JetType cachedType = trace.getBindingContext().get(BindingContext.TYPE, typeReference);
        if (cachedType != null) return cachedType;

        final List<AnnotationDescriptor> annotations = annotationResolver.createAnnotationStubs(typeReference.getAnnotations(), trace);

        JetTypeElement typeElement = typeReference.getTypeElement();
        JetType type = resolveTypeElement(scope, annotations, typeElement, false, trace, checkBounds);
        trace.record(BindingContext.TYPE, typeReference, type);

        return type;
    }

    @NotNull
    private JetType resolveTypeElement(final JetScope scope, final List<AnnotationDescriptor> annotations,
            JetTypeElement typeElement, final boolean nullable, final BindingTrace trace, final boolean checkBounds) {

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

                    ClassifierDescriptor classifierDescriptor = resolveClass(scope, type, trace);
                    if (classifierDescriptor == null) {
                        resolveTypeProjections(scope, ErrorUtils.createErrorType("No type").getConstructor(), type.getTypeArguments(), trace, checkBounds);
                        return;
                    }

                    if (classifierDescriptor instanceof TypeParameterDescriptor) {
                        TypeParameterDescriptor typeParameterDescriptor = (TypeParameterDescriptor) classifierDescriptor;

                        trace.record(BindingContext.REFERENCE_TARGET, referenceExpression, typeParameterDescriptor);

                        JetScope scopeForTypeParameter = getScopeForTypeParameter(typeParameterDescriptor, checkBounds);
                        if (scopeForTypeParameter instanceof ErrorUtils.ErrorScope) {
                            result[0] = ErrorUtils.createErrorType("?");
                        } else {
                            result[0] = new JetTypeImpl(
                                    annotations,
                                    typeParameterDescriptor.getTypeConstructor(),
                                    nullable || TypeUtils.hasNullableLowerBound(typeParameterDescriptor),
                                    Collections.<TypeProjection>emptyList(),
                                    scopeForTypeParameter
                            );
                        }

                        resolveTypeProjections(scope, ErrorUtils.createErrorType("No type").getConstructor(), type.getTypeArguments(), trace, checkBounds);
                    }
                    else if (classifierDescriptor instanceof ClassDescriptor) {
                        ClassDescriptor classDescriptor = (ClassDescriptor) classifierDescriptor;

                        trace.record(BindingContext.REFERENCE_TARGET, referenceExpression, classifierDescriptor);
                        TypeConstructor typeConstructor = classifierDescriptor.getTypeConstructor();
                        List<TypeProjection> arguments = resolveTypeProjections(scope, typeConstructor, type.getTypeArguments(), trace, checkBounds);
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
                                            descriptorResolver.checkBounds(typeReference, argument, parameter, substitutor, trace);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                @Override
                public void visitNullableType(JetNullableType nullableType) {
                    result[0] = resolveTypeElement(scope, annotations, nullableType.getInnerType(), true, trace, checkBounds);
                }

                @Override
                public void visitTupleType(JetTupleType type) {
                    // TODO labels
                    result[0] = JetStandardClasses.getTupleType(resolveTypes(scope, type.getComponentTypeRefs(), trace, checkBounds));
                }

                @Override
                public void visitFunctionType(JetFunctionType type) {
                    JetTypeReference receiverTypeRef = type.getReceiverTypeRef();
                    JetType receiverType = receiverTypeRef == null ? null : resolveType(scope, receiverTypeRef, trace, checkBounds);

                    List<JetType> parameterTypes = new ArrayList<JetType>();
                    for (JetParameter parameter : type.getParameters()) {
                        parameterTypes.add(resolveType(scope, parameter.getTypeReference(), trace, checkBounds));
                    }

                    JetTypeReference returnTypeRef = type.getReturnTypeRef();
                    JetType returnType;
                    if (returnTypeRef != null) {
                        returnType = resolveType(scope, returnTypeRef, trace, checkBounds);
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

    private JetScope getScopeForTypeParameter(final TypeParameterDescriptor typeParameterDescriptor, boolean checkBounds) {
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

    private List<JetType> resolveTypes(JetScope scope, List<JetTypeReference> argumentElements, BindingTrace trace, boolean checkBounds) {
        final List<JetType> arguments = new ArrayList<JetType>();
        for (JetTypeReference argumentElement : argumentElements) {
            arguments.add(resolveType(scope, argumentElement, trace, checkBounds));
        }
        return arguments;
    }

    @NotNull
    private List<TypeProjection> resolveTypeProjections(JetScope scope, TypeConstructor constructor, List<JetTypeProjection> argumentElements, BindingTrace trace, boolean checkBounds) {
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
                type = resolveType(scope, argumentElement.getTypeReference(), trace, checkBounds);
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
    public ClassifierDescriptor resolveClass(JetScope scope, JetUserType userType, BindingTrace trace) {
        Collection<? extends DeclarationDescriptor> descriptors = qualifiedExpressionResolver.lookupDescriptorsForUserType(userType, scope, trace);
        for (DeclarationDescriptor descriptor : descriptors) {
            if (descriptor instanceof ClassifierDescriptor) {
                return (ClassifierDescriptor) descriptor;
            }
        }
        return null;
    }
}
