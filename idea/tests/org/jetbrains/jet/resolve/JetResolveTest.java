package org.jetbrains.jet.resolve;

import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.projectRoots.impl.JavaSdkImpl;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.types.*;
import org.jetbrains.jet.parsing.JetParsingTest;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

/**
 * @author abreslav
 */
public class JetResolveTest extends ExtensibleResolveTestCase {

    @Override
    protected ExpectedResolveData getExpectedResolveData() {
        Project project = getProject();
        JetStandardLibrary lib = JetStandardLibrary.getJetStandardLibrary(project);
        Map<String, DeclarationDescriptor> nameToDescriptor = new HashMap<String, DeclarationDescriptor>();
        nameToDescriptor.put("std::Int.plus(Int)", standardFunction(lib.getInt(), "plus", lib.getIntType()));
        FunctionDescriptor descriptorForGet = standardFunction(lib.getArray(), Collections.singletonList(new TypeProjection(lib.getIntType())), "get", lib.getIntType());
        nameToDescriptor.put("std::Array.get(Int)", descriptorForGet.getOriginal());
        @NotNull
        FunctionDescriptor descriptorForSet = standardFunction(lib.getArray(), Collections.singletonList(new TypeProjection(lib.getIntType())), "set", lib.getIntType(), lib.getIntType());
        nameToDescriptor.put("std::Array.set(Int, Int)", descriptorForSet.getOriginal());

        Map<String,PsiElement> nameToDeclaration = new HashMap<String, PsiElement>();
        nameToDeclaration.put("java::java.util.Collections.emptyList()", findMethod(findClass("java.util.Collections"), "emptyList"));
        nameToDeclaration.put("java::java.util.Collections", findClass("java.util.Collections"));
        nameToDeclaration.put("java::java.util.List", findClass("java.util.List"));
        nameToDeclaration.put("java::java", findPackage("java"));
        nameToDeclaration.put("java::java.util", findPackage("java.util"));
        nameToDeclaration.put("java::java.lang", findPackage("java.lang"));
        nameToDeclaration.put("java::java.lang.Object", findClass("java.lang.Object"));
        nameToDeclaration.put("java::java.lang.System", findClass("java.lang.System"));
        PsiMethod[] methods = findClass("java.io.PrintStream").findMethodsByName("print", true);
        nameToDeclaration.put("java::java.io.PrintStream.print(Object)", methods[8]);
        nameToDeclaration.put("java::java.io.PrintStream.print(Int)", methods[2]);
        nameToDeclaration.put("java::java.lang.System.out", findClass("java.lang.System").findFieldByName("out", true));

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
    private FunctionDescriptor standardFunction(ClassDescriptor classDescriptor, String name, Type parameterType) {
        List<TypeProjection> typeArguments = Collections.emptyList();
        return standardFunction(classDescriptor, typeArguments, name, parameterType);
    }

    @NotNull
    private FunctionDescriptor standardFunction(ClassDescriptor classDescriptor, List<TypeProjection> typeArguments, String name, Type... parameterType) {
        FunctionGroup functionGroup = classDescriptor.getMemberScope(typeArguments).getFunctionGroup(name);
        List<Type> parameterTypeList = Arrays.asList(parameterType);
        Collection<FunctionDescriptor> functions = functionGroup.getPossiblyApplicableFunctions(Collections.<Type>emptyList(), parameterTypeList);
        for (FunctionDescriptor function : functions) {
            List<ValueParameterDescriptor> unsubstitutedValueParameters = function.getUnsubstitutedValueParameters();
            for (int i = 0, unsubstitutedValueParametersSize = unsubstitutedValueParameters.size(); i < unsubstitutedValueParametersSize; i++) {
                ValueParameterDescriptor unsubstitutedValueParameter = unsubstitutedValueParameters.get(i);
                if (unsubstitutedValueParameter.getType().equals(parameterType[i])) {
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
        Properties properties = new Properties();
        try {
            FileReader reader = new FileReader(getHomeDirectory() + "/idea/idea.properties");
            properties.load(reader);
            reader.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        String home = properties.getProperty("idea.home");
        return new JavaSdkImpl().createJdk("JDK", home + "/java/mockJDK-1.7/jre", true);
    }

    private static String getHomeDirectory() {
       return new File(PathManager.getResourceRoot(JetParsingTest.class, "/org/jetbrains/jet/parsing/JetParsingTest.class")).getParentFile().getParentFile().getParent();
    }

    public void testBasic() throws Exception {
        doTest("/resolve/Basic.jet", true, true);
    }

    public void testResolveToJava() throws Exception {
        doTest("/resolve/ResolveToJava.jet", true, true);
    }

}