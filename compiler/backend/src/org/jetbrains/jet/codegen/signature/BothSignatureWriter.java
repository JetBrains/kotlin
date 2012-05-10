/*
 * Copyright 2010-2012 JetBrains s.r.o.
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

package org.jetbrains.jet.codegen.signature;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.codegen.JetTypeMapper;
import org.jetbrains.jet.codegen.signature.JvmMethodParameterKind;
import org.jetbrains.jet.codegen.signature.JvmMethodParameterSignature;
import org.jetbrains.jet.codegen.signature.JvmMethodSignature;
import org.jetbrains.jet.lang.resolve.java.JetSignatureUtils;
import org.jetbrains.jet.lang.types.Variance;
import org.jetbrains.jet.rt.signature.JetSignatureAdapter;
import org.jetbrains.jet.rt.signature.JetSignatureReader;
import org.jetbrains.jet.rt.signature.JetSignatureVariance;
import org.jetbrains.jet.rt.signature.JetSignatureWriter;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.Method;
import org.objectweb.asm.signature.SignatureVisitor;
import org.objectweb.asm.signature.SignatureWriter;
import org.objectweb.asm.util.CheckSignatureAdapter;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

/**
 * @author Stepan Koltsov
 */
public class BothSignatureWriter {

    private static final boolean DEBUG_SIGNATURE_WRITER = true;

    public enum Mode {
        METHOD(CheckSignatureAdapter.METHOD_SIGNATURE),
        CLASS(CheckSignatureAdapter.CLASS_SIGNATURE),
        ;
        
        private final int asmType;

        Mode(int asmType) {
            this.asmType = asmType;
        }
    }

    private enum State {
        START,
        TYPE_PARAMETERS,

        PARAMETERS,
        PARAMETER,
        RETURN_TYPE,
        METHOD_END,

        SUPERS,
        CLASS_END,
    }
    
    private final SignatureWriter signatureWriter = new SignatureWriter();
    private final SignatureVisitor signatureVisitor;

    private JetSignatureWriter jetSignatureWriter;
    
    private String kotlinClassParameters;
    private String kotlinClassSignature;
    
    private List<JvmMethodParameterSignature> kotlinParameterTypes = new ArrayList<JvmMethodParameterSignature>();
    private String kotlinReturnType;
    
    private int jvmCurrentTypeArrayLevel;
    private Type jvmCurrentType;
    private Type jvmReturnType;

    private JvmMethodParameterKind currentParameterKind;

    private final Mode mode;
    private final boolean needGenerics;

    private State state = State.START;

    private boolean generic = false;

    public BothSignatureWriter(Mode mode, boolean needGenerics) {
        this.mode = mode;
        this.needGenerics = needGenerics;

        if (DEBUG_SIGNATURE_WRITER) {
            signatureVisitor = new CheckSignatureAdapter(mode.asmType, signatureWriter);
        }
        else {
            signatureVisitor = signatureWriter;
        }
    }

    // TODO: ignore when debugging is disabled
    private Stack<SignatureVisitor> visitors = new Stack<SignatureVisitor>();

    private void push(SignatureVisitor visitor) {
        visitors.push(visitor);
    }

    private void pop() {
        visitors.pop();
    }



    private SignatureVisitor signatureVisitor() {
        return !visitors.isEmpty() ? visitors.peek() : signatureVisitor;
    }
    
    private void checkTopLevel() {
        if (DEBUG_SIGNATURE_WRITER) {
            if (!visitors.isEmpty()) {
                throw new IllegalStateException();
            }
        }
    }
    
    private void checkMode(Mode mode) {
        if (DEBUG_SIGNATURE_WRITER) {
            if (mode != this.mode) {
                throw new IllegalStateException();
            }
        }
    }
    
    private void checkState(State state) {
        if (DEBUG_SIGNATURE_WRITER) {
            if (state != this.state) {
                throw new IllegalStateException();
            }
            if (jetSignatureWriter != null) {
                throw new IllegalStateException();
            }
            checkTopLevel();
        }
    }
    
    private void transitionState(State from, State to) {
        checkState(from);
        state = to;
    }


    /**
     * Shortcut
     */
    public void writeAsmType(Type asmType, boolean nullable) {
        switch (asmType.getSort()) {
            case Type.OBJECT:
                writeClassBegin(asmType.getInternalName(), nullable, false);
                writeClassEnd();
                return;
            case Type.ARRAY:
                writeArrayType(nullable);
                writeAsmType(asmType.getElementType(), false);
                writeArrayEnd();
                return;
            default:
                String descriptor = asmType.getDescriptor();
                if (descriptor.length() != 1) {
                    throw new IllegalStateException();
                }
                writeBaseType(descriptor.charAt(0), nullable);
        }
    }

    private void writeBaseType(char c, boolean nullable) {
        if (nullable) {
            throw new IllegalStateException();
        }
        signatureVisitor().visitBaseType(c);
        jetSignatureWriter.visitBaseType(c, nullable);
        writeAsmType0(Type.getType(String.valueOf(c)));
    }

    public void writeNothing(boolean nullable) {
        if (nullable) {
            signatureVisitor().visitClassType("java/lang/Object");
            signatureVisitor().visitEnd();
        }
        else {
            signatureVisitor().visitBaseType('V');
        }
        jetSignatureWriter.visitClassType("jet/Nothing", nullable, false);
        jetSignatureWriter.visitEnd();
        if (nullable) {
            writeAsmType0(JetTypeMapper.TYPE_OBJECT);
        }
        else {
            writeAsmType0(Type.VOID_TYPE);
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

    public void writeClassBegin(String internalName, boolean nullable, boolean real) {
        signatureVisitor().visitClassType(internalName);
        jetSignatureWriter.visitClassType(internalName, nullable, real);
        writeAsmType0(Type.getObjectType(internalName));
    }

    public void writeClassEnd() {
        signatureVisitor().visitEnd();
        jetSignatureWriter.visitEnd();
    }

    public void writeArrayType(boolean nullable) {
        push(signatureVisitor().visitArrayType());
        jetSignatureWriter.visitArrayType(nullable);
        if (jvmCurrentType == null) {
            ++jvmCurrentTypeArrayLevel;
        }
    }
    
    public void writeArrayEnd() {
        pop();
    }
    
    private static JetSignatureVariance toJetSignatureVariance(Variance variance) {
        switch (variance) {
            case INVARIANT: return JetSignatureVariance.INVARIANT;
            case IN_VARIANCE: return JetSignatureVariance.IN;
            case OUT_VARIANCE: return JetSignatureVariance.OUT;
            default: throw new IllegalStateException();
        }
    }

    public void writeTypeArgument(Variance variance) {
        JetSignatureVariance jsVariance = toJetSignatureVariance(variance);
        push(signatureVisitor().visitTypeArgument(jsVariance.getC()));
        jetSignatureWriter.visitTypeArgument(jsVariance);
        generic = true;
    }
    
    public void writeTypeArgumentEnd() {
        pop();
    }

    public void writeTypeVariable(final String name, boolean nullable, Type asmType) {
        signatureVisitor().visitTypeVariable(name);
        jetSignatureWriter.visitTypeVariable(name, nullable);
        generic = true;
        writeAsmType0(asmType);
    }

    public void writeFormalTypeParameter(final String name, Variance variance, boolean reified) {
        checkTopLevel();

        signatureVisitor().visitFormalTypeParameter(name);
        jetSignatureWriter.visitFormalTypeParameter(name, JetSignatureUtils.translateVariance(variance), reified);

        generic = true;
    }
    
    public void writeFormalTypeParameterEnd() {
        jetSignatureWriter.visitFormalTypeParameterEnd();
    }

    public void writeFormalTypeParametersStart() {
        checkTopLevel();
        transitionState(State.START, State.TYPE_PARAMETERS);
        jetSignatureWriter = new JetSignatureWriter();
    }

    public void writeFormalTypeParametersEnd() {
        jetSignatureWriter.visitSuperclass(); // just to call endFormals

        kotlinClassParameters = jetSignatureWriter.toString();

        jetSignatureWriter = null;

        if (DEBUG_SIGNATURE_WRITER) {
            new JetSignatureReader(kotlinClassParameters).acceptFormalTypeParametersOnly(new JetSignatureAdapter());
        }

        checkState(State.TYPE_PARAMETERS);
    }

    public void writeClassBound() {
        push(signatureVisitor().visitClassBound());
        jetSignatureWriter.visitClassBound();
    }
    
    public void writeClassBoundEnd() {
        pop();
    }

    public void writeInterfaceBound() {
        push(signatureVisitor().visitInterfaceBound());
        jetSignatureWriter.visitInterfaceBound();
    }

    public void writeInterfaceBoundEnd() {
        pop();
    }
    
    public void writeParametersStart() {
        transitionState(State.TYPE_PARAMETERS, State.PARAMETERS);

        // hacks
        jvmCurrentType = null;
        jvmCurrentTypeArrayLevel = 0;
    }

    public void writeParametersEnd() {
        checkState(State.PARAMETERS);
    }

    public void writeParameterType(JvmMethodParameterKind parameterKind) {
        transitionState(State.PARAMETERS, State.PARAMETER);

        push(signatureVisitor().visitParameterType());
        jetSignatureWriter = new JetSignatureWriter();
        if (jvmCurrentType != null || jvmCurrentTypeArrayLevel != 0) {
            throw new IllegalStateException();
        }

        if (currentParameterKind != null) {
            throw new IllegalStateException();
        }
        this.currentParameterKind = parameterKind;

        //jetSignatureWriter.visitParameterType();
    }
    
    public void writeParameterTypeEnd() {
        pop();

        if (jvmCurrentType == null) {
            throw new IllegalStateException();
        }

        String signature = jetSignatureWriter.toString();
        kotlinParameterTypes.add(new JvmMethodParameterSignature(jvmCurrentType, signature, currentParameterKind));

        if (DEBUG_SIGNATURE_WRITER) {
            new JetSignatureReader(signature).acceptTypeOnly(new JetSignatureAdapter());
        }

        currentParameterKind = null;
        jvmCurrentType = null;
        jvmCurrentTypeArrayLevel = 0;

        jetSignatureWriter = null;
        transitionState(State.PARAMETER, State.PARAMETERS);
    }

    public void writeReturnType() {
        transitionState(State.PARAMETERS, State.RETURN_TYPE);

        jetSignatureWriter = new JetSignatureWriter();

        if (jvmCurrentType != null) {
            throw new IllegalStateException();
        }

        push(signatureVisitor().visitReturnType());
        //jetSignatureWriter.visitReturnType();
    }
    
    public void writeReturnTypeEnd() {
        pop();
        
        kotlinReturnType = jetSignatureWriter.toString();

        if (jvmCurrentType == null) {
            throw new IllegalStateException();
        }

        jvmReturnType = jvmCurrentType;
        jvmCurrentType = null;
        jvmCurrentTypeArrayLevel = 0;

        if (DEBUG_SIGNATURE_WRITER) {
            new JetSignatureReader(kotlinReturnType).acceptTypeOnly(new JetSignatureAdapter());
        }

        jetSignatureWriter = null;
        transitionState(State.RETURN_TYPE, State.METHOD_END);
    }
    
    public void writeVoidReturn() {
        writeReturnType();
        writeAsmType(Type.VOID_TYPE, false);
        writeReturnTypeEnd();
    }
    
    public void writeSupersStart() {
        transitionState(State.TYPE_PARAMETERS, State.SUPERS);
        jetSignatureWriter = new JetSignatureWriter();
    }

    public void writeSupersEnd() {
        kotlinClassSignature = jetSignatureWriter.toString();
        jetSignatureWriter = null;

        if (DEBUG_SIGNATURE_WRITER) {
            new JetSignatureReader(kotlinClassSignature).accept(new JetSignatureAdapter());
        }

        transitionState(State.SUPERS, State.CLASS_END);
    }

    public void writeSuperclass() {
        push(signatureVisitor().visitSuperclass());
        jetSignatureWriter.visitSuperclass();
    }
    
    public void writeSuperclassEnd() {
        pop();
        if (!visitors.isEmpty()) {
            throw new IllegalStateException();
        }
    }

    public void writeInterface() {
        checkTopLevel();
        checkMode(Mode.CLASS);

        push(signatureVisitor().visitInterface());
        jetSignatureWriter.visitInterface();
    }

    public void writeInterfaceEnd() {
        pop();
        if (!visitors.isEmpty()) {
            throw new IllegalStateException();
        }
    }



    @NotNull
    public Method makeAsmMethod(String name) {
        List<Type> jvmParameterTypes = new ArrayList<Type>(kotlinParameterTypes.size());
        for (JvmMethodParameterSignature p : kotlinParameterTypes) {
            jvmParameterTypes.add(p.getAsmType());
        }
        return new Method(name, jvmReturnType, jvmParameterTypes.toArray(new Type[0]));
    }

    @Nullable
    public String makeJavaString() {
        if (state != State.METHOD_END && state != State.CLASS_END) {
            throw new IllegalStateException();
        }
        checkTopLevel();
        return generic ? signatureWriter.toString() : null;
    }
    
    @NotNull
    public List<JvmMethodParameterSignature> makeKotlinParameterTypes() {
        checkState(State.METHOD_END);
        // TODO: return nulls if equal to #makeJavaString
        return kotlinParameterTypes;
    }

    @NotNull
    public String makeKotlinReturnTypeSignature() {
        checkState(State.METHOD_END);
        return kotlinReturnType;
    }
    
    public String makeKotlinMethodTypeParameters() {
        checkState(State.METHOD_END);
        return kotlinClassParameters;
    }

    @Nullable
    public String makeKotlinClassSignature() {
        checkState(State.CLASS_END);
        if (kotlinClassParameters == null) {
            throw new IllegalStateException();
        }
        if (kotlinClassSignature == null) {
            throw new IllegalStateException();
        }
        return kotlinClassParameters + kotlinClassSignature;
    }

    @NotNull
    public JvmMethodSignature makeJvmMethodSignature(String name) {
        if (needGenerics) {
            return new JvmMethodSignature(
                    makeAsmMethod(name),
                    makeJavaString(),
                    makeKotlinMethodTypeParameters(),
                    makeKotlinParameterTypes(),
                    makeKotlinReturnTypeSignature()
            );
        }
        else {
            return new JvmMethodSignature(makeAsmMethod(name), makeKotlinParameterTypes());
        }
    }

}
