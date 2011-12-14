package org.jetbrains.jet.codegen;

import jet.typeinfo.TypeInfoVariance;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import jet.typeinfo.internal.signature.JetSignatureAdapter;
import jet.typeinfo.internal.signature.JetSignatureReader;
import jet.typeinfo.internal.signature.JetSignatureWriter;
import org.jetbrains.jet.lang.types.Variance;
import org.objectweb.asm.Type;
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

    enum Mode {
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
    
    private List<String> kotlinParameterTypes = new ArrayList<String>();
    private String kotlinReturnType;

    private final Mode mode;
    private State state = State.START;

    public BothSignatureWriter(Mode mode) {
        this.mode = mode;
        if (DEBUG_SIGNATURE_WRITER) {
            signatureVisitor = new CheckSignatureAdapter(mode.asmType, signatureWriter);
        } else {
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
                writeClassBegin(asmType.getInternalName(), nullable);
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
    }

    public void writeClassBegin(String internalName, boolean nullable) {
        signatureVisitor().visitClassType(internalName);
        jetSignatureWriter.visitClassType(internalName, nullable);
    }

    public void writeClassEnd() {
        signatureVisitor().visitEnd();
        jetSignatureWriter.visitEnd();
    }

    public void writeArrayType(boolean nullable) {
        push(signatureVisitor().visitArrayType());
        jetSignatureWriter.visitArrayType(nullable);
    }
    
    public void writeArrayEnd() {
        pop();
    }

    public void writeTypeArgument(char c) {
        push(signatureVisitor().visitTypeArgument(c));
        jetSignatureWriter.visitTypeArgument(c);
    }
    
    public void writeTypeArgumentEnd() {
        pop();
    }

    public void writeTypeVariable(final String name, boolean nullable) {
        signatureVisitor().visitTypeVariable(name);
        jetSignatureWriter.visitTypeVariable(name, nullable);
    }
    
    private TypeInfoVariance translateVariance(Variance variance) {
        switch (variance) {
            case IN_VARIANCE: return TypeInfoVariance.IN;
            case OUT_VARIANCE: return TypeInfoVariance.OUT;
            case INVARIANT: return TypeInfoVariance.INVARIANT;
            default: throw new IllegalStateException();
        }
    }
    
    public void writeFormalTypeParameter(final String name, Variance variance) {
        checkTopLevel();

        signatureVisitor().visitFormalTypeParameter(name);
        jetSignatureWriter.visitFormalTypeParameter(name, translateVariance(variance));
    }

    public void writerFormalTypeParametersStart() {
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
    }

    public void writeParametersEnd() {
        checkState(State.PARAMETERS);
    }

    public void writeParameterType() {
        push(signatureVisitor().visitParameterType());
        jetSignatureWriter = new JetSignatureWriter();
        //jetSignatureWriter.visitParameterType();
    }
    
    public void writeParameterTypeEnd() {
        pop();
        String signature = jetSignatureWriter.toString();
        kotlinParameterTypes.add(signature);

        if (DEBUG_SIGNATURE_WRITER) {
            new JetSignatureReader(signature).acceptTypeOnly(new JetSignatureAdapter());
        }

        jetSignatureWriter = null;
    }

    public void writeReturnType() {
        transitionState(State.PARAMETERS, State.RETURN_TYPE);

        jetSignatureWriter = new JetSignatureWriter();

        push(signatureVisitor().visitReturnType());
        //jetSignatureWriter.visitReturnType();
    }
    
    public void writeReturnTypeEnd() {
        pop();
        
        kotlinReturnType = jetSignatureWriter.toString();

        if (DEBUG_SIGNATURE_WRITER) {
            new JetSignatureReader(kotlinReturnType).acceptTypeOnly(new JetSignatureAdapter());
        }

        jetSignatureWriter = null;
        transitionState(State.RETURN_TYPE, State.METHOD_END);
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




    @Nullable
    public String makeJavaString() {
        if (state != State.METHOD_END && state != State.CLASS_END) {
            throw new IllegalStateException();
        }
        checkTopLevel();
        // TODO: return null if not generic
        return signatureWriter.toString();
    }

    @NotNull
    public List<String> makeKotlinSignatures() {
        checkState(State.METHOD_END);
        // TODO: return nulls if equal to #makeJavaString
        return kotlinParameterTypes;
    }

    @Nullable
    public String makeKotlinReturnTypeSignature() {
        checkState(State.METHOD_END);
        return kotlinReturnType;
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

}
