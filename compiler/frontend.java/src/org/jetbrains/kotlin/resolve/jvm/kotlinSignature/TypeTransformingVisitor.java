/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.resolve.jvm.kotlinSignature;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;
import org.jetbrains.kotlin.builtins.KotlinBuiltIns;
import org.jetbrains.kotlin.descriptors.ClassDescriptor;
import org.jetbrains.kotlin.descriptors.ClassifierDescriptor;
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor;
import org.jetbrains.kotlin.descriptors.impl.TypeParameterDescriptorImpl;
import org.jetbrains.kotlin.load.java.components.TypeUsage;
import org.jetbrains.kotlin.name.ClassId;
import org.jetbrains.kotlin.name.FqNameUnsafe;
import org.jetbrains.kotlin.platform.JavaToKotlinClassMap;
import org.jetbrains.kotlin.psi.*;
import org.jetbrains.kotlin.renderer.DescriptorRenderer;
import org.jetbrains.kotlin.resolve.DescriptorUtils;
import org.jetbrains.kotlin.resolve.TypeResolver;
import org.jetbrains.kotlin.resolve.jvm.JvmPackage;
import org.jetbrains.kotlin.resolve.scopes.JetScope;
import org.jetbrains.kotlin.types.*;

import java.util.*;

import static org.jetbrains.kotlin.load.java.components.TypeUsage.TYPE_ARGUMENT;
import static org.jetbrains.kotlin.types.Variance.INVARIANT;

public class TypeTransformingVisitor extends JetVisitor<JetType, Void> {
    private static boolean strictMode = false;

    private final JetType originalType;
    private final Map<TypeParameterDescriptor, TypeParameterDescriptorImpl> originalToAltTypeParameters;

    private final TypeUsage typeUsage;

    private TypeTransformingVisitor(
            JetType originalType,
            Map<TypeParameterDescriptor, TypeParameterDescriptorImpl> originalToAltTypeParameters,
            TypeUsage typeUsage
    ) {
        this.originalType = originalType;
        this.typeUsage = typeUsage;
        this.originalToAltTypeParameters = Collections.unmodifiableMap(originalToAltTypeParameters);
    }

    @NotNull
    public static JetType computeType(
            @NotNull JetTypeElement alternativeTypeElement,
            @NotNull JetType originalType,
            @NotNull Map<TypeParameterDescriptor, TypeParameterDescriptorImpl> originalToAltTypeParameters,
            @NotNull TypeUsage typeUsage
    ) {
        JetType computedType = alternativeTypeElement.accept(new TypeTransformingVisitor(originalType, originalToAltTypeParameters, typeUsage), null);
        assert (computedType != null);
        return computedType;
    }

    @Override
    public JetType visitNullableType(@NotNull JetNullableType nullableType, Void aVoid) {
        if (!TypeUtils.isNullableType(originalType) && typeUsage != TYPE_ARGUMENT) {
            throw new AlternativeSignatureMismatchException("Auto type '%s' is not-null, while type in alternative signature is nullable: '%s'",
                 DescriptorRenderer.FQ_NAMES_IN_TYPES.renderType(originalType), nullableType.getText());
        }
        JetTypeElement innerType = nullableType.getInnerType();
        assert innerType != null : "Syntax error: " + nullableType.getText();
        return TypeUtils.makeNullable(computeType(innerType, originalType, originalToAltTypeParameters, typeUsage));
    }

    @Override
    public JetType visitFunctionType(@NotNull JetFunctionType type, Void data) {
        return visitCommonType(type.getReceiverTypeReference() == null
                ? KotlinBuiltIns.getInstance().getFunction(type.getParameters().size())
                : KotlinBuiltIns.getInstance().getExtensionFunction(type.getParameters().size()), type);
    }

    @Override
    public JetType visitUserType(@NotNull JetUserType type, Void data) {
        JetUserType qualifier = type.getQualifier();

        //noinspection ConstantConditions
        String shortName = type.getReferenceExpression().getReferencedName();
        String longName = (qualifier == null ? "" : qualifier.getText() + ".") + shortName;

        return visitCommonType(longName, type);
    }

    private JetType visitCommonType(@NotNull ClassDescriptor classDescriptor, @NotNull JetTypeElement type) {
        return visitCommonType(DescriptorUtils.getFqNameSafe(classDescriptor).asString(), type);
    }

    @NotNull
    private JetType visitCommonType(@NotNull String qualifiedName, @NotNull JetTypeElement type) {
        if (originalType.isError()) {
            return originalType;
        }
        TypeConstructor originalTypeConstructor = originalType.getConstructor();
        ClassifierDescriptor declarationDescriptor = originalTypeConstructor.getDeclarationDescriptor();
        assert declarationDescriptor != null;
        FqNameUnsafe originalClassFqName = DescriptorUtils.getFqName(declarationDescriptor);
        ClassDescriptor classFromLibrary = getAutoTypeAnalogWithinBuiltins(originalClassFqName, qualifiedName);
        if (!isSameName(qualifiedName, originalClassFqName.asString()) && classFromLibrary == null) {
            throw new AlternativeSignatureMismatchException("Alternative signature type mismatch, expected: %s, actual: %s",
                                                            qualifiedName, originalClassFqName);
        }

        TypeConstructor typeConstructor;
        if (classFromLibrary != null) {
            typeConstructor = classFromLibrary.getTypeConstructor();
        }
        else {
            typeConstructor = originalTypeConstructor;
        }
        ClassifierDescriptor typeConstructorClassifier = typeConstructor.getDeclarationDescriptor();
        if (typeConstructorClassifier instanceof TypeParameterDescriptor && originalToAltTypeParameters.containsKey(typeConstructorClassifier)) {
            typeConstructor = originalToAltTypeParameters.get(typeConstructorClassifier).getTypeConstructor();
        }

        List<TypeProjection> arguments = originalType.getArguments();

        if (arguments.size() != type.getTypeArgumentsAsTypes().size()) {
            if (JvmPackage.getPLATFORM_TYPES()) return originalType;

            throw new AlternativeSignatureMismatchException("'%s' type in method signature has %d type arguments, while '%s' in alternative signature has %d of them",
                 DescriptorRenderer.FQ_NAMES_IN_TYPES.renderType(originalType), arguments.size(), type.getText(),
                 type.getTypeArgumentsAsTypes().size());
        }

        List<TypeProjection> altArguments = new ArrayList<TypeProjection>();
        for (int i = 0, size = arguments.size(); i < size; i++) {
            altArguments.add(getAltArgument(type, typeConstructor, i, arguments.get(i)));
        }

        JetScope memberScope;
        if (typeConstructorClassifier instanceof TypeParameterDescriptor) {
            memberScope = ((TypeParameterDescriptor) typeConstructorClassifier).getUpperBoundsAsType().getMemberScope();
        }
        else if (typeConstructorClassifier instanceof ClassDescriptor) {
            memberScope = ((ClassDescriptor) typeConstructorClassifier).getMemberScope(altArguments);
        }
        else {
            throw new AssertionError("Unexpected class of type constructor classifier "
                                     + (typeConstructorClassifier == null ? "null" : typeConstructorClassifier.getClass().getName()));
        }
        return JetTypeImpl.create(originalType.getAnnotations(), typeConstructor, false, altArguments, memberScope);
    }

    @NotNull
    private TypeProjection getAltArgument(
            @NotNull JetTypeElement type,
            @NotNull TypeConstructor typeConstructor,
            int i,
            @NotNull TypeProjection originalArgument
    ) {
        JetTypeReference typeReference = type.getTypeArgumentsAsTypes().get(i); // process both function type and user type

        if (typeReference == null) {
            // star projection
            assert type instanceof JetUserType
                   && ((JetUserType) type).getTypeArguments().get(i).getProjectionKind() == JetProjectionKind.STAR;

            return originalArgument;
        }

        JetTypeElement argumentAlternativeTypeElement = typeReference.getTypeElement();
        assert argumentAlternativeTypeElement != null;

        TypeParameterDescriptor parameter = typeConstructor.getParameters().get(i);
        JetType alternativeArgumentType = computeType(argumentAlternativeTypeElement, originalArgument.getType(), originalToAltTypeParameters, TYPE_ARGUMENT);
        Variance projectionKind = originalArgument.getProjectionKind();
        Variance altProjectionKind;
        if (type instanceof JetUserType) {
            JetTypeProjection typeProjection = ((JetUserType) type).getTypeArguments().get(i);
            altProjectionKind = TypeResolver.resolveProjectionKind(typeProjection.getProjectionKind());
            if (altProjectionKind != projectionKind && projectionKind != Variance.INVARIANT && !JvmPackage.getPLATFORM_TYPES()) {
                throw new AlternativeSignatureMismatchException("Projection kind mismatch, actual: %s, in alternative signature: %s",
                                                                projectionKind, altProjectionKind);
            }
            if (altProjectionKind != INVARIANT && parameter.getVariance() != INVARIANT) {
                if (altProjectionKind == parameter.getVariance()) {
                    if (strictMode) {
                        throw new AlternativeSignatureMismatchException("Projection kind '%s' is redundant",
                                altProjectionKind, DescriptorUtils.getFqName(typeConstructor.getDeclarationDescriptor()));
                    }
                    else {
                        altProjectionKind = projectionKind;
                    }
                }
                else {
                    throw new AlternativeSignatureMismatchException("Projection kind '%s' is conflicting with variance of %s",
                            altProjectionKind, DescriptorUtils.getFqName(typeConstructor.getDeclarationDescriptor()));
                }
            }
        }
        else {
            altProjectionKind = projectionKind;
        }
        return new TypeProjectionImpl(altProjectionKind, alternativeArgumentType);
    }

    @Nullable
    private static ClassDescriptor getAutoTypeAnalogWithinBuiltins(
            @NotNull FqNameUnsafe originalClassFqName,
            @NotNull String qualifiedName
    ) {
        ClassId javaClassId = JavaToKotlinClassMap.INSTANCE.mapKotlinToJava(originalClassFqName);
        if (javaClassId == null) return null;

        Collection<ClassDescriptor> descriptors = JavaToKotlinClassMap.INSTANCE.mapPlatformClass(javaClassId.asSingleFqName());
        for (ClassDescriptor descriptor : descriptors) {
            String fqName = DescriptorUtils.getFqName(descriptor).asString();
            if (isSameName(qualifiedName, fqName)) {
                return descriptor;
            }
        }
        return null;
    }

    @Override
    public JetType visitSelfType(@NotNull JetSelfType type, Void data) {
        throw new UnsupportedOperationException("Self-types are not supported yet");
    }

    private static boolean isSameName(String qualifiedName, String fullyQualifiedName) {
        return fullyQualifiedName.equals(qualifiedName) || fullyQualifiedName.endsWith("." + qualifiedName);
    }

    @TestOnly
    public static void setStrictMode(boolean strictMode) {
        TypeTransformingVisitor.strictMode = strictMode;
    }
}
