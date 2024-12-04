/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen;

import com.intellij.util.ArrayUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.codegen.intrinsics.HashCode;
import org.jetbrains.kotlin.codegen.intrinsics.IntrinsicMethods;
import org.jetbrains.kotlin.config.JvmTarget;
import org.jetbrains.kotlin.config.LanguageFeature;
import org.jetbrains.kotlin.config.LanguageVersionSettings;
import org.jetbrains.kotlin.descriptors.CallableDescriptor;
import org.jetbrains.kotlin.descriptors.FunctionDescriptor;
import org.jetbrains.kotlin.descriptors.VariableAccessorDescriptor;
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmProtoBufUtil;
import org.jetbrains.kotlin.metadata.jvm.serialization.JvmStringTable;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.protobuf.MessageLite;
import org.jetbrains.org.objectweb.asm.AnnotationVisitor;
import org.jetbrains.org.objectweb.asm.MethodVisitor;
import org.jetbrains.org.objectweb.asm.Type;
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter;

import static org.jetbrains.kotlin.codegen.AsmUtil.*;

public class DescriptorAsmUtil {
    private DescriptorAsmUtil() {
    }

    @NotNull
    public static String getNameForReceiverParameter(
            @NotNull CallableDescriptor descriptor,
            @NotNull LanguageVersionSettings languageVersionSettings
    ) {
        if (!languageVersionSettings.supportsFeature(LanguageFeature.NewCapturedReceiverFieldNamingConvention)) {
            return RECEIVER_PARAMETER_NAME;
        }

        Name callableName = null;

        if (descriptor instanceof FunctionDescriptor) {
            if (descriptor instanceof VariableAccessorDescriptor) {
                VariableAccessorDescriptor accessor = (VariableAccessorDescriptor) descriptor;
                callableName = accessor.getCorrespondingVariable().getName();
            }
        }

        if (callableName == null) {
            callableName = descriptor.getName();
        }

        if (callableName.isSpecial()) {
            return RECEIVER_PARAMETER_NAME;
        }

        return getLabeledThisName(callableName.asString(), LABELED_THIS_PARAMETER, RECEIVER_PARAMETER_NAME);
    }

    public static void genHashCode(MethodVisitor mv, InstructionAdapter iv, Type type, JvmTarget jvmTarget) {
        if (type.getSort() == Type.ARRAY) {
            Type elementType = correctElementType(type);
            if (elementType.getSort() == Type.OBJECT || elementType.getSort() == Type.ARRAY) {
                iv.invokestatic("java/util/Arrays", "hashCode", "([Ljava/lang/Object;)I", false);
            }
            else {
                iv.invokestatic("java/util/Arrays", "hashCode", "(" + type.getDescriptor() + ")I", false);
            }
        }
        else if (type.getSort() == Type.OBJECT) {
            iv.invokevirtual("java/lang/Object", "hashCode", "()I", false);
        }
        else {
            HashCode.Companion.invokeHashCode(iv, type);
        }
    }

    public static void genAreEqualCall(InstructionAdapter v) {
        v.invokestatic(IntrinsicMethods.INTRINSICS_CLASS_NAME, "areEqual", "(Ljava/lang/Object;Ljava/lang/Object;)Z", false);
    }

    public static void genIncrement(Type baseType, int myDelta, InstructionAdapter v) {
        Type operationType = numberFunctionOperandType(baseType);
        numConst(myDelta, operationType, v);
        v.add(operationType);
        StackValue.coerce(operationType, baseType, v);
    }

    public static void writeAnnotationData(
            @NotNull AnnotationVisitor av, @NotNull MessageLite message, @NotNull JvmStringTable stringTable
    ) {
        AsmUtil.writeAnnotationData(av, JvmProtoBufUtil.writeData(message, stringTable), ArrayUtil.toStringArray(stringTable.getStrings()));
    }
}
