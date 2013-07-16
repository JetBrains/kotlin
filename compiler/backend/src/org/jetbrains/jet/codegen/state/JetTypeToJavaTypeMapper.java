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

import org.jetbrains.asm4.Type;
import org.jetbrains.jet.codegen.signature.BothSignatureWriter;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.resolve.java.AsmTypeConstants;
import org.jetbrains.jet.lang.resolve.java.JvmAbi;
import org.jetbrains.jet.lang.resolve.java.JvmClassName;
import org.jetbrains.jet.lang.resolve.java.KotlinToJavaTypesMap;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.TypeProjection;
import org.jetbrains.jet.lang.types.Variance;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;

import static org.jetbrains.jet.codegen.AsmUtil.boxType;
import static org.jetbrains.jet.codegen.binding.CodegenBinding.getJvmInternalName;

public class JetTypeToJavaTypeMapper extends JetTypeToJavaTypeMapperNoMatching implements DeclarationDescriptorVisitor<Type, JetType> {
    private final BothSignatureWriter signatureVisitor;
    private final JetTypeMapper self;
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
        self = s;
        builtin = b;
        howThisTypeIsUsed = h;
        mode = kind;
        withBuiltinsToJava = mapBuiltinsToJava;
    }

    @Override
    public Type visitTypeParameterDescriptor(TypeParameterDescriptor typeParameterDescriptor,
                                             JetType jetType) {
        Type type = self.mapType(typeParameterDescriptor.getUpperBoundsAsType(), mode);
        if (signatureVisitor != null) {
            signatureVisitor.writeTypeVariable(typeParameterDescriptor.getName(), jetType.isNullable(), type);
        }
        self.checkValidType(type);
        return type;
    }

    private Type builtinArrayToJava(JetType jetType) {
        if (jetType.getArguments().size() != 1) {
            throw new UnsupportedOperationException("arrays must have one type argument");
        }
        TypeProjection memberProjection = jetType.getArguments().get(0);
        JetType memberType = memberProjection.getType();

        if (signatureVisitor != null) {
            signatureVisitor.writeArrayType(jetType.isNullable(), memberProjection.getProjectionKind());
            self.mapType(memberType, signatureVisitor, JetTypeMapperMode.TYPE_PARAMETER);
            signatureVisitor.writeArrayEnd();
        }

        Type r;
        if (!isGenericsArray(jetType)) {
            r = Type.getType("[" + boxType(self.mapType(memberType, mode)).getDescriptor());
        }
        else {
            r = AsmTypeConstants.JAVA_ARRAY_GENERIC_TYPE;
        }
        self.checkValidType(r);
        return r;
    }

    private static boolean isGenericsArray(JetType type) {
        return KotlinBuiltIns.getInstance().isArray(type) &&
               type.getArguments().get(0).getType().getConstructor().getDeclarationDescriptor() instanceof TypeParameterDescriptor;
    }

    @Override
    public Type visitClassDescriptor(ClassDescriptor descriptor, JetType jetType) {
        if (withBuiltinsToJava)
            if (builtin.builtinCanBeFound())
                return builtin.builtinToJava();
            else if (KotlinBuiltIns.getInstance().isArray(jetType))
                return builtinArrayToJava(jetType);

        JvmClassName name = getJvmInternalName(self.getBindingTrace(), descriptor);
        Type asmType = asmTypeFor(name);
        boolean forceReal = KotlinToJavaTypesMap.getInstance().isForceReal(name);

        self.writeGenericType(signatureVisitor, asmType, jetType, forceReal, howThisTypeIsUsed);

        self.checkValidType(asmType);
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
