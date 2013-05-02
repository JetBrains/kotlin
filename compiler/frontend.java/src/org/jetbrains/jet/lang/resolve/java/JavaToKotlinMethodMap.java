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

import com.google.common.collect.*;
import com.intellij.openapi.util.Pair;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiSubstitutor;
import com.intellij.psi.util.PsiFormatUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;
import org.jetbrains.jet.renderer.DescriptorRenderer;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.intellij.psi.util.PsiFormatUtilBase.*;

public class JavaToKotlinMethodMap {
    public static final JavaToKotlinMethodMap INSTANCE = new JavaToKotlinMethodMap();

    private final JavaToKotlinMethodMapGenerated mapContainer = new JavaToKotlinMethodMapGenerated();

    private JavaToKotlinMethodMap() {
    }

    @NotNull
    public List<FunctionDescriptor> getFunctions(@NotNull PsiMethod psiMethod, @NotNull ClassDescriptor containingClass) {
        ImmutableCollection<ClassData> classDatas = mapContainer.map.get(psiMethod.getContainingClass().getQualifiedName());

        List<FunctionDescriptor> result = Lists.newArrayList();

        Set<ClassDescriptor> allSuperClasses = DescriptorUtils.getAllSuperClasses(containingClass);

        String serializedPsiMethod = serializePsiMethod(psiMethod);
        for (ClassData classData : classDatas) {
            String expectedSerializedFunction = classData.method2Function.get(serializedPsiMethod);
            if (expectedSerializedFunction == null) continue;

            ClassDescriptor kotlinClass = classData.kotlinClass;
            if (!allSuperClasses.contains(kotlinClass)) continue;


            Collection<FunctionDescriptor> functions =
                    kotlinClass.getDefaultType().getMemberScope().getFunctions(Name.identifier(psiMethod.getName()));

            for (FunctionDescriptor function : functions) {
                if (expectedSerializedFunction.equals(serializeFunction(function))) {
                    result.add(function);
                }
            }
        }

        return result;
    }

    @NotNull
    public static String serializePsiMethod(@NotNull PsiMethod psiMethod) {
        return PsiFormatUtil.formatMethod(
                psiMethod, PsiSubstitutor.EMPTY, SHOW_NAME | SHOW_PARAMETERS, SHOW_TYPE | SHOW_FQ_CLASS_NAMES);
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
