package org.jetbrains.jet.types;

import com.intellij.codeInsight.daemon.LightDaemonAnalyzerTestCase;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.project.Project;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.*;
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
        assertType("1", JetStandardClasses.getIntType());
        assertType("0x1", JetStandardClasses.getIntType());
        assertType("0X1", JetStandardClasses.getIntType());
        assertType("0b1", JetStandardClasses.getIntType());
        assertType("0B1", JetStandardClasses.getIntType());

        assertType("1l", JetStandardClasses.getLongType());
        assertType("1L", JetStandardClasses.getLongType());

        assertType("1.0", JetStandardClasses.getDoubleType());
        assertType("1.0d", JetStandardClasses.getDoubleType());
        assertType("1.0D", JetStandardClasses.getDoubleType());
        assertType("0x1.fffffffffffffp1023", JetStandardClasses.getDoubleType());

        assertType("1.0f", JetStandardClasses.getFloatType());
        assertType("1.0F", JetStandardClasses.getFloatType());
        assertType("0x1.fffffffffffffp1023f", JetStandardClasses.getFloatType());

        assertType("true", JetStandardClasses.getBooleanType());
        assertType("false", JetStandardClasses.getBooleanType());

        assertType("'d'", JetStandardClasses.getCharType());

        assertType("\"d\"", JetStandardClasses.getStringType());
        assertType("\"\"\"d\"\"\"", JetStandardClasses.getStringType());

        assertType("()", JetStandardClasses.getUnitType());

        assertType("null", JetStandardClasses.getNullableNothingType());
    }

    public void testTupleConstants() throws Exception {
        assertType("()", JetStandardClasses.getUnitType());

        assertType("(1, 'a')", JetStandardClasses.getTupleType(JetStandardClasses.getIntType(), JetStandardClasses.getCharType()));
    }

    public void testJumps() throws Exception {
        assertType("throw e", JetStandardClasses.getNothingType());
        assertType("return", JetStandardClasses.getNothingType());
        assertType("return 1", JetStandardClasses.getNothingType());
        assertType("continue", JetStandardClasses.getNothingType());
        assertType("continue \"foo\"", JetStandardClasses.getNothingType());
        assertType("break", JetStandardClasses.getNothingType());
        assertType("break \"foo\"", JetStandardClasses.getNothingType());
    }

    public void testIf() throws Exception {
        assertType("if (true) 1", "Unit");
        assertType("if (true) 1 else 1", "Int");
        assertType("if (true) 1 else return", "Int");
        assertType("if (true) return else 1", "Int");
        assertType("if (true) return else return", "Nothing");

        assertType("if (true) 1 else null", "Int?");
        assertType("if (true) null else null", "Nothing?");

        assertType("if (true) 1 else '1'", "Any");
    }

    public void testWhen() throws Exception {
        assertType("when (1) { is 1 => 2; } ", "Int");
        assertType("when (1) { is 1 => 2; is 1 => '2'} ", "Any");
        assertType("when (1) { is 1 => 2; is 1 => '2'; is 1 => null} ", "Any?");
        assertType("when (1) { is 1 => 2; is 1 => '2'; else => null} ", "Any?");
        assertType("when (1) { is 1 => 2; is 1 => '2'; else continue} ", "Any");
        assertType("when (1) { is 1 => 2; is 1 => '2'; is 1 when(e) {is 1 => null}} ", "Any?");
        assertType("when (1) { is 1 => 2; is 1 => '2'; is 1 => when(e) {is 1 => null}} ", "Any?");
    }

    public void testTry() throws Exception {
        assertType("try {1} finally{2}", "Int");
        assertType("try {1} catch (e : e) {'a'} finally{2}", "Int");
        assertType("try {1} catch (e : e) {'a'} finally{'2'}", "Any");
        assertType("try {1} catch (e : e) {'a'}", "Any");
        assertType("try {1} catch (e : e) {'a'} catch (e : e) {null}", "Any?");
        assertType("try {} catch (e : e) {}", "Unit");
    }

    public void testCommonSupertypes() throws Exception {
        assertCommonSupertype("Int", "Int", "Int");

        assertCommonSupertype("Int", "Int", "Nothing");
        assertCommonSupertype("Int", "Nothing", "Int");
        assertCommonSupertype("Nothing", "Nothing", "Nothing");

        assertCommonSupertype("Int?", "Int", "Nothing?");
        assertCommonSupertype("Nothing?", "Nothing?", "Nothing?");

        assertCommonSupertype("Any", "Int", "Char");

        assertCommonSupertype("Base_T<*>", "Base_T<*>", "Derived_T<*>");
        assertCommonSupertype("Any", "Base_inT<*>", "Derived_T<*>");
        assertCommonSupertype("Derived_T<Int>", "DDerived_T<Int>", "Derived_T<Int>");
        assertCommonSupertype("Derived_T<Int>", "DDerived_T<Int>", "DDerived1_T<Int>");

        assertCommonSupertype("Base_T<out Any>", "Base_T<Int>", "Base_T<Boolean>");
        assertCommonSupertype("Base_T<in Int>", "Base_T<Int>", "Base_T<in Int>");
        assertCommonSupertype("Base_T<in Int>", "Derived_T<Int>", "Base_T<in Int>");
        assertCommonSupertype("Base_T<in Int>", "Derived_T<in Int>", "Base_T<Int>");
        assertCommonSupertype("Base_T<*>", "Base_T<Int>", "Base_T<*>");
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
        assertSubtype("(Byte)", "(Byte)");
        assertSubtype("(Char)",    "(Char)");
        assertSubtype("(Short)",   "(Short)");
        assertSubtype("(Int)", "(Int)");
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
        assertSubtype("(Float)", "(Float)");
        assertSubtype("(Double)", "(Double)");
        assertSubtype("(Unit)", "(Unit)");
        assertSubtype("(Unit, Unit)", "(Unit, Unit)");

        assertNotSubtype("(Unit)", "(Int)");

        assertSubtype("(Unit)", "(Any)");
        assertSubtype("(Unit, Unit)", "(Any, Any)");
        assertSubtype("(Unit, Unit)", "(Any, Unit)");
        assertSubtype("(Unit, Unit)", "(Unit, Any)");
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

        assertSubtype("Derived_T<Int>", "Base_T<Int>");
        assertSubtype("Derived_outT<Int>", "Base_outT<Int>");
        assertSubtype("Derived_inT<Int>", "Base_inT<Int>");
        assertSubtype("Derived_T<*>", "Base_T<*>");

        assertNotSubtype("Derived_T<Int>", "Base_T<Any>");

        assertSubtype("Derived_outT<Int>", "Base_outT<Any>");
        assertSubtype("Derived_T<Int>", "Base_T<out Any>");
        assertSubtype("Derived_T<Any>", "Base_T<in Int>");
    }

    public void testNullable() throws Exception {
        assertSubtype("Any?", "Any?");
        assertSubtype("Any", "Any?");
        assertNotSubtype("Any?", "Any");
        assertSubtype("Int", "Any?");
        assertSubtype("Int?", "Any?");
        assertNotSubtype("Int?", "Any");
    }

    public void testNothing() throws Exception {
        assertSubtype("Nothing", "Any");
        assertSubtype("Nothing?", "Any?");
        assertNotSubtype("Nothing?", "Any");

        assertSubtype("Nothing", "Int");
        assertSubtype("Nothing?", "Int?");
        assertNotSubtype("Nothing?", "Int");

        assertSubtype("Nothing?", "Base_T<*>?");
        assertSubtype("Nothing?", "Derived_T<*>?");
    }

    public void testThis() throws Exception {
        assertType("Derived_T<Int>", "this", "Derived_T<Int>");
        assertType("Derived_T<Int>", "this<Base_T>", "Base_T<Int>");
    }

    public void testLoops() throws Exception {
        assertType("while (1) {1}", "Unit");
        assertType("do {1} while(1)", "Unit");
        assertType("for (i in 1) {1}", "Unit");
    }

    public void testImplicitConversions() throws Exception {
        assertConvertibleTo("1", JetStandardClasses.getByteType());
    }

    private static void assertSubtype(String type1, String type2) {
        assertSubtypingRelation(type1, type2, true);
    }

    private static void assertNotSubtype(String type1, String type2) {
        assertSubtypingRelation(type1, type2, false);
    }

    private void assertCommonSupertype(String expected, String... types) {
        Collection<Type> subtypes = new ArrayList<Type>();
        for (String type : types) {
            subtypes.add(makeType(type));
        }
        Type result = JetTypeChecker.INSTANCE.commonSupertype(subtypes);
        assertTrue(result + " != " + expected, JetTypeChecker.INSTANCE.equalTypes(result, makeType(expected)));
    }

    private static void assertSubtypingRelation(String type1, String type2, boolean expected) {
        Type typeNode1 = makeType(type1);
        Type typeNode2 = makeType(type2);
        boolean result = JetTypeChecker.INSTANCE.isSubtypeOf(
                typeNode1,
                typeNode2);
        String modifier = expected ? "not " : "";
        assertTrue(typeNode1 + " is " + modifier + "a subtype of " + typeNode2, result == expected);
    }

    private static void assertConvertibleTo(String expression, Type type) {
        JetExpression jetExpression = JetChangeUtil.createExpression(getProject(), expression);
        assertTrue(
                expression + " must be convertible to " + type,
                JetTypeChecker.INSTANCE.isConvertibleTo(jetExpression, type));
    }

    private static void assertNotConvertibleTo(String expression, Type type) {
        JetExpression jetExpression = JetChangeUtil.createExpression(getProject(), expression);
        assertFalse(
                expression + " must not be convertible to " + type,
                JetTypeChecker.INSTANCE.isConvertibleTo(jetExpression, type));
    }

    private static void assertType(String expression, Type expectedType) {
        Project project = getProject();
        JetExpression jetExpression = JetChangeUtil.createExpression(project, expression);
        Type type = JetTypeChecker.INSTANCE.getType(ClassDefinitions.BASIC_SCOPE, jetExpression);
        assertTrue(type + " != " + expectedType, JetTypeChecker.INSTANCE.equalTypes(type, expectedType));
    }

    private void assertType(String contextType, String expression, String expectedType) {
        final Type thisType = makeType(contextType);
        JetScope scope = new JetScopeAdapter(ClassDefinitions.BASIC_SCOPE) {
            @Override
            public Type getThisType() {
                return thisType;
            }
        };
        assertType(scope, expression, expectedType);
    }

    private static void assertType(String expression, String expectedTypeStr) {
        assertType(ClassDefinitions.BASIC_SCOPE, expression, expectedTypeStr);
    }

    private static void assertType(JetScope scope, String expression, String expectedTypeStr) {
        Project project = getProject();
        JetExpression jetExpression = JetChangeUtil.createExpression(project, expression);
        Type type = JetTypeChecker.INSTANCE.getType(scope, jetExpression);
        Type expectedType = makeType(expectedTypeStr);
        assertTrue(type + " != " + expectedType, JetTypeChecker.INSTANCE.equalTypes(type, expectedType));
    }

    private static Type makeType(String typeStr) {
        return makeType(ClassDefinitions.BASIC_SCOPE, typeStr);
    }

    private static Type makeType(JetScope scope, String typeStr) {
        return TypeResolver.INSTANCE.resolveType(scope, JetChangeUtil.createType(getProject(), typeStr));
    }

    private static class ClassDefinitions {
        private static Map<String, ClassDescriptor> CLASSES = new HashMap<String, ClassDescriptor>();
        private static String[] CLASS_DECLARATIONS = {
            "open class Base_T<T>",
            "open class Derived_T<T> : Base_T<T>",
            "open class DDerived_T<T> : Derived_T<T>",
            "open class DDerived1_T<T> : Derived_T<T>",
            "open class Base_inT<in T>",
            "open class Derived_inT<in T> : Base_inT<T>",
            "open class Base_outT<out T>",
            "open class Derived_outT<out T> : Base_outT<T>",
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
                } else if ("Nothing".equals(name)) {
                    return JetStandardClasses.getNothing();
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
