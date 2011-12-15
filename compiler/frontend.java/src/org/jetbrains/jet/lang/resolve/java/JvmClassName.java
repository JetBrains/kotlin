package org.jetbrains.jet.lang.resolve.java;

import org.jetbrains.annotations.NotNull;

/**
 * @author Stepan Koltsov
 */
public class JvmClassName {
    
    private final String fqName;

    public JvmClassName(@NotNull String fqName) {
        this.fqName = fqName;
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
}
