package org.jetbrains.jet.codegen;

import java.lang.reflect.Method;
import java.util.List;

/**
 * @author yole
 */
public class ClassGenTest extends CodegenTestCase {
    public void testPSVMClass() throws Exception {
        loadFile("classes/simpleClass.jet");

        final Class aClass = loadClass("SimpleClass", generateClassesInFile());
        final Method[] methods = aClass.getDeclaredMethods();
        assertEquals(1, methods.length);
    }

    public void testArrayListInheritance() throws Exception {
        loadFile("classes/inheritingFromArrayList.jet");

        final Class aClass = loadClass("Foo", generateClassesInFile());
        checkInterface(aClass, List.class);
    }

    public void testInheritanceAndDelegation_DelegatingDefaultConstructorProperties() throws Exception {
        blackBoxFile("classes/inheritance.jet");
    }

    public void testFunDelegation() throws Exception {
        blackBoxFile("classes/funDelegation.jet");
    }

    public void testPropertyDelegation() throws Exception {
        blackBoxFile("classes/propertyDelegation.jet");
    }

    public void testDiamondInheritance() throws Exception {
        blackBoxFile("classes/diamondInheritance.jet");
    }

    public void testRightHandOverride() throws Exception {
        blackBoxFile("classes/rightHandOverride.jet");
    }

    private static void checkInterface(Class aClass, Class ifs) {
        for (Class anInterface : aClass.getInterfaces()) {
            if (anInterface == ifs) return;
        }
        fail(aClass.getName() + " must have " + ifs.getName() + " in its interfaces");
    }

    public void testNewInstanceExplicitConstructor() throws Exception {
        loadFile("classes/newInstanceDefaultConstructor.jet");
        System.out.println(generateToText());
        final Method method = generateFunction("test");
        final Integer returnValue = (Integer) method.invoke(null);
        assertEquals(610, returnValue.intValue());
    }

    public void testInnerClass() throws Exception {
        blackBoxFile("classes/innerClass.jet");
    }

    public void testInheritedInnerClass() throws Exception {
        blackBoxFile("classes/inheritedInnerClass.jet");
    }

    public void testInitializerBlock() throws Exception {
        blackBoxFile("classes/initializerBlock.jet");
    }

    public void testAbstractMethod() throws Exception {
        loadText("class Foo { abstract fun x(): String; fun y(): Int = 0 }");

        final ClassFileFactory codegens = generateClassesInFile();
        final Class aClass = loadClass("Foo", codegens);
        assertNotNull(aClass.getMethod("x"));
        final Class implClass = loadClass("Foo$$Impl", codegens);
        assertNull(findMethodByName(implClass, "x"));
        assertNotNull(findMethodByName(implClass, "y"));
    }

    public void testInheritedMethod() throws Exception {
        blackBoxFile("classes/inheritedMethod.jet");
    }

    public void testInitializerBlockDImpl() throws Exception {
        blackBoxFile("classes/initializerBlockDImpl.jet");
    }

    public void testPropertyInInitializer() throws Exception {
        blackBoxFile("classes/propertyInInitializer.jet");
    }

    public void testOuterThis() throws Exception {
        blackBoxFile("classes/outerThis.jet");
    }

    public void testSecondaryConstructors() throws Exception {
        blackBoxFile("classes/secondaryConstructors.jet");
    }

    public void testExceptionConstructor() throws Exception {
        blackBoxFile("classes/exceptionConstructor.jet");
    }

    public void testSimpleBox() throws Exception {
        blackBoxFile("classes/simpleBox.jet");
    }
}
