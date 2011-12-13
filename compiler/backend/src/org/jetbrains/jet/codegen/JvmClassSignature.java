package org.jetbrains.jet.codegen;

import java.util.List;

/**
 * @author Stepan Koltsov
 */
public class JvmClassSignature {
    private final String name;
    private final String superclassName;
    private final List<String> interfaces;
    private final String genericSignature;

    public JvmClassSignature(String name, String superclassName, List<String> interfaces, String genericSignature) {
        this.name = name;
        this.superclassName = superclassName;
        this.interfaces = interfaces;
        this.genericSignature = genericSignature;
    }

    public String getName() {
        return name;
    }

    public String getSuperclassName() {
        return superclassName;
    }

    public List<String> getInterfaces() {
        return interfaces;
    }

    public String getGenericSignature() {
        return genericSignature;
    }
}
