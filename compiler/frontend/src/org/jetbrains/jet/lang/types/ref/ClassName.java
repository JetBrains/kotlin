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

package org.jetbrains.jet.lang.types.ref;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.ClassifierDescriptor;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.types.JetType;

/**
 * @author Stepan Koltsov
 *
 * @see JetTypeName
 */
public final class ClassName {

    @NotNull
    private final FqName fqName;
    private final int typeParameterCount;

    public ClassName(@NotNull FqName fqName, int typeParameterCount) {
        this.fqName = fqName;
        this.typeParameterCount = typeParameterCount;
    }

    @NotNull
    public FqName getFqName() {
        return fqName;
    }

    public int getTypeParameterCount() {
        return typeParameterCount;
    }


    public boolean is(@NotNull ClassDescriptor clazz) {
        return DescriptorUtils.getFQName(clazz).equalsTo(fqName);
    }

    public boolean is(@NotNull JetType type) {
        ClassifierDescriptor classifier = type.getConstructor().getDeclarationDescriptor();
        return classifier instanceof ClassDescriptor && is((ClassDescriptor) classifier);
    }


    @Override
    public String toString() {
        return fqName + "<" + typeParameterCount + ">";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ClassName className = (ClassName) o;

        if (typeParameterCount != className.typeParameterCount) return false;
        if (!fqName.equals(className.fqName)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = fqName.hashCode();
        result = 31 * result + typeParameterCount;
        return result;
    }
}
