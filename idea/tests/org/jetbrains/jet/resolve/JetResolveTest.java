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

    private PsiElement findPackage(String qualifiedName) {
        JavaPsiFacade javaFacade = JavaPsiFacade.getInstance(getProject());
        return javaFacade.findPackage(qualifiedName);
    }

    private PsiMethod findMethod(PsiClass psiClass, String name) {
        PsiMethod[] emptyLists = psiClass.findMethodsByName(name, true);
        return emptyLists[0];
    }

    private PsiClass findClass(String qualifiedName) {
        Project project = getProject();
        JavaPsiFacade javaFacade = JavaPsiFacade.getInstance(project);
        GlobalSearchScope javaSearchScope = GlobalSearchScope.allScope(project);
        return javaFacade.findClass(qualifiedName, javaSearchScope);
    }

    private DeclarationDescriptor standardFunction(ClassDescriptor classDescriptor, String name, Type parameterType) {
        FunctionGroup functionGroup = classDescriptor.getMemberScope(Collections.<TypeProjection>emptyList()).getFunctionGroup(name);
        Collection<FunctionDescriptor> functions = functionGroup.getPossiblyApplicableFunctions(Collections.<Type>emptyList(), Collections.singletonList(parameterType));
        for (FunctionDescriptor function : functions) {
            if (function.getUnsubstitutedValueParameters().get(0).getType().equals(parameterType)) {
                return function;
            }
        }
        throw new IllegalArgumentException("Not found: std::" + classDescriptor.getName() + "." + name + "(" + parameterType + ")");
    }

    @Override
    protected String getTestDataPath() {
        return getHomeDirectory() + "/idea/testData";
    }

    @Override
    protected Sdk getProjectJDK() {
        Properties properties = new Properties();
        try {
            properties.load(new FileReader(getHomeDirectory() + "/idea/idea.properties"));
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