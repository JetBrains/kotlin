// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB
// RENDER_DIAGNOSTICS_FULL_TEXT
// FILE: J.java
public class J {
    public static <T> T makeFlexible(T t) {
        return t;
    }
}
// FILE: test.kt
typealias A<T> = Array<T>

fun foo(p: Int) {
    val a = arrayOf(1, 2, 3)
    val b = arrayOf(3, 2, 1)
    val c : A<Int> = arrayOf(3, 2, 1)

    if (a <!ARRAY_EQUALITY_OPERATOR_CAN_BE_REPLACED_WITH_CONTENT_EQUALS!>==<!> b) { }
    if (a <!ARRAY_EQUALITY_OPERATOR_CAN_BE_REPLACED_WITH_CONTENT_EQUALS!>==<!> c) { }
}

fun testsFromIdea() {
    val a = arrayOf("a")
    val b = a
    val c: Any? = null
    a <!ARRAY_EQUALITY_OPERATOR_CAN_BE_REPLACED_WITH_CONTENT_EQUALS!>==<!> b
    a == c
    a <!ARRAY_EQUALITY_OPERATOR_CAN_BE_REPLACED_WITH_CONTENT_EQUALS!>!=<!> b
}

// Smart casts
fun <T> eq1(a: T, b: T): Boolean =
    if (a is IntArray && b is IntArray) a <!ARRAY_EQUALITY_OPERATOR_CAN_BE_REPLACED_WITH_CONTENT_EQUALS!>==<!> b else false

inline fun <reified T> eq2(a: T, b: T): Boolean =
    if (a is IntArray && b is IntArray) a <!ARRAY_EQUALITY_OPERATOR_CAN_BE_REPLACED_WITH_CONTENT_EQUALS!>==<!> b else false

fun <T : Cloneable> eq3(a: T, b: T): Boolean =
    if (a is IntArray && b is IntArray) a <!ARRAY_EQUALITY_OPERATOR_CAN_BE_REPLACED_WITH_CONTENT_EQUALS!>==<!> b else false

fun eq4(a: Any?, b: Any?): Boolean =
    if (a is IntArray && b is IntArray) a <!ARRAY_EQUALITY_OPERATOR_CAN_BE_REPLACED_WITH_CONTENT_EQUALS!>==<!> b else false

fun flexible(a: Array<String>, b: Array<String>) {
    a <!ARRAY_EQUALITY_OPERATOR_CAN_BE_REPLACED_WITH_CONTENT_EQUALS!>==<!> J.makeFlexible(b)
}

class Box<T>(val item: T)

fun captured(box: Box<out IntArray>, box2: Box<out IntArray>) {
    box.item <!ARRAY_EQUALITY_OPERATOR_CAN_BE_REPLACED_WITH_CONTENT_EQUALS!>==<!> box2.item
}

/* GENERATED_FIR_TAGS: equalityExpression, functionDeclaration, ifExpression, integerLiteral, localProperty,
nullableType, propertyDeclaration, stringLiteral, typeAliasDeclaration, typeAliasDeclarationWithTypeParameter,
typeParameter */
