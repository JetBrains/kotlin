/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.types;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.builtins.KotlinBuiltIns;
import org.jetbrains.kotlin.cli.jvm.compiler.JetCoreEnvironment;
import org.jetbrains.kotlin.descriptors.ModuleDescriptor;
import org.jetbrains.kotlin.descriptors.PackageViewDescriptor;
import org.jetbrains.kotlin.descriptors.ReceiverParameterDescriptor;
import org.jetbrains.kotlin.descriptors.impl.ReceiverParameterDescriptorImpl;
import org.jetbrains.kotlin.di.InjectorForTests;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.psi.JetExpression;
import org.jetbrains.kotlin.resolve.BindingTraceContext;
import org.jetbrains.kotlin.resolve.ImportPath;
import org.jetbrains.kotlin.resolve.TypeResolver;
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowInfo;
import org.jetbrains.kotlin.resolve.lazy.LazyResolveTestUtil;
import org.jetbrains.kotlin.resolve.scopes.*;
import org.jetbrains.kotlin.resolve.scopes.receivers.ExpressionReceiver;
import org.jetbrains.kotlin.test.ConfigurationKind;
import org.jetbrains.kotlin.test.JetLiteFixture;
import org.jetbrains.kotlin.test.JetTestUtils;
import org.jetbrains.kotlin.types.checker.JetTypeChecker;
import org.jetbrains.kotlin.types.expressions.ExpressionTypingServices;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static org.jetbrains.kotlin.psi.PsiPackage.JetPsiFactory;

public class JetTypeCheckerTest extends JetLiteFixture {

    private KotlinBuiltIns builtIns;
    private JetScope scopeWithImports;
    private TypeResolver typeResolver;
    private ExpressionTypingServices expressionTypingServices;


    public JetTypeCheckerTest() {
        super("");
    }

    @Override
    protected JetCoreEnvironment createEnvironment() {
        return createEnvironmentWithMockJdk(ConfigurationKind.ALL);
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();

        builtIns = KotlinBuiltIns.getInstance();

        InjectorForTests injector = new InjectorForTests(getProject(), JetTestUtils.createEmptyModule());
        typeResolver = injector.getTypeResolver();
        expressionTypingServices = injector.getExpressionTypingServices();

        scopeWithImports = getDeclarationsScope("compiler/testData/type-checker-test.kt");
    }

    @Override
    protected void tearDown() throws Exception {
        scopeWithImports = null;

        expressionTypingServices = null;
        typeResolver = null;

        builtIns = null;

        super.tearDown();
    }

    public void testConstants() throws Exception {
        assertType("1", builtIns.getIntType());
        assertType("0x1", builtIns.getIntType());
        assertType("0X1", builtIns.getIntType());
        assertType("0b1", builtIns.getIntType());
        assertType("0B1", builtIns.getIntType());

        assertType("1.toLong()", builtIns.getLongType());

        assertType("1.0", builtIns.getDoubleType());
        assertType("1.0.toDouble()", builtIns.getDoubleType());
        assertType("0x1.fffffffffffffp1023", builtIns.getDoubleType());

        assertType("1.0.toFloat()", builtIns.getFloatType());
        assertType("0x1.fffffffffffffp1023.toFloat()", builtIns.getFloatType());

        assertType("true", builtIns.getBooleanType());
        assertType("false", builtIns.getBooleanType());

        assertType("'d'", builtIns.getCharType());

        assertType("\"d\"", builtIns.getStringType());
        assertType("\"\"\"d\"\"\"", builtIns.getStringType());

        assertType("Unit", KotlinBuiltIns.getInstance().getUnitType());

        assertType("null", KotlinBuiltIns.getInstance().getNullableNothingType());
    }

    public void testTypeInfo() throws Exception {
// todo: obsolete since removal of typeinfo
//        assertType("typeinfo<Int>", "TypeInfo<Int>");
//        assertType("typeinfo<TypeInfo<Int>>", "TypeInfo<TypeInfo<Int>>");
    }

    public void testJumps() throws Exception {
        assertType("throw java.lang.Exception()", KotlinBuiltIns.getInstance().getNothingType());
        assertType("continue", KotlinBuiltIns.getInstance().getNothingType());
        assertType("break", KotlinBuiltIns.getInstance().getNothingType());
    }

    public void testIf() throws Exception {
        assertType("if (true) 1", "Unit");
        assertType("if (true) 1 else 1", "Int");
        assertType("if (true) 1 else throw Exception()", "Int");
        assertType("if (true) throw Exception() else 1", "Int");
        assertType("if (true) throw Exception() else throw Exception()", "Nothing");

        assertType("if (true) 1 else null", "Int?");
        assertType("if (true) null else null", "Nothing?");

        assertType("if (true) 1 else '1'", "Comparable<out Any?>");

        assertType("if (true) else '1'", "Unit");
        assertType("if (true) else { var a = 0; a = 1 }", "Unit");
    }

    public void testWhen() throws Exception {
        assertType("when (1) { is 1 -> 2; } ", "Int");
        assertType("when (1) { is 1 -> 2; is 1 -> '2'} ", "Comparable<out Any?>");
        assertType("when (1) { is 1 -> 2; is 1 -> '2'; is 1 -> null} ", "Comparable<out Any?>?");
        assertType("when (1) { is 1 -> 2; is 1 -> '2'; else -> null} ", "Comparable<out Any?>?");
        assertType("when (1) { is 1 -> 2; is 1 -> '2'; is 1 -> when(2) {is 1 -> null}} ", "Comparable<out Any?>?");
    }

    public void testTry() throws Exception {
        assertType("try {1} finally{2}", "Int");
        assertType("try {1} catch (e : Exception) {'a'} finally{2}", "Comparable<out Any?>");
        assertType("try {1} catch (e : Exception) {2} finally{'a'}", "Int");
        assertType("try {1} catch (e : Exception) {'a'} finally{'2'}", "Comparable<out Any?>");
        assertType("try {1} catch (e : Exception) {'a'}", "Comparable<out Any?>");
        assertType("try {1} catch (e : Exception) {'a'} catch (e : Exception) {null}", "Comparable<out Any?>?");
        assertType("try {} catch (e : Exception) {}", "Unit");
    }

    public void testCommonSupertypes() throws Exception {
        assertCommonSupertype("Int", "Int", "Int");

        assertCommonSupertype("Int", "Int", "Nothing");
        assertCommonSupertype("Int", "Nothing", "Int");
        assertCommonSupertype("Nothing", "Nothing", "Nothing");

        assertCommonSupertype("Int?", "Int", "Nothing?");
        assertCommonSupertype("Nothing?", "Nothing?", "Nothing?");

        assertCommonSupertype("Any", "Char", "Number");
        assertCommonSupertype("Comparable<out Any?>", "Int", "Char");

        assertCommonSupertype("Base_T<*>", "Base_T<*>", "Derived_T<*>");
        assertCommonSupertype("Any", "Base_inT<*>", "Derived_T<*>");
        assertCommonSupertype("Derived_T<Int>", "DDerived_T<Int>", "Derived_T<Int>");
        assertCommonSupertype("Derived_T<Int>", "DDerived_T<Int>", "DDerived1_T<Int>");

        assertCommonSupertype("Comparable<*>", "Comparable<Int>", "Comparable<Boolean>");
        assertCommonSupertype("Base_T<out Comparable<*>>", "Base_T<Int>", "Base_T<String>");
        assertCommonSupertype("Base_T<in Int>", "Base_T<Int>", "Base_T<in Int>");
        assertCommonSupertype("Base_T<in Int>", "Derived_T<Int>", "Base_T<in Int>");
        assertCommonSupertype("Base_T<in Int>", "Derived_T<in Int>", "Base_T<Int>");
        assertCommonSupertype("Base_T<*>", "Base_T<Int>", "Base_T<*>");

        assertCommonSupertype("Base_T<out Parent>", "Base_T<A>", "Base_T<B>");
    }

    public void testCommonSupertypesForRecursive() throws Exception {
        assertCommonSupertype("Rec<out Rec<out Rec<out Rec<out Rec<out Any?>>>>>", "ARec", "BRec");
    }

    public void testIntersect() throws Exception {
        assertIntersection("Long", "Long?", "Number");
        assertIntersection("Long", "Number", "Long?");
        assertIntersection("Number", "Number?", "Number");

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
        assertSubtype("ArrayList<Int>", "InvList<in Int>");

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

    public void testStars() throws Exception {
        assertSubtype("SubStar<*>", "Star<*>");
        assertSubtype("SubStar<SubStar<*>>", "Star<*>");
        assertSubtype("SubStar<SubStar<*>>", "Star<SubStar<*>>");
        assertNotSubtype("SubStar<SubStar<*>>", "Star<Star<*>>");

        assertSubtype("Star<Star<*>>", "Star<*>");
        assertSubtype("Star<*>", "Star<out Star<*>>");

        assertNotSubtype("Star<*>", "Star<Star<*>>");

        assertSubtype("SubRec<*>", "Rec<*>");
        assertSubtype("SubRec<*>", "Rec<out Any?>");
        assertSubtype("Rec<*>", "Rec<out Any?>");
        assertNotSubtype("Rec<*>", "Rec<out Any>");
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
        assertType("Functions<Double>().f(Pair(1, 1))", "Double");
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
        assertSubtype("Base_outT<Int>", "Base_outT<Int>");
        assertSubtype("Base_outT<out Int>", "Base_outT<out Int>");
        assertSubtype("Base_outT<out Int>", "Base_outT<Int>");
        assertSubtype("Base_outT<Int>", "Base_outT<out Int>");
        assertSubtype("Base_outT<in Int>", "Base_outT<out Any?>");
        assertSubtype("Base_outT<out Any?>", "Base_outT<in String>");
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
        JetType result = TypeUtils.intersect(JetTypeChecker.DEFAULT, typesToIntersect);
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
        boolean result = JetTypeChecker.DEFAULT.isSubtypeOf(
                typeNode1,
                typeNode2);
        String modifier = expected ? "not " : "";
        assertTrue(typeNode1 + " is " + modifier + "a subtype of " + typeNode2, result == expected);
    }

    private void assertType(String expression, JetType expectedType) {
        Project project = getProject();
        JetExpression jetExpression = JetPsiFactory(project).createExpression(expression);
        JetType type = expressionTypingServices.getType(scopeWithImports, jetExpression, TypeUtils.NO_EXPECTED_TYPE, DataFlowInfo.EMPTY, JetTestUtils.DUMMY_TRACE);
        assertTrue(type + " != " + expectedType, type.equals(expectedType));
    }

    private void assertErrorType(String expression) {
        Project project = getProject();
        JetExpression jetExpression = JetPsiFactory(project).createExpression(expression);
        JetType type = expressionTypingServices.safeGetType(scopeWithImports, jetExpression, TypeUtils.NO_EXPECTED_TYPE, DataFlowInfo.EMPTY, JetTestUtils.DUMMY_TRACE);
        assertTrue("Error type expected but " + type + " returned", type.isError());
    }

    private void assertType(String contextType, final String expression, String expectedType) {
        final JetType thisType = makeType(contextType);
        JetScope scope = new AbstractScopeAdapter() {
            @NotNull
            @Override
            protected JetScope getWorkerScope() {
                return scopeWithImports;
            }

            @NotNull
            @Override
            public List<ReceiverParameterDescriptor> getImplicitReceiversHierarchy() {
                return Lists.<ReceiverParameterDescriptor>newArrayList(new ReceiverParameterDescriptorImpl(
                        getContainingDeclaration(),
                        thisType,
                        new ExpressionReceiver(JetPsiFactory(getProject()).createExpression(expression), thisType)
                ));
            }
        };
        assertType(scope, expression, expectedType);
    }

    private void assertType(String expression, String expectedTypeStr) {
        assertType(scopeWithImports, expression, expectedTypeStr);
    }

    private void assertType(JetScope scope, String expression, String expectedTypeStr) {
        Project project = getProject();
        JetExpression jetExpression = JetPsiFactory(project).createExpression(expression);
        JetType type = expressionTypingServices.getType(
                addImports(scope), jetExpression, TypeUtils.NO_EXPECTED_TYPE, DataFlowInfo.EMPTY, new BindingTraceContext());
        JetType expectedType = expectedTypeStr == null ? null : makeType(expectedTypeStr);
        assertEquals(expectedType, type);
    }

    private WritableScope getDeclarationsScope(String path) throws IOException {
        ModuleDescriptor moduleDescriptor = LazyResolveTestUtil.resolve(
                getProject(),
                Collections.singletonList(JetTestUtils.loadJetFile(getProject(), new File(path)))
        );

        FqName fqName = new FqName("testData");
        PackageViewDescriptor packageView = moduleDescriptor.getPackage(fqName);
        assertNotNull("Package " + fqName + " not found", packageView);
        return addImports(packageView.getMemberScope());
    }

    @SuppressWarnings("ConstantConditions")
    private WritableScopeImpl addImports(JetScope scope) {
        WritableScopeImpl writableScope = new WritableScopeImpl(
                scope, scope.getContainingDeclaration(), RedeclarationHandler.DO_NOTHING, "JetTypeCheckerTest.addImports"
        );
        ModuleDescriptor module = LazyResolveTestUtil.resolveProject(getProject());
        for (ImportPath defaultImport : module.getDefaultImports()) {
            FqName fqName = defaultImport.fqnPart();
            if (defaultImport.isAllUnder()) {
                writableScope.importScope(module.getPackage(fqName).getMemberScope());
            }
            else {
                writableScope.addClassifierAlias(defaultImport.getImportedName(),
                                                 module.getPackage(fqName.parent()).getMemberScope().getClassifier(fqName.shortName()));
            }
        }
        writableScope.importScope(module.getPackage(FqName.ROOT).getMemberScope());
        writableScope.changeLockLevel(WritableScope.LockLevel.BOTH);
        return writableScope;
    }

    private JetType makeType(String typeStr) {
        return makeType(scopeWithImports, typeStr);
    }

    private JetType makeType(JetScope scope, String typeStr) {
        return typeResolver.resolveType(scope, JetPsiFactory(getProject()).createType(typeStr), JetTestUtils.DUMMY_TRACE, true);
    }
}
