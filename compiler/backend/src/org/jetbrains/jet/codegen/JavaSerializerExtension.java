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

package org.jetbrains.jet.codegen;

import com.intellij.openapi.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.asm4.Type;
import org.jetbrains.asm4.commons.Method;
import org.jetbrains.jet.descriptors.serialization.JavaProtoBufUtil;
import org.jetbrains.jet.descriptors.serialization.NameTable;
import org.jetbrains.jet.descriptors.serialization.ProtoBuf;
import org.jetbrains.jet.descriptors.serialization.SerializerExtension;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.resolve.name.Name;

public class JavaSerializerExtension extends SerializerExtension {
    private final MemberMap memberMap;

    public JavaSerializerExtension(@NotNull MemberMap memberMap) {
        this.memberMap = memberMap;
    }

    @Override
    public void serializeCallable(
            @NotNull CallableMemberDescriptor callable,
            @NotNull ProtoBuf.Callable.Builder proto,
            @NotNull NameTable nameTable
    ) {
        saveSignature(callable, proto, nameTable);
        saveSrcClassName(callable, proto, nameTable);
    }

    private void saveSignature(
            @NotNull CallableMemberDescriptor callable,
            @NotNull ProtoBuf.Callable.Builder proto,
            @NotNull NameTable nameTable
    ) {
        if (callable instanceof FunctionDescriptor) {
            Method method = memberMap.getMethodOfDescriptor((FunctionDescriptor) callable);
            if (method != null) {
                JavaProtoBufUtil.saveMethodSignature(proto, method, nameTable);
            }
        }
        else if (callable instanceof PropertyDescriptor) {
            PropertyDescriptor property = (PropertyDescriptor) callable;

            PropertyGetterDescriptor getter = property.getGetter();
            PropertySetterDescriptor setter = property.getSetter();
            Method getterMethod = getter == null ? null : memberMap.getMethodOfDescriptor(getter);
            Method setterMethod = setter == null ? null : memberMap.getMethodOfDescriptor(setter);

            Pair<Type, String> field = memberMap.getFieldOfProperty(property);
            Type fieldType;
            String fieldName;
            String syntheticMethodName;
            if (field != null) {
                fieldType = field.first;
                fieldName = field.second;
                syntheticMethodName = null;
            }
            else {
                fieldType = null;
                fieldName = null;
                syntheticMethodName = memberMap.getSyntheticMethodNameOfProperty(property);
            }

            JavaProtoBufUtil.savePropertySignature(proto, fieldType, fieldName, syntheticMethodName, getterMethod, setterMethod, nameTable);
        }
    }

    private void saveSrcClassName(
            @NotNull CallableMemberDescriptor callable,
            @NotNull ProtoBuf.Callable.Builder proto,
            @NotNull NameTable nameTable
    ) {
        Name name = memberMap.getSrcClassNameOfCallable(callable);
        if (name != null) {
            JavaProtoBufUtil.saveSrcClassName(proto, name, nameTable);
        }
    }
}
