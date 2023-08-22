/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen.signature;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.codegen.AsmUtil;
import org.jetbrains.kotlin.load.kotlin.JvmDescriptorTypeWriter;
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodGenericSignature;
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodParameterKind;
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodParameterSignature;
import org.jetbrains.kotlin.types.Variance;
import org.jetbrains.org.objectweb.asm.Type;
import org.jetbrains.org.objectweb.asm.commons.Method;

import java.util.ArrayList;
import java.util.List;

public class JvmSignatureWriter extends JvmDescriptorTypeWriter<Type> {

    private final List<JvmMethodParameterSignature> kotlinParameterTypes = new ArrayList<>();

    private Type jvmReturnType;

    private JvmMethodParameterKind currentParameterKind;

    private int currentSignatureSize = 0;

    public JvmSignatureWriter() {
        super(AsmTypeFactory.INSTANCE);
    }

    @Override
    public void writeClass(@NotNull Type objectType) {
        writeClassBegin(objectType);
        writeClassEnd();
    }

    public void writeAsmType(@NotNull Type asmType) {
        switch (asmType.getSort()) {
            case Type.OBJECT:
                writeClassBegin(asmType);
                writeClassEnd();
                return;
            case Type.ARRAY:
                writeArrayType();
                writeAsmType(AsmUtil.correctElementType(asmType));
                writeArrayEnd();
                return;
            default:
                writeJvmTypeAsIs(asmType);
        }
    }

    public void writeClassBegin(Type asmType) {
        writeJvmTypeAsIs(asmType);
    }

    public void writeOuterClassBegin(Type resultingAsmType, String outerInternalName) {
        writeJvmTypeAsIs(resultingAsmType);
    }

    public void writeInnerClass(String name) {
    }

    public void writeClassEnd() {
    }

    public void writeTypeArgument(@NotNull Variance projectionKind) {
    }

    public void writeUnboundedWildcard() {
    }

    public void writeTypeArgumentEnd() {
    }

    public void writeFormalTypeParameter(String name) {
    }

    public void writeClassBound() {
    }

    public void writeClassBoundEnd() {
    }

    public void writeInterfaceBound() {
    }

    public void writeInterfaceBoundEnd() {
    }

    public void writeParametersStart() {
        // hacks
        clearCurrentType();
    }

    public void writeParameterType(JvmMethodParameterKind parameterKind) {
        currentParameterKind = parameterKind;
    }

    public void writeParameterTypeEnd() {
        //noinspection ConstantConditions
        kotlinParameterTypes.add(new JvmMethodParameterSignature(getJvmCurrentType(), currentParameterKind));
        currentSignatureSize += getJvmCurrentType().getSize();

        currentParameterKind = null;
        clearCurrentType();
    }

    public void writeReturnType() {
    }

    public void writeReturnTypeEnd() {
        jvmReturnType = getJvmCurrentType();
        clearCurrentType();
    }

    public void writeSuperclass() {
    }

    public void writeSuperclassEnd() {
    }

    public void writeInterface() {
    }

    public void writeInterfaceEnd() {
    }

    @Nullable
    public String makeJavaGenericSignature() {
        return null;
    }

    @NotNull
    public JvmMethodGenericSignature makeJvmMethodSignature(@NotNull String name) {
        Type[] types = new Type[kotlinParameterTypes.size()];
        for (int i = 0; i < kotlinParameterTypes.size(); i++) {
            types[i] = kotlinParameterTypes.get(i).getAsmType();
        }
        Method asmMethod = new Method(name, jvmReturnType, types);
        return new JvmMethodGenericSignature(asmMethod, kotlinParameterTypes, makeJavaGenericSignature());
    }

    public int getCurrentSignatureSize() {
        return currentSignatureSize;
    }

    public boolean skipGenericSignature() {
        return true;
    }

    @Override
    public String toString() {
        return "empty";
    }
}

