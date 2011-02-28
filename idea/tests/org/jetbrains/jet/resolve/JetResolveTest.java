package org.jetbrains.jet.resolve;

import com.intellij.codeInsight.daemon.LightDaemonAnalyzerTestCase;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.util.io.FileUtil;
import org.jetbrains.jet.lang.JetSemanticServices;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.*;
import org.jetbrains.jet.lang.types.*;
import org.jetbrains.jet.parsing.JetParsingTest;

import java.io.File;
import java.io.FileReader;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @author abreslav
 */
public class JetResolveTest extends LightDaemonAnalyzerTestCase {
    private JetStandardLibrary library;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        library = new JetStandardLibrary(getProject());
    }

    @Override
    protected String getTestDataPath() {
        return getHomeDirectory() + "/idea/testData";
    }

    private static String getHomeDirectory() {
       return new File(PathManager.getResourceRoot(JetParsingTest.class, "/org/jetbrains/jet/parsing/JetParsingTest.class")).getParentFile().getParentFile().getParent();
    }

    public void testBasic() throws Exception {
        JetFile jetFile = JetChangeUtil.createFile(getProject(), FileUtil.loadTextAndClose(new FileReader(getTestDataPath() + "/resolve/Basic.jet")));
        List<JetDeclaration> declarations = jetFile.getRootNamespace().getDeclarations();
        BindingContext bindingContext = new TopDownAnalyzer(new JetSemanticServices(library)).process(library.getLibraryScope(), declarations);

        JetClass classADecl = (JetClass) declarations.get(0);
        ClassDescriptor classA = bindingContext.getClassDescriptor(classADecl);
        assertNotNull(classA);

        JetScope membersOfA = classA.getMemberScope(Collections.<TypeProjection>emptyList());
        ClassDescriptor classB = membersOfA.getClass("B");
        assertNotNull(classB);

        {
            FunctionGroup fooFG = membersOfA.getFunctionGroup("foo");
            assertFalse(fooFG.isEmpty());
        }

        assertReturnType(membersOfA, "foo", library.getIntType());
        assertReturnType(membersOfA, "foo1", new TypeImpl(classB));
        assertReturnType(membersOfA, "fooB", library.getIntType());

        JetFunction fooDecl = (JetFunction) classADecl.getDeclarations().get(1);
        Type expressionType = bindingContext.getExpressionType(fooDecl.getBodyExpression());
        assertEquals(library.getIntType(), expressionType);

        {
            DeclarationDescriptor resolve = bindingContext.resolve((JetReferenceExpression) fooDecl.getBodyExpression());
            assertSame(bindingContext.getFunctionDescriptor(fooDecl).getUnsubstitutedValueParameters().get(0), resolve);
        }

        {
            JetFunction fooBDecl = (JetFunction) classADecl.getDeclarations().get(2);
            JetCallExpression fooBBody = (JetCallExpression) fooBDecl.getBodyExpression();
            JetReferenceExpression refToFoo = (JetReferenceExpression) fooBBody.getCalleeExpression();
            FunctionDescriptor mustBeFoo = (FunctionDescriptor) bindingContext.resolve(refToFoo);
            assertSame(bindingContext.getFunctionDescriptor(fooDecl), FunctionDescriptorUtil.getOriginal(mustBeFoo));
        }

        {
            JetFunction fooIntDecl = (JetFunction) classADecl.getDeclarations().get(3);
            JetCallExpression fooIntBody = (JetCallExpression) fooIntDecl.getBodyExpression();
            JetDotQualifiedExpression qualifiedPlus = (JetDotQualifiedExpression) fooIntBody.getCalleeExpression();
            JetReferenceExpression refToPlus = (JetReferenceExpression) qualifiedPlus.getSelectorExpression();
            FunctionDescriptor mustBePlus = (FunctionDescriptor) bindingContext.resolve(refToPlus);
            FunctionGroup plusGroup = library.getInt().getMemberScope(Collections.<TypeProjection>emptyList()).getFunctionGroup("plus");
            Collection<FunctionDescriptor> pluses = plusGroup.getPossiblyApplicableFunctions(Collections.<Type>emptyList(), Collections.singletonList(library.getIntType()));
            FunctionDescriptor intPlus = null;
            for (FunctionDescriptor plus : pluses) {
                intPlus = plus;
            }
            assertSame(intPlus, FunctionDescriptorUtil.getOriginal(mustBePlus));
        }

        {
            PropertyDescriptor a = classA.getMemberScope(Collections.<TypeProjection>emptyList()).getProperty("a");
            JetProperty aDecl = (JetProperty) classADecl.getDeclarations().get(5);
            PropertyDescriptor mustBeA = bindingContext.getPropertyDescriptor(aDecl);
            assertSame(a, mustBeA);

            JetTypeReference propertyTypeRef = aDecl.getPropertyTypeRef();
            Type type = bindingContext.getType(propertyTypeRef);
            assertEquals(library.getIntType(), type);
        }

        JetClass classCDecl = (JetClass) declarations.get(1);
        ClassDescriptor classC = bindingContext.getClassDescriptor(classCDecl);
        assertNotNull(classC);
        assertEquals(1, classC.getTypeConstructor().getSupertypes().size());
        assertEquals(classA.getTypeConstructor(), classC.getTypeConstructor().getSupertypes().iterator().next().getConstructor());

        JetScope cScope = classC.getMemberScope(Collections.<TypeProjection>emptyList());
        ClassDescriptor classC_B = cScope.getClass("B");
        assertNotNull(classC_B);
        assertNotSame(classC_B, classB);
        assertEquals(classC.getTypeConstructor(), classC_B.getTypeConstructor().getSupertypes().iterator().next().getConstructor());
    }

    private void assertReturnType(JetScope membersOfA, String foo, Type returnType) {
        OverloadDomain overloadsForFoo = OverloadResolver.INSTANCE.getOverloadDomain(null, membersOfA, foo);
        FunctionDescriptor descriptorForFoo = overloadsForFoo.getFunctionDescriptorForPositionedArguments(Collections.<Type>emptyList(), Collections.<Type>emptyList());
        assertNotNull(descriptorForFoo);
        Type fooType = descriptorForFoo.getUnsubstitutedReturnType();
        assertEquals(returnType, fooType);
    }
}