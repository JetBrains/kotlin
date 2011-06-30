package org.jetbrains.jet.resolve;

import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.search.GlobalSearchScope;
import junit.framework.Test;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.JetTestCaseBase;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.resolve.OverloadResolutionResult;
import org.jetbrains.jet.lang.types.*;
import org.jetbrains.jet.parsing.JetParsingTest;

import java.io.File;
import java.util.*;

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
        JetStandardLibrary lib = JetStandardLibrary.getJetStandardLibrary(project);
        Map<String, DeclarationDescriptor> nameToDescriptor = new HashMap<String, DeclarationDescriptor>();
        nameToDescriptor.put("std::Int.plus(Int)", standardFunction(lib.getInt(), "plus", lib.getIntType()));
        FunctionDescriptor descriptorForGet = standardFunction(lib.getArray(), Collections.singletonList(new TypeProjection(lib.getIntType())), "get", lib.getIntType());
        nameToDescriptor.put("std::Array.get(Int)", descriptorForGet.getOriginal());
        nameToDescriptor.put("std::Int.compareTo(Double)", standardFunction(lib.getInt(), "compareTo", lib.getDoubleType()));
        @NotNull
        FunctionDescriptor descriptorForSet = standardFunction(lib.getArray(), Collections.singletonList(new TypeProjection(lib.getIntType())), "set", lib.getIntType(), lib.getIntType());
        nameToDescriptor.put("std::Array.set(Int, Int)", descriptorForSet.getOriginal());

        Map<String,PsiElement> nameToDeclaration = new HashMap<String, PsiElement>();
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

        return new ExpectedResolveData(nameToDescriptor, nameToDeclaration);
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
        JavaPsiFacade javaFacade = JavaPsiFacade.getInstance(project);
        GlobalSearchScope javaSearchScope = GlobalSearchScope.allScope(project);
        return javaFacade.findClass(qualifiedName, javaSearchScope);
    }

    @NotNull
    private FunctionDescriptor standardFunction(ClassDescriptor classDescriptor, String name, JetType parameterType) {
        List<TypeProjection> typeArguments = Collections.emptyList();
        return standardFunction(classDescriptor, typeArguments, name, parameterType);
    }

    @NotNull
    private FunctionDescriptor standardFunction(ClassDescriptor classDescriptor, List<TypeProjection> typeArguments, String name, JetType... parameterType) {
        FunctionGroup functionGroup = classDescriptor.getMemberScope(typeArguments).getFunctionGroup(name);
        List<JetType> parameterTypeList = Arrays.asList(parameterType);
        OverloadResolutionResult functions = functionGroup.getPossiblyApplicableFunctions(Collections.<JetType>emptyList(), parameterTypeList);
        for (FunctionDescriptor function : functions.getFunctionDescriptors()) {
            List<ValueParameterDescriptor> unsubstitutedValueParameters = function.getValueParameters();
            for (int i = 0, unsubstitutedValueParametersSize = unsubstitutedValueParameters.size(); i < unsubstitutedValueParametersSize; i++) {
                ValueParameterDescriptor unsubstitutedValueParameter = unsubstitutedValueParameters.get(i);
                if (unsubstitutedValueParameter.getOutType().equals(parameterType[i])) {
                    return function;
                }
            }
        }
        throw new IllegalArgumentException("Not found: std::" + classDescriptor.getName() + "." + name + "(" + parameterTypeList + ")");
    }

    @Override
    protected String getTestDataPath() {
        return getHomeDirectory() + "/idea/testData";
    }

    @Override
    protected Sdk getProjectJDK() {
        return JetTestCaseBase.jdkFromIdeaHome();
    }

    private static String getHomeDirectory() {
        return new File(PathManager.getResourceRoot(JetParsingTest.class, "/org/jetbrains/jet/parsing/JetParsingTest.class")).getParentFile().getParentFile().getParent();
    }

    @Override
    public String getName() {
        return "test" + name;
    }

    @Override
    protected void runTest() throws Throwable {
        doTest(path, true, false);
    }

//    public void testBasic() throws Exception {
//        doTest("/resolve/Basic.jet", true, true);
//    }
//
//    public void testResolveToJava() throws Exception {
//        doTest("/resolve/ResolveToJava.jet", true, true);
//    }
//
//    public void testResolveOfInfixExpressions() throws Exception {
//        doTest("/resolve/ResolveOfInfixExpressions.jet", true, true);
//    }
//
//    public void testProjections() throws Exception {
//        doTest("/resolve/Projections.jet", true, true);
//    }
//
//    public void testPrimaryConstructors() throws Exception {
//        doTest("/resolve/PrimaryConstructors.jet", true, true);
//    }
//
//    public void testClassifiers() throws Exception {
//        doTest("/resolve/Classifiers.jet", true, true);
//    }
//
//    public void testConstructorsAndInitializers() throws Exception {
//        doTest("/resolve/ConstructorsAndInitializers.jet", true, true);
//    }
//
//    public void testNamespaces() throws Exception {
//        doTest("/resolve/Namespaces.jet", true, true);
//    }
//
//    public void testTryCatch() throws Exception {
//        doTest("/resolve/TryCatch.jet", true, true);
//    }

    public static Test suite() {
        return JetTestCaseBase.suiteForDirectory(getHomeDirectory() + "/idea/testData/", "/resolve/", true, new JetTestCaseBase.NamedTestFactory() {
            @NotNull
            @Override
            public Test createTest(@NotNull String dataPath, @NotNull String name) {
                return new JetResolveTest(dataPath + "/" + name + ".jet", name);
            }
        });
    }
}