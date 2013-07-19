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
import org.jetbrains.jet.codegen.signature.BothSignatureWriter;
import org.jetbrains.jet.lang.descriptors.ClassifierDescriptor;
import org.jetbrains.jet.lang.resolve.java.AsmTypeConstants;
import org.jetbrains.jet.lang.resolve.java.JavaToKotlinClassMap;
import org.jetbrains.jet.lang.resolve.java.JvmClassName;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.Variance;

public class ContainedBuiltinToJavaMapping extends BuiltinToJavaMapping {
    private JetType containingType = null;

    public ContainedBuiltinToJavaMapping(
            BothSignatureWriter signatureWriter,
            JetTypeMapper typeMapper,
            JetType jetTypeToMap,
            Variance variance,
            JetTypeMapperMode kind
    ) {
        super(signatureWriter, typeMapper, jetTypeToMap, variance, kind);
    }

    public void containInType(JetType containingTypeOfThis) {
        containingType = containingTypeOfThis;
    }

    @Override
    public Type mapKnownParameterAsmType(
            JetType jetType,
            Type asmType,
            @Nullable BothSignatureWriter signatureVisitor,
            @NotNull Variance howThisTypeIsUsed
    ) {
        if (signatureVisitor != null) {
            if (jetType.getArguments().isEmpty()) {
                if (howThisTypeIsUsed == Variance.IN_VARIANCE) {
                    asmType = AsmTypeConstants.OBJECT_TYPE;
                }
                String kotlinTypeName = kotlinTypeNameForArray(jetType, asmType);
                signatureVisitor.writeAsmType(asmType, jetType.isNullable(), kotlinTypeName);
            }
            else {
                JetTypeToJavaTypeMapper.writeGenericType(typeMapper, signatureVisitor, asmType, jetType, false, howThisTypeIsUsed);
            }
        }
        typeMapper.checkValidType(asmType);
        return asmType;
    }

    @Nullable
    private static String kotlinTypeNameForArray(@NotNull JetType jetType, @NotNull Type asmType) {
        ClassifierDescriptor descriptor = jetType.getConstructor().getDeclarationDescriptor();
        if (descriptor == null) return null;
        if (asmType.getSort() != Type.OBJECT) return null;

        JvmClassName jvmClassName = JvmClassName.byType(asmType);
        if (JavaToKotlinClassMap.getInstance().mapPlatformClass(jvmClassName.getFqName()).size() >= 1) {
            return JvmClassName.byClassDescriptor(descriptor).getSignatureName();
        }
        return null;
    }

}
