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
import org.jetbrains.jet.lang.resolve.FqName;
import org.objectweb.asm.Type;

/**
 * @author Stepan Koltsov
 */
public class JvmClassName {

    @NotNull
    private final FqName fqName;

    public JvmClassName(@NotNull String fqName) {
        this(new FqName(fqName));
    }

    public JvmClassName(@NotNull FqName fqName) {
        this.fqName = fqName;
    }

    public static JvmClassName byInternalName(@NotNull String internalName) {
        JvmClassName r = new JvmClassName(internalName.replace('/', '.'));
        r.internalName = internalName;
        return r;
    }

    public static JvmClassName byType(@NotNull Type type) {
        if (type.getSort() != Type.OBJECT) {
            throw new IllegalArgumentException(
                    "must be an object to be converted to " + JvmClassName.class.getSimpleName());
        }
        return byInternalName(type.getInternalName());
    }

    @NotNull
    public FqName getFqName() {
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
            StringBuilder sb = new StringBuilder(fqName.getFqName().length() + 2);
            sb.append('L');
            sb.append(fqName.getFqName().replace('.', '/'));
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
