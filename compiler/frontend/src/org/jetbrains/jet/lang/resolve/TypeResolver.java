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

import jet.Function0;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.resolve.scopes.LazyScopeAdapter;
import org.jetbrains.jet.lang.types.*;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.jetbrains.jet.lang.diagnostics.Errors.*;
import static org.jetbrains.jet.lang.resolve.PossiblyBareType.type;
import static org.jetbrains.jet.lang.types.Variance.*;
import static org.jetbrains.jet.storage.LockBasedStorageManager.NO_LOCKS;

public class TypeResolver {

    private AnnotationResolver annotationResolver;
    private QualifiedExpressionResolver qualifiedExpressionResolver;
    private ModuleDescriptor moduleDescriptor;

    @Inject
    public void setAnnotationResolver(AnnotationResolver annotationResolver) {
        this.annotationResolver = annotationResolver;
    }

    @Inject
    public void setQualifiedExpressionResolver(QualifiedExpressionResolver qualifiedExpressionResolver) {
        this.qualifiedExpressionResolver = qualifiedExpressionResolver;
    }

    @Inject
    public void setModuleDescriptor(@NotNull ModuleDescriptor moduleDescriptor) {
        this.moduleDescriptor = moduleDescriptor;
    }

    @NotNull
    public JetType resolveType(@NotNull JetScope scope, @NotNull JetTypeReference typeReference, BindingTrace trace, boolean checkBounds) {
        // bare types are not allowed
        return resolveType(new TypeResolutionContext(scope, trace, checkBounds, false), typeReference);
    }

    @NotNull
    public JetType resolveType(@NotNull TypeResolutionContext c, @NotNull JetTypeReference typeReference) {
        assert !c.allowBareTypes : "Use resolvePossiblyBareType() when bare types are allowed";
        return resolvePossiblyBareType(c, typeReference).getActualType();
    }

    @NotNull
    public PossiblyBareType resolvePossiblyBareType(@NotNull TypeResolutionContext c, @NotNull JetTypeReference typeReference) {
        JetType cachedType = c.trace.getBindingContext().get(BindingContext.TYPE, typeReference);
        if (cachedType != null) return type(cachedType);

        List<AnnotationDescriptor> annotations = annotationResolver.getResolvedAnnotations(typeReference.getAnnotations(), c.trace);

        JetTypeElement typeElement = typeReference.getTypeElement();
        PossiblyBareType type = resolveTypeElement(c, annotations, typeElement);
        if (!type.isBare()) {
            c.trace.record(BindingContext.TYPE, typeReference, type.getActualType());
        }
        c.trace.record(BindingContext.TYPE_RESOLUTION_SCOPE, typeReference, c.scope);

        return type;
    }

    @NotNull
    private PossiblyBareType resolveTypeElement(
            final TypeResolutionContext c,
            final List<AnnotationDescriptor> annotations,
            JetTypeElement typeElement
    ) {

        final PossiblyBareType[] result = new PossiblyBareType[1];
        if (typeElement != null) {
            typeElement.accept(new JetVisitorVoid() {
                @Override
                public void visitUserType(@NotNull JetUserType type) {
                    JetSimpleNameExpression referenceExpression = type.getReferenceExpression();
                    String referencedName = type.getReferencedName();
                    if (referenceExpression == null || referencedName == null) {
                        return;
                    }

                    ClassifierDescriptor classifierDescriptor = resolveClass(c.scope, type, c.trace);
                    if (classifierDescriptor == null) {
                        resolveTypeProjections(c, ErrorUtils.createErrorType("No type").getConstructor(), type.getTypeArguments());
                        return;
                    }

                    c.trace.record(BindingContext.REFERENCE_TARGET, referenceExpression, classifierDescriptor);

                    if (classifierDescriptor instanceof TypeParameterDescriptor) {
                        TypeParameterDescriptor typeParameterDescriptor = (TypeParameterDescriptor) classifierDescriptor;

                        JetScope scopeForTypeParameter = getScopeForTypeParameter(c, typeParameterDescriptor);
                        if (scopeForTypeParameter instanceof ErrorUtils.ErrorScope) {
                            result[0] = type(ErrorUtils.createErrorType("?"));
                        }
                        else {
                            result[0] = type(new JetTypeImpl(
                                    annotations,
                                    typeParameterDescriptor.getTypeConstructor(),
                                    TypeUtils.hasNullableLowerBound(typeParameterDescriptor),
                                    Collections.<TypeProjection>emptyList(),
                                    scopeForTypeParameter
                            ));
                        }

                        resolveTypeProjections(c, ErrorUtils.createErrorType("No type").getConstructor(), type.getTypeArguments());

                        DeclarationDescriptor containing = typeParameterDescriptor.getContainingDeclaration();
                        if (containing instanceof ClassDescriptor) {
                            DescriptorResolver.checkHasOuterClassInstance(c.scope, c.trace, referenceExpression, (ClassDescriptor) containing);
                        }
                    }
                    else if (classifierDescriptor instanceof ClassDescriptor) {
                        ClassDescriptor classDescriptor = (ClassDescriptor) classifierDescriptor;

                        TypeConstructor typeConstructor = classifierDescriptor.getTypeConstructor();
                        List<TypeProjection> arguments = resolveTypeProjections(c, typeConstructor, type.getTypeArguments());
                        List<TypeParameterDescriptor> parameters = typeConstructor.getParameters();
                        int expectedArgumentCount = parameters.size();
                        int actualArgumentCount = arguments.size();
                        if (ErrorUtils.isError(classDescriptor)) {
                            result[0] = type(ErrorUtils.createErrorType("[Error type: " + typeConstructor + "]"));
                        }
                        else {
                            if (actualArgumentCount != expectedArgumentCount) {
                                if (actualArgumentCount == 0) {
                                    // See docs for PossiblyBareType
                                    if (c.allowBareTypes) {
                                        result[0] = PossiblyBareType.bare(typeConstructor, false);
                                        return;
                                    }
                                    c.trace.report(WRONG_NUMBER_OF_TYPE_ARGUMENTS.on(type, expectedArgumentCount));
                                }
                                else {
                                    c.trace.report(WRONG_NUMBER_OF_TYPE_ARGUMENTS.on(type.getTypeArgumentList(), expectedArgumentCount));
                                }
                            }
                            else {
                                JetTypeImpl resultingType = new JetTypeImpl(
                                        annotations,
                                        typeConstructor,
                                        false,
                                        arguments,
                                        classDescriptor.getMemberScope(arguments)
                                );
                                result[0] = type(resultingType);
                                if (c.checkBounds) {
                                    TypeSubstitutor substitutor = TypeSubstitutor.create(resultingType);
                                    for (int i = 0, parametersSize = parameters.size(); i < parametersSize; i++) {
                                        TypeParameterDescriptor parameter = parameters.get(i);
                                        JetType argument = arguments.get(i).getType();
                                        JetTypeReference typeReference = type.getTypeArguments().get(i).getTypeReference();

                                        if (typeReference != null) {
                                            DescriptorResolver.checkBounds(typeReference, argument, parameter, substitutor, c.trace);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                @Override
                public void visitNullableType(@NotNull JetNullableType nullableType) {
                    PossiblyBareType baseType = resolveTypeElement(c, annotations, nullableType.getInnerType());
                    if (baseType.isNullable()) {
                        c.trace.report(REDUNDANT_NULLABLE.on(nullableType));
                    }
                    else if (!baseType.isBare() && TypeUtils.hasNullableSuperType(baseType.getActualType())) {
                        c.trace.report(BASE_WITH_NULLABLE_UPPER_BOUND.on(nullableType, baseType.getActualType()));
                    }
                    result[0] = baseType.makeNullable();
                }

                @Override
                public void visitFunctionType(@NotNull JetFunctionType type) {
                    JetTypeReference receiverTypeRef = type.getReceiverTypeRef();
                    JetType receiverType = receiverTypeRef == null ? null : resolveType(c.noBareTypes(), receiverTypeRef);

                    List<JetType> parameterTypes = new ArrayList<JetType>();
                    for (JetParameter parameter : type.getParameters()) {
                        parameterTypes.add(resolveType(c.noBareTypes(), parameter.getTypeReference()));
                    }

                    JetTypeReference returnTypeRef = type.getReturnTypeRef();
                    JetType returnType;
                    if (returnTypeRef != null) {
                        returnType = resolveType(c.noBareTypes(), returnTypeRef);
                    }
                    else {
                        returnType = KotlinBuiltIns.getInstance().getUnitType();
                    }
                    result[0] = type(KotlinBuiltIns.getInstance().getFunctionType(annotations, receiverType, parameterTypes, returnType));
                }

                @Override
                public void visitJetElement(@NotNull JetElement element) {
                    c.trace.report(UNSUPPORTED.on(element, "Self-types are not supported yet"));
                }
            });
        }
        if (result[0] == null) {
            return type(ErrorUtils.createErrorType(typeElement == null ? "No type element" : typeElement.getText()));
        }
        return result[0];
    }

    private JetScope getScopeForTypeParameter(TypeResolutionContext c, final TypeParameterDescriptor typeParameterDescriptor) {
        if (c.checkBounds) {
            return typeParameterDescriptor.getUpperBoundsAsType().getMemberScope();
        }
        else {
            return new LazyScopeAdapter(NO_LOCKS.createLazyValue(new Function0<JetScope>() {
                @Override
                public JetScope invoke() {
                    return typeParameterDescriptor.getUpperBoundsAsType().getMemberScope();
                }
            }));
        }
    }

    @NotNull
    private List<TypeProjection> resolveTypeProjections(
            TypeResolutionContext c,
            TypeConstructor constructor,
            List<JetTypeProjection> argumentElements
    ) {
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
                    arguments.add(new TypeProjectionImpl(OUT_VARIANCE, ErrorUtils.createErrorType("*")));
                }
            }
            else {
                // TODO : handle the Foo<in *> case
                type = resolveType(c.noBareTypes(), argumentElement.getTypeReference());
                Variance kind = resolveProjectionKind(projectionKind);
                if (constructor.getParameters().size() > i) {
                    TypeParameterDescriptor parameterDescriptor = constructor.getParameters().get(i);
                    if (kind != INVARIANT && parameterDescriptor.getVariance() != INVARIANT) {
                        if (kind == parameterDescriptor.getVariance()) {
                            c.trace.report(REDUNDANT_PROJECTION.on(argumentElement, constructor.getDeclarationDescriptor()));
                        }
                        else {
                            c.trace.report(CONFLICTING_PROJECTION.on(argumentElement, constructor.getDeclarationDescriptor()));
                        }
                    }
                }
                arguments.add(new TypeProjectionImpl(kind, type));
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
                ImportsResolver.reportPlatformClassMappedToKotlin(moduleDescriptor, trace, userType, descriptor);
                return (ClassifierDescriptor) descriptor;
            }
        }
        return null;
    }
}
