/*
 * Copyright 2010-2017 JetBrains s.r.o.
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
import org.jetbrains.kotlin.builtins.DefaultBuiltIns;
import org.jetbrains.kotlin.builtins.KotlinBuiltIns;
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment;
import org.jetbrains.kotlin.config.CommonConfigurationKeysKt;
import org.jetbrains.kotlin.config.LanguageVersionSettings;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl;
import org.jetbrains.kotlin.incremental.components.NoLookupLocation;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.psi.KtExpression;
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall;
import org.jetbrains.kotlin.resolve.calls.results.OverloadResolutionResults;
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowInfoFactory;
import org.jetbrains.kotlin.resolve.descriptorUtil.DescriptorUtilsKt;
import org.jetbrains.kotlin.resolve.lazy.JvmResolveUtil;
import org.jetbrains.kotlin.resolve.scopes.ImportingScope;
import org.jetbrains.kotlin.resolve.scopes.LexicalScopeImpl;
import org.jetbrains.kotlin.resolve.scopes.LexicalScopeKind;
import org.jetbrains.kotlin.test.KotlinTestUtils;
import org.jetbrains.kotlin.tests.di.ContainerForTests;
import org.jetbrains.kotlin.tests.di.InjectionKt;
import org.jetbrains.kotlin.types.KotlinType;
import org.jetbrains.kotlin.types.TypeUtils;
import org.jetbrains.kotlin.types.expressions.ExpressionTypingContext;
import org.jetbrains.kotlin.types.expressions.ExpressionTypingUtils;
import org.jetbrains.kotlin.types.expressions.FakeCallKind;

import java.util.*;

import static org.jetbrains.kotlin.psi.KtPsiFactoryKt.KtPsiFactory;
import static org.junit.Assert.assertNotNull;

public class ExpectedResolveDataUtil {
    private ExpectedResolveDataUtil() {
    }

    @NotNull
    public static Map<String, DeclarationDescriptor> prepareDefaultNameToDescriptors(@NotNull KotlinCoreEnvironment environment) {
        Project project = environment.getProject();
        KotlinBuiltIns builtIns = DefaultBuiltIns.getInstance();

        Map<String, DeclarationDescriptor> nameToDescriptor = new HashMap<>();
        nameToDescriptor.put("kotlin::Int.plus(Int)", standardFunction(builtIns.getInt(), "plus", project,  environment, builtIns.getIntType()));
        FunctionDescriptor descriptorForGet = standardFunction(builtIns.getArray(), "get", project, environment, builtIns.getIntType());
        nameToDescriptor.put("kotlin::Array.get(Int)", descriptorForGet.getOriginal());
        nameToDescriptor.put("kotlin::Int.compareTo(Double)",
                             standardFunction(builtIns.getInt(), "compareTo", project, environment, builtIns.getDoubleType()));
        @NotNull
        FunctionDescriptor descriptorForSet = standardFunction(builtIns.getArray(), "set", project, environment,
                                                               builtIns.getIntType(), builtIns.getIntType());
        nameToDescriptor.put("kotlin::Array.set(Int, Int)", descriptorForSet.getOriginal());

        return nameToDescriptor;
    }

    @NotNull
    public static Map<String, PsiElement> prepareDefaultNameToDeclaration(@NotNull KotlinCoreEnvironment environment) {
        Project project = environment.getProject();
        Map<String, PsiElement> nameToDeclaration = new HashMap<>();

        PsiClass java_util_Collections = findClass("java.util.Collections", environment);
        nameToDeclaration.put("java::java.util.Collections.emptyList()", findMethod(java_util_Collections, "emptyList"));
        nameToDeclaration.put("java::java.util.Collections", java_util_Collections);
        PsiClass java_util_List = findClass("java.util.ArrayList", environment);
        nameToDeclaration.put("java::java.util.List", findClass("java.util.List", environment));
        nameToDeclaration.put("java::java.util.ArrayList", java_util_List);
        nameToDeclaration.put("java::java.util.ArrayList.set()", java_util_List.findMethodsByName("set", true)[0]);
        nameToDeclaration.put("java::java.util.ArrayList.get()", java_util_List.findMethodsByName("get", true)[0]);
        nameToDeclaration.put("java::java", findPackage("java", project));
        nameToDeclaration.put("java::java.util", findPackage("java.util", project));
        nameToDeclaration.put("java::java.lang", findPackage("java.lang", project));
        nameToDeclaration.put("java::java.lang.Object", findClass("java.lang.Object", environment));
        nameToDeclaration.put("java::java.lang.Comparable", findClass("java.lang.Comparable", environment));
        PsiClass java_lang_System = findClass("java.lang.System", environment);
        nameToDeclaration.put("java::java.lang.System", java_lang_System);
        PsiMethod[] methods = findClass("java.io.PrintStream", environment).findMethodsByName("print", true);
        nameToDeclaration.put("java::java.io.PrintStream.print(Object)", methods[8]);
        nameToDeclaration.put("java::java.io.PrintStream.print(Int)", methods[2]);
        nameToDeclaration.put("java::java.io.PrintStream.print(char[])", methods[6]);
        nameToDeclaration.put("java::java.io.PrintStream.print(Double)", methods[5]);
        PsiField outField = java_lang_System.findFieldByName("out", true);
        assertNotNull("'out' property wasn't found", outField);
        nameToDeclaration.put("java::java.lang.System.out", outField);
        PsiClass java_lang_Number = findClass("java.lang.Number", environment);
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
    private static PsiClass findClass(String qualifiedName, KotlinCoreEnvironment environment) {
        ModuleDescriptor module = JvmResolveUtil.analyze(environment).getModuleDescriptor();
        ClassDescriptor classDescriptor = DescriptorUtilsKt.resolveTopLevelClass(module, new FqName(qualifiedName), NoLookupLocation.FROM_TEST);
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
            KotlinCoreEnvironment environment,
            KotlinType... parameterTypes
    ) {
        ModuleDescriptorImpl emptyModule = KotlinTestUtils.createEmptyModule();
        ContainerForTests container = InjectionKt.createContainerForTests(project, emptyModule);
        emptyModule.setDependencies(emptyModule);
        emptyModule.initialize(PackageFragmentProvider.Empty.INSTANCE);

        LexicalScopeImpl lexicalScope = new LexicalScopeImpl(ImportingScope.Empty.INSTANCE, classDescriptor, false,
                                                             classDescriptor.getThisAsReceiverParameter(), Collections.emptyList(),
                                                             LexicalScopeKind.SYNTHETIC);

        LanguageVersionSettings languageVersionSettings = CommonConfigurationKeysKt.getLanguageVersionSettings(environment.getConfiguration());
        ExpressionTypingContext context = ExpressionTypingContext.newContext(
                new BindingTraceContext(), lexicalScope,
                DataFlowInfoFactory.EMPTY, TypeUtils.NO_EXPECTED_TYPE, languageVersionSettings, container.getDataFlowValueFactory());

        KtExpression callElement = KtPsiFactory(project).createExpression(name);

        TemporaryBindingTrace traceWithFakeArgumentInfo =
                TemporaryBindingTrace.create(context.trace, "trace to store fake argument for", name);
        List<KtExpression> fakeArguments = new ArrayList<>(parameterTypes.length);
        for (KotlinType type : parameterTypes) {
            fakeArguments.add(ExpressionTypingUtils.createFakeExpressionOfType(
                    project, traceWithFakeArgumentInfo, "fakeArgument" + fakeArguments.size(), type
            ));
        }

        OverloadResolutionResults<FunctionDescriptor> functions = container.getFakeCallResolver().resolveFakeCall(
                context, null, Name.identifier(name), callElement, callElement, FakeCallKind.OTHER, fakeArguments
        );

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
