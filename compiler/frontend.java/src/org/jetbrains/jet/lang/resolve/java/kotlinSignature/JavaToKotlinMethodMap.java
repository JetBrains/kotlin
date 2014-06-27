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

package org.jetbrains.jet.lang.resolve.java.kotlinSignature;

import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Lists;
import com.intellij.openapi.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.resolve.java.structure.JavaMethod;
import org.jetbrains.jet.lang.resolve.java.structure.JavaSignatureFormatter;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;
import org.jetbrains.jet.renderer.DescriptorRenderer;

import java.util.*;

public class JavaToKotlinMethodMap {
    public static final JavaToKotlinMethodMap INSTANCE = new JavaToKotlinMethodMap();

    private final JavaToKotlinMethodMapGenerated mapContainer = new JavaToKotlinMethodMapGenerated();

    private JavaToKotlinMethodMap() {
    }

    @NotNull
    public List<FunctionDescriptor> getFunctions(
            @NotNull JavaMethod javaMethod,
            @NotNull FqName classFqName,
            @NotNull ClassDescriptor containingClass
    ) {
        ImmutableCollection<ClassData> classDatas = mapContainer.map.get(classFqName.asString());

        List<FunctionDescriptor> result = Lists.newArrayList();

        Set<ClassDescriptor> allSuperClasses = new HashSet<ClassDescriptor>(DescriptorUtils.getSuperclassDescriptors(containingClass));

        String serializedMethod = JavaSignatureFormatter.getInstance().formatMethod(javaMethod);
        for (ClassData classData : classDatas) {
            String expectedSerializedFunction = classData.method2Function.get(serializedMethod);
            if (expectedSerializedFunction == null) continue;

            ClassDescriptor kotlinClass = classData.kotlinClass;
            if (!allSuperClasses.contains(kotlinClass)) continue;

            Collection<FunctionDescriptor> functions = kotlinClass.getDefaultType().getMemberScope().getFunctions(javaMethod.getName());

            for (FunctionDescriptor function : functions) {
                if (expectedSerializedFunction.equals(serializeFunction(function))) {
                    result.add(function);
                }
            }
        }

        return result;
    }

    @NotNull
    public static String serializeFunction(@NotNull FunctionDescriptor fun) {
        return DescriptorRenderer.COMPACT.render(fun);
    }

    // used in generated code
    static Pair<String, String> pair(String a, String b) {
        return Pair.create(a, b);
    }

    // used in generated code
    static void put(
            ImmutableMultimap.Builder<String, ClassData> builder,
            String javaFqName,
            String kotlinQualifiedName,
            Pair<String, String>... methods2Functions
    ) {
        ImmutableMap<String, String> methods2FunctionsMap = pairs2Map(methods2Functions);

        ClassDescriptor kotlinClass;
        if (kotlinQualifiedName.contains(".")) { // Map.Entry and MutableMap.MutableEntry
            String[] kotlinNames = kotlinQualifiedName.split("\\.");
            assert kotlinNames.length == 2 : "unexpected qualified name " + kotlinQualifiedName;

            ClassDescriptor outerClass = KotlinBuiltIns.getInstance().getBuiltInClassByName(Name.identifier(kotlinNames[0]));
            kotlinClass = DescriptorUtils.getInnerClassByName(outerClass, kotlinNames[1]);
            assert kotlinClass != null : "Class not found: " + kotlinQualifiedName;
        }
        else {
            kotlinClass = KotlinBuiltIns.getInstance().getBuiltInClassByName(Name.identifier(kotlinQualifiedName));
        }

        builder.put(javaFqName, new ClassData(kotlinClass, methods2FunctionsMap));
    }

    private static ImmutableMap<String, String> pairs2Map(Pair<String, String>[] pairs) {
        ImmutableMap.Builder<String, String> builder = ImmutableMap.builder();
        for (Pair<String, String> pair : pairs) {
            builder.put(pair.first, pair.second);
        }
        return builder.build();
    }

    static class ClassData {
        @NotNull
        public final ClassDescriptor kotlinClass;
        @NotNull
        public Map<String, String> method2Function;

        public ClassData(@NotNull ClassDescriptor kotlinClass, @NotNull Map<String, String> method2Function) {
            this.kotlinClass = kotlinClass;
            this.method2Function = method2Function;
        }
    }
}
