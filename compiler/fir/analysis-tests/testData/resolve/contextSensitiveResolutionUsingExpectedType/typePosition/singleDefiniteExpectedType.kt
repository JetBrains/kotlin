// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-75061
// LANGUAGE: +ContextSensitiveResolutionUsingExpectedType

// FILE: JClass.java
public class JClass {
    public static SealedJClass staticProp = null;
}

// FILE: SealedJClass.java
public sealed class SealedJClass permits SealedJClass.SCOption1, SealedJClass.SCOption2 {
    public static final class SCOption1 extends SealedJClass {
        public int prop1 = 1;
    }
    public static final class SCOption2 extends SealedJClass {
        public int prop2 = 2;
    }
}

// FILE: test.kt
sealed interface SealedInterface {
    class NestedInheritor(prop: String): SealedInterface {
        class NestedNestedInheritor(propNested: String): SealedInterface
    }
}

sealed class SealedClass {
    class SealedInheritor1(val prop1: String = "1"): SealedClass()
    class SealedInheritor2(val prop2: Int = 2): SealedClass()
}

sealed class SealedGeneric<T> {
    class SGOption1<T>(val prop1: Int): SealedGeneric<T>()
    class SGOption2<T>(val prop2: Int): SealedGeneric<T>()
}

fun <T: SealedClass>testTypeParam(instance: T): String = when (instance) {
    is SealedInheritor1 -> instance.prop1
    !is SealedInheritor1 if instance is SealedInheritor2 -> instance.prop2.toString()
    else -> "100"
}

interface OtherI

fun <T>testTypeParams(instance: T): String where T : SealedClass, T : OtherI = when (instance) {
    is <!UNRESOLVED_REFERENCE!>SealedInheritor1<!> -> instance.<!UNRESOLVED_REFERENCE!>prop1<!>
    !is <!UNRESOLVED_REFERENCE!>SealedInheritor1<!> if instance is <!UNRESOLVED_REFERENCE!>SealedInheritor2<!> -> instance.<!UNRESOLVED_REFERENCE!>prop2<!>.toString()
    else -> "100"
}

fun testNullable(instance: SealedClass?): String = <!WHEN_ON_SEALED!>when(instance) {
    is SealedInheritor1 -> instance.prop1
    is SealedInheritor2 -> instance.prop2.toString()
    null -> "100"
}<!>

interface ITestContravariant<in T> {
    fun test(arg: T): String
}

class TestContravariant: ITestContravariant<SealedClass> {
    override fun test(arg: SealedClass): String = <!WHEN_ON_SEALED!>when (arg) {
        is SealedInheritor1 -> "100"
        is SealedInheritor2 -> "201"
    }<!>
}

fun <T: SealedClass?>testDefinitelyNotNullIntersection(instance: T & Any): String = when(instance) {
    is SealedInheritor1 -> instance.prop1
    is SealedInheritor2 -> instance.prop2.toString()
    <!REDUNDANT_ELSE_IN_WHEN!>else<!> -> "100"
}

fun testFakeIntersection2(instance: Any): String {
    if (instance is SealedInterface && instance is SealedInterface.NestedInheritor) {
        return when(instance) {
            <!USELESS_IS_CHECK!>is NestedInheritor<!> -> instance.<!UNRESOLVED_REFERENCE!>prop<!>
            is <!UNRESOLVED_REFERENCE!>NestedNestedInheritor<!> -> instance.<!UNRESOLVED_REFERENCE!>propNested<!>
            <!REDUNDANT_ELSE_IN_WHEN!>else<!> -> "100"
        }
    }
    return "100"
}

fun testIntersection(instance: Any): String {
    if (instance is SealedInterface && <!USELESS_IS_CHECK!>instance is Any<!>) {
        return <!WHEN_ON_SEALED!>when(instance) {
            is NestedInheritor -> instance.<!UNRESOLVED_REFERENCE!>prop<!>
            is <!UNRESOLVED_REFERENCE!>NestedNestedInheritor<!> -> instance.<!UNRESOLVED_REFERENCE!>propNested<!>
            else -> "100"
        }<!>
    }
    return "100"
}

fun <T>testRegularIntersection(instance: T): String where T : SealedInterface, T : SealedClass = when(instance) {
    is <!UNRESOLVED_REFERENCE!>NestedInheritor<!> -> instance.<!UNRESOLVED_REFERENCE!>prop<!>
    is <!UNRESOLVED_REFERENCE!>SealedInheritor1<!> -> instance.<!UNRESOLVED_REFERENCE!>prop1<!>
    else -> "100"
}

fun testFlexible(): Int {
    return <!WHEN_ON_SEALED!>when(val i = JClass.staticProp) {
        is SCOption1 -> i.prop1
        is SCOption2 -> i.prop2
        else -> 101
    }<!>
}

class ITestContravariant2<in T>(val a: @UnsafeVariance T) {
}

fun test(arg: ITestContravariant2<<!REDUNDANT_PROJECTION!>in<!> SealedClass>): String = when (arg.a) {
    is <!UNRESOLVED_REFERENCE!>SealedInheritor1<!> -> "100"
    is <!UNRESOLVED_REFERENCE!>SealedInheritor2<!> -> "201"
    else -> "301"
}

/* GENERATED_FIR_TAGS: andExpression, classDeclaration, dnnType, equalityExpression, flexibleType, functionDeclaration,
guardCondition, ifExpression, in, inProjection, integerLiteral, interfaceDeclaration, intersectionType, isExpression,
javaProperty, javaType, localProperty, nestedClass, nullableType, override, primaryConstructor, propertyDeclaration,
sealed, smartcast, stringLiteral, typeConstraint, typeParameter, whenExpression, whenWithSubject */
