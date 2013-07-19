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

package org.jetbrains.jet.codegen.state;

import com.intellij.openapi.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.asm4.Type;
import org.jetbrains.jet.codegen.signature.BothSignatureWriter;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.resolve.java.*;
import org.jetbrains.jet.lang.types.*;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.jetbrains.jet.codegen.AsmUtil.boxType;
import static org.jetbrains.jet.codegen.binding.CodegenBinding.getJvmInternalName;

public class JetTypeToJavaTypeMapper extends JetTypeToJavaTypeMapperNoMatching implements DeclarationDescriptorVisitor<Type, JetType> {
    private final BothSignatureWriter signatureVisitor;
    private final JetTypeMapper typeMapper;
    private final BuiltinToJavaMapping builtin;
    private final Variance howThisTypeIsUsed;
    private final JetTypeMapperMode mode;
    private final boolean withBuiltinsToJava;

    JetTypeToJavaTypeMapper(BothSignatureWriter sV,
                            JetTypeMapper s,
                            BuiltinToJavaMapping b,
                            Variance h,
                            JetTypeMapperMode kind,
                            boolean mapBuiltinsToJava,
                            DeclarationDescriptor descriptor) {
        super(descriptor);
        signatureVisitor = sV;
        typeMapper = s;
        builtin = b;
        howThisTypeIsUsed = h;
        mode = kind;
        withBuiltinsToJava = mapBuiltinsToJava;
    }

    @Override
    public Type visitTypeParameterDescriptor(TypeParameterDescriptor typeParameterDescriptor,
                                             JetType jetType) {
        Type type = typeMapper.mapType(typeParameterDescriptor.getUpperBoundsAsType(), mode);
        if (signatureVisitor != null) {
            signatureVisitor.writeTypeVariable(typeParameterDescriptor.getName(), jetType.isNullable(), type);
        }
        typeMapper.checkValidType(type);
        return type;
    }

    private Type builtinArrayToJava(JetType arrayJetType) {
        if (arrayJetType.getArguments().size() != 1) {
            throw new UnsupportedOperationException("arrays must have one type argument");
        }
        TypeProjection memberProjection = arrayJetType.getArguments().get(0);
        JetType memberType = memberProjection.getType();

        if (signatureVisitor != null) {
            signatureVisitor.writeArrayType(arrayJetType.isNullable(), memberProjection.getProjectionKind());
            mapContainedType(memberType, arrayJetType, signatureVisitor, JetTypeMapperMode.TYPE_PARAMETER,
                             memberProjection.getProjectionKind());
            signatureVisitor.writeArrayEnd();
        }

        Type r;
        if (!isGenericsArray(arrayJetType)) {
            r = Type.getType("[" + boxType(typeMapper.mapType(memberType, mode)).getDescriptor());
        }
        else {
            r = AsmTypeConstants.JAVA_ARRAY_GENERIC_TYPE;
        }
        typeMapper.checkValidType(r);
        return r;
    }

    @NotNull
    private Type mapContainedType(
            @NotNull  JetType             jetType,
            @NotNull  JetType             containingType,
            @Nullable BothSignatureWriter signatureVisitor,
            @NotNull  JetTypeMapperMode   kind,
            @NotNull  Variance            howThisTypeIsUsed
    ) {
        ContainedBuiltinToJavaMapping builtin = new ContainedBuiltinToJavaMapping(
                signatureVisitor, typeMapper, jetType, howThisTypeIsUsed, kind);

        builtin.containInType(containingType);
        return typeMapper.mapTypeWithBuiltin(jetType, signatureVisitor, kind, howThisTypeIsUsed, builtin);
    }

    private static boolean isGenericsArray(JetType type) {
        return KotlinBuiltIns.getInstance().isArray(type) &&
               type.getArguments().get(0).getType().getConstructor().getDeclarationDescriptor() instanceof TypeParameterDescriptor;
    }

    static public void writeGenericType(
            JetTypeMapper typeMapper,
            BothSignatureWriter signatureVisitor,
            Type asmType,
            JetType jetType,
            boolean forceReal,
            Variance howThisTypeIsUsed
    ) {
        if (signatureVisitor != null) {
            String kotlinTypeName = getKotlinTypeNameForSignature(jetType, asmType);
            signatureVisitor.writeClassBegin(asmType.getInternalName(), jetType.isNullable(), forceReal, kotlinTypeName);

            List<TypeProjection> arguments = jetType.getArguments();
            for (TypeParameterDescriptor parameter : jetType.getConstructor().getParameters()) {
                TypeProjection argument = arguments.get(parameter.getIndex());

                Variance projectionKindForKotlin = argument.getProjectionKind();
                Variance projectionKindForJava = getEffectiveVariance(
                        parameter.getVariance(),
                        projectionKindForKotlin,
                        howThisTypeIsUsed
                );
                signatureVisitor.writeTypeArgument(projectionKindForKotlin, projectionKindForJava);

                typeMapper.mapType(argument.getType(), signatureVisitor, JetTypeMapperMode.TYPE_PARAMETER);
                signatureVisitor.writeTypeArgumentEnd();
            }
            signatureVisitor.writeClassEnd();
        }
    }

    private static Variance getEffectiveVariance(Variance parameterVariance, Variance projectionKind, Variance howThisTypeIsUsed) {
        // Return type must not contain wildcards
        if (howThisTypeIsUsed == Variance.OUT_VARIANCE) return projectionKind;

        if (parameterVariance == Variance.INVARIANT) {
            return projectionKind;
        }
        if (projectionKind == Variance.INVARIANT) {
            return parameterVariance;
        }
        if (parameterVariance == projectionKind) {
            return parameterVariance;
        }

        // In<out X> = In<*>
        // Out<in X> = Out<*>
        return Variance.OUT_VARIANCE;
    }

    @Nullable
    public static Pair<ClassifierDescriptor, Collection<ClassDescriptor>> platformClassesFor(@NotNull JetType jetType, @NotNull Type asmType) {
        ClassifierDescriptor descriptor = jetType.getConstructor().getDeclarationDescriptor();
        if (descriptor == null) return null;
        if (asmType.getSort() != Type.OBJECT) return null;

        JvmClassName jvmClassName = JvmClassName.byType(asmType);
        return Pair.create(descriptor,
                           JavaToKotlinClassMap.getInstance().mapPlatformClass(jvmClassName.getFqName()));
    }

    @Nullable
    public static String getKotlinTypeNameForSignature(@NotNull JetType jetType, @NotNull Type asmType) {
        Pair<ClassifierDescriptor, Collection<ClassDescriptor>> descriptorAndClassesFound = platformClassesFor(jetType, asmType);
        if (descriptorAndClassesFound != null) {
            Collection<ClassDescriptor> classesFound = descriptorAndClassesFound.getSecond();
            if (classesFound.size() > 1) {
                return JvmClassName.byClassDescriptor(descriptorAndClassesFound.getFirst()).getSignatureName();
            }
        }
        return null;
    }

    @Override
    public Type visitClassDescriptor(ClassDescriptor descriptor, JetType jetType) {
        if (withBuiltinsToJava)
            if (builtin.builtinCanBeFound())
                return builtin.builtinToJava();
            else if (KotlinBuiltIns.getInstance().isArray(jetType))
                return builtinArrayToJava(jetType);

        JvmClassName name = getJvmInternalName(typeMapper.getBindingTrace(), descriptor);
        Type asmType = asmTypeFor(name);
        boolean forceReal = KotlinToJavaTypesMap.getInstance().isForceReal(name);

        writeGenericType(typeMapper, signatureVisitor, asmType, jetType, forceReal, howThisTypeIsUsed);

        typeMapper.checkValidType(asmType);
        return asmType;
    }

    private Type asmTypeFor(JvmClassName name) {
        Type asmType;
        if (mode == JetTypeMapperMode.TRAIT_IMPL) {
            asmType = Type.getObjectType(name.getInternalName() + JvmAbi.TRAIT_IMPL_SUFFIX);
        }
        else {
            asmType = name.getAsmType();
        }
        return asmType;
    }
}
