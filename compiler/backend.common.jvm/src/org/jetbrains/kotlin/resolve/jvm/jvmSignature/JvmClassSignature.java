/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.jvm.jvmSignature;

import org.jetbrains.annotations.Nullable;

import java.util.List;

public class JvmClassSignature {
    private final String name;
    private final String superclassName;
    private final List<String> interfaces;
    private final String javaGenericSignature;

    public JvmClassSignature(String name, String superclassName, List<String> interfaces, @Nullable String javaGenericSignature) {
        this.name = name;
        this.superclassName = superclassName;
        this.interfaces = interfaces;
        this.javaGenericSignature = javaGenericSignature;
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
}
