/*
 * Copyright 2010-2016 JetBrains s.r.o.
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
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodParameterKind;
import org.jetbrains.kotlin.types.Variance;
import org.jetbrains.org.objectweb.asm.Type;
import org.jetbrains.org.objectweb.asm.signature.SignatureVisitor;
import org.jetbrains.org.objectweb.asm.signature.SignatureWriter;
import org.jetbrains.org.objectweb.asm.util.CheckSignatureAdapter;

public class BothSignatureWriter extends JvmSignatureWriter {
    public enum Mode {
        METHOD(CheckSignatureAdapter.METHOD_SIGNATURE),
        CLASS(CheckSignatureAdapter.CLASS_SIGNATURE),
        TYPE(CheckSignatureAdapter.TYPE_SIGNATURE),
        // Expected to be used only from light classes for type mapping
        // It's needed because CheckSignatureAdapter.TYPE_SIGNATURE doesn't allow V (void) types.
        // They're only allowed with CheckSignatureAdapter.METHOD_SIGNATURE after calling `visitReturnType` while in light classes,
        // we only need to map distinct types
        SKIP_CHECKS(null);

        private final Integer asmType;

        Mode(Integer asmType) {
            this.asmType = asmType;
        }
    }

    private final SignatureWriter signatureWriter = new SignatureWriter();
    private final SignatureVisitor signatureVisitor;

    private boolean generic = false;

    public BothSignatureWriter(@NotNull Mode mode) {
        this.signatureVisitor =
                mode.asmType != null
                ? new CheckSignatureAdapter(mode.asmType, signatureWriter)
                : signatureWriter
        ;
    }

    private final Stack<SignatureVisitor> visitors = new Stack<>();

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
    @Override
    public void writeAsmType(@NotNull Type asmType) {
        if (asmType.getSort() != Type.OBJECT && asmType.getSort() != Type.ARRAY) {
            signatureVisitor().visitBaseType(asmType.getDescriptor().charAt(0));
        }
        super.writeAsmType(asmType);
    }


    @Override
    public void writeClassBegin(Type asmType) {
        signatureVisitor().visitClassType(asmType.getInternalName());
        super.writeClassBegin(asmType);
    }

    @Override
    public void writeOuterClassBegin(Type resultingAsmType, String outerInternalName) {
        signatureVisitor().visitClassType(outerInternalName);
        super.writeOuterClassBegin(resultingAsmType, outerInternalName);
    }

    @Override
    public void writeInnerClass(String name) {
        signatureVisitor().visitInnerClassType(name);
        super.writeInnerClass(name);
    }

    @Override
    public void writeClassEnd() {
        signatureVisitor().visitEnd();
        super.writeClassEnd();
    }

    @Override
    public void writeArrayType() {
        push(signatureVisitor().visitArrayType());
        super.writeArrayType();
    }

    @Override
    public void writeArrayEnd() {
        pop();
        super.writeArrayEnd();
    }

    private static char toJvmVariance(@NotNull Variance variance) {
        switch (variance) {
            case INVARIANT: return '=';
            case IN_VARIANCE: return '-';
            case OUT_VARIANCE: return '+';
            default: throw new IllegalStateException("Unknown variance: " + variance);
        }
    }

    @Override
    public void writeTypeArgument(@NotNull Variance projectionKind) {
        push(signatureVisitor().visitTypeArgument(toJvmVariance(projectionKind)));
        generic = true;
        super.writeTypeArgument(projectionKind);
    }

    @Override
    public void writeUnboundedWildcard() {
        signatureVisitor().visitTypeArgument();
        generic = true;
        super.writeUnboundedWildcard();
    }

    @Override
    public void writeTypeArgumentEnd() {
        pop();
        super.writeTypeArgumentEnd();
    }

    @Override
    public void writeTypeVariable(@NotNull Name name, @NotNull Type asmType) {
        signatureVisitor().visitTypeVariable(name.asString());
        generic = true;
        super.writeTypeVariable(name, asmType);
    }

    @Override
    public void writeFormalTypeParameter(String name) {
        signatureVisitor().visitFormalTypeParameter(name);
        generic = true;
        super.writeFormalTypeParameter(name);
    }

    @Override
    public void writeClassBound() {
        push(signatureVisitor().visitClassBound());
        super.writeClassBound();
    }

    @Override
    public void writeClassBoundEnd() {
        pop();
        super.writeClassBoundEnd();
    }

    @Override
    public void writeInterfaceBound() {
        push(signatureVisitor().visitInterfaceBound());
        super.writeInterfaceBound();
    }

    @Override
    public void writeInterfaceBoundEnd() {
        pop();
        super.writeInterfaceBoundEnd();
    }

    @Override
    public void writeParametersStart() {
        super.writeParametersStart();
    }

    @Override
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
        super.writeParameterType(parameterKind);
    }

    @Override
    public void writeParameterTypeEnd() {
        pop();
        super.writeParameterTypeEnd();
    }

    @Override
    public void writeReturnType() {
        push(signatureVisitor().visitReturnType());
        super.writeReturnType();
    }

    @Override
    public void writeReturnTypeEnd() {
        pop();
        super.writeReturnTypeEnd();
    }

    @Override
    public void writeSuperclass() {
        push(signatureVisitor().visitSuperclass());
        super.writeSuperclass();
    }

    @Override
    public void writeSuperclassEnd() {
        pop();
        super.writeSuperclassEnd();
    }

    @Override
    public void writeInterface() {
        push(signatureVisitor().visitInterface());
        super.writeInterface();
    }

    @Override
    public void writeInterfaceEnd() {
        pop();
        super.writeInterfaceEnd();
    }

    @Override
    @Nullable
    public String makeJavaGenericSignature() {
        return generic ? signatureWriter.toString() : null;
    }

    @Override
    public boolean skipGenericSignature() {
        return false;
    }

    @Override
    public String toString() {
        return signatureWriter.toString();
    }
}

