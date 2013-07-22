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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.asm4.Type;
import static org.jetbrains.jet.codegen.AsmUtil.boxType;
import org.jetbrains.jet.codegen.signature.BothSignatureWriter;
import org.jetbrains.jet.lang.resolve.java.KotlinToJavaTypesMap;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.Variance;

public class BuiltinToJavaMapping {
    private final BothSignatureWriter signatureVisitor;
    private final JetTypeMapper typeMapper;
    private final JetType jetType;
    private final Type known;
    private final Variance howThisTypeIsUsed;
    private final JetTypeMapperMode mode;

    public BuiltinToJavaMapping(BothSignatureWriter signatureWriter,
                                JetTypeMapper typeMapper,
                                JetType jetTypeToMap,
                                Variance variance,
                                JetTypeMapperMode kind) {
        signatureVisitor = signatureWriter;
        this.typeMapper = typeMapper;
        jetType = jetTypeToMap;
        known = KotlinToJavaTypesMap.getInstance().getJavaAnalog(jetType);
        howThisTypeIsUsed = variance;
        mode = kind;
    }

    public boolean builtinCanBeFound() { return known != null; }

    public Type builtinToJava() {
        switch (mode) {
            case VALUE:
                return mapKnownAsmType(jetType, known, signatureVisitor, howThisTypeIsUsed);
            case TYPE_PARAMETER:
                return mapKnownAsmType(jetType, boxType(known), signatureVisitor, howThisTypeIsUsed);
            case TRAIT_IMPL:
                throw new IllegalStateException("TRAIT_IMPL is not possible for " + jetType);
            case IMPL:
                return mapKnownAsmType(jetType, known, signatureVisitor, howThisTypeIsUsed);
            default:
                throw new IllegalStateException("unknown kind: " + mode);
        }
    }

    public Type mapKnownAsmType(
            JetType jetType,
            Type asmType,
            @Nullable BothSignatureWriter signatureVisitor,
            @NotNull Variance howThisTypeIsUsed
    ) {
        if (signatureVisitor != null) {
            if (jetType.getArguments().isEmpty()) {
                String kotlinTypeName = JetTypeToJavaTypeMapper.getKotlinTypeNameForSignature(jetType, asmType);
                signatureVisitor.writeAsmType(asmType, jetType.isNullable(), kotlinTypeName);
            }
            else {
                JetTypeToJavaTypeMapper.writeGenericType(typeMapper, signatureVisitor, asmType, jetType, false, howThisTypeIsUsed);
            }
        }
        typeMapper.checkValidType(asmType);
        return asmType;
    }
}
