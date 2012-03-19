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

package org.jetbrains.jet.types;

import com.google.common.collect.Sets;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.JetLiteFixture;
import org.jetbrains.jet.JetTestCaseBuilder;
import org.jetbrains.jet.JetTestUtils;
import org.jetbrains.jet.di.InjectorForTests;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.*;
import org.jetbrains.jet.lang.resolve.java.JavaBridgeConfiguration;
import org.jetbrains.jet.lang.resolve.java.JavaDescriptorResolver;
import org.jetbrains.jet.lang.resolve.java.JavaPackageScope;
import org.jetbrains.jet.lang.resolve.java.JavaSemanticServices;
import org.jetbrains.jet.lang.resolve.scopes.*;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ExpressionReceiver;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverDescriptor;
import org.jetbrains.jet.lang.types.CommonSupertypes;
import org.jetbrains.jet.lang.types.ErrorUtils;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.TypeUtils;
import org.jetbrains.jet.lang.types.checker.JetTypeChecker;
import org.jetbrains.jet.lang.types.expressions.ExpressionTypingServices;
import org.jetbrains.jet.lang.types.lang.JetStandardClasses;
import org.jetbrains.jet.lang.types.lang.JetStandardLibrary;
import org.jetbrains.jet.lexer.JetTokens;

import java.util.*;

/**
 * @author abreslav
 */
public class JetTypeCheckerTest extends JetLiteFixture {

    private JetStandardLibrary library;
    private ClassDefinitions classDefinitions;
    private DescriptorResolver descriptorResolver;
    private JetScope scopeWithImports;
    private TypeResolver typeResolver;
    private ExpressionTypingServices expressionTypingServices;


    public JetTypeCheckerTest() {
        super("");
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
        library          = JetStandardLibrary.getInstance();
        classDefinitions = new ClassDefinitions();

        InjectorForTests injector = new InjectorForTests(getProject());
        descriptorResolver = injector.getDescriptorResolver();
        typeResolver = injector.getTypeResolver();
        expressionTypingServices = injector.getExpressionTypingServices();

        scopeWithImports = addImports(classDefinitions.BASIC_SCOPE);
    }

    @Override
    protected String getTestDataPath() {
        return JetTestCaseBuilder.getTestDataPathBase();
    }

    public void testConstants() throws Exception {
        assertType("1", library.getIntType());
        assertType("0x1", library.getIntType());
        assertType("0X1", library.getIntType());
        assertType("0b1", library.getIntType());
        assertType("0B1", library.getIntType());

        assertType("1.toLong()", library.getLongType());

        assertType("1.0", library.getDoubleType());
        assertType("1.0.toDouble()", library.getDoubleType());
        assertType("0x1.fffffffffffffp1023", library.getDoubleType());

        assertType("1.0.toFloat()", library.getFloatType());
        assertType("0x1.fffffffffffffp1023.toFloat()", library.getFloatType());

        assertType("true", library.getBooleanType());
        assertType("false", library.getBooleanType());

        assertType("'d'", library.getCharType());

        assertType("\"d\"", library.getStringType());
        assertType("\"\"\"d\"\"\"", library.getStringType());

        assertType("#()", JetStandardClasses.getUnitType());

        assertType("null", JetStandardClasses.getNullableNothingType());
    }

    public void testTupleConstants() throws Exception {
        assertType("#()", JetStandardClasses.getUnitType());

        assertType("#(1, 'a')", JetStandardClasses.getTupleType(library.getIntType(), library.getCharType()));
    }

    public void testTypeInfo() throws Exception {
// todo: obsolete since removal of typeinfo
//        assertType("typeinfo<Int>", "TypeInfo<Int>");
//        assertType("typeinfo<TypeInfo<Int>>", "TypeInfo<TypeInfo<Int>>");
    }

    public void testJumps() throws Exception {
        assertType("throw java.lang.Exception()", JetStandardClasses.getNothingType());
        assertType("continue", JetStandardClasses.getNothingType());
        assertType("break", JetStandardClasses.getNothingType());
    }

    public void testIf() throws Exception {
        assertType("if (true) 1", "Unit");
        assertType("if (true) 1 else 1", "Int");
        assertType("if (true) 1 else return", "Int");
        assertType("if (true) return else 1", "Int");
        assertType("if (true) throw Exception() else throw Exception()", "Nothing");

        assertType("if (true) 1 else null", "Int?");
        assertType("if (true) null else null", "Nothing?");

        assertType("if (true) 1 else '1'", "Any");

        assertType("if (true) else '1'", "Unit");
        assertType("if (true) else { var a = 0; a = 1 }", "Unit");
    }

    public void testWhen() throws Exception {
        assertType("when (1) { is 1 -> 2; } ", "Int");
        assertType("when (1) { is 1 -> 2; is 1 -> '2'} ", "Any");
        assertType("when (1) { is 1 -> 2; is 1 -> '2'; is 1 -> null} ", "Any?");
        assertType("when (1) { is 1 -> 2; is 1 -> '2'; else -> null} ", "Any?");
        assertType("when (1) { is 1 -> 2; is 1 -> '2'; is 1 -> when(2) {is 1 -> null}} ", "Any?");
    }

    public void testTry() throws Exception {
        assertType("try {1} finally{2}", "Int");
        assertType("try {1} catch (e : Exception) {'a'} finally{2}", "Int");
        assertType("try {1} catch (e : Exception) {'a'} finally{'2'}", "Any");
        assertType("try {1} catch (e : Exception) {'a'}", "Any");
        assertType("try {1} catch (e : Exception) {'a'} catch (e : Exception) {null}", "Any?");
        assertType("try {} catch (e : Exception) {}", "Unit");
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

        assertIntersection("Int", "Any", "Int");
        assertIntersection("Int", "Int", "Any");

        assertIntersection("Int", "Any", "Int?");
        assertIntersection("Int", "Int?", "Any");
        assertIntersection("Int", "Any?", "Int");
        assertIntersection("Int", "Int", "Any?");

        assertIntersection("Nothing", "Nothing", "Nothing");
        assertIntersection("Nothing?", "Nothing?", "Nothing?");
        assertIntersection("Nothing", "Nothing", "Nothing?");
        assertIntersection("Nothing", "Nothing?", "Nothing");

        assertIntersection("Nothing?", "String?", "Nothing?");
        assertIntersection("Nothing?", "Nothing?", "String?");
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
        assertSubtype("Unit", "#()");
        assertSubtype("#()", "Unit");
        assertSubtype("#()", "#()");

        assertSubtype("#(Boolean)", "#(Boolean)");
        assertSubtype("#(Byte)", "#(Byte)");
        assertSubtype("#(Char)",    "#(Char)");
        assertSubtype("#(Short)",   "#(Short)");
        assertSubtype("#(Int)", "#(Int)");
        assertSubtype("#(Long)",    "#(Long)");
        assertSubtype("#(Float)",   "#(Float)");
        assertSubtype("#(Double)",  "#(Double)");
        assertSubtype("#(Unit)",    "#(Unit)");
        assertSubtype("#(Unit, Unit)",    "#(Unit, Unit)");

        assertSubtype("#(Boolean)", "#(Boolean)");
        assertSubtype("#(Byte)",    "#(Byte)");
        assertSubtype("#(Char)",    "#(Char)");
        assertSubtype("#(Short)",   "#(Short)");
        assertSubtype("#(Int)",     "#(Int)");
        assertSubtype("#(Long)",    "#(Long)");
        assertSubtype("#(Float)", "#(Float)");
        assertSubtype("#(Double)", "#(Double)");
        assertSubtype("#(Unit)", "#(Unit)");
        assertSubtype("#(Unit, Unit)", "#(Unit, Unit)");

        assertNotSubtype("#(Unit)", "#(Int)");

        assertSubtype("#(Unit)", "#(Any)");
        assertSubtype("#(Unit, Unit)", "#(Any, Any)");
        assertSubtype("#(Unit, Unit)", "#(Any, Unit)");
        assertSubtype("#(Unit, Unit)", "#(Unit, Any)");
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

//        assertSubtype("java.lang.Integer", "java.lang.Comparable<java.lang.Integer>?");
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
//        assertType("Derived_T<Int>", "super<Base_T>", "Base_T<Int>");
    }

    public void testLoops() throws Exception {
        assertType("{ while (1) {1} }", "() -> Unit");
        assertType("{ do {1} while(1) }", "() -> Unit");
        assertType("{ for (i in 1) {1} }", "() -> Unit");
    }

    public void testFunctionLiterals() throws Exception {
        assertType("{() -> }", "() -> Unit");
        assertType("{() : Int -> }", "() -> Int");
        assertType("{() -> 1}", "() -> Int");

        assertType("{(a : Int) -> 1}", "(a : Int) -> Int");
        assertType("{(a : Int, b : String) -> 1}", "(a : Int, b : String) -> Int");

        assertType("{(a : Int) -> 1}", "(Int) -> Int");
        assertType("{(a : Int, b : String) -> 1}", "(Int, String) -> Int");

        assertType("{Any.() -> 1}", "Any.() -> Int");

        assertType("{Any.(a : Int) -> 1}", "Any.(a : Int) -> Int");
        assertType("{Any.(a : Int, b : String) -> 1}", "Any.(a : Int, b : String) -> Int");

        assertType("{Any.(a : Int) -> 1}", "Any.(Int) -> Int");
        assertType("{Any.(a : Int, b : String) -> 1}", "Any.(Int, String) -> Int");

        assertType("{Any.(a : Int, b : String) -> b}", "Any.(Int, String) -> String");
    }

    public void testBlocks() throws Exception {
        assertType("if (1) {val a = 1; a} else {null}", "Int?");
        assertType("if (1) {() -> val a = 1; a} else {() -> null}", "Function0<Int?>");
        assertType("if (1) {() -> val a = 1; a; var b : Boolean; b} else null", "Function0<Boolean>?");
        assertType("if (1) {() -> val a = 1; a; var b = a; b} else null", "Function0<Int>?");
    }

    public void testNew() throws Exception {
        assertType("Base_T<Int>()", "Base_T<Int>");
    }

    public void testPropertiesInClasses() throws Exception {
        assertType("Properties().p", "Int");
        assertType("Props<Int>().p", "Int");
        assertType("Props<out Int>().p", "Int");
        assertType("Props<Properties>().p.p", "Int");

        assertType("(return : Props<in Int>).p", "Any?");
    }

    public void testOverloads() throws Exception {
        assertType("Functions<String>().f()", "Unit");
        assertType("Functions<String>().f(1)", "Int");
        assertType("Functions<Double>().f(#(1, 1))", "Double");
        assertType("Functions<Double>().f(1.0)", "Any");
        assertType("Functions<Byte>().f<String>(\"\")", "Byte");

        assertType("f()", "Unit");
        assertType("f(1)", "Int");
        assertType("f(1.toFloat(), 1)", "Float");
        assertType("f<String>(1.toFloat())", "String");
    }

    public void testPlus() throws Exception {
        assertType("1.0.plus(1.toDouble())", "Double");
        assertType("1.0.plus(1.toFloat())", "Double");
        assertType("1.0.plus(1.toLong())", "Double");
        assertType("1.0.plus(1)", "Double");

        assertType("1.toFloat().plus(1.toDouble())", "Double");
        assertType("1.toFloat().plus(1.toFloat())", "Float");
        assertType("1.toFloat().plus(1.toLong())", "Float");
        assertType("1.toFloat().plus(1)", "Float");

        assertType("1.toLong().plus(1.toDouble())", "Double");
        assertType("1.toLong().plus(1.toFloat())", "Float");
        assertType("1.toLong().plus(1.toLong())", "Long");
        assertType("1.toLong().plus(1)", "Long");

        assertType("1.plus(1.toDouble())", "Double");
        assertType("1.plus(1.toFloat())", "Float");
        assertType("1.plus(1.toLong())", "Long");
        assertType("1.plus(1)", "Int");

        assertType("'1'.plus(1.toDouble())", "Double");
        assertType("'1'.plus(1.toFloat())", "Float");
        assertType("'1'.plus(1.toLong())", "Long");
        assertType("'1'.plus(1)", "Int");
        assertType("'1'.minus('1')", "Int"); // Plus is not available for char

        assertType("(1:Short).plus(1.toDouble())", "Double");
        assertType("(1:Short).plus(1.toFloat())", "Float");
        assertType("(1:Short).plus(1.toLong())", "Long");
        assertType("(1:Short).plus(1)", "Int");
        assertType("(1:Short).plus(1:Short)", "Int");

        assertType("(1:Byte).plus(1.toDouble())", "Double");
        assertType("(1:Byte).plus(1.toFloat())", "Float");
        assertType("(1:Byte).plus(1.toLong())", "Long");
        assertType("(1:Byte).plus(1)", "Int");
        assertType("(1:Byte).plus(1:Short)", "Int");
        assertType("(1:Byte).plus(1:Byte)", "Int");

        assertType("\"1\".plus(1.toDouble())", "String");
        assertType("\"1\".plus(1.toFloat())", "String");
        assertType("\"1\".plus(1.toLong())", "String");
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
//        assertType("WithPredicate()?isValid()", "WithPredicate?");
//        assertType("WithPredicate()?isValid(1)", "WithPredicate?");
//        assertType("WithPredicate()?p", "WithPredicate?");
    }

    public void testSupertypes() throws Exception {
        assertSupertypes("DDerived1_T<Int>", "Derived_T<Int>", "Base_T<Int>", "Any");
        assertSupertypes("DDerived2_T<Int>", "Derived_T<Int>", "Base_T<Int>", "Any");
        assertSupertypes("Derived1_inT<Int>", "Derived_T<Int>", "Base_T<Int>", "Any", "Base_inT<Int>");
    }

    public void testEffectiveProjectionKinds() throws Exception {
        assertSubtype("Tuple1<Int>", "Tuple1<Int>");
        assertSubtype("Tuple1<out Int>", "Tuple1<out Int>");
        assertSubtype("Tuple1<out Int>", "Tuple1<Int>");
        assertSubtype("Tuple1<Int>", "Tuple1<out Int>");
        assertSubtype("Tuple1<in Int>", "Tuple1<out Any?>");
        assertSubtype("Tuple1<out Any?>", "Tuple1<in String>");
        assertSubtype("Base_inT<Int>", "Base_inT<Int>");
        assertSubtype("Base_inT<in Int>", "Base_inT<in Int>");
        assertSubtype("Base_inT<in Int>", "Base_inT<Int>");
        assertSubtype("Base_inT<Int>", "Base_inT<in Int>");
        assertSubtype("Base_inT<out Int>", "Base_inT<out Any?>");
        assertSubtype("Base_inT<out Any?>", "Base_inT<out Int>");
    }

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private void assertSupertypes(String typeStr, String... supertypeStrs) {
        Set<JetType> allSupertypes = TypeUtils.getAllSupertypes(makeType(scopeWithImports, typeStr));
        Set<JetType> expected = Sets.newHashSet();
        for (String supertypeStr : supertypeStrs) {
            JetType supertype = makeType(scopeWithImports, supertypeStr);
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
        JetType result = TypeUtils.intersect(JetTypeChecker.INSTANCE, typesToIntersect);
//        assertNotNull("Intersection is null for " + typesToIntersect, result);
        assertEquals(makeType(expected), result);
    }

    private void assertCommonSupertype(String expected, String... types) {
        Collection<JetType> subtypes = new ArrayList<JetType>();
        for (String type : types) {
            subtypes.add(makeType(type));
        }
        JetType result = CommonSupertypes.commonSupertype(subtypes);
        assertTrue(result + " != " + expected, result.equals(makeType(expected)));
    }

    private void assertSubtypingRelation(String subtype, String supertype, boolean expected) {
        JetType typeNode1 = makeType(subtype);
        JetType typeNode2 = makeType(supertype);
        boolean result = JetTypeChecker.INSTANCE.isSubtypeOf(
                typeNode1,
                typeNode2);
        String modifier = expected ? "not " : "";
        assertTrue(typeNode1 + " is " + modifier + "a subtype of " + typeNode2, result == expected);
    }

    private void assertType(String expression, JetType expectedType) {
        Project project = getProject();
        JetExpression jetExpression = JetPsiFactory.createExpression(project, expression);
        JetType type = expressionTypingServices.getType(scopeWithImports, jetExpression, TypeUtils.NO_EXPECTED_TYPE, JetTestUtils.DUMMY_TRACE);
        assertTrue(type + " != " + expectedType, type.equals(expectedType));
    }

    private void assertErrorType(String expression) {
        Project project = getProject();
        JetExpression jetExpression = JetPsiFactory.createExpression(project, expression);
        JetType type = expressionTypingServices.safeGetType(scopeWithImports, jetExpression, TypeUtils.NO_EXPECTED_TYPE, JetTestUtils.DUMMY_TRACE);
        assertTrue("Error type expected but " + type + " returned", ErrorUtils.isErrorType(type));
    }

    private void assertType(String contextType, final String expression, String expectedType) {
        final JetType thisType = makeType(contextType);
        JetScope scope = new JetScopeAdapter(classDefinitions.BASIC_SCOPE) {
            @NotNull
            @Override
            public ReceiverDescriptor getImplicitReceiver() {
                return new ExpressionReceiver(JetPsiFactory.createExpression(getProject(), expression), thisType);
            }
        };
        assertType(scope, expression, expectedType);
    }

    private void assertType(String expression, String expectedTypeStr) {
        assertType(scopeWithImports, expression, expectedTypeStr);
    }

    private void assertType(JetScope scope, String expression, String expectedTypeStr) {
        Project project = getProject();
        JetExpression jetExpression = JetPsiFactory.createExpression(project, expression);
        JetType type = expressionTypingServices.getType(addImports(scope), jetExpression, TypeUtils.NO_EXPECTED_TYPE, JetTestUtils.DUMMY_TRACE);
        JetType expectedType = expectedTypeStr == null ? null : makeType(expectedTypeStr);
        assertEquals(expectedType, type);
    }

    private WritableScopeImpl addImports(JetScope scope) {
        WritableScopeImpl writableScope = new WritableScopeImpl(scope, scope.getContainingDeclaration(), RedeclarationHandler.DO_NOTHING);
        writableScope.importScope(library.getLibraryScope());
        JavaSemanticServices javaSemanticServices = new JavaSemanticServices(getProject(), JetTestUtils.DUMMY_TRACE);
        writableScope.importScope(javaSemanticServices.getDescriptorResolver().resolveNamespace(FqName.ROOT).getMemberScope());
        writableScope.importScope(javaSemanticServices.getDescriptorResolver().resolveNamespace(new FqName("java.lang")).getMemberScope());
        writableScope.changeLockLevel(WritableScope.LockLevel.BOTH);
        return writableScope;
    }

    private JetType makeType(String typeStr) {
        return makeType(scopeWithImports, typeStr);
    }

    private JetType makeType(JetScope scope, String typeStr) {
        return typeResolver.resolveType(scope, JetPsiFactory.createType(getProject(), typeStr), JetTestUtils.DUMMY_TRACE, true);
    }

    private class ClassDefinitions {
        private Map<String, ClassDescriptor> CLASSES = new HashMap<String, ClassDescriptor>();
        private String[] CLASS_DECLARATIONS = {
            "open class Base_T<T>()",
            "open class Derived_T<T>() : Base_T<T>",
            "open class DDerived_T<T>() : Derived_T<T>",
            "open class DDerived1_T<T>() : Derived_T<T>",
            "open class DDerived2_T<T>() : Derived_T<T>, Base_T<T>",
            "open class Base_inT<in T>()",
            "open class Derived_inT<in T>() : Base_inT<T>",
            "open class Derived1_inT<in T>() : Base_inT<T>, Derived_T<T>",
            "open class Base_outT<out T>()",
            "open class Derived_outT<out T>() : Base_outT<T>",
            "open class MDerived_T<T>() : Base_outT<out T>, Base_T<T>",
            "class Properties() { val p : Int }",
            "class Props<T>() { val p : T }",
            "class Functions<T>() { " +
                    "fun f() : Unit {} " +
                    "fun f(a : Int) : Int {} " +
                    "fun f(a : T) : Any {} " +
                    "fun f(a : #(Int, Int)) : T {} " +
                    "fun f<E>(a : E) : T {} " +
                    "}",
            "class WithPredicate() { " +
                    "fun isValid() : Boolean " +
                    "fun isValid(x : Int) : Boolean " +
                    "val p : Boolean " +
            "}",

            "open class List<E>()",
            "open class AbstractList<E> : List<E?>",
            "open class ArrayList<E>() : Any, AbstractList<E?>, List<E?>"

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
                        JetClass classElement = JetPsiFactory.createClass(getProject(), classDeclaration);
                        ClassDescriptor classDescriptor = resolveClassDescriptor(this, classElement);
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
            public Set<FunctionDescriptor> getFunctions(@NotNull String name) {
                Set<FunctionDescriptor> writableFunctionGroup = Sets.newLinkedHashSet();
                ModuleDescriptor module = new ModuleDescriptor("TypeCheckerTest");
                for (String funDecl : FUNCTION_DECLARATIONS) {
                    FunctionDescriptor functionDescriptor = descriptorResolver.resolveFunctionDescriptor(module, this, JetPsiFactory.createFunction(getProject(), funDecl), JetTestUtils.DUMMY_TRACE);
                    if (name.equals(functionDescriptor.getName())) {
                        writableFunctionGroup.add(functionDescriptor);
                    }
                }
                return writableFunctionGroup;
            }
        };

        @Nullable
        public ClassDescriptor resolveClassDescriptor(@NotNull JetScope scope, @NotNull JetClass classElement) {
            final ClassDescriptorImpl classDescriptor = new ClassDescriptorImpl(
                    scope.getContainingDeclaration(),
                    Collections.<AnnotationDescriptor>emptyList(),
                    JetPsiUtil.safeName(classElement.getName()));

            BindingTrace trace = JetTestUtils.DUMMY_TRACE;

            trace.record(BindingContext.CLASS, classElement, classDescriptor);

            final WritableScope parameterScope = new WritableScopeImpl(scope, classDescriptor, new TraceBasedRedeclarationHandler(trace));
            parameterScope.changeLockLevel(WritableScope.LockLevel.BOTH);

            // This call has side-effects on the parameterScope (fills it in)
            List<TypeParameterDescriptor> typeParameters
                    = descriptorResolver.resolveTypeParameters(classDescriptor, parameterScope, classElement.getTypeParameters(), JetTestUtils.DUMMY_TRACE);
            descriptorResolver.resolveGenericBounds(classElement, parameterScope, typeParameters, JetTestUtils.DUMMY_TRACE);

            List<JetDelegationSpecifier> delegationSpecifiers = classElement.getDelegationSpecifiers();
            // TODO : assuming that the hierarchy is acyclic
            Collection<JetType> supertypes = delegationSpecifiers.isEmpty()
                    ? Collections.singleton(JetStandardClasses.getAnyType())
                    : descriptorResolver.resolveDelegationSpecifiers(parameterScope, delegationSpecifiers, typeResolver, JetTestUtils.DUMMY_TRACE, true);
    //        for (JetType supertype: supertypes) {
    //            if (supertype.getConstructor().isSealed()) {
    //                trace.getErrorHandler().genericError(classElement.getNameAsDeclaration().getNode(), "Class " + classElement.getName() + " can not extend final type " + supertype);
    //            }
    //        }
            boolean open = classElement.hasModifier(JetTokens.OPEN_KEYWORD);

            final WritableScope memberDeclarations = new WritableScopeImpl(JetScope.EMPTY, classDescriptor, new TraceBasedRedeclarationHandler(trace));
            memberDeclarations.changeLockLevel(WritableScope.LockLevel.BOTH);

            List<JetDeclaration> declarations = classElement.getDeclarations();
            for (JetDeclaration declaration : declarations) {
                declaration.accept(new JetVisitorVoid() {
                    @Override
                    public void visitProperty(JetProperty property) {
                        if (property.getPropertyTypeRef() != null) {
                            memberDeclarations.addPropertyDescriptor(descriptorResolver.resolvePropertyDescriptor(classDescriptor, parameterScope, property, JetTestUtils.DUMMY_TRACE));
                        } else {
                            // TODO : Caution: a cyclic dependency possible
                            throw new UnsupportedOperationException();
                        }
                    }

                    @Override
                    public void visitNamedFunction(JetNamedFunction function) {
                        if (function.getReturnTypeRef() != null) {
                            memberDeclarations.addFunctionDescriptor(descriptorResolver.resolveFunctionDescriptor(classDescriptor, parameterScope, function, JetTestUtils.DUMMY_TRACE));
                        } else {
                            // TODO : Caution: a cyclic dependency possible
                            throw new UnsupportedOperationException();
                        }
                    }

                    @Override
                    public void visitJetElement(JetElement element) {
                        throw new UnsupportedOperationException(element.toString());
                    }
                });
            }

            Set<ConstructorDescriptor> constructors = Sets.newLinkedHashSet();
            classDescriptor.initialize(
                    !open,
                    typeParameters,
                    supertypes,
                    memberDeclarations,
                    constructors,
                    null
            );
            for (JetSecondaryConstructor constructor : classElement.getSecondaryConstructors()) {
                ConstructorDescriptorImpl functionDescriptor = descriptorResolver.resolveSecondaryConstructorDescriptor(memberDeclarations, classDescriptor, constructor, JetTestUtils.DUMMY_TRACE);
                functionDescriptor.setReturnType(classDescriptor.getDefaultType());
                constructors.add(functionDescriptor);
            }
            ConstructorDescriptorImpl primaryConstructorDescriptor = descriptorResolver.resolvePrimaryConstructorDescriptor(scope, classDescriptor, classElement, JetTestUtils.DUMMY_TRACE);
            if (primaryConstructorDescriptor != null) {
                primaryConstructorDescriptor.setReturnType(classDescriptor.getDefaultType());
                constructors.add(primaryConstructorDescriptor);
                classDescriptor.setPrimaryConstructor(primaryConstructorDescriptor);
            }
            return classDescriptor;
        }

    }
}
