/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.resolve;

import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.builtins.KotlinBuiltIns;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.di.InjectorForTests;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall;
import org.jetbrains.kotlin.resolve.calls.results.OverloadResolutionResults;
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowInfo;
import org.jetbrains.kotlin.resolve.descriptorUtil.DescriptorUtilPackage;
import org.jetbrains.kotlin.resolve.lazy.LazyResolveTestUtil;
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue;
import org.jetbrains.kotlin.test.JetTestUtils;
import org.jetbrains.kotlin.types.JetType;
import org.jetbrains.kotlin.types.TypeUtils;
import org.jetbrains.kotlin.types.expressions.ExpressionTypingContext;
import org.jetbrains.kotlin.types.expressions.ExpressionTypingServices;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertNotNull;

public class JetExpectedResolveDataUtil {
    private JetExpectedResolveDataUtil() {
    }

    public static Map<String, DeclarationDescriptor> prepareDefaultNameToDescriptors(Project project) {
        KotlinBuiltIns builtIns = KotlinBuiltIns.getInstance();

        Map<String, DeclarationDescriptor> nameToDescriptor = new HashMap<String, DeclarationDescriptor>();
        nameToDescriptor.put("kotlin::Int.plus(Int)", standardFunction(builtIns.getInt(), "plus", project, builtIns.getIntType()));
        FunctionDescriptor descriptorForGet = standardFunction(builtIns.getArray(), "get", project, builtIns.getIntType());
        nameToDescriptor.put("kotlin::Array.get(Int)", descriptorForGet.getOriginal());
        nameToDescriptor.put("kotlin::Int.compareTo(Double)", standardFunction(builtIns.getInt(), "compareTo", project, builtIns.getDoubleType()));
        @NotNull
        FunctionDescriptor descriptorForSet = standardFunction(builtIns.getArray(), "set", project, builtIns.getIntType(), builtIns.getIntType());
        nameToDescriptor.put("kotlin::Array.set(Int, Int)", descriptorForSet.getOriginal());

        return nameToDescriptor;
    }

    public static Map<String, PsiElement> prepareDefaultNameToDeclaration(Project project) {
        Map<String, PsiElement> nameToDeclaration = new HashMap<String, PsiElement>();

        PsiClass java_util_Collections = findClass("java.util.Collections", project);
        nameToDeclaration.put("java::java.util.Collections.emptyList()", findMethod(java_util_Collections, "emptyList"));
        nameToDeclaration.put("java::java.util.Collections", java_util_Collections);
        PsiClass java_util_List = findClass("java.util.ArrayList", project);
        nameToDeclaration.put("java::java.util.List", findClass("java.util.List", project));
        nameToDeclaration.put("java::java.util.ArrayList", java_util_List);
        nameToDeclaration.put("java::java.util.ArrayList.set()", java_util_List.findMethodsByName("set", true)[0]);
        nameToDeclaration.put("java::java.util.ArrayList.get()", java_util_List.findMethodsByName("get", true)[0]);
        nameToDeclaration.put("java::java", findPackage("java", project));
        nameToDeclaration.put("java::java.util", findPackage("java.util", project));
        nameToDeclaration.put("java::java.lang", findPackage("java.lang", project));
        nameToDeclaration.put("java::java.lang.Object", findClass("java.lang.Object", project));
        nameToDeclaration.put("java::java.lang.Comparable", findClass("java.lang.Comparable", project));
        PsiClass java_lang_System = findClass("java.lang.System", project);
        nameToDeclaration.put("java::java.lang.System", java_lang_System);
        PsiMethod[] methods = findClass("java.io.PrintStream", project).findMethodsByName("print", true);
        nameToDeclaration.put("java::java.io.PrintStream.print(Object)", methods[8]);
        nameToDeclaration.put("java::java.io.PrintStream.print(Int)", methods[2]);
        nameToDeclaration.put("java::java.io.PrintStream.print(char[])", methods[6]);
        nameToDeclaration.put("java::java.io.PrintStream.print(Double)", methods[5]);
        PsiField outField = java_lang_System.findFieldByName("out", true);
        assertNotNull("'out' property wasn't found", outField);
        nameToDeclaration.put("java::java.lang.System.out", outField);
        PsiClass java_lang_Number = findClass("java.lang.Number", project);
        nameToDeclaration.put("java::java.lang.Number", java_lang_Number);
        nameToDeclaration.put("java::java.lang.Number.intValue()", java_lang_Number.findMethodsByName("intValue", true)[0]);

        return nameToDeclaration;
    }

    @NotNull
    private static PsiElement findPackage(String qualifiedName, Project project) {
        JavaPsiFacade javaFacade = JavaPsiFacade.getInstance(project);
        PsiPackage javaFacadePackage = javaFacade.findPackage(qualifiedName);
        assertNotNull("Package wasn't found: " + qualifiedName, javaFacadePackage);
        return javaFacadePackage;
    }

    @NotNull
    private static PsiMethod findMethod(PsiClass psiClass, String name) {
        PsiMethod[] emptyLists = psiClass.findMethodsByName(name, true);
        return emptyLists[0];
    }

    @NotNull
    private static PsiClass findClass(String qualifiedName, Project project) {
        ModuleDescriptor module = LazyResolveTestUtil.resolveProject(project);
        ClassDescriptor classDescriptor = DescriptorUtilPackage.resolveTopLevelClass(module, new FqName(qualifiedName));
        assertNotNull("Class descriptor wasn't resolved: " + qualifiedName, classDescriptor);
        PsiClass psiClass = (PsiClass) DescriptorToSourceUtils.getSourceFromDescriptor(classDescriptor);
        assertNotNull("Class declaration wasn't found: " + classDescriptor, psiClass);
        return psiClass;
    }

    @NotNull
    private static FunctionDescriptor standardFunction(
            ClassDescriptor classDescriptor,
            String name,
            Project project,
            JetType... parameterTypes
    ) {
        ModuleDescriptor emptyModule = JetTestUtils.createEmptyModule();
        InjectorForTests injector = new InjectorForTests(project, emptyModule);
        ExpressionTypingServices expressionTypingServices = injector.getExpressionTypingServices();

        ExpressionTypingContext context = ExpressionTypingContext.newContext(
                expressionTypingServices, new BindingTraceContext(), classDescriptor.getDefaultType().getMemberScope(),
                DataFlowInfo.EMPTY, TypeUtils.NO_EXPECTED_TYPE);

        OverloadResolutionResults<FunctionDescriptor> functions = injector.getExpressionTypingUtils().resolveFakeCall(
                context, ReceiverValue.NO_RECEIVER, Name.identifier(name), null, parameterTypes);

        for (ResolvedCall<? extends FunctionDescriptor> resolvedCall : functions.getResultingCalls()) {
            List<ValueParameterDescriptor> unsubstitutedValueParameters = resolvedCall.getResultingDescriptor().getValueParameters();
            for (int i = 0, unsubstitutedValueParametersSize = unsubstitutedValueParameters.size(); i < unsubstitutedValueParametersSize; i++) {
                ValueParameterDescriptor unsubstitutedValueParameter = unsubstitutedValueParameters.get(i);
                if (unsubstitutedValueParameter.getType().equals(parameterTypes[i])) {
                    return resolvedCall.getResultingDescriptor();
                }
            }
        }
        throw new IllegalArgumentException("Not found: kotlin::" + classDescriptor.getName() + "." + name + "(" +
                                           Arrays.toString(parameterTypes) + ")");
    }
}
