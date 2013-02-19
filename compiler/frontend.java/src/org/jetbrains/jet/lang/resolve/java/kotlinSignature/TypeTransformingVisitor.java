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

package org.jetbrains.jet.lang.resolve.java.kotlinSignature;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;
import org.jetbrains.asm4.Type;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.ClassifierDescriptor;
import org.jetbrains.jet.lang.descriptors.TypeParameterDescriptor;
import org.jetbrains.jet.lang.descriptors.impl.TypeParameterDescriptorImpl;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.resolve.TypeResolver;
import org.jetbrains.jet.lang.resolve.java.JavaToKotlinClassMap;
import org.jetbrains.jet.lang.resolve.java.JvmClassName;
import org.jetbrains.jet.lang.resolve.java.KotlinToJavaTypesMap;
import org.jetbrains.jet.lang.resolve.java.TypeUsage;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.types.*;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;
import org.jetbrains.jet.renderer.DescriptorRenderer;

import java.util.*;

import static org.jetbrains.jet.lang.resolve.java.TypeUsage.TYPE_ARGUMENT;
import static org.jetbrains.jet.lang.types.Variance.INVARIANT;

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
    public JetType visitNullableType(JetNullableType nullableType, Void aVoid) {
        if (!originalType.isNullable() && typeUsage != TYPE_ARGUMENT) {
            throw new AlternativeSignatureMismatchException("Auto type '%s' is not-null, while type in alternative signature is nullable: '%s'",
                 DescriptorRenderer.TEXT.renderType(originalType), nullableType.getText());
        }
        return TypeUtils.makeNullable(computeType(nullableType.getInnerType(), originalType, originalToAltTypeParameters, typeUsage));
    }

    @Override
    public JetType visitFunctionType(JetFunctionType type, Void data) {
        return visitCommonType(type.getReceiverTypeRef() == null
                ? KotlinBuiltIns.getInstance().getFunction(type.getParameters().size())
                : KotlinBuiltIns.getInstance().getExtensionFunction(type.getParameters().size()), type);
    }

    @Override
    public JetType visitTupleType(JetTupleType type, Void data) {
        return visitCommonType(KotlinBuiltIns.getInstance().getTuple(type.getComponentTypeRefs().size()), type);
    }

    @Override
    public JetType visitUserType(JetUserType type, Void data) {
        JetUserType qualifier = type.getQualifier();

        //noinspection ConstantConditions
        String shortName = type.getReferenceExpression().getReferencedName();
        String longName = (qualifier == null ? "" : qualifier.getText() + ".") + shortName;

        if (KotlinBuiltIns.UNIT_ALIAS.getName().equals(longName)) {
            return visitCommonType(KotlinBuiltIns.getInstance().getTuple(0), type);
        }

        return visitCommonType(longName, type);
    }

    private JetType visitCommonType(@NotNull ClassDescriptor classDescriptor, @NotNull JetTypeElement type) {
        return visitCommonType(DescriptorUtils.getFQName(classDescriptor).toSafe().getFqName(), type);
    }

    private JetType visitCommonType(@NotNull String qualifiedName, @NotNull JetTypeElement type) {
        TypeConstructor originalTypeConstructor = originalType.getConstructor();
        ClassifierDescriptor declarationDescriptor = originalTypeConstructor.getDeclarationDescriptor();
        assert declarationDescriptor != null;
        String fqName = DescriptorUtils.getFQName(declarationDescriptor).toSafe().getFqName();
        ClassDescriptor classFromLibrary = getAutoTypeAnalogWithinBuiltins(qualifiedName);
        if (!isSameName(qualifiedName, fqName) && classFromLibrary == null) {
            throw new AlternativeSignatureMismatchException("Alternative signature type mismatch, expected: %s, actual: %s", qualifiedName, fqName);
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
            throw new AlternativeSignatureMismatchException("'%s' type in method signature has %d type arguments, while '%s' in alternative signature has %d of them",
                 DescriptorRenderer.TEXT.renderType(originalType), arguments.size(), type.getText(),
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
        return new JetTypeImpl(originalType.getAnnotations(), typeConstructor, false,
                               altArguments, memberScope);
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
            if (altProjectionKind != projectionKind && projectionKind != Variance.INVARIANT) {
                throw new AlternativeSignatureMismatchException("Projection kind mismatch, actual: %s, in alternative signature: %s",
                                                                projectionKind, altProjectionKind);
            }
            if (altProjectionKind != INVARIANT && parameter.getVariance() != INVARIANT) {
                if (altProjectionKind == parameter.getVariance()) {
                    if (strictMode) {
                        throw new AlternativeSignatureMismatchException("Projection kind '%s' is redundant",
                                altProjectionKind, DescriptorUtils.getFQName(typeConstructor.getDeclarationDescriptor()));
                    }
                    else {
                        altProjectionKind = projectionKind;
                    }
                }
                else {
                    throw new AlternativeSignatureMismatchException("Projection kind '%s' is conflicting with variance of %s",
                            altProjectionKind, DescriptorUtils.getFQName(typeConstructor.getDeclarationDescriptor()));
                }
            }
        }
        else {
            altProjectionKind = projectionKind;
        }
        return new TypeProjection(altProjectionKind, alternativeArgumentType);
    }

    @Nullable
    private ClassDescriptor getAutoTypeAnalogWithinBuiltins(String qualifiedName) {
        Type javaAnalog = KotlinToJavaTypesMap.getInstance().getJavaAnalog(originalType);
        if (javaAnalog == null || javaAnalog.getSort() != Type.OBJECT)  return null;
        Collection<ClassDescriptor> descriptors =
                JavaToKotlinClassMap.getInstance().mapPlatformClass(JvmClassName.byType(javaAnalog).getFqName());
        for (ClassDescriptor descriptor : descriptors) {
            String fqName = DescriptorUtils.getFQName(descriptor).getFqName();
            if (isSameName(qualifiedName, fqName)) {
                return descriptor;
            }
        }
        return null;
    }

    @Override
    public JetType visitSelfType(JetSelfType type, Void data) {
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
