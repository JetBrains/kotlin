package org.jetbrains.jet.types;

import com.intellij.codeInsight.daemon.LightDaemonAnalyzerTestCase;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.project.Project;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.ClassDescriptorResolver;
import org.jetbrains.jet.lang.resolve.JetScope;
import org.jetbrains.jet.lang.resolve.JetScopeImpl;
import org.jetbrains.jet.lang.resolve.TypeResolver;
import org.jetbrains.jet.lang.types.*;
import org.jetbrains.jet.parsing.JetParsingTest;

import java.io.File;
import java.util.*;

/**
 * @author abreslav
 */
public class JetTypeCheckerTest extends LightDaemonAnalyzerTestCase {

    @Override
    protected String getTestDataPath() {
        return getHomeDirectory() + "/idea/testData";
    }

    private static String getHomeDirectory() {
       return new File(PathManager.getResourceRoot(JetParsingTest.class, "/org/jetbrains/jet/parsing/JetParsingTest.class")).getParentFile().getParentFile().getParent();
    }

    public void testConstants() throws Exception {
        assertType("1", JetStandardTypes.getInt());
        assertType("0x1", JetStandardTypes.getInt());
        assertType("0X1", JetStandardTypes.getInt());
        assertType("0b1", JetStandardTypes.getInt());
        assertType("0B1", JetStandardTypes.getInt());

        assertType("1l", JetStandardTypes.getLong());
        assertType("1L", JetStandardTypes.getLong());

        assertType("1.0", JetStandardTypes.getDouble());
        assertType("1.0d", JetStandardTypes.getDouble());
        assertType("1.0D", JetStandardTypes.getDouble());
        assertType("0x1.fffffffffffffp1023", JetStandardTypes.getDouble());

        assertType("1.0f", JetStandardTypes.getFloat());
        assertType("1.0F", JetStandardTypes.getFloat());
        assertType("0x1.fffffffffffffp1023f", JetStandardTypes.getFloat());

        assertType("true", JetStandardTypes.getBoolean());
        assertType("false", JetStandardTypes.getBoolean());

        assertType("'d'", JetStandardTypes.getChar());

        assertType("\"d\"", JetStandardTypes.getString());
        assertType("\"\"\"d\"\"\"", JetStandardTypes.getString());

        assertType("()", JetStandardTypes.getUnit());
    }

    public void testBasicSubtyping() throws Exception {
        assertSubtype("Boolean", "Boolean");
        assertSubtype("Byte", "Byte");
        assertSubtype("Char", "Char");
        assertSubtype("Short", "Short");
        assertSubtype("Int", "Int");
        assertSubtype("Long", "Long");
        assertSubtype("Float", "Float");
        assertSubtype("Double", "Double");
        assertSubtype("Unit", "Unit");
        assertSubtype("Boolean", "Any");
        assertSubtype("Byte", "Any");
        assertSubtype("Char", "Any");
        assertSubtype("Short", "Any");
        assertSubtype("Int", "Any");
        assertSubtype("Long", "Any");
        assertSubtype("Float", "Any");
        assertSubtype("Double", "Any");
        assertSubtype("Unit", "Any");
        assertSubtype("Any", "Any");

        assertNotSubtype("Boolean", "Byte");
        assertNotSubtype("Byte", "Short");
        assertNotSubtype("Char", "Int");
        assertNotSubtype("Short", "Int");
        assertNotSubtype("Int", "Long");
        assertNotSubtype("Long", "Double");
        assertNotSubtype("Float", "Double");
        assertNotSubtype("Double", "Int");
        assertNotSubtype("Unit", "Int");
    }

    public void testTuples() throws Exception {
        assertSubtype("Unit", "()");
        assertSubtype("()", "Unit");
        assertSubtype("()", "()");

        assertSubtype("(Boolean)", "(Boolean)");
        assertSubtype("(Byte)",    "(Byte)");
        assertSubtype("(Char)",    "(Char)");
        assertSubtype("(Short)",   "(Short)");
        assertSubtype("(Int)",     "(Int)");
        assertSubtype("(Long)",    "(Long)");
        assertSubtype("(Float)",   "(Float)");
        assertSubtype("(Double)",  "(Double)");
        assertSubtype("(Unit)",    "(Unit)");
        assertSubtype("(Unit, Unit)",    "(Unit, Unit)");

        assertSubtype("(Boolean)", "(Boolean)");
        assertSubtype("(Byte)",    "(Byte)");
        assertSubtype("(Char)",    "(Char)");
        assertSubtype("(Short)",   "(Short)");
        assertSubtype("(Int)",     "(Int)");
        assertSubtype("(Long)",    "(Long)");
        assertSubtype("(Float)",   "(Float)");
        assertSubtype("(Double)",  "(Double)");
        assertSubtype("(Unit)",    "(Unit)");
        assertSubtype("(Unit, Unit)",    "(Unit, Unit)");

        assertNotSubtype("(Unit)", "(Int)");

        assertSubtype("(Unit)",    "(Any)");
        assertSubtype("(Unit, Unit)",    "(Any, Any)");
        assertSubtype("(Unit, Unit)",    "(Any, Unit)");
        assertSubtype("(Unit, Unit)",    "(Unit, Any)");
    }

    public void testProjections() throws Exception {
        assertSubtype("Base_T<Int>", "Base_T<Int>");
        assertNotSubtype("Base_T<Int>", "Base_T<Any>");

        assertSubtype("Base_inT<Int>", "Base_inT<Int>");
        assertSubtype("Base_inT<Any>", "Base_inT<Int>");
        assertNotSubtype("Base_inT<Int>", "Base_inT<Any>");

        assertSubtype("Base_outT<Int>", "Base_outT<Int>");
        assertSubtype("Base_outT<Int>", "Base_outT<Any>");
        assertNotSubtype("Base_outT<Any>", "Base_outT<Int>");

        assertSubtype("Base_T<Int>", "Base_T<out Any>");
        assertSubtype("Base_T<Any>", "Base_T<in Int>");

        assertSubtype("Base_T<out Int>", "Base_T<out Int>");
        assertSubtype("Base_T<in Int>", "Base_T<in Int>");

        assertSubtype("Base_inT<out Int>", "Base_inT<out Int>");
        assertSubtype("Base_inT<in Int>", "Base_inT<in Int>");

        assertSubtype("Base_outT<out Int>", "Base_outT<out Int>");
        assertSubtype("Base_outT<in Int>", "Base_outT<in Int>");

        assertSubtype("Base_T<Int>", "Base_T<*>");
        assertSubtype("Base_T<*>", "Base_T<*>");
        assertSubtype("Base_T<Int>", "Base_T<out Any>");
        assertSubtype("Base_T<Any>", "Base_T<in Int>");

        assertNotSubtype("Base_T<out Any>", "Base_T<in Int>");
        assertNotSubtype("Base_T<in Int>", "Base_T<out Int>");
        assertNotSubtype("Base_T<*>", "Base_T<out Int>");

        assertNotSubtype("Derived_T<Any>", "Base_T<Any>");
        assertSubtype("Derived_outT<Any>", "Base_outT<Any>");
        assertNotSubtype("Derived_T<Int>", "Base_T<Any>");
        assertSubtype("Derived_outT<Int>", "Base_outT<Any>");
        assertSubtype("Derived_T<Int>", "Base_T<out Any>");
        assertSubtype("Derived_T<Any>", "Base_T<in Int>");
    }

    public void testImplicitConversions() throws Exception {
        assertConvertibleTo("1", JetStandardTypes.getByte());
    }

    private static void assertSubtype(String type1, String type2) {
        assertSubtypingRelation(type1, type2, true);
    }

    private static void assertNotSubtype(String type1, String type2) {
        assertSubtypingRelation(type1, type2, false);
    }

    private static void assertSubtypingRelation(String type1, String type2, boolean expected) {
        Type typeNode1 = TypeResolver.INSTANCE.resolveType(ClassDefinitions.BASIC_SCOPE, JetChangeUtil.createType(getProject(), type1));
        Type typeNode2 = TypeResolver.INSTANCE.resolveType(ClassDefinitions.BASIC_SCOPE, JetChangeUtil.createType(getProject(), type2));
        boolean result = new JetTypeChecker().isSubtypeOf(
                typeNode1,
                typeNode2);
        String modifier = expected ? "not " : "";
        assertTrue(typeNode1 + " is " + modifier + "a subtype of " + typeNode2, result == expected);
    }

    private static void assertConvertibleTo(String expression, Type type) {
        JetExpression jetExpression = JetChangeUtil.createExpression(getProject(), expression);
        assertTrue(
                expression + " must be convertible to " + type,
                new JetTypeChecker().isConvertibleTo(jetExpression, type));
    }

    private static void assertNotConvertibleTo(String expression, Type type) {
        JetExpression jetExpression = JetChangeUtil.createExpression(getProject(), expression);
        assertFalse(
                expression + " must not be convertible to " + type,
                new JetTypeChecker().isConvertibleTo(jetExpression, type));
    }

    private static void assertType(String expression, Type expectedType) {
        Project project = getProject();
        JetExpression jetExpression = JetChangeUtil.createExpression(project, expression);
        Type type = new JetTypeChecker().getType(jetExpression);
        assertEquals(type, expectedType);
    }

    private static class ClassDefinitions {
        private static Map<String, ClassDescriptor> CLASSES = new HashMap<String, ClassDescriptor>();
        private static String[] CLASS_DECLARATIONS = {
            "class Base_T<T>",
            "class Derived_T<T> : Base_T<T>",
            "class Base_inT<in T>",
            "class Derived_inT<in T> : Base_inT<T>",
            "class Base_outT<out T>",
            "class Derived_outT<out T> : Base_outT<T>",
        };

        public static JetScope BASIC_SCOPE = new JetScopeImpl() {
            @Override
            public ClassDescriptor getClass(String name) {
                if ("Int".equals(name)) {
                    return JetStandardClasses.getInt();
                } else if ("Boolean".equals(name)) {
                    return JetStandardClasses.getBoolean();
                } else if ("Byte".equals(name)) {
                    return JetStandardClasses.getByte();
                } else if ("Char".equals(name)) {
                    return JetStandardClasses.getChar();
                } else if ("Short".equals(name)) {
                    return JetStandardClasses.getShort();
                } else if ("Long".equals(name)) {
                    return JetStandardClasses.getLong();
                } else if ("Float".equals(name)) {
                    return JetStandardClasses.getFloat();
                } else if ("Double".equals(name)) {
                    return JetStandardClasses.getDouble();
                } else if ("Unit".equals(name)) {
                    return JetStandardClasses.getTuple(0);
                } else if ("Any".equals(name)) {
                    return JetStandardClasses.getAny();
                }
                if (CLASSES.isEmpty()) {
                    for (String classDeclaration : CLASS_DECLARATIONS) {
                        JetClass classElement = JetChangeUtil.createClass(getProject(), classDeclaration);
                        ClassDescriptor classDescriptor = ClassDescriptorResolver.INSTANCE.resolveClassDescriptor(this, classElement);
                        CLASSES.put(classDescriptor.getName(), classDescriptor);
                    }
                }
                ClassDescriptor classDescriptor = CLASSES.get(name);
                if (classDescriptor != null) {
                    return classDescriptor;
                }
                return null;
            }
        };
    }
}
