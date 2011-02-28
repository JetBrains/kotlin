package org.jetbrains.jet.resolve;

import com.intellij.codeInsight.daemon.LightDaemonAnalyzerTestCase;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.util.io.FileUtil;
import org.jetbrains.jet.lang.psi.JetChangeUtil;
import org.jetbrains.jet.lang.psi.JetClass;
import org.jetbrains.jet.lang.psi.JetDeclaration;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.*;
import org.jetbrains.jet.lang.types.*;
import org.jetbrains.jet.parsing.JetParsingTest;

import java.io.File;
import java.io.FileReader;
import java.util.Collections;
import java.util.List;

/**
 * @author abreslav
 */
public class JetResolveTest extends LightDaemonAnalyzerTestCase {

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
        BindingContext bindingContext = new TopDownAnalyzer().process(JetStandardClasses.STANDARD_CLASSES, declarations);

        JetDeclaration declaration = declarations.get(0);
        ClassDescriptor classA = bindingContext.getClassDescriptor((JetClass) declaration);
        assertNotNull(classA);

        JetScope membersOfA = classA.getMemberScope(Collections.<TypeProjection>emptyList());
        ClassDescriptor classB = membersOfA.getClass("B");
        assertNotNull(classB);

        FunctionGroup foo = membersOfA.getFunctionGroup("foo");
        assertFalse(foo.isEmpty());

        OverloadDomain overloadsForFoo = OverloadResolver.INSTANCE.getOverloadDomain(null, membersOfA, "foo");
        Type fooType = overloadsForFoo.getReturnTypeForPositionedArguments(Collections.<Type>emptyList(), Collections.<Type>emptyList());
        assertEquals(JetStandardClasses.getIntType(), fooType);

        OverloadDomain overloadsForFoo1 = OverloadResolver.INSTANCE.getOverloadDomain(null, membersOfA, "foo1");
        Type foo1Type = overloadsForFoo1.getReturnTypeForPositionedArguments(Collections.<Type>emptyList(), Collections.<Type>emptyList());
        assertEquals(new TypeImpl(classB), foo1Type);

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
}