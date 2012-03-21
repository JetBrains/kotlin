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
import org.jetbrains.jet.lang.resolve.FqName;
import org.jetbrains.jet.lang.resolve.calls.CallResolver;
import org.jetbrains.jet.lang.resolve.calls.OverloadResolutionResults;
import org.jetbrains.jet.lang.resolve.calls.ResolvedCall;
import org.jetbrains.jet.lang.resolve.java.JavaDescriptorResolver;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverDescriptor;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.TypeProjection;
import org.jetbrains.jet.lang.types.lang.JetStandardLibrary;
import org.jetbrains.jet.parsing.JetParsingTest;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author abreslav
 */
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
        JetStandardLibrary lib = JetStandardLibrary.getInstance();
        Map<String, DeclarationDescriptor> nameToDescriptor = new HashMap<String, DeclarationDescriptor>();
        nameToDescriptor.put("kotlin::Int.plus(Int)", standardFunction(lib.getInt(), "plus", lib.getIntType()));
        FunctionDescriptor descriptorForGet = standardFunction(lib.getArray(), Collections.singletonList(new TypeProjection(lib.getIntType())), "get", lib.getIntType());
        nameToDescriptor.put("kotlin::Array.get(Int)", descriptorForGet.getOriginal());
        nameToDescriptor.put("kotlin::Int.compareTo(Double)", standardFunction(lib.getInt(), "compareTo", lib.getDoubleType()));
        @NotNull
        FunctionDescriptor descriptorForSet = standardFunction(lib.getArray(), Collections.singletonList(new TypeProjection(lib.getIntType())), "set", lib.getIntType(), lib.getIntType());
        nameToDescriptor.put("kotlin::Array.set(Int, Int)", descriptorForSet.getOriginal());

        Map<String, PsiElement> nameToDeclaration = new HashMap<String, PsiElement>();
        PsiClass java_util_Collections = findClass("java.util.Collections");
        nameToDeclaration.put("java::java.util.Collections.emptyList()", findMethod(java_util_Collections, "emptyList"));
        nameToDeclaration.put("java::java.util.Collections", java_util_Collections);
        PsiClass java_util_List = findClass("java.util.List");
        nameToDeclaration.put("java::java.util.List", java_util_List);
        nameToDeclaration.put("java::java.util.List.set()", java_util_List.findMethodsByName("set", true)[0]);
        nameToDeclaration.put("java::java.util.List.get()", java_util_List.findMethodsByName("get", true)[0]);
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

        return new ExpectedResolveData(nameToDescriptor, nameToDeclaration) {
            @Override
            protected JetFile createJetFile(String fileName, String text) {
                return createCheckAndReturnPsiFile(fileName, text);
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
        JavaDescriptorResolver javaDescriptorResolver = new InjectorForJavaSemanticServices(project).getJavaDescriptorResolver();
        return javaDescriptorResolver.findClass(new FqName(qualifiedName));
    }

    @NotNull
    private FunctionDescriptor standardFunction(ClassDescriptor classDescriptor, String name, JetType parameterType) {
        List<TypeProjection> typeArguments = Collections.emptyList();
        return standardFunction(classDescriptor, typeArguments, name, parameterType);
    }

    @NotNull
    private FunctionDescriptor standardFunction(ClassDescriptor classDescriptor, List<TypeProjection> typeArguments, String name, JetType... parameterType) {
        List<JetType> parameterTypeList = Arrays.asList(parameterType);
//        JetTypeInferrer.Services typeInferrerServices = JetSemanticServices.createSemanticServices(getProject()).getTypeInferrerServices(new BindingTraceContext());


        CallResolver callResolver = new InjectorForTests(getProject()).getCallResolver();
        OverloadResolutionResults<FunctionDescriptor> functions = callResolver.resolveExactSignature(
                classDescriptor.getMemberScope(typeArguments), ReceiverDescriptor.NO_RECEIVER, name, parameterTypeList);
        for (ResolvedCall<? extends FunctionDescriptor> resolvedCall : functions.getResultingCalls()) {
            List<ValueParameterDescriptor> unsubstitutedValueParameters = resolvedCall.getResultingDescriptor().getValueParameters();
            for (int i = 0, unsubstitutedValueParametersSize = unsubstitutedValueParameters.size(); i < unsubstitutedValueParametersSize; i++) {
                ValueParameterDescriptor unsubstitutedValueParameter = unsubstitutedValueParameters.get(i);
                if (unsubstitutedValueParameter.getType().equals(parameterType[i])) {
                    return resolvedCall.getResultingDescriptor();
                }
            }
        }
        throw new IllegalArgumentException("Not found: kotlin::" + classDescriptor.getName() + "." + name + "(" + parameterTypeList + ")");
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
