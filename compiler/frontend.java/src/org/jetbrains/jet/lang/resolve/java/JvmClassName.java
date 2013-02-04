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

package org.jetbrains.jet.lang.resolve.java;

import com.google.common.collect.Lists;
import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.asm4.Type;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.ClassifierDescriptor;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.resolve.name.FqName;

import java.util.List;

public class JvmClassName {

    @NotNull
    public static JvmClassName byInternalName(@NotNull String internalName) {
        return new JvmClassName(internalName);
    }

    @NotNull
    public static JvmClassName byType(@NotNull Type type) {
        if (type.getSort() != Type.OBJECT) {
            throw new IllegalArgumentException("Type is not convertible to " + JvmClassName.class.getSimpleName() + ": " + type);
        }
        return byInternalName(type.getInternalName());
    }

    /**
     * WARNING: fq name cannot be uniquely mapped to JVM class name.
     */
    @NotNull
    public static JvmClassName byFqNameWithoutInnerClasses(@NotNull FqName fqName) {
        JvmClassName r = new JvmClassName(fqNameToInternalName(fqName));
        r.fqName = fqName;
        return r;
    }

    @NotNull
    public static JvmClassName byFqNameWithoutInnerClasses(@NotNull String fqName) {
        return byFqNameWithoutInnerClasses(new FqName(fqName));
    }

    @NotNull
    public static JvmClassName bySignatureName(@NotNull String signatureName) {
        JvmClassName className = new JvmClassName(signatureNameToInternalName(signatureName));
        className.signatureName = signatureName;
        return className;
    }

    private static String encodeSpecialNames(String str) {
        String encodedObjectNames = StringUtil.replace(str, JvmAbi.CLASS_OBJECT_CLASS_NAME, CLASS_OBJECT_REPLACE_GUARD);
        return StringUtil.replace(encodedObjectNames, JvmAbi.TRAIT_IMPL_CLASS_NAME, TRAIT_IMPL_REPLACE_GUARD);
    }

    private static String decodeSpecialNames(String str) {
        String decodedObjectNames = StringUtil.replace(str, CLASS_OBJECT_REPLACE_GUARD, JvmAbi.CLASS_OBJECT_CLASS_NAME);
        return StringUtil.replace(decodedObjectNames, TRAIT_IMPL_REPLACE_GUARD, JvmAbi.TRAIT_IMPL_CLASS_NAME);
    }

    @NotNull
    private static JvmClassName byFqNameAndInnerClassList(@NotNull FqName fqName, @NotNull List<String> innerClassList) {
        String outerClassName = fqNameToInternalName(fqName);
        StringBuilder sb = new StringBuilder(outerClassName);
        for (String innerClassName : innerClassList) {
            sb.append("$").append(innerClassName);
        }
        return new JvmClassName(sb.toString());
    }

    @NotNull
    public static JvmClassName byClassDescriptor(@NotNull ClassifierDescriptor classDescriptor) {
        DeclarationDescriptor descriptor = classDescriptor;

        List<String> innerClassNames = Lists.newArrayList();
        while (descriptor.getContainingDeclaration() instanceof ClassDescriptor) {
            innerClassNames.add(descriptor.getName().getName());
            descriptor = descriptor.getContainingDeclaration();
            assert descriptor != null;
        }

        return byFqNameAndInnerClassList(DescriptorUtils.getFQName(descriptor).toSafe(), innerClassNames);
    }

    @NotNull
    private static String fqNameToInternalName(@NotNull FqName fqName) {
        return fqName.getFqName().replace('.', '/');
    }

    @NotNull
    private static String signatureNameToInternalName(@NotNull String signatureName) {
        return signatureName.replace('.', '$');
    }

    @NotNull
    private static String internalNameToFqName(@NotNull String name) {
        return decodeSpecialNames(encodeSpecialNames(name).replace('$', '.').replace('/', '.'));
    }

    @NotNull
    private static String internalNameToSignatureName(@NotNull String name) {
        return decodeSpecialNames(encodeSpecialNames(name).replace('$', '.'));
    }

    @NotNull
    private static String signatureNameToFqName(@NotNull String name) {
        return name.replace('/', '.');
    }


    private final static String CLASS_OBJECT_REPLACE_GUARD = "<class_object>";
    private final static String TRAIT_IMPL_REPLACE_GUARD = "<trait_impl>";

    // Internal name:  jet/Map$Entry
    // FqName:         jet.Map.Entry
    // Signature name: jet/Map.Entry

    private final String internalName;
    private FqName fqName;
    private String descriptor;
    private String signatureName;

    private Type asmType;

    private JvmClassName(@NotNull String internalName) {
        this.internalName = internalName;
    }

    @NotNull
    public FqName getFqName() {
        if (fqName == null) {
            this.fqName = new FqName(internalNameToFqName(internalName));
        }
        return fqName;
    }

    @NotNull
    public String getInternalName() {
        return internalName;
    }

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

    @NotNull
    public Type getAsmType() {
        if (asmType == null) {
            asmType = Type.getType(getDescriptor());
        }
        return asmType;
    }

    @NotNull
    public String getSignatureName() {
        if (signatureName == null) {
            signatureName = internalNameToSignatureName(internalName);
        }
        return signatureName;
    }

    @NotNull
    public FqName getOuterClassFqName() {
        String signatureName = getSignatureName();
        int index = signatureName.indexOf('.');
        String outerClassName = index != -1 ? signatureName.substring(0, index) : signatureName;
        return new FqName(signatureNameToFqName(outerClassName));
    }

    @NotNull
    public List<String> getInnerClassNameList() {
        List<String> innerClassList = Lists.newArrayList();
        String signatureName = getSignatureName();
        int index = signatureName.indexOf('.');
        while (index != -1) {
            int nextIndex = signatureName.indexOf('.', index + 1);
            String innerClassName = nextIndex != -1 ? signatureName.substring(index + 1, nextIndex) : signatureName.substring(index + 1);
            innerClassList.add(innerClassName);
            index = nextIndex;
        }
        return innerClassList;
    }

    @Override
    public String toString() {
        return getInternalName();
    }

    @Override
    public boolean equals(Object o) {
        // generated by Idea
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        JvmClassName name = (JvmClassName) o;

        if (!internalName.equals(name.internalName)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        // generated by Idea
        return internalName.hashCode();
    }
}
