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

package org.jetbrains.jet.lang.resolve.java;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.objectweb.asm.Type;

/**
 * @author Stepan Koltsov
 */
public class JvmClassName {

    @NotNull
    private final String internalName;

    private JvmClassName(@NotNull String internalName) {
        this.internalName = internalName;
    }

    @NotNull
    public static JvmClassName byInternalName(@NotNull String internalName) {
        return new JvmClassName(internalName);
    }

    @NotNull
    public static JvmClassName byType(@NotNull Type type) {
        if (type.getSort() != Type.OBJECT) {
            throw new IllegalArgumentException(
                    "must be an object to be converted to " + JvmClassName.class.getSimpleName());
        }
        return byInternalName(type.getInternalName());
    }

    /**
     * WARNING: fq name is cannot be uniquely mapped to JVM class name.
     */
    @NotNull
    public static JvmClassName byFqNameWithoutInnerClasses(@NotNull FqName fqName) {
        JvmClassName r = new JvmClassName(fqName.getFqName().replace('.', '/'));
        r.fqName = fqName;
        return r;
    }

    @NotNull
    public static JvmClassName byFqNameWithoutInnerClasses(@NotNull String fqName) {
        return byFqNameWithoutInnerClasses(new FqName(fqName));
    }



    private transient FqName fqName;

    @NotNull
    public FqName getFqName() {
        if (fqName == null) {
            this.fqName = new FqName(internalName.replace('$', '.').replace('/', '.'));
        }
        return fqName;
    }



    @NotNull
    public String getInternalName() {
        return internalName;
    }
    
    private transient String descriptor;

    @NotNull
    public String getDescriptor() {
        if (descriptor == null) {
            StringBuilder sb = new StringBuilder(internalName.length() + 2);
            sb.append('L');
            sb.append(internalName);
            sb.append(';');
            descriptor = sb.toString();
        }
        return descriptor;
    }
    
    private transient Type asmType;

    @NotNull
    public Type getAsmType() {
        if (asmType == null) {
            asmType = Type.getType(getDescriptor());
        }
        return asmType;
    }

    @Override
    public String toString() {
        return getInternalName();
    }
}
