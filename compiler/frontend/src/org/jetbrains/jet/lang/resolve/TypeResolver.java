/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.ModuleConfiguration;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.ClassifierDescriptor;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.TypeParameterDescriptor;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.resolve.scopes.LazyScopeAdapter;
import org.jetbrains.jet.lang.types.*;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;
import org.jetbrains.jet.util.lazy.RecursionIntolerantLazyValue;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.jetbrains.jet.lang.diagnostics.Errors.*;
import static org.jetbrains.jet.lang.types.Variance.*;

public class TypeResolver {

    private AnnotationResolver annotationResolver;
    private DescriptorResolver descriptorResolver;
    private QualifiedExpressionResolver qualifiedExpressionResolver;
    private ModuleConfiguration moduleConfiguration;

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

    @Inject
    public void setModuleConfiguration(@NotNull ModuleConfiguration moduleConfiguration) {
        this.moduleConfiguration = moduleConfiguration;
    }

    @NotNull
    public JetType resolveType(@NotNull JetScope scope, @NotNull JetTypeReference typeReference, BindingTrace trace, boolean checkBounds) {
        JetType cachedType = trace.getBindingContext().get(BindingContext.TYPE, typeReference);
        if (cachedType != null) return cachedType;

        List<AnnotationDescriptor> annotations = annotationResolver.getResolvedAnnotations(typeReference.getAnnotations(), trace);

        JetTypeElement typeElement = typeReference.getTypeElement();
        JetType type = resolveTypeElement(scope, annotations, typeElement, trace, checkBounds);
        trace.record(BindingContext.TYPE, typeReference, type);
        trace.record(BindingContext.TYPE_RESOLUTION_SCOPE, typeReference, scope);

        return type;
    }

    @NotNull
    private JetType resolveTypeElement(final JetScope scope, final List<AnnotationDescriptor> annotations,
            JetTypeElement typeElement, final BindingTrace trace, final boolean checkBounds) {

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

                    trace.record(BindingContext.REFERENCE_TARGET, referenceExpression, classifierDescriptor);

                    if (classifierDescriptor instanceof TypeParameterDescriptor) {
                        TypeParameterDescriptor typeParameterDescriptor = (TypeParameterDescriptor) classifierDescriptor;

                        JetScope scopeForTypeParameter = getScopeForTypeParameter(typeParameterDescriptor, checkBounds);
                        if (scopeForTypeParameter instanceof ErrorUtils.ErrorScope) {
                            result[0] = ErrorUtils.createErrorType("?");
                        }
                        else {
                            result[0] = new JetTypeImpl(
                                    annotations,
                                    typeParameterDescriptor.getTypeConstructor(),
                                    TypeUtils.hasNullableLowerBound(typeParameterDescriptor),
                                    Collections.<TypeProjection>emptyList(),
                                    scopeForTypeParameter
                            );
                        }

                        resolveTypeProjections(scope, ErrorUtils.createErrorType("No type").getConstructor(), type.getTypeArguments(), trace, checkBounds);

                        DeclarationDescriptor containing = typeParameterDescriptor.getContainingDeclaration();
                        if (containing instanceof ClassDescriptor) {
                            DescriptorResolver.checkHasOuterClassInstance(scope, trace, referenceExpression, (ClassDescriptor) containing);
                        }
                    }
                    else if (classifierDescriptor instanceof ClassDescriptor) {
                        ClassDescriptor classDescriptor = (ClassDescriptor) classifierDescriptor;

                        TypeConstructor typeConstructor = classifierDescriptor.getTypeConstructor();
                        List<TypeProjection> arguments = resolveTypeProjections(scope, typeConstructor, type.getTypeArguments(), trace, checkBounds);
                        List<TypeParameterDescriptor> parameters = typeConstructor.getParameters();
                        int expectedArgumentCount = parameters.size();
                        int actualArgumentCount = arguments.size();
                        if (ErrorUtils.isError(typeConstructor)) {
                            result[0] = ErrorUtils.createErrorType("[Error type: " + typeConstructor + "]");
                        }
                        else {
                            if (actualArgumentCount != expectedArgumentCount) {
                                if (actualArgumentCount == 0) {
                                    if (rhsOfIsExpression(type) || rhsOfIsPattern(type)) {
                                        trace.report(NO_TYPE_ARGUMENTS_ON_RHS_OF_IS_EXPRESSION.on(type, expectedArgumentCount, allStarProjectionsString(typeConstructor)));
                                    }
                                    else {
                                        trace.report(WRONG_NUMBER_OF_TYPE_ARGUMENTS.on(type, expectedArgumentCount));
                                    }
                                }
                                else {
                                    trace.report(WRONG_NUMBER_OF_TYPE_ARGUMENTS.on(type.getTypeArgumentList(), expectedArgumentCount));
                                }
                            }
                            else {
                                result[0] = new JetTypeImpl(
                                        annotations,
                                        typeConstructor,
                                        false,
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
                    JetType baseType = resolveTypeElement(scope, annotations, nullableType.getInnerType(), trace, checkBounds);
                    if (baseType.isNullable()) {
                        trace.report(REDUNDANT_NULLABLE.on(nullableType));
                    }
                    else if (TypeUtils.hasNullableSuperType(baseType)) {
                        trace.report(BASE_WITH_NULLABLE_UPPER_BOUND.on(nullableType, baseType));
                    }
                    result[0] = TypeUtils.makeNullable(baseType);
                }

                @Override
                public void visitTupleType(JetTupleType type) {
                    // TODO: remove this method completely when tuples are droppped
                    throw new IllegalStateException("Tuples are not supported: " + type.getText());
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
                        returnType = KotlinBuiltIns.getInstance().getUnitType();
                    }
                    result[0] = KotlinBuiltIns.getInstance().getFunctionType(annotations, receiverType, parameterTypes, returnType);
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
        return result[0];
    }

    private static boolean rhsOfIsExpression(@NotNull JetUserType type) {
        // Look for the FIRST expression containing this type
        JetExpression outerExpression = PsiTreeUtil.getParentOfType(type, JetExpression.class);
        if (outerExpression instanceof JetIsExpression) {
            JetIsExpression isExpression = (JetIsExpression) outerExpression;
            // If this expression is JetIsExpression, and the type is the outermost on the RHS
            if (type.getParent() == isExpression.getTypeRef()) {
                return true;
            }
        }
        return false;
    }

    private static boolean rhsOfIsPattern(@NotNull JetUserType type) {
        // Look for the is-pattern containing this type
        JetWhenConditionIsPattern outerPattern = PsiTreeUtil.getParentOfType(type, JetWhenConditionIsPattern.class, false, JetExpression.class);
        if (outerPattern == null) return false;
        // We are interested only in the outermost type on the RHS
        return type.getParent() == outerPattern.getTypeRef();
    }

    private JetScope getScopeForTypeParameter(final TypeParameterDescriptor typeParameterDescriptor, boolean checkBounds) {
        if (checkBounds) {
            return typeParameterDescriptor.getUpperBoundsAsType().getMemberScope();
        }
        else {
            return new LazyScopeAdapter(new RecursionIntolerantLazyValue<JetScope>() {
                @Override
                protected JetScope compute() {
                    return typeParameterDescriptor.getUpperBoundsAsType().getMemberScope();
                }
            });
        }
    }

    private List<JetType> resolveTypes(JetScope scope, List<JetTypeReference> argumentElements, BindingTrace trace, boolean checkBounds) {
        List<JetType> arguments = new ArrayList<JetType>();
        for (JetTypeReference argumentElement : argumentElements) {
            arguments.add(resolveType(scope, argumentElement, trace, checkBounds));
        }
        return arguments;
    }

    @NotNull
    private List<TypeProjection> resolveTypeProjections(JetScope scope, TypeConstructor constructor, List<JetTypeProjection> argumentElements, BindingTrace trace, boolean checkBounds) {
        List<TypeProjection> arguments = new ArrayList<TypeProjection>();
        for (int i = 0, argumentElementsSize = argumentElements.size(); i < argumentElementsSize; i++) {
            JetTypeProjection argumentElement = argumentElements.get(i);

            JetProjectionKind projectionKind = argumentElement.getProjectionKind();
            JetType type;
            if (projectionKind == JetProjectionKind.STAR) {
                List<TypeParameterDescriptor> parameters = constructor.getParameters();
                if (parameters.size() > i) {
                    TypeParameterDescriptor parameterDescriptor = parameters.get(i);
                    arguments.add(SubstitutionUtils.makeStarProjection(parameterDescriptor));
                }
                else {
                    arguments.add(new TypeProjection(OUT_VARIANCE, ErrorUtils.createErrorType("*")));
                }
            }
            else {
                // TODO : handle the Foo<in *> case
                type = resolveType(scope, argumentElement.getTypeReference(), trace, checkBounds);
                Variance kind = resolveProjectionKind(projectionKind);
                if (constructor.getParameters().size() > i) {
                    TypeParameterDescriptor parameterDescriptor = constructor.getParameters().get(i);
                    if (kind != INVARIANT && parameterDescriptor.getVariance() != INVARIANT) {
                        if (kind == parameterDescriptor.getVariance()) {
                            trace.report(REDUNDANT_PROJECTION.on(argumentElement, constructor.getDeclarationDescriptor()));
                        }
                        else {
                            trace.report(CONFLICTING_PROJECTION.on(argumentElement, constructor.getDeclarationDescriptor()));
                        }
                    }
                }
                arguments.add(new TypeProjection(kind, type));
            }
        }
        return arguments;
    }

    @NotNull
    public static Variance resolveProjectionKind(@NotNull JetProjectionKind projectionKind) {
        Variance kind = null;
        switch (projectionKind) {
            case IN:
                kind = IN_VARIANCE;
                break;
            case OUT:
                kind = OUT_VARIANCE;
                break;
            case NONE:
                kind = INVARIANT;
                break;
            default:
                // NOTE: Star projections must be handled before this method is called
                throw new IllegalStateException("Illegal projection kind:" + projectionKind);
        }
        return kind;
    }

    @Nullable
    public ClassifierDescriptor resolveClass(JetScope scope, JetUserType userType, BindingTrace trace) {
        Collection<? extends DeclarationDescriptor> descriptors = qualifiedExpressionResolver.lookupDescriptorsForUserType(userType, scope, trace);
        for (DeclarationDescriptor descriptor : descriptors) {
            if (descriptor instanceof ClassifierDescriptor) {
                ImportsResolver.reportPlatformClassMappedToKotlin(moduleConfiguration, trace, userType, descriptor);
                return (ClassifierDescriptor) descriptor;
            }
        }
        return null;
    }

    @NotNull
    private static String allStarProjectionsString(@NotNull TypeConstructor constructor) {
        int size = constructor.getParameters().size();
        assert size != 0 : "No projections possible for a nilary type constructor" + constructor;
        ClassifierDescriptor declarationDescriptor = constructor.getDeclarationDescriptor();
        assert declarationDescriptor != null : "No declaration descriptor for type constructor " + constructor;
        String name = declarationDescriptor.getName().getName();

        return TypeUtils.getTypeNameAndStarProjectionsString(name, size);
    }
}
