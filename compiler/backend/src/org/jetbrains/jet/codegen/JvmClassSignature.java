package org.jetbrains.jet.codegen;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * @author Stepan Koltsov
 */
public class JvmClassSignature {
    private final String name;
    private final String superclassName;
    private final List<String> interfaces;
    private final String javaGenericSignature;
    private final String kotlinGenericSignature;

    public JvmClassSignature(String name, String superclassName, List<String> interfaces,
            @Nullable String javaGenericSignature, @Nullable String kotlinGenericSignature) {
        this.name = name;
        this.superclassName = superclassName;
        this.interfaces = interfaces;
        this.javaGenericSignature = javaGenericSignature;
        this.kotlinGenericSignature = kotlinGenericSignature;
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

    public String getJavaGenericSignature() {
        return javaGenericSignature;
    }

    public String getKotlinGenericSignature() {
        return kotlinGenericSignature;
    }
}
