package org.jetbrains.jet.types;

import com.google.common.collect.Sets;
import com.intellij.codeInsight.daemon.LightDaemonAnalyzerTestCase;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.JetTestUtils;
import org.jetbrains.jet.lang.JetSemanticServices;
import org.jetbrains.jet.lang.cfg.JetFlowInformationProvider;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.psi.JetChangeUtil;
import org.jetbrains.jet.lang.psi.JetClass;
import org.jetbrains.jet.lang.psi.JetExpression;
import org.jetbrains.jet.lang.resolve.*;
import org.jetbrains.jet.lang.types.*;
import org.jetbrains.jet.parsing.JetParsingTest;

import java.io.File;
import java.util.*;

/**
 * @author abreslav
 */
public class JetTypeCheckerTest extends LightDaemonAnalyzerTestCase {

    private JetStandardLibrary library;
    private JetSemanticServices semanticServices;
    private ClassDefinitions classDefinitions;
    private ClassDescriptorResolver classDescriptorResolver;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        library          = JetStandardLibrary.getJetStandardLibrary(getProject());
        semanticServices = JetSemanticServices.createSemanticServices(library);
        classDefinitions = new ClassDefinitions();
        classDescriptorResolver = semanticServices.getClassDescriptorResolver(JetTestUtils.DUMMY);
    }

    @Override
    protected String getTestDataPath() {
        return getHomeDirectory() + "/idea/testData";
    }

    private static String getHomeDirectory() {
       return new File(PathManager.getResourceRoot(JetParsingTest.class, "/org/jetbrains/jet/parsing/JetParsingTest.class")).getParentFile().getParentFile().getParent();
    }

    public void testConstants() throws Exception {
        assertType("1", library.getIntType());
        assertType("0x1", library.getIntType());
        assertType("0X1", library.getIntType());
        assertType("0b1", library.getIntType());
        assertType("0B1", library.getIntType());

        assertType("1.lng", library.getLongType());

        assertType("1.0", library.getDoubleType());
        assertType("1.0.dbl", library.getDoubleType());
        assertType("0x1.fffffffffffffp1023", library.getDoubleType());

        assertType("1.0.flt", library.getFloatType());
        assertType("0x1.fffffffffffffp1023.flt", library.getFloatType());

        assertType("true", library.getBooleanType());
        assertType("false", library.getBooleanType());

        assertType("'d'", library.getCharType());

        assertType("\"d\"", library.getStringType());
        assertType("\"\"\"d\"\"\"", library.getStringType());

        assertType("()", JetStandardClasses.getUnitType());

        assertType("null", JetStandardClasses.getNullableNothingType());
    }

    public void testTupleConstants() throws Exception {
        assertType("()", JetStandardClasses.getUnitType());

        assertType("(1, 'a')", JetStandardClasses.getTupleType(library.getIntType(), library.getCharType()));
    }

    public void testTypeInfo() throws Exception {
        assertType("typeof(1)", "typeinfo.TypeInfo<Int>");
        assertType("typeof(typeof(1))", "typeinfo.TypeInfo<typeinfo.TypeInfo<Int>>");
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

        assertType("if (true) else '1'", "Unit");
        assertType("if (true) else a = 0", "Unit");
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

        assertCommonSupertype("Comparable<*>", "Comparable<Int>", "Comparable<Boolean>");
        assertCommonSupertype("Base_T<out Comparable<*>>", "Base_T<Int>", "Base_T<Boolean>");
        assertCommonSupertype("Base_T<in Int>", "Base_T<Int>", "Base_T<in Int>");
        assertCommonSupertype("Base_T<in Int>", "Derived_T<Int>", "Base_T<in Int>");
        assertCommonSupertype("Base_T<in Int>", "Derived_T<in Int>", "Base_T<Int>");
        assertCommonSupertype("Base_T<*>", "Base_T<Int>", "Base_T<*>");
    }

    public void testIntersect() throws Exception {
        assertIntersection("Int?", "Int?", "Int?");
        assertIntersection("Int", "Int?", "Int");
        assertIntersection("Int", "Int", "Int?");
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

        assertSubtype("Derived_T<Int>", "Base_T<in Int>");
        assertSubtype("MDerived_T<Int>", "Base_T<in Int>");
        assertSubtype("ArrayList<Int>", "List<in Int>");

        assertSubtype("Integer", "java.lang.Comparable<Integer>?");
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

    public void testFunctionLiterals() throws Exception {
        assertType("{() => }", "{() : Unit}");
        assertType("{() : Int => }", "{() : Int}");
        assertType("{() => 1}", "{() : Int}");

        assertType("{(a : Int) => 1}", "{(a : Int) : Int}");
        assertType("{(a : Int, b : String) => 1}", "{(a : Int, b : String) : Int}");

        assertType("{(a : Int) => 1}", "{(Int) : Int}");
        assertType("{(a : Int, b : String) => 1}", "{(Int, String) : Int}");

        assertType("{Any.() => 1}", "{Any.() : Int}");

        assertType("{Any.(a : Int) => 1}", "{Any.(a : Int) : Int}");
        assertType("{Any.(a : Int, b : String) => 1}", "{Any.(a : Int, b : String) : Int}");

        assertType("{Any.(a : Int) => 1}", "{Any.(Int) : Int}");
        assertType("{Any.(a : Int, b : String) => 1}", "{Any.(Int, String) : Int}");

        assertType("{Any.(a : Int, b : String) => b}", "{Any.(Int, String) : String}");
    }

    public void testBlocks() throws Exception {
        assertType("if (1) {val a = 1; a} else {null}", "Int?");
        assertType("if (1) {() => val a = 1; a} else {() => null}", "Function0<Int?>");
        assertType("if (1) {() => val a = 1; a; var b : Boolean; b} else null", "Function0<Boolean>?");
        assertType("if (1) {() => val a = 1; a; var b = a; b} else null", "Function0<Int>?");
    }

    public void testNew() throws Exception {
        assertType("new Base_T<Int>()", "Base_T<Int>");
    }

    public void testPropertiesInClasses() throws Exception {
        assertType("new Properties().p", "Int");
        assertType("new Props<Int>().p", "Int");
        assertType("new Props<out Int>().p", "Int");
        assertType("new Props<Properties>().p.p", "Int");

        assertType("(return : Props<in Int>).p", "Any?");
    }

    public void testOverloads() throws Exception {
        assertType("new Functions<String>().f()", "Unit");
        assertType("new Functions<String>().f(1)", "Int");
        assertType("new Functions<String>().f(1.0)", (String) null);
        assertType("new Functions<Double>().f((1, 1))", "Double");
        assertType("new Functions<Double>().f(1.0)", "Any");
        assertType("new Functions<Byte>().f<String>(\"\")", "Byte");
        assertType("new Functions<Byte>().f<String>(1)", (String) null);

        assertType("f()", "Unit");
        assertType("f(1)", "Int");
        assertType("f(1.flt, 1)", "Float");
        assertType("f<String>(1.flt)", "String");
        assertType("f(1.0)", (String) null);
    }

    public void testPlus() throws Exception {
        assertType("1.0.plus(1.dbl)", "Double");
        assertType("1.0.plus(1.flt)", "Double");
        assertType("1.0.plus(1.lng)", "Double");
        assertType("1.0.plus(1)", "Double");

        assertType("1.flt.plus(1.dbl)", "Double");
        assertType("1.flt.plus(1.flt)", "Float");
        assertType("1.flt.plus(1.lng)", "Float");
        assertType("1.flt.plus(1)", "Float");

        assertType("1.lng.plus(1.dbl)", "Double");
        assertType("1.lng.plus(1.flt)", "Float");
        assertType("1.lng.plus(1.lng)", "Long");
        assertType("1.lng.plus(1)", "Long");

        assertType("1.plus(1.dbl)", "Double");
        assertType("1.plus(1.flt)", "Float");
        assertType("1.plus(1.lng)", "Long");
        assertType("1.plus(1)", "Int");

        assertType("'1'.plus(1.dbl)", "Double");
        assertType("'1'.plus(1.flt)", "Float");
        assertType("'1'.plus(1.lng)", "Long");
        assertType("'1'.plus(1)", "Int");
        assertType("'1'.plus('1')", "Int");

        assertType("(1:Short).plus(1.dbl)", "Double");
        assertType("(1:Short).plus(1.flt)", "Float");
        assertType("(1:Short).plus(1.lng)", "Long");
        assertType("(1:Short).plus(1)", "Int");
        assertType("(1:Short).plus(1:Short)", "Int");

        assertType("(1:Byte).plus(1.dbl)", "Double");
        assertType("(1:Byte).plus(1.flt)", "Float");
        assertType("(1:Byte).plus(1.lng)", "Long");
        assertType("(1:Byte).plus(1)", "Int");
        assertType("(1:Byte).plus(1:Short)", "Int");
        assertType("(1:Byte).plus(1:Byte)", "Int");

        assertType("\"1\".plus(1.dbl)", "String");
        assertType("\"1\".plus(1.flt)", "String");
        assertType("\"1\".plus(1.lng)", "String");
        assertType("\"1\".plus(1)", "String");
        assertType("\"1\".plus('1')", "String");
    }

    public void testBinaryOperations() throws Exception {
        assertType("1 as Any", "Any");
        assertType("1 is Char", "Boolean");
        assertType("1 === null", "Boolean");
        assertType("1 !== null", "Boolean");
        assertType("true && false", "Boolean");
        assertType("true || false", "Boolean");
        assertType("null ?: false", "Boolean");
        assertType("new WithPredicate()?isValid()", "WithPredicate?");
        assertType("new WithPredicate()?isValid(1)", "WithPredicate?");
        assertType("new WithPredicate()?p", "WithPredicate?");
    }

    public void testSupertypes() throws Exception {
        assertSupertypes("DDerived1_T<Int>", "Derived_T<Int>", "Base_T<Int>", "Any");
        assertSupertypes("DDerived2_T<Int>", "Derived_T<Int>", "Base_T<Int>", "Any");
        assertSupertypes("Derived1_inT<Int>", "Derived_T<Int>", "Base_T<Int>", "Any", "Base_inT<Int>");
    }

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private void assertSupertypes(String typeStr, String... supertypeStrs) {
        Set<JetType> allSupertypes = TypeUtils.getAllSupertypes(makeType(classDefinitions.BASIC_SCOPE, typeStr));
        Set<JetType> expected = Sets.newHashSet();
        for (String supertypeStr : supertypeStrs) {
            JetType supertype = makeType(classDefinitions.BASIC_SCOPE, supertypeStr);
            expected.add(supertype);
        }
        assertEquals(expected, allSupertypes);
    }

    private void assertSubtype(String subtype, String supertype) {
        assertSubtypingRelation(subtype, supertype, true);
    }

    private void assertNotSubtype(String subtype, String supertype) {
        assertSubtypingRelation(subtype, supertype, false);
    }

    private void assertIntersection(String expected, String... types) {
        Set<JetType> typesToIntersect = new LinkedHashSet<JetType>();
        for (String type : types) {
            typesToIntersect.add(makeType(type));
        }
        JetType result = TypeUtils.intersect(semanticServices.getTypeChecker(), typesToIntersect);
//        assertNotNull("Intersection is null for " + typesToIntersect, result);
        assertEquals(makeType(expected), result);
    }

    private void assertCommonSupertype(String expected, String... types) {
        Collection<JetType> subtypes = new ArrayList<JetType>();
        for (String type : types) {
            subtypes.add(makeType(type));
        }
        JetType result = semanticServices.getTypeChecker().commonSupertype(subtypes);
        assertTrue(result + " != " + expected, result.equals(makeType(expected)));
    }

    private void assertSubtypingRelation(String subtype, String supertype, boolean expected) {
        JetType typeNode1 = makeType(subtype);
        JetType typeNode2 = makeType(supertype);
        boolean result = semanticServices.getTypeChecker().isSubtypeOf(
                typeNode1,
                typeNode2);
        String modifier = expected ? "not " : "";
        assertTrue(typeNode1 + " is " + modifier + "a subtype of " + typeNode2, result == expected);
    }

    private void assertType(String expression, JetType expectedType) {
        Project project = getProject();
        JetExpression jetExpression = JetChangeUtil.createExpression(project, expression);
        JetType type = semanticServices.getTypeInferrer(JetTestUtils.DUMMY, JetFlowInformationProvider.NONE).getType(classDefinitions.BASIC_SCOPE, jetExpression, false);
        assertTrue(type + " != " + expectedType, type.equals(expectedType));
    }

    private void assertErrorType(String expression) {
        Project project = getProject();
        JetExpression jetExpression = JetChangeUtil.createExpression(project, expression);
        JetType type = semanticServices.getTypeInferrer(JetTestUtils.DUMMY, JetFlowInformationProvider.NONE).safeGetType(classDefinitions.BASIC_SCOPE, jetExpression, false);
        assertTrue("Error type expected but " + type + " returned", ErrorUtils.isErrorType(type));
    }

    private void assertType(String contextType, String expression, String expectedType) {
        final JetType thisType = makeType(contextType);
        JetScope scope = new JetScopeAdapter(classDefinitions.BASIC_SCOPE) {
            @NotNull
            @Override
            public JetType getThisType() {
                return thisType;
            }
        };
        assertType(scope, expression, expectedType);
    }

    private void assertType(String expression, String expectedTypeStr) {
        assertType(classDefinitions.BASIC_SCOPE, expression, expectedTypeStr);
    }

    private void assertType(JetScope scope, String expression, String expectedTypeStr) {
        Project project = getProject();
        JetExpression jetExpression = JetChangeUtil.createExpression(project, expression);
        JetType type = semanticServices.getTypeInferrer(JetTestUtils.DUMMY, JetFlowInformationProvider.NONE).getType(scope, jetExpression, false);
        JetType expectedType = expectedTypeStr == null ? null : makeType(expectedTypeStr);
        assertEquals(expectedType, type);
    }

    private JetType makeType(String typeStr) {
        return makeType(classDefinitions.BASIC_SCOPE, typeStr);
    }

    private JetType makeType(JetScope scope, String typeStr) {
        return new TypeResolver(semanticServices, JetTestUtils.DUMMY, true).resolveType(scope, JetChangeUtil.createType(getProject(), typeStr));
    }

    private class ClassDefinitions {
        private Map<String, ClassDescriptor> CLASSES = new HashMap<String, ClassDescriptor>();
        private String[] CLASS_DECLARATIONS = {
            "open class Base_T<T>",
            "open class Derived_T<T> : Base_T<T>",
            "open class DDerived_T<T> : Derived_T<T>",
            "open class DDerived1_T<T> : Derived_T<T>",
            "open class DDerived2_T<T> : Derived_T<T>, Base_T<T>",
            "open class Base_inT<in T>",
            "open class Derived_inT<in T> : Base_inT<T>",
            "open class Derived1_inT<in T> : Base_inT<T>, Derived_T<T>",
            "open class Base_outT<out T>",
            "open class Derived_outT<out T> : Base_outT<T>",
            "open class MDerived_T> : Base_outT<out T>, Base_T<T>",
            "class Properties { val p : Int }",
            "class Props<T> { val p : T }",
            "class Functions<T> { " +
                    "fun f() : Unit {} " +
                    "fun f(a : Int) : Int {} " +
                    "fun f(a : T) : Any {} " +
                    "fun f(a : (Int, Int)) : T {} " +
                    "fun f<E>(a : E) : T {} " +
                    "}",
            "class WithPredicate { " +
                    "fun isValid() : Boolean " +
                    "fun isValid(x : Int) : Boolean " +
                    "val p : Boolean " +
            "}",

            "open class List<E>",
            "open class AbstractList<E> : List<E?>",
            "open class ArrayList<E> : Any, AbstractList<E?>, List<E?>"

        };
        private String[] FUNCTION_DECLARATIONS = {
            "fun f() : Unit {}",
            "fun f(a : Int) : Int {a}",
            "fun f(a : Float, b : Int) : Float {a}",
            "fun f<T>(a : Float) : T {a}",
        };

        public JetScope BASIC_SCOPE = new JetScopeAdapter(library.getLibraryScope()) {
            @Override
            public ClassifierDescriptor getClassifier(@NotNull String name) {
                if (CLASSES.isEmpty()) {
                    for (String classDeclaration : CLASS_DECLARATIONS) {
                        JetClass classElement = JetChangeUtil.createClass(getProject(), classDeclaration);
                        ClassDescriptor classDescriptor = classDescriptorResolver.resolveClassDescriptor(this, classElement);
                        CLASSES.put(classDescriptor.getName(), classDescriptor);
                    }
                }
                ClassDescriptor classDescriptor = CLASSES.get(name);
                if (classDescriptor != null) {
                    return classDescriptor;
                }
                return super.getClassifier(name);
            }

            @NotNull
            @Override
            public FunctionGroup getFunctionGroup(@NotNull String name) {
                WritableFunctionGroup writableFunctionGroup = new WritableFunctionGroup(name);
                for (String funDecl : FUNCTION_DECLARATIONS) {
                    FunctionDescriptor functionDescriptor = classDescriptorResolver.resolveFunctionDescriptor(JetStandardClasses.getAny(), this, JetChangeUtil.createFunction(getProject(), funDecl));
                    if (name.equals(functionDescriptor.getName())) {
                        writableFunctionGroup.addFunction(functionDescriptor);
                    }
                }
                return writableFunctionGroup;
            }
        };
    }
}