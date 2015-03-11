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

package org.jetbrains.kotlin.codegen.signature;

import com.intellij.util.containers.Stack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodParameterKind;
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodParameterSignature;
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodSignature;
import org.jetbrains.org.objectweb.asm.Type;
import org.jetbrains.org.objectweb.asm.commons.Method;
import org.jetbrains.org.objectweb.asm.signature.SignatureVisitor;
import org.jetbrains.org.objectweb.asm.signature.SignatureWriter;
import org.jetbrains.org.objectweb.asm.util.CheckSignatureAdapter;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.types.Variance;

import java.util.ArrayList;
import java.util.List;

public class BothSignatureWriter {
    public enum Mode {
        METHOD(CheckSignatureAdapter.METHOD_SIGNATURE),
        CLASS(CheckSignatureAdapter.CLASS_SIGNATURE),
        TYPE(CheckSignatureAdapter.TYPE_SIGNATURE);

        private final int asmType;

        Mode(int asmType) {
            this.asmType = asmType;
        }
    }

    private final SignatureWriter signatureWriter = new SignatureWriter();
    private final SignatureVisitor signatureVisitor;

    private final List<JvmMethodParameterSignature> kotlinParameterTypes = new ArrayList<JvmMethodParameterSignature>();

    private int jvmCurrentTypeArrayLevel;
    private Type jvmCurrentType;
    private Type jvmReturnType;

    private JvmMethodParameterKind currentParameterKind;

    private boolean generic = false;

    private int currentSignatureSize = 0;

    public BothSignatureWriter(@NotNull Mode mode) {
        this.signatureVisitor = new CheckSignatureAdapter(mode.asmType, signatureWriter);
    }

    private final Stack<SignatureVisitor> visitors = new Stack<SignatureVisitor>();

    private void push(SignatureVisitor visitor) {
        visitors.push(visitor);
    }

    private void pop() {
        visitors.pop();
    }

    private SignatureVisitor signatureVisitor() {
        return !visitors.isEmpty() ? visitors.peek() : signatureVisitor;
    }

    /**
     * Shortcut
     */
    public void writeAsmType(Type asmType) {
        switch (asmType.getSort()) {
            case Type.OBJECT:
                writeClassBegin(asmType);
                writeClassEnd();
                return;
            case Type.ARRAY:
                writeArrayType();
                writeAsmType(asmType.getElementType());
                writeArrayEnd();
                return;
            default:
                signatureVisitor().visitBaseType(asmType.getDescriptor().charAt(0));
                writeAsmType0(asmType);
        }
    }

    private String makeArrayPrefix() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < jvmCurrentTypeArrayLevel; ++i) {
            sb.append('[');
        }
        return sb.toString();
    }

    private void writeAsmType0(Type type) {
        if (jvmCurrentType == null) {
            jvmCurrentType = Type.getType(makeArrayPrefix() + type.getDescriptor());
        }
    }

    public void writeClassBegin(Type asmType) {
        signatureVisitor().visitClassType(asmType.getInternalName());
        writeAsmType0(asmType);
    }

    public void writeClassEnd() {
        signatureVisitor().visitEnd();
    }

    public void writeArrayType() {
        push(signatureVisitor().visitArrayType());
        if (jvmCurrentType == null) {
            ++jvmCurrentTypeArrayLevel;
        }
    }

    public void writeArrayEnd() {
        pop();
    }

    private static char toJvmVariance(@NotNull Variance variance) {
        switch (variance) {
            case INVARIANT: return '=';
            case IN_VARIANCE: return '-';
            case OUT_VARIANCE: return '+';
            default: throw new IllegalStateException("Unknown variance: " + variance);
        }
    }

    public void writeTypeArgument(@NotNull Variance projectionKind) {
        push(signatureVisitor().visitTypeArgument(toJvmVariance(projectionKind)));

        generic = true;
    }

    public void writeUnboundedWildcard() {
        signatureVisitor().visitTypeArgument();

        generic = true;
    }

    public void writeTypeArgumentEnd() {
        pop();
    }

    public void writeTypeVariable(Name name, Type asmType) {
        signatureVisitor().visitTypeVariable(name.asString());
        generic = true;
        writeAsmType0(asmType);
    }

    public void writeFormalTypeParameter(String name) {
        signatureVisitor().visitFormalTypeParameter(name);

        generic = true;
    }

    public void writeClassBound() {
        push(signatureVisitor().visitClassBound());
    }

    public void writeClassBoundEnd() {
        pop();
    }

    public void writeInterfaceBound() {
        push(signatureVisitor().visitInterfaceBound());
    }

    public void writeInterfaceBoundEnd() {
        pop();
    }

    public void writeParametersStart() {
        // hacks
        jvmCurrentType = null;
        jvmCurrentTypeArrayLevel = 0;
    }

    public void writeParameterType(JvmMethodParameterKind parameterKind) {
        // This magic mimics the behavior of javac that enum constructor have these synthetic parameters in erased signature, but doesn't
        // have them in generic signature. IDEA, javac and their friends rely on this behavior.
        if (parameterKind.isSkippedInGenericSignature()) {
            generic = true;

            // pushing dummy visitor, because we don't want these parameters to appear in generic JVM signature
            push(new SignatureWriter());
        }
        else {
            push(signatureVisitor().visitParameterType());
        }

        this.currentParameterKind = parameterKind;
    }

    public void writeParameterTypeEnd() {
        pop();

        kotlinParameterTypes.add(new JvmMethodParameterSignature(jvmCurrentType, currentParameterKind));
        currentSignatureSize += jvmCurrentType.getSize();

        currentParameterKind = null;
        jvmCurrentType = null;
        jvmCurrentTypeArrayLevel = 0;
    }

    public void writeReturnType() {
        push(signatureVisitor().visitReturnType());
    }

    public void writeReturnTypeEnd() {
        pop();

        jvmReturnType = jvmCurrentType;
        jvmCurrentType = null;
        jvmCurrentTypeArrayLevel = 0;
    }

    public void writeSuperclass() {
        push(signatureVisitor().visitSuperclass());
    }

    public void writeSuperclassEnd() {
        pop();
    }

    public void writeInterface() {
        push(signatureVisitor().visitInterface());
    }

    public void writeInterfaceEnd() {
        pop();
    }


    @Nullable
    public String makeJavaGenericSignature() {
        return generic ? signatureWriter.toString() : null;
    }

    @NotNull
    public JvmMethodSignature makeJvmMethodSignature(@NotNull String name) {
        List<Type> types = new ArrayList<Type>(kotlinParameterTypes.size());
        for (JvmMethodParameterSignature parameter : kotlinParameterTypes) {
            types.add(parameter.getAsmType());
        }
        Method asmMethod = new Method(name, jvmReturnType, types.toArray(new Type[types.size()]));
        return new JvmMethodSignature(asmMethod, makeJavaGenericSignature(), kotlinParameterTypes);
    }

    public int getCurrentSignatureSize() {
        return currentSignatureSize;
    }

    @Override
    public String toString() {
        return signatureWriter.toString();
    }
}

