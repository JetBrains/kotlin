/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.analyzer.AnalysisResult;
import org.jetbrains.kotlin.builtins.KotlinBuiltIns;
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment;
import org.jetbrains.kotlin.descriptors.PackageFragmentProvider;
import org.jetbrains.kotlin.descriptors.annotations.Annotations;
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl;
import org.jetbrains.kotlin.descriptors.impl.ReceiverParameterDescriptorImpl;
import org.jetbrains.kotlin.psi.KtExpression;
import org.jetbrains.kotlin.psi.KtFile;
import org.jetbrains.kotlin.psi.KtPsiFactoryKt;
import org.jetbrains.kotlin.resolve.BindingContext;
import org.jetbrains.kotlin.resolve.BindingTraceContext;
import org.jetbrains.kotlin.resolve.TypeResolver;
import org.jetbrains.kotlin.resolve.calls.components.InferenceSession;
import org.jetbrains.kotlin.resolve.calls.smartcasts.DataFlowInfoFactory;
import org.jetbrains.kotlin.resolve.lazy.JvmResolveUtil;
import org.jetbrains.kotlin.resolve.scopes.LexicalScope;
import org.jetbrains.kotlin.resolve.scopes.LexicalScopeImpl;
import org.jetbrains.kotlin.resolve.scopes.LexicalScopeKind;
import org.jetbrains.kotlin.resolve.scopes.receivers.TransientReceiver;
import org.jetbrains.kotlin.test.ConfigurationKind;
import org.jetbrains.kotlin.test.DummyTraces;
import org.jetbrains.kotlin.test.KotlinTestUtils;
import org.jetbrains.kotlin.test.KotlinTestWithEnvironment;
import org.jetbrains.kotlin.tests.di.ContainerForTests;
import org.jetbrains.kotlin.tests.di.InjectionKt;
import org.jetbrains.kotlin.types.checker.KotlinTypeChecker;
import org.jetbrains.kotlin.types.expressions.ExpressionTypingServices;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class KotlinTypeCheckerTest extends KotlinTestWithEnvironment {
    private KotlinBuiltIns builtIns;
    private LexicalScope scopeWithImports;
    private TypeResolver typeResolver;
    private ExpressionTypingServices expressionTypingServices;

    @Override
    protected KotlinCoreEnvironment createEnvironment() {
        return createEnvironmentWithMockJdk(ConfigurationKind.ALL);
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();

        ModuleDescriptorImpl module = KotlinTestUtils.createEmptyModule();
        builtIns = module.getBuiltIns();
        ContainerForTests container = InjectionKt.createContainerForTests(getProject(), module);
        module.setDependencies(Collections.singletonList(module));
        module.initialize(PackageFragmentProvider.Empty.INSTANCE);
        typeResolver = container.getTypeResolver();
        expressionTypingServices = container.getExpressionTypingServices();

        scopeWithImports = getDeclarationsScope();
    }

    @Override
    protected void tearDown() throws Exception {
        scopeWithImports = null;

        expressionTypingServices = null;
        typeResolver = null;

        builtIns = null;

        super.tearDown();
    }

    public void testConstants() {
        assertType("1", builtIns.getIntType());
        assertType("0x1", builtIns.getIntType());
        assertType("0X1", builtIns.getIntType());
        assertType("0b1", builtIns.getIntType());
        assertType("0B1", builtIns.getIntType());

        assertType("1.toLong()", builtIns.getLongType());

        assertType("1.0", builtIns.getDoubleType());
        assertType("1.0.toDouble()", builtIns.getDoubleType());

        assertType("1.0.toFloat()", builtIns.getFloatType());

        assertType("true", builtIns.getBooleanType());
        assertType("false", builtIns.getBooleanType());

        assertType("'d'", builtIns.getCharType());

        assertType("\"d\"", builtIns.getStringType());
        assertType("\"\"\"d\"\"\"", builtIns.getStringType());

        assertType("Unit", builtIns.getUnitType());

        assertType("null", builtIns.getNullableNothingType());
    }

    public void testJumps() {
        assertType("throw Exception()", builtIns.getNothingType());
        assertType("continue", builtIns.getNothingType());
        assertType("break", builtIns.getNothingType());
    }

    public void testIf() {
        assertType("if (true) 1", "Unit");
        assertType("if (true) 1 else 1", "Int");
        assertType("if (true) 1 else throw Exception()", "Int");
        assertType("if (true) throw Exception() else 1", "Int");
        assertType("if (true) throw Exception() else throw Exception()", "Nothing");

        assertType("if (true) 1 else null", "Int?");
        assertType("if (true) null else null", "Nothing?");

        assertType("if (true) AI() else BI()", "I");

        assertType("if (true) else '1'", "Unit");
        assertType("if (true) else { var a = 0; a = 1 }", "Unit");
    }

    public void testWhen() {
        assertType("when (1) { is 1 -> 2; }", "Int");
        assertType("when (1) { is 1 -> AI(); is 1 -> BI()}", "I");
        assertType("when (1) { is 1 -> AI(); is 1 -> BI(); is 1 -> null}", "I?");
        assertType("when (1) { is 1 -> AI(); is 1 -> BI(); else -> null}", "I?");
        assertType("when (1) { is 1 -> AI(); is 1 -> BI(); is 1 -> when(2) {is 1 -> null}}", "I?");
    }

    public void testTry() {
        assertType("try {1} finally{2}", "Int");
        assertType("try { AI() } catch (e : Exception) { BI() } finally{ CI() }", "I");
        assertType("try {1} catch (e : Exception) {2} finally{'a'}", "Int");
        assertType("try {CI()} catch (e : Exception) {AI(} finally{BI()}", "I");
        assertType("try {AI()} catch (e : Exception) {BI()}", "I");
        assertType("try {AI()} catch (e : Exception) {BI()} catch (e : Exception) {null}", "I?");
        assertType("try {} catch (e : Exception) {}", "Unit");
    }

    public void testCommonSupertypes() {
        assertCommonSupertype("Int", "Int", "Int");

        assertCommonSupertype("Int", "Int", "Nothing");
        assertCommonSupertype("Int", "Nothing", "Int");
        assertCommonSupertype("Nothing", "Nothing", "Nothing");

        assertCommonSupertype("Int?", "Int", "Nothing?");
        assertCommonSupertype("Nothing?", "Nothing?", "Nothing?");

        assertCommonSupertype("I", "AI", "BI");

        assertCommonSupertype("Base_T<*>", "Base_T<*>", "Derived_T<*>");
        assertCommonSupertype("Any", "Base_inT<*>", "Derived_T<*>");
        assertCommonSupertype("Derived_T<Int>", "DDerived_T<Int>", "Derived_T<Int>");
        assertCommonSupertype("Derived_T<Int>", "DDerived_T<Int>", "DDerived1_T<Int>");

        assertCommonSupertype("Comparable<*>", "Comparable<Int>", "Comparable<Boolean>");
        assertCommonSupertype("Base_T<out I>", "Base_T<AI>", "Base_T<BI>");
        assertCommonSupertype("Base_T<in Int>", "Base_T<Int>", "Base_T<in Int>");
        assertCommonSupertype("Base_T<in Int>", "Derived_T<Int>", "Base_T<in Int>");
        assertCommonSupertype("Base_T<in Int>", "Derived_T<in Int>", "Base_T<Int>");
        assertCommonSupertype("Base_T<out Any?>", "Base_T<Int>", "Base_T<*>");

        assertCommonSupertype("Base_T<out Parent>", "Base_T<A>", "Base_T<B>");
    }

    public void testCommonSupertypesForRecursive() {
        assertCommonSupertype("Rec<out Rec<out Rec<out Rec<out Rec<*>>>>>", "ARec", "BRec");
    }

    public void testIntersect() {
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

    public void testBasicSubtyping() {
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

    public void testProjections() {
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

    public void testNullable() {
        assertSubtype("Any?", "Any?");
        assertSubtype("Any", "Any?");
        assertNotSubtype("Any?", "Any");
        assertSubtype("Int", "Any?");
        assertSubtype("Int?", "Any?");
        assertNotSubtype("Int?", "Any");
    }

    public void testNothing() {
        assertSubtype("Nothing", "Any");
        assertSubtype("Nothing?", "Any?");
        assertNotSubtype("Nothing?", "Any");

        assertSubtype("Nothing", "Int");
        assertSubtype("Nothing?", "Int?");
        assertNotSubtype("Nothing?", "Int");

        assertSubtype("Nothing?", "Base_T<*>?");
        assertSubtype("Nothing?", "Derived_T<*>?");
    }

    public void testStars() {
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

    public void testThis() {
        assertType("Derived_T<Int>", "this", "Derived_T<Int>");
//        assertType("Derived_T<Int>", "super<Base_T>", "Base_T<Int>");
    }

    public void testLoops() {
        assertType("{ while (1) {1} }", "() -> Unit");
        assertType("{ do {1} while(1) }", "() -> Unit");
        assertType("{ for (i in 1) {1} }", "() -> Unit");
    }

    public void testFunctionLiterals() {
        assertType("{ -> }", "() -> Unit");
        assertType("fun(): Int = 1", "() -> Int");
        assertType("{ 1}", "() -> Int");

        assertType("{ a : Int -> 1}", "(a : Int) -> Int");
        assertType("{ a : Int, b : String -> 1}", "(a : Int, b : String) -> Int");

        assertType("{ a : Int -> 1}", "(Int) -> Int");
        assertType("{ a : Int, b : String -> 1}", "(Int, String) -> Int");

        assertType("fun Any.(): Int = 1", "Any.() -> Int");

        assertType("fun Any.(a : Int) = 1", "Any.(a : Int) -> Int");
        assertType("fun Any.(a : Int, b : String) = 1", "Any.(a : Int, b : String) -> Int");

        assertType("fun Any.(a : Int) = 1", "Any.(Int) -> Int");
        assertType("fun Any.(a : Int, b : String) = 1", "Any.(Int, String) -> Int");

        assertType("fun Any.(a : Int, b : String) = b", "Any.(Int, String) -> String");
    }

    public void testBlocks() {
        assertType("if (1) {val a = 1; a} else {null}", "Int?");
        assertType("if (1) { -> val a = 1; a} else { -> null}", "Function0<Int?>");
        assertType("if (1) (fun (): Boolean { val a = 1; a; var b : Boolean; return b }) else null", "Function0<Boolean>?");
        assertType("if (1) (fun (): Int { val a = 1; a; var b = a; return b }) else null", "Function0<Int>?");
    }

    public void testNew() {
        assertType("Base_T<Int>()", "Base_T<Int>");
    }

    public void testPropertiesInClasses() {
        assertType("Properties().p", "Int");
        assertType("Props<Int>().p", "Int");
        assertType("Props<out Int>().p", "Int");
        assertType("Props<Properties>().p.p", "Int");

        assertType("(return as Props<in Int>).p", "Any?");
    }

    public void testOverloads() {
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

    public void testPlus() {
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

        assertType("'1'.plus(1)", "Char");
        assertType("'1'.minus(1)", "Char");
        assertType("'1'.minus('1')", "Int");

        assertType("(1.toShort()).plus(1.toDouble())", "Double");
        assertType("(1.toShort()).plus(1.toFloat())", "Float");
        assertType("(1.toShort()).plus(1.toLong())", "Long");
        assertType("(1.toShort()).plus(1)", "Int");
        assertType("(1.toShort()).plus(1.toShort())", "Int");

        assertType("(1.toByte()).plus(1.toDouble())", "Double");
        assertType("(1.toByte()).plus(1.toFloat())", "Float");
        assertType("(1.toByte()).plus(1.toLong())", "Long");
        assertType("(1.toByte()).plus(1)", "Int");
        assertType("(1.toByte()).plus(1.toShort())", "Int");
        assertType("(1.toByte()).plus(1.toByte())", "Int");

        assertType("\"1\".plus(1.toDouble())", "String");
        assertType("\"1\".plus(1.toFloat())", "String");
        assertType("\"1\".plus(1.toLong())", "String");
        assertType("\"1\".plus(1)", "String");
        assertType("\"1\".plus('1')", "String");
    }

    public void testBinaryOperations() {
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

    public void testSupertypes() {
        assertSupertypes("DDerived1_T<Int>", "Derived_T<Int>", "Base_T<Int>", "Any");
        assertSupertypes("DDerived2_T<Int>", "Derived_T<Int>", "Base_T<Int>", "Any");
        assertSupertypes("Derived1_inT<Int>", "Derived_T<Int>", "Base_T<Int>", "Any", "Base_inT<Int>");
    }

    public void testEffectiveProjectionKinds() {
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

    private void assertSupertypes(String typeStr, String... supertypeStrings) {
        Set<KotlinType> allSupertypes = TypeUtils.getAllSupertypes(makeType(scopeWithImports, typeStr));
        Set<KotlinType> expected = new HashSet<>();
        for (String supertypeStr : supertypeStrings) {
            KotlinType supertype = makeType(scopeWithImports, supertypeStr);
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
        Set<KotlinType> typesToIntersect = new LinkedHashSet<>();
        for (String type : types) {
            typesToIntersect.add(makeType(type));
        }
        KotlinType result = TypeIntersector.intersectTypes(typesToIntersect);
//        assertNotNull("Intersection is null for " + typesToIntersect, result);
        assertEquals(makeType(expected), result);
    }

    private void assertCommonSupertype(String expected, String... types) {
        Collection<KotlinType> subtypes = new ArrayList<>();
        for (String type : types) {
            subtypes.add(makeType(type));
        }
        KotlinType result = CommonSupertypes.commonSupertype(subtypes);
        assertEquals(result + " != " + expected, makeType(expected), result);
    }

    private void assertSubtypingRelation(String subtype, String supertype, boolean expected) {
        KotlinType typeNode1 = makeType(subtype);
        KotlinType typeNode2 = makeType(supertype);
        boolean result = KotlinTypeChecker.DEFAULT.isSubtypeOf(
                typeNode1,
                typeNode2);
        String modifier = expected ? "not " : "";
        assertEquals(typeNode1 + " is " + modifier + "a subtype of " + typeNode2, expected, result);
    }

    private void assertType(String expression, KotlinType expectedType) {
        Project project = getProject();
        KtExpression ktExpression = KtPsiFactoryKt.KtPsiFactory(project).createExpression(expression);
        KotlinType type = expressionTypingServices.getType(
                scopeWithImports, ktExpression, TypeUtils.NO_EXPECTED_TYPE,
                DataFlowInfoFactory.EMPTY, InferenceSession.Companion.getDefault(),
                DummyTraces.DUMMY_TRACE
        );
        assertNotNull(type);
        assertEquals(type + " != " + expectedType, expectedType, type);
    }

    @SuppressWarnings("SameParameterValue")
    private void assertType(String contextType, String expression, String expectedType) {
        KotlinType thisType = makeType(contextType);
        ReceiverParameterDescriptorImpl receiverParameterDescriptor = new ReceiverParameterDescriptorImpl(
                scopeWithImports.getOwnerDescriptor(),
                new TransientReceiver(thisType),
                Annotations.Companion.getEMPTY()
        );

        LexicalScope scope = new LexicalScopeImpl(scopeWithImports, scopeWithImports.getOwnerDescriptor(), false,
                                                  Collections.singletonList(receiverParameterDescriptor), LexicalScopeKind.SYNTHETIC);
        assertType(scope, expression, expectedType);
    }

    private void assertType(String expression, String expectedTypeStr) {
        assertType(scopeWithImports, expression, expectedTypeStr);
    }

    private void assertType(LexicalScope scope, String expression, String expectedTypeStr) {
        Project project = getProject();
        KtExpression ktExpression = KtPsiFactoryKt.KtPsiFactory(project).createExpression(expression);
        KotlinType type = expressionTypingServices.getType(
                scope, ktExpression, TypeUtils.NO_EXPECTED_TYPE,
                DataFlowInfoFactory.EMPTY, InferenceSession.Companion.getDefault(),
                new BindingTraceContext()
        );
        KotlinType expectedType = expectedTypeStr == null ? null : makeType(expectedTypeStr);
        assertEquals(expectedType, type);
    }

    @NotNull
    private LexicalScope getDeclarationsScope() throws IOException {
        KtFile ktFile = KotlinTestUtils.loadJetFile(getProject(), new File("compiler/testData/type-checker-test.kt"));
        AnalysisResult result = JvmResolveUtil.analyze(ktFile, getEnvironment());
        //noinspection ConstantConditions
        return result.getBindingContext().get(BindingContext.LEXICAL_SCOPE, ktFile);
    }

    private KotlinType makeType(String typeStr) {
        return makeType(scopeWithImports, typeStr);
    }

    private KotlinType makeType(LexicalScope scope, String typeStr) {
        return typeResolver.resolveType(scope, KtPsiFactoryKt.KtPsiFactory(getProject()).createType(typeStr), DummyTraces.DUMMY_TRACE, true);
    }
}
