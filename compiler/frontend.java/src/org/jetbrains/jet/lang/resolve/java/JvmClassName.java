package org.jetbrains.jet.lang.resolve.java;

import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.Type;

/**
 * @author Stepan Koltsov
 */
public class JvmClassName {
    
    private final String fqName;

    public JvmClassName(@NotNull String fqName) {
        this.fqName = fqName;
    }
    
    public static JvmClassName byInternalName(@NotNull String internalName) {
        JvmClassName r = new JvmClassName(internalName.replace('/', '.'));
        r.internalName = internalName;
        return r;
    }

    public String getFqName() {
        return fqName;
    }
    
    private String internalName;
    
    public String getInternalName() {
        if (internalName == null) {
            String descriptor = getDescriptor();
            internalName = descriptor.substring(1, descriptor.length() - 1);
        }
        return internalName;
    }
    
    private String descriptor;

    public String getDescriptor() {
        if (descriptor == null) {
            StringBuilder sb = new StringBuilder(fqName.length() + 2);
            sb.append('L');
            sb.append(fqName.replace('.', '/'));
            sb.append(';');
            descriptor = sb.toString();
        }
        return descriptor;
    }
    
    private Type asmType;
    
    public Type getAsmType() {
        if (asmType == null) {
            asmType = Type.getType(getDescriptor());
        }
        return asmType;
    }
}
