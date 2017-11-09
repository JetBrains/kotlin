// FILE: MyEnum.java
public enum MyEnum {
    SINGLE;

    public static MyEnum getInstance() {
        return SINGLE;
    }
}


// FILE: test.kt

sealed class X {
    class A : X()
    class B : X()
}

fun foo(x: X) = when (x) {
    is X.A -> {}
    is X.B -> {}
    <!REDUNDANT_ELSE_IN_WHEN!>else<!> -> {}
}

fun bar(x: X?): String = when (x) {
    is X.A -> "A"
    is X.B -> "B"
    null -> "null"
    <!REDUNDANT_ELSE_IN_WHEN!>else<!> -> "Unreachable"
}

fun justUse(x: X) {
    when (x) {
        is X.A -> {}
        is X.B -> {}
        // Redundant even in statement position
        <!REDUNDANT_ELSE_IN_WHEN!>else<!> -> {}
    }
}

enum class E {
    A, B
}

fun foo(e: E): String = when (e) {
    E.A -> "A"
    E.B -> "B"
    <!REDUNDANT_ELSE_IN_WHEN!>else<!> -> ""
}

fun bar(e: E?): String = when (e) {
    E.A -> "A"
    E.B -> "B"
    else -> "" // no warning
}

fun foo(b: Boolean) = when (b) {
    true -> 1
    false -> 0
    <!REDUNDANT_ELSE_IN_WHEN!>else<!> -> -1
}

fun useJava(): String {
    val me = MyEnum.getInstance()
    return when (me) {
        MyEnum.SINGLE -> "OK"
        else -> "FAIL" // no warning
    }
}