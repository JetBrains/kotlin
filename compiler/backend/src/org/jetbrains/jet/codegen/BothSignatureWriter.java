package org.jetbrains.jet.codegen;

import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.codegen.signature.JetSignatureWriter;
import org.objectweb.asm.Type;
import org.objectweb.asm.signature.SignatureVisitor;
import org.objectweb.asm.signature.SignatureWriter;
import org.objectweb.asm.util.CheckSignatureAdapter;

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
    
    private final SignatureWriter signatureWriter = new SignatureWriter();
    private final SignatureVisitor signatureVisitor;

    private final JetSignatureWriter jetSignatureWriter = new JetSignatureWriter();

    private final Mode mode;

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


    /**
     * Shortcut
     */
    public void writeAsmType(Type asmType) {
        switch (asmType.getSort()) {
            case Type.OBJECT:
                writeClassBegin(asmType.getInternalName());
                writeClassEnd();
                return;
            case Type.ARRAY:
                writeArrayType();
                writeAsmType(asmType.getElementType());
                writeArrayEnd();
                return;
            default:
                String descriptor = asmType.getDescriptor();
                if (descriptor.length() != 1) {
                    throw new IllegalStateException();
                }
                signatureVisitor().visitBaseType(descriptor.charAt(0));
        }
    }
    
    public void writeClassBegin(String internalName) {
        signatureVisitor().visitClassType(internalName);
    }

    public void writeClassEnd() {
        signatureVisitor().visitEnd();
    }

    public void writeArrayType() {
        push(signatureVisitor().visitArrayType());
    }
    
    public void writeArrayEnd() {
        pop();
    }

    public void writeTypeArgument(char c) {
        push(signatureVisitor().visitTypeArgument(c));
    }
    
    public void writeTypeArgumentEnd() {
        pop();
    }

    public void writeTypeVariable(final String name) {
        signatureVisitor().visitTypeVariable(name);
    }
    
    public void writeFormalTypeParameter(final String name) {
        checkTopLevel();

        signatureVisitor().visitFormalTypeParameter(name);
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

    public void writeParameterType() {
        push(signatureVisitor().visitParameterType());
    }
    
    public void writeParameterTypeEnd() {
        pop();
    }

    public void writeReturnType() {
        push(signatureVisitor().visitReturnType());
    }
    
    public void writeReturnTypeEnd() {
        pop();
    }

    public void writeSuperclass() {
        checkTopLevel();
        checkMode(Mode.CLASS);

        push(signatureVisitor().visitSuperclass());
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
    }

    public void writeInterfaceEnd() {
        pop();
        if (!visitors.isEmpty()) {
            throw new IllegalStateException();
        }
    }




    @Nullable
    public String makeJavaString() {
        if (!visitors.isEmpty()) {
            throw new IllegalStateException();
        }
        return signatureWriter.toString();
    }

    @Nullable
    public String makeKotlinString() {
        // TODO: not implemented yet
        return null;
    }

}
