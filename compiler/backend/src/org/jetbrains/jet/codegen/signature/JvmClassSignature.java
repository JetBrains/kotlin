/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

import java.util.List;

public class JvmClassSignature {
    private final String name;
    private final String superclassName;
    private final List<String> interfaces;
    private final String javaGenericSignature;
    private final String kotlinGenericSignature;

    public JvmClassSignature(
            String name, String superclassName, List<String> interfaces,
            @Nullable String javaGenericSignature, @NotNull String kotlinGenericSignature
    ) {
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

    @NotNull
    public String getKotlinGenericSignature() {
        return kotlinGenericSignature;
    }
}
