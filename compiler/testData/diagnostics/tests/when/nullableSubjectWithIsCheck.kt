// ISSUE: KT-49710
// WITH_STDLIB

// FILE: JClass.java

import org.jetbrains.annotations.Nullable;

public class JClass {
    @Nullable
    public static int intProp = 0;
    @Nullable
    public static Integer integerProp = 0;
    @Nullable
    public static String stringProp = "";
    @Nullable
    public static Object objectProp = new Object();
}


// FILE: test.kt

fun Int?.isNull() = <!NO_ELSE_IN_WHEN!>when<!> (this) {
    null -> true
    <!USELESS_IS_CHECK!>is Int<!> -> false
}

fun <T> List<T>.isNull() = <!NO_ELSE_IN_WHEN!>when<!> (this) {
    <!USELESS_IS_CHECK!>is List<T><!> -> false
}

fun <T> List<T>.isNull1() = <!NO_ELSE_IN_WHEN!>when<!> (this) {
    <!USELESS_IS_CHECK!>is List<*><!> -> false
}

fun <T: Int?> isNull(arg: T) = <!NO_ELSE_IN_WHEN!>when<!>(arg) {
    is Int -> false
    null -> true
}

fun testNullableInt(arg: Int?) = <!NO_ELSE_IN_WHEN!>when<!> (arg) {
    null -> true
    <!USELESS_IS_CHECK!>is Int<!> -> false
}

fun testNullable(arg: Any?) = <!NO_ELSE_IN_WHEN!>when<!> (arg) {
    null -> true
    <!USELESS_IS_CHECK!>is Any<!> -> false
}

fun testNullable(arg: Nothing?) = <!NO_ELSE_IN_WHEN!>when<!> (<!DEBUG_INFO_CONSTANT!>arg<!>) {
    null -> true
}

fun testNullable(arg: Unit?) = <!NO_ELSE_IN_WHEN!>when<!> (arg) {
    null -> true
    <!USELESS_IS_CHECK!>is Unit<!> -> false
}

fun testNullable(arg: IntArray?) = <!NO_ELSE_IN_WHEN!>when<!> (arg) {
    null -> true
    <!USELESS_IS_CHECK!>is IntArray<!> -> false
}

fun testNullable(arg: UInt?) = <!NO_ELSE_IN_WHEN!>when<!> (arg) {
    null -> true
    <!USELESS_IS_CHECK!>is UInt<!> -> false
}

typealias NullableInt = Int?

fun testTypeAliasToNullable(arg: NullableInt) = <!NO_ELSE_IN_WHEN!>when<!> (arg) {
    null -> true
    <!USELESS_IS_CHECK!>is NullableInt<!> -> false
}

fun NullableInt.isNotNull() = <!NO_ELSE_IN_WHEN!>when<!> (this) {
    null -> false
    <!USELESS_IS_CHECK!>is NullableInt<!> -> true
}

class KClassWithGetter {
    var prop: Int? = 0
        get() = <!NO_ELSE_IN_WHEN!>when<!> (prop) {
            null -> null
            is Int -> prop
        }
}

fun testSubclass(arg: String?) = <!NO_ELSE_IN_WHEN!>when<!> (arg) {
    null -> true
    <!USELESS_IS_CHECK!>is CharSequence<!> -> false
}

fun testSmartCast(x: Any?) {
    if (x !is String?) return

    return <!NO_ELSE_IN_WHEN!>when<!> (x) {
        is CharSequence -> Unit
        null -> Unit
    }
}

val testLambda = {arg: String? -> <!NO_ELSE_IN_WHEN!>when<!> (arg) {
    null -> true
    <!USELESS_IS_CHECK!>is CharSequence<!> -> false
}}

fun testLambda(arg: (() -> Unit)?) = <!NO_ELSE_IN_WHEN!>when<!> (arg) {
    null -> true
    <!USELESS_IS_CHECK!>is ()->Unit<!> -> false
}

sealed class SealedClass {
    class A(val a: String) : SealedClass()
    class B(val b: String) : SealedClass()
}

fun testSealedClass(arg: SealedClass?) {
    <!NO_ELSE_IN_WHEN!>when<!> (arg) {
        is SealedClass.A? -> println(<!DEBUG_INFO_SMARTCAST!>arg<!>?.a)
        is SealedClass.B -> println(<!DEBUG_INFO_SMARTCAST!>arg<!>.b)
    }
}

fun testJavaNullableProps() {
    var <!ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE!>a<!> = <!NO_ELSE_IN_WHEN!>when<!> (JClass.intProp) {
        <!SENSELESS_NULL_IN_WHEN!>null<!> -> true
        <!USELESS_IS_CHECK!>is Int<!> -> false
    }

    a = <!NO_ELSE_IN_WHEN!>when<!> (JClass.intProp) {
        <!USELESS_IS_CHECK!>is Int<!> -> false
    }

    a = <!NO_ELSE_IN_WHEN!>when<!> (JClass.integerProp) {
        null -> true
        is Int -> false
    }

    a = <!NO_ELSE_IN_WHEN!>when<!> (JClass.stringProp) {
        null -> true
        is String -> false
    }

    a = <!NO_ELSE_IN_WHEN!>when<!> (JClass.objectProp) {
        null -> true
        is Any -> false
    }
}

fun testWhenStatementWithComma(arg: Int?): Int {
    return <!NO_ELSE_IN_WHEN!>when<!>(arg) {
        is Int, null -> 2
    }
}

sealed class Value

fun test(value: Value?) {
    val x = <!NO_ELSE_IN_WHEN!>when<!> (value) {
        is Value -> 1
        null -> 2
    }
}

class Inv<T>(val x: T)

fun testCaptured1(inv1: Inv<*>, inv2: Inv<out Number?>) {
    val arg1 = <!NO_ELSE_IN_WHEN!>when<!> (inv1.x) {
        is Any -> true
        null -> false
    }

    val arg2 = <!NO_ELSE_IN_WHEN!>when<!> (inv2.x) {
        is Number -> true
        null -> false
    }
}

fun typeErased(list: MutableList<String>?) = <!NO_ELSE_IN_WHEN!>when<!> (list) {
    is MutableList -> 1
    null -> 2
}

fun <T> testDNN(arg: T& Any) = <!NO_ELSE_IN_WHEN!>when<!> (arg) {
    <!USELESS_IS_CHECK!>is T<!> -> false
}

fun isNullable(a: Int?) = <!NO_ELSE_IN_WHEN!>when<!> (a) {
    <!USELESS_IS_CHECK!>is Number?<!> -> false
}
