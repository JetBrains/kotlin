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

package org.jetbrains.jet.resolve;

import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import junit.framework.Test;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.JetTestCaseBuilder;
import org.jetbrains.jet.di.InjectorForJavaSemanticServices;
import org.jetbrains.jet.di.InjectorForTests;
import org.jetbrains.jet.lang.descriptors.ClassDescriptor;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor;
import org.jetbrains.jet.lang.descriptors.ValueParameterDescriptor;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.BindingTraceContext;
import org.jetbrains.jet.lang.resolve.calls.autocasts.DataFlowInfo;
import org.jetbrains.jet.lang.resolve.calls.model.ResolvedCall;
import org.jetbrains.jet.lang.resolve.calls.results.OverloadResolutionResults;
import org.jetbrains.jet.lang.resolve.java.PsiClassFinder;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverValue;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.TypeUtils;
import org.jetbrains.jet.lang.types.expressions.ExpressionTypingContext;
import org.jetbrains.jet.lang.types.expressions.ExpressionTypingServices;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;
import org.jetbrains.jet.parsing.JetParsingTest;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.jetbrains.jet.lang.types.expressions.ExpressionTypingUtils.resolveFakeCall;

public class JetResolveTest extends ExtensibleResolveTestCase {

    private final String path;
    private final String name;

    public JetResolveTest(String path, String name) {
        this.path = path;
        this.name = name;
    }

    @Override
    protected ExpectedResolveData getExpectedResolveData() {
        Project project = getProject();
        KotlinBuiltIns builtIns = KotlinBuiltIns.getInstance();
        Map<String, DeclarationDescriptor> nameToDescriptor = new HashMap<String, DeclarationDescriptor>();
        nameToDescriptor.put("kotlin::Int.plus(Int)", standardFunction(builtIns.getInt(), "plus", builtIns.getIntType()));
        FunctionDescriptor descriptorForGet = standardFunction(builtIns.getArray(), "get", builtIns.getIntType());
        nameToDescriptor.put("kotlin::Array.get(Int)", descriptorForGet.getOriginal());
        nameToDescriptor.put("kotlin::Int.compareTo(Double)", standardFunction(builtIns.getInt(), "compareTo", builtIns.getDoubleType()));
        @NotNull
        FunctionDescriptor descriptorForSet = standardFunction(builtIns.getArray(), "set", builtIns.getIntType(), builtIns.getIntType());
        nameToDescriptor.put("kotlin::Array.set(Int, Int)", descriptorForSet.getOriginal());

        Map<String, PsiElement> nameToDeclaration = new HashMap<String, PsiElement>();
        PsiClass java_util_Collections = findClass("java.util.Collections");
        nameToDeclaration.put("java::java.util.Collections.emptyList()", findMethod(java_util_Collections, "emptyList"));
        nameToDeclaration.put("java::java.util.Collections", java_util_Collections);
        PsiClass java_util_List = findClass("java.util.ArrayList");
        nameToDeclaration.put("java::java.util.ArrayList", java_util_List);
        nameToDeclaration.put("java::java.util.ArrayList.set()", java_util_List.findMethodsByName("set", true)[0]);
        nameToDeclaration.put("java::java.util.ArrayList.get()", java_util_List.findMethodsByName("get", true)[0]);
        nameToDeclaration.put("java::java", findPackage("java"));
        nameToDeclaration.put("java::java.util", findPackage("java.util"));
        nameToDeclaration.put("java::java.lang", findPackage("java.lang"));
        nameToDeclaration.put("java::java.lang.Object", findClass("java.lang.Object"));
        PsiClass java_lang_System = findClass("java.lang.System");
        nameToDeclaration.put("java::java.lang.System", java_lang_System);
        PsiMethod[] methods = findClass("java.io.PrintStream").findMethodsByName("print", true);
        nameToDeclaration.put("java::java.io.PrintStream.print(Object)", methods[8]);
        nameToDeclaration.put("java::java.io.PrintStream.print(Int)", methods[2]);
        nameToDeclaration.put("java::java.io.PrintStream.print(char[])", methods[6]);
        nameToDeclaration.put("java::java.io.PrintStream.print(Double)", methods[5]);
        nameToDeclaration.put("java::java.lang.System.out", java_lang_System.findFieldByName("out", true));
        PsiClass java_lang_Number = findClass("java.lang.Number");
        nameToDeclaration.put("java::java.lang.Number", java_lang_Number);
        nameToDeclaration.put("java::java.lang.Number.intValue()", java_lang_Number.findMethodsByName("intValue", true)[0]);

        return new ExpectedResolveData(nameToDescriptor, nameToDeclaration, getEnvironment()) {
            @Override
            protected JetFile createJetFile(String fileName, String text) {
                return createCheckAndReturnPsiFile(fileName, null, text);
            }
        };
    }

    @NotNull
    private PsiElement findPackage(String qualifiedName) {
        JavaPsiFacade javaFacade = JavaPsiFacade.getInstance(getProject());
        return javaFacade.findPackage(qualifiedName);
    }

    @NotNull
    private PsiMethod findMethod(PsiClass psiClass, String name) {
        PsiMethod[] emptyLists = psiClass.findMethodsByName(name, true);
        return emptyLists[0];
    }

    @NotNull
    private PsiClass findClass(String qualifiedName) {
        Project project = getProject();
        InjectorForJavaSemanticServices injector = new InjectorForJavaSemanticServices(project);
        return injector.getPsiClassFinder().findPsiClass(new FqName(qualifiedName), PsiClassFinder.RuntimeClassesHandleMode.THROW);
    }

    @NotNull
    private FunctionDescriptor standardFunction(ClassDescriptor classDescriptor, String name, JetType... parameterTypes) {

        ExpressionTypingServices expressionTypingServices = new InjectorForTests(getProject()).getExpressionTypingServices();

        ExpressionTypingContext context = ExpressionTypingContext.newContext(
                expressionTypingServices, new BindingTraceContext(), classDescriptor.getDefaultType().getMemberScope(),
                DataFlowInfo.EMPTY, TypeUtils.NO_EXPECTED_TYPE, false);

        OverloadResolutionResults<FunctionDescriptor> functions = resolveFakeCall(
                context, ReceiverValue.NO_RECEIVER, Name.identifier(name), parameterTypes);

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

    @Override
    protected String getTestDataPath() {
        return getHomeDirectory() + "/compiler/testData";
    }

/*
    @Override
    protected Sdk getProjectJDK() {
        return PluginTestCaseBase.jdkFromIdeaHome();
    }
*/

    private static String getHomeDirectory() {
        return new File(PathManager.getResourceRoot(JetParsingTest.class, "/org/jetbrains/jet/parsing/JetParsingTest.class")).getParentFile().getParentFile().getParent();
    }

    @Override
    public String getName() {
        return "test" + name;
    }

    @Override
    protected void runTest() throws Throwable {
        doTest(path);
    }

    public static Test suite() {
//        TestSuite suite = new TestSuite();
//        suite.addTest(new JetResolveTest("/resolve/Basic.jet", "basic"));
//        return suite;
        return JetTestCaseBuilder.suiteForDirectory(getHomeDirectory() + "/compiler/testData/", "/resolve/", true, new JetTestCaseBuilder.NamedTestFactory() {
            @NotNull
            @Override
            public Test createTest(@NotNull String dataPath, @NotNull String name, @NotNull File file) {
                return new JetResolveTest(dataPath + "/" + name + ".jet", name);
            }
        });
    }
}
